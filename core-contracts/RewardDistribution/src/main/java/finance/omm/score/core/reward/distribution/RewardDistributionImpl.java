package finance.omm.score.core.reward.distribution;


import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.UserAssetInput;
import finance.omm.libs.structs.UserDetails;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class RewardDistributionImpl extends AbstractRewardDistribution {

    public static final String TAG = "Reward Distribution Controller";
    public static final String USERS_UNCLAIMED_REWARDS = "usersUnclaimedRewards";
    public static final String DAY = "day";
    public static final String TOKEN_DIST_TRACKER = "tokenDistTracker";
    public static final String IS_INITIALIZED = "isInitialized";
    public static final String IS_REWARD_CLAIM_ENABLED = "isRewardClaimEnabled";

    public final VarDB<BigInteger> _day = Context.newVarDB(DAY, BigInteger.class);
    public final VarDB<Boolean> _isInitialized = Context.newVarDB(IS_INITIALIZED, Boolean.class);
    public final VarDB<Boolean> _isRewardClaimEnabled = Context.newVarDB(IS_REWARD_CLAIM_ENABLED, Boolean.class);
    public final BranchDB<Address, DictDB<Address, BigInteger>> _usersUnclaimedRewards = Context.newBranchDB(
            USERS_UNCLAIMED_REWARDS, BigInteger.class);
    public final DictDB<String, BigInteger> _tokenDistTracker = Context.newDictDB(TOKEN_DIST_TRACKER, BigInteger.class);

    public RewardDistributionImpl() {
        super(null);
    }

    @EventLog(indexed = 2)
    public void Distribution(String _recipient, Address _user, BigInteger _value) {}

    @EventLog()
    public void OmmTokenMinted(BigInteger _day, BigInteger _value) {}

    @EventLog()
    public void RewardsAccrued(Address _user, Address _asset, BigInteger _rewards) {}

    @EventLog()
    public void RewardsClaimed(Address _user, BigInteger _rewards, String _msg) {}

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External(readonly = true)
    public String[] getRecipients() {
        return _rewardConfig.getRecipients();
    }

    @External()
    public void handleAction(UserDetails _userAssetDetails) {
        Address _asset = Context.getCaller();
        _handleAction(_asset, _userAssetDetails);
    }

    @External()
    public void disableRewardClaim() {
        checkGovernance();
        _isRewardClaimEnabled.set(Boolean.FALSE);
    }

    @External()
    public void enableRewardClaim() {
        checkGovernance();
        _isRewardClaimEnabled.set(Boolean.FALSE);
    }

    @External(readonly = true)
    public boolean isRewardClaimEnabled() {
        return _isRewardClaimEnabled.get();
    }

    @External
    public void handleLPAction(Address _asset, UserDetails _userDetails) {
        checkStakeLp();
        _handleAction(_asset, _userDetails);
    }

    private void _handleAction(Address _asset, UserDetails _userDetails) {
        BigInteger _decimals = _userDetails._decimals;
        Address _user = _userDetails._user;
        BigInteger _userBalance = MathUtils.convertToExa(_userDetails._userBalance, _decimals);
        BigInteger _totalSupply = MathUtils.convertToExa(_userDetails._totalBalance, _decimals);
        Context.require(_rewardConfig.is_valid_asset(_asset), TAG + " Asset not authorized ");
        BigInteger accruedRewards = _updateUserReserveInternal(_user, _asset, _userBalance, _totalSupply);
        if (!BigInteger.ZERO.equals(accruedRewards)) {
            _usersUnclaimedRewards.at(_user).set(_asset, accruedRewards);
            RewardsAccrued(_user, _asset, accruedRewards);

        }
    }

    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day) {
        if (_day == null || BigInteger.ZERO.equals(_day)) {
            _day = getDay();
        }
        BigInteger _distribution = tokenDistributionPerDay(_day);
        BigInteger _totalRewards = BigInteger.ZERO;
        List<Address> _assets = _rewardConfig.getAssets();
        Map<String, Object> response = new HashMap<>();
        for (Address _asset : _assets) {
            String _assetName = _rewardConfig.getAssetName(_asset);
            String _entity = _rewardConfig.getEntity(_asset);
            if (_entity == null) {
                throw RewardDistributionException.invalidAsset("Unsupported entity ::" + _asset);
            }
            BigInteger _percentage = _rewardConfig.getAssetPercentage(_asset);
            Map<String, BigInteger> entityMap = (Map<String, BigInteger>) response.get(_entity);
            if (entityMap == null) {
                entityMap = new scorex.util.HashMap<>() {{
                    put("total", BigInteger.ZERO);
                }};
            }
            BigInteger total = entityMap.get("total");
            BigInteger _distributionValue = MathUtils.exaMultiply(_distribution, _percentage);
            entityMap.put(_assetName, _distributionValue);
            entityMap.put("total", total.add(_distributionValue));
            response.put(_entity, entityMap);
            _totalRewards = _totalRewards.add(_distributionValue);
        }
        response.put("day", _day);
        response.put("total", _totalRewards);
        return response;

    }


    @External(readonly = true)
    public Map<String, ?> getRewards(Address _user) {
        BigInteger totalRewards = BigInteger.ZERO;
        Map<String, Object> response = new HashMap<>();
        List<Address> _assets = _rewardConfig.getAssets();
        for (Address _asset : _assets) {
            String _assetName = _rewardConfig.getAssetName(_asset);
            String _entity = _rewardConfig.getEntity(_asset);
            Map<String, BigInteger> entityMap = (Map<String, BigInteger>) response.get(_entity);
            if (entityMap == null) {
                entityMap = new HashMap<>() {{
                    put("total", BigInteger.ZERO);
                }};
            }
            BigInteger total = entityMap.get("total");
            UserAssetInput userAssetDetails = _getUserAssetDetails(_asset, _user);
            BigInteger unclaimedRewards = _usersUnclaimedRewards.at(_user).get(_asset);
            unclaimedRewards = unclaimedRewards.add(_getUnclaimedRewards(_user, userAssetDetails));
            entityMap.put(_assetName, unclaimedRewards);
            entityMap.put("total", total.add(unclaimedRewards));
            response.put(_entity, entityMap);
            totalRewards = totalRewards.add(unclaimedRewards);
        }
        response.put("total", totalRewards);
        BigInteger timeInSeconds = TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);
        response.put("now", timeInSeconds);
        return response;

    }

    @External
    public void startDistribution() {
        checkOwner();
        if (BigInteger.ZERO.equals(getDay()) && !_isInitialized.getOrDefault(false)) {
            _mintDailyOmm();
            updateEmissionPerSecond();
            _isInitialized.set(Boolean.TRUE);
        }
    }

    @External
    public void claimRewards(Address _user) {
        checkLendingPool();
        Context.require(isRewardClaimEnabled(), "The reward claim is not enabled");
        BigInteger unclaimedRewards = BigInteger.ZERO;
        BigInteger accruedRewards = BigInteger.ZERO;
        List<Address> _assets = _rewardConfig.getAssets();
        for (Address _asset : _assets) {
            unclaimedRewards = unclaimedRewards.add(_usersUnclaimedRewards.at(_user).get(_asset));
            UserAssetInput userAssetDetails = _getUserAssetDetails(_asset, _user);
            accruedRewards = accruedRewards.add(
                    _updateUserReserveInternal(_user, userAssetDetails.asset, userAssetDetails.userBalance,
                            userAssetDetails.totalBalance));
            _usersUnclaimedRewards.at(_user).set(_asset, BigInteger.ZERO);
        }
        if (!BigInteger.ZERO.equals(accruedRewards)) {
            unclaimedRewards = unclaimedRewards.add(accruedRewards);
        }
        if (BigInteger.ZERO.equals(unclaimedRewards)) {
            return;
        }
        Context.call(this.getAddress(Contracts.OMM_TOKEN.name()), "transfer", _user, unclaimedRewards);
        RewardsClaimed(_user, unclaimedRewards, "Asset rewards");
    }

    @External
    public void distribute() {
        BigInteger day = _day.get();
        if (day.compareTo(getDay()) > 0) {
            return;
        }
        Address workerTokenAddress = this.getAddress(Contracts.WORKER_TOKEN.name());
        Address ommTokenAddress = this.getAddress(Contracts.OMM_TOKEN.name());
        BigInteger totalSupply = Context.call(BigInteger.class, workerTokenAddress, "totalSupply");
        BigInteger tokenDistTracker = _tokenDistTracker.get("worker");
        List<Address> walletHolders = (List<Address>) Context.call(workerTokenAddress, "getWallets");
        for (Address user : walletHolders) {
            BigInteger userWorkerTokenBalance = Context.call(BigInteger.class, workerTokenAddress, "balanceOf", user);
            if (BigInteger.ZERO.compareTo(tokenDistTracker) != 0) {
                BigInteger tokenAmount = MathUtils.exaMultiply(MathUtils.exaDivide(userWorkerTokenBalance, totalSupply),
                        tokenDistTracker);
                Distribution("worker", user, tokenAmount);
                Context.call(ommTokenAddress, "transfer", user, tokenAmount);
                totalSupply = totalSupply.subtract(userWorkerTokenBalance);
                tokenDistTracker = tokenDistTracker.subtract(tokenAmount);
            }
        }
        Address daoFundAddress = this.getAddress(Contracts.DAO_FUND.name());
        BigInteger tokenDistTrackerDaoFund = _tokenDistTracker.get("daoFund");
        Context.call(ommTokenAddress, "transfer", daoFundAddress, tokenDistTrackerDaoFund);
        Distribution("daoFund", daoFundAddress, tokenDistTrackerDaoFund);
        _day.set(day.add(BigInteger.ONE));
        _mintDailyOmm();
    }

    @External(readonly = true)
    public BigInteger getDistributedDay() {
        return _day.get();
    }

    private void _mintDailyOmm() {
        BigInteger day = _day.get();
        BigInteger tokenDistributionPerDay = tokenDistributionPerDay(day);
        Address ommTokenAddress = this.getAddress(Contracts.OMM_TOKEN.name());
        Context.call(ommTokenAddress, "mint", tokenDistributionPerDay);
        List<String> recipients = new ArrayList<>() {{
            add("worker");
            add("daoFund");
        }};
        for (String recipient : recipients) {
            BigInteger _distributionPercentage = getDistributionPercentage(recipient);
            _tokenDistTracker.set(recipient, MathUtils.exaMultiply(tokenDistributionPerDay, _distributionPercentage));
        }
        OmmTokenMinted(day, tokenDistributionPerDay);
    }

    @External
    public void transferOmmToDaoFund(BigInteger _value) {
        checkGovernance();
        Address ommTokenAddress = this.getAddress(Contracts.OMM_TOKEN.name());
        Address daoFundAddress = this.getAddress(Contracts.DAO_FUND.name());
        Context.call(ommTokenAddress, "transfer", daoFundAddress, _value);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {

    }

    private void checkStakeLp() {
        if (!Context.getCaller()
                .equals(this.getAddress(Contracts.STAKED_LP.name()))) {
            throw RewardDistributionException.notStakedLp();
        }
    }

    private void checkLendingPool() {
        if (!Context.getCaller()
                .equals(this.getAddress(Contracts.LENDING_POOL.name()))) {
            throw RewardDistributionException.notLendingPool();
        }
    }

}

