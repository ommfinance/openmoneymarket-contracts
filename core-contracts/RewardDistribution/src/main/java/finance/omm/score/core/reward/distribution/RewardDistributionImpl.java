package finance.omm.score.core.reward.distribution;

import static finance.omm.utils.constants.TimeConstants.SECOND;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.UserDetails;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.libs.structs.WorkingBalance;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.constants.TimeConstants.Timestamp;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.HashMap;

public class RewardDistributionImpl extends AbstractRewardDistribution {

    public static final String TAG = "Reward distribution";
    public static final String DAY = "day";

    public static final String IS_INITIALIZED = "isInitialized";


    public final VarDB<BigInteger> distributedDay = Context.newVarDB(DAY, BigInteger.class);
    public final VarDB<Boolean> _isInitialized = Context.newVarDB(IS_INITIALIZED, Boolean.class);


    public final VarDB<Boolean> IS_ASSET_INDEX_UPDATED = Context.newVarDB("bOMM-migration-is-asset-index-updated",
            Boolean.class);


    public RewardDistributionImpl(Address addressProvider, BigInteger bOMMRewardStartDate) {
        super(addressProvider);
        if (this.bOMMRewardStartDate.get() == null) {
            TimeConstants.checkIsValidTimestamp(bOMMRewardStartDate, Timestamp.SECONDS);
            this.bOMMRewardStartDate.set(bOMMRewardStartDate);
            isHandleActionEnabled.set(Boolean.FALSE);
            isRewardClaimEnabled.set(Boolean.FALSE);
        }
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @Override
    @External(readonly = true)
    public Map<String, BigInteger> getAssetEmission() {
        return null;
    }

    @Override
    @External(readonly = true)
    public List<Address> getAssets() {
        return this.assets.keySet();
    }

    @Override
    @External(readonly = true)
    public Map<String, String> getAssetNames() {
        return this.assets.getAssetName();
    }

    @Override
    @External(readonly = true)
    public Map<String, BigInteger> getIndexes(Address _user, Address _asset) {
        return Map.of("userIndex", this.assets.getUserIndex(_asset, _user)
                , "assetIndex", this.assets.getAssetIndex(_asset));
    }

    @External(readonly = true)
    public BigInteger getAssetIndex(Address _asset) {
        return this.assets.getAssetIndex(_asset);
    }

    @External(readonly = true)
    public BigInteger getLastUpdatedTimestamp(Address _asset) {
        return this.getIndexUpdateTimestamp(_asset);
    }


    @Override
    @Deprecated
    @External
    public void setAssetName(Address _asset, String _name) {
        Context.println("setAssetName-called" + Context.getCaller());
    }


    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#setTypeWeight(TypeWeightStruct[],
     * BigInteger)}
     */
    @Override
    @Deprecated
    @External
    public void setDistributionPercentage(DistPercentage[] _distPercentage) {
        Context.println("setDistributionPercentage-called" + Context.getCaller());
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getTypeWeight(String,
     * BigInteger)}
     */
    @Override
    @Deprecated
    @External(readonly = true)
    public BigInteger getDistributionPercentage(String _recipient) {
        return call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getTypeWeight", _recipient);
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getAllTypeWeight(BigInteger)}
     */
    @Override
    @External(readonly = true)
    @Deprecated
    public Map<String, BigInteger> getAllDistributionPercentage() {
        return call(Map.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getAllTypeWeight");
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getAssetWeight(Address,
     * BigInteger)}
     */
    @Override
    @Deprecated
    @External(readonly = true)
    public BigInteger assetDistPercentage(Address asset) {
        return call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getAssetWeight", asset);
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getAllAssetDistributionPercentage(BigInteger)}
     */
    @Deprecated
    @External(readonly = true)
    public Map<String, ?> allAssetDistPercentage() {
        return call(Map.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getAllAssetDistributionPercentage");
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getDistPercentageOfLP(BigInteger)}
     */
    @Deprecated
    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> distPercentageOfAllLP() {
        return call(Map.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getDistPercentageOfLP");
    }

    @External(readonly = true)
    public Map<String, BigInteger> getLiquidityProviders() {
        return this.assets.getLiquidityProviders();
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#setAssetWeight(String,
     * WeightStruct[], BigInteger)}}
     */
    @Deprecated
    @External
    public void configureAssetConfigs(AssetConfig[] _assetConfig) {
        Context.println("configureAssetConfigs-called" + Context.getCaller());
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#setAssetWeight(String,
     * WeightStruct[], BigInteger)}}
     */
    @Deprecated
    @External
    public void configureAssetConfig(AssetConfig _assetConfig) {
        Context.println("configureAssetConfig-called" + Context.getCaller());
    }

    @Override
    @Deprecated
    @External
    public void removeAssetConfig(Address _asset) {
        Context.println("removeAssetConfig-called" + Context.getCaller());
    }

    @Override
    @Deprecated
    @External
    public void updateEmissionPerSecond() {
        Context.println("updateEmissionPerSecond-called" + Context.getCaller());
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#tokenDistributionPerDay(BigInteger)}
     * BigInteger)}}
     */
    @Deprecated
    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day) {
        return call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "tokenDistributionPerDay", _day);
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getDay()}
     */
    @Deprecated
    @External(readonly = true)
    public BigInteger getDay() {
        return call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getDay");
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getStartTimestamp()}
     * BigInteger)}}
     */
    @Deprecated
    @Override
    @External(readonly = true)
    public BigInteger getStartTimestamp() {
        return call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getStartTimestamp");
    }

    @Override
    @External(readonly = true)
    public BigInteger getPoolIDByAsset(Address _asset) {
        return this.assets.getPoolIDByAddress(_asset);
    }

    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getTypes()}
     */
    @Deprecated
    @External(readonly = true)
    public String[] getRecipients() {
        return call(String[].class, Contracts.REWARD_WEIGHT_CONTROLLER, "getTypes");
    }


    @External()
    public void disableRewardClaim() {
        checkGovernance();
        isRewardClaimEnabled.set(Boolean.FALSE);
    }

    @External()
    public void enableRewardClaim() {
        checkGovernance();
        isRewardClaimEnabled.set(Boolean.TRUE);
    }

    @External()
    public void disableHandleActions() {
        checkGovernance();
        isHandleActionEnabled.set(Boolean.FALSE);
    }

    @External()
    public void enableHandleActions() {
        checkGovernance();
        isHandleActionEnabled.set(Boolean.TRUE);
    }


    /**
     * @deprecated use {@link finance.omm.score.core.reward.RewardWeightControllerImpl#getDailyRewards(BigInteger)}
     */
    @Deprecated
    @Override
    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day) {
        return call(Map.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getDailyRewards", _day);
    }

    @Override
    @Deprecated
    @External
    public void startDistribution() {
        checkOwner();
        if (BigInteger.ZERO.equals(getDay()) && !_isInitialized.getOrDefault(false)) {
            _isInitialized.set(Boolean.TRUE);
        }
    }


    @External
    public void distribute() {
        BigInteger day = distributedDay.getOrDefault(BigInteger.ZERO);  //0

        @SuppressWarnings("unchecked")
        Class<Map<String, ?>> clazz = (Class) Map.class;
        Map<String, ?> precomputeInfo = call(clazz, Contracts.REWARD_WEIGHT_CONTROLLER, "precompute",
                day);
        Boolean isValid = (Boolean) precomputeInfo.get("isValid");

        if (!isValid) {
            throw RewardDistributionException.unknown("invalid day to distribute rewards " + day);
        }
        BigInteger amountToMint = (BigInteger) precomputeInfo.get("amountToMint");

        if (amountToMint.equals(BigInteger.ZERO)) {
            throw RewardDistributionException.unknown("no token to mint " + amountToMint);
        }

        BigInteger newDay = (BigInteger) precomputeInfo.get("day");

        BigInteger toTimestamp = (BigInteger) precomputeInfo.get("timestamp");

        distributedDay.set(newDay);
        call(Contracts.OMM_TOKEN, "mint", amountToMint);
        OmmTokenMinted(newDay, amountToMint, newDay.subtract(day));

        BigInteger transferToContract = BigInteger.ZERO;

        for (Address key : this.platformRecipientMap.keySet()) {
            BigInteger oldIndex = this.assets.getAssetIndex(key);
            BigInteger newIndex = this.getAssetIndex(key, ICX, toTimestamp, false);
            BigInteger accruedRewards = calculateReward(ICX, newIndex, oldIndex);
            transferToContract = transferToContract.add(accruedRewards);

            if (Contracts.WORKER_TOKEN.getKey().equals(platformRecipientMap.get(key))) {
                distributeWorkerToken(accruedRewards);
            } else if (Contracts.DAO_FUND.getKey().equals(platformRecipientMap.get(key))) {
                Address daoFundAddress = getAddress(Contracts.DAO_FUND.getKey());
                call(Contracts.OMM_TOKEN, "transfer", daoFundAddress, accruedRewards);
                Distribution("daoFund", daoFundAddress, accruedRewards);
            }
        }

        if (transferToContract.compareTo(amountToMint) > 0) {
            throw RewardDistributionException.unknown("transfer to contract exceed total distribution");
        }

    }

    /**
     * @param reward - BigInteger
     * @deprecated use tokenFallback to distribute token to workerToken holders
     */
    @Deprecated
    private void distributeWorkerToken(BigInteger reward) {
        Address[] walletHolders = call(Address[].class, Contracts.WORKER_TOKEN, "getWallets");
        BigInteger totalSupply = call(BigInteger.class, Contracts.WORKER_TOKEN, "totalSupply");
        BigInteger total = BigInteger.ZERO;
        for (Address user : walletHolders) {
            BigInteger userWorkerTokenBalance = call(BigInteger.class, Contracts.WORKER_TOKEN, "balanceOf", user);
            BigInteger amount = MathUtils.exaMultiply(MathUtils.exaDivide(userWorkerTokenBalance, totalSupply), reward);
            Distribution("worker", user, amount);
            call(Contracts.OMM_TOKEN, "transfer", user, amount);
            total = total.add(amount);
        }
        if (total.compareTo(reward) > 0) {
            throw RewardDistributionException.unknown("total "+ total + "  reward" + reward +
                    " worker token distribution exceed accrued reward");
        }
    }


    @External(readonly = true)
    public BigInteger getDistributedDay() {
        return this.distributedDay.get();
    }

    @External
    public void transferOmmToDaoFund(BigInteger _value) {
        checkGovernance();
        Address daoFundAddress = this.getAddress(Contracts.DAO_FUND.getKey());
        call(Contracts.OMM_TOKEN, "transfer", daoFundAddress, _value);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {

    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserDailyReward(Address user) {
        Map<String, String> assets = this.assets.getAssetName(this.platformRecipientMap.keySet());

        Map<String, BigInteger> dailyRewards = call(Map.class, Contracts.REWARD_WEIGHT_CONTROLLER,
                "getAssetDailyRewards");

        DictDB<Address, BigInteger> balances = workingBalance.at(user);
        Map<String, BigInteger> response = new HashMap<>();
        for (Map.Entry<String, String> entry : assets.entrySet()) {
            Address assetAddr = Address.fromString(entry.getKey());
            String name = entry.getValue();
            BigInteger userWorkingBalance = balances.getOrDefault(assetAddr, BigInteger.ZERO);
            BigInteger assetWorkingTotal = workingTotal.getOrDefault(assetAddr, BigInteger.ZERO);
            BigInteger dailyReward = dailyRewards.get(name);
            if (!assetWorkingTotal.equals(BigInteger.ZERO)) {
                response.put(name, exaMultiply(dailyReward, exaDivide(userWorkingBalance, assetWorkingTotal)));
            } else {
                response.put(name, BigInteger.ZERO);
            }
        }
        return response;
    }


    @External
    public void kick(Address userAddr) {
        Map<String, BigInteger> bOMMBalances = getBoostedBalance(userAddr);
        if (!bOMMBalances.get("bOMMUserBalance").equals(BigInteger.ZERO)) {
            throw RewardDistributionException.unknown(userAddr + " OMM locking is not expired");
        }
        List<Address> assets = this.assets.keySet(this.platformRecipientMap.keySet());
        BigInteger toTimestampInSeconds = TimeConstants.getBlockTimestampInSecond();
        for (Address assetAddr : assets) {
            Asset asset = this.assets.get(assetAddr);
            if (asset == null) {
                continue;
            }
            updateIndexes(assetAddr, userAddr, toTimestampInSeconds);

            WorkingBalance workingBalance = getUserBalance(userAddr, assetAddr, asset.lpID);
            workingBalance.bOMMUserBalance = bOMMBalances.get("bOMMUserBalance");
            workingBalance.bOMMTotalSupply = bOMMBalances.get("bOMMTotalSupply");

            updateWorkingBalance(workingBalance);
        }
        UserKicked(userAddr);
    }

    @Override
    @External()
    public void handleAction(UserDetails _userAssetDetails) {
        Address _asset = Context.getCaller();
        _handleAction(_asset, _userAssetDetails);
    }

    @Override
    @External
    public void handleLPAction(Address _asset, UserDetails _userDetails) {
        checkStakeLp();
        _handleAction(_asset, _userDetails);
    }

    private void _handleAction(Address assetAddr, UserDetails _userDetails) {
        if (!isHandleActionEnabled()) {
            throw RewardDistributionException.handleActionDisabled();
        }

        Asset asset = this.assets.get(assetAddr);
        if (asset == null) {
            throw RewardDistributionException.invalidAsset("Asset is null (" + assetAddr + ")");
        }

        Address userAddr = _userDetails._user;
        BigInteger toTimestampInSeconds = TimeConstants.getBlockTimestampInSecond();
        updateIndexes(assetAddr, userAddr, toTimestampInSeconds);

        Map<String, BigInteger> boostedBalance = getBoostedBalance(userAddr);
        WorkingBalance balance = getUserBalance(userAddr, assetAddr, asset.lpID);
        balance.bOMMUserBalance = boostedBalance.get("bOMMUserBalance");
        balance.bOMMTotalSupply = boostedBalance.get("bOMMTotalSupply");
        updateWorkingBalance(balance);
    }

    /**
     * calculate old reward indexes of all asset to bOMM cutOff timestamp
     */
    @External
    public void updateAssetIndexes() {
        checkOwner();
        BigInteger bOMMCutOffTimestamp = bOMMRewardStartDate.get();
        BigInteger currentTimestamp = TimeConstants.getBlockTimestamp().divide(SECOND);
        if (bOMMCutOffTimestamp.compareTo(currentTimestamp) > 0) {
            throw RewardDistributionException.unknown(
                    "bOMMCutOffTimestamp is future timestamp (" + bOMMCutOffTimestamp + ")");
        }
        if (IS_ASSET_INDEX_UPDATED.getOrDefault(false)) {
            throw RewardDistributionException.unknown(
                    "Asset index already updated (" + bOMMCutOffTimestamp + ")");
        }
        List<Address> assetAddrs = this.legacyRewards.getAssets();
        for (Address assetAddr : assetAddrs) {
            Integer poolId = this.legacyRewards.getPoolID(assetAddr);
            Map<String, BigInteger> map = null;
            BigInteger decimals = null;
            BigInteger totalSupply = null;

            if (poolId > 0) {
                map = Context.call(Map.class, getAddress(Contracts.STAKED_LP.getKey()),
                        "getTotalStaked", poolId);
                decimals = map.get("decimals");
                totalSupply = convertToExa(map.get("totalStaked"), decimals);
            } else {
                map = Context.call(Map.class, assetAddr, "getPrincipalSupply", Context.getCaller());
                decimals = map.get("decimals");
                totalSupply = convertToExa(map.get("principalTotalSupply"), decimals);
            }

            this.legacyRewards.updateAssetIndex(assetAddr, totalSupply, bOMMCutOffTimestamp);
            LegacyAssetIndexUpdated(assetAddr);
        }
        IS_ASSET_INDEX_UPDATED.set(Boolean.TRUE);
    }

    /**
     * calculate old accrued reward of users to bOMM cutOff timestamp, also update working balance of users
     */
    @External
    public void migrateUserRewards(Address[] userAddresses) {
        checkOwner();
        if (!IS_ASSET_INDEX_UPDATED.getOrDefault(false)) {
            throw RewardDistributionException.unknown(
                    "Asset indexes are not migrated, Please migrate asset index first");
        }
        List<Address> assetAddrs = this.legacyRewards.getAssets();
        Address bOMMAddress = getAddress(Contracts.BOOSTED_OMM.getKey());
        for (Address assetAddr : assetAddrs) {
            Integer poolId = this.legacyRewards.getPoolID(assetAddr);
            for (Address userAddr : userAddresses) {
                WorkingBalance workingBalance = getUserBalance(userAddr, assetAddr, BigInteger.valueOf(poolId));
                workingBalance.bOMMUserBalance = BigInteger.ZERO;
                workingBalance.bOMMTotalSupply = BigInteger.ZERO;

                BigInteger totalReward = legacyRewards.accumulateUserRewards(workingBalance);

                if (assetAddr.equals(getAddress(Contracts.OMM_TOKEN.getKey()))) {
                    this.assets.setAccruedRewards(userAddr, bOMMAddress, totalReward);
                } else {
                    this.assets.setAccruedRewards(userAddr, assetAddr, totalReward);
                    updateWorkingBalance(workingBalance);
                }
                LegacyUserIndexUpdated(userAddr, assetAddr);
            }
        }
    }

    @External(readonly = true)
    public Map<String, ?> getAllAssetLegacyIndexes() {
        return this.legacyRewards.getAllAssetIndexes();
    }


    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> getUserAllLegacyIndexes(Address _user) {
        return this.legacyRewards.getUserAllIndexes(_user);
    }

    @EventLog(indexed = 1)
    public void UserKicked(Address _user) {
    }

    @EventLog(indexed = 1)
    public void LegacyAssetIndexUpdated(Address _asset) {
    }


    @EventLog(indexed = 2)
    public void LegacyUserIndexUpdated(Address _user, Address _asset) {
    }

    @EventLog()
    public void OmmTokenMinted(BigInteger _day, BigInteger _value, BigInteger _days) {}

    @EventLog(indexed = 2)
    public void Distribution(String _recipient, Address _user, BigInteger _value) {}

}
