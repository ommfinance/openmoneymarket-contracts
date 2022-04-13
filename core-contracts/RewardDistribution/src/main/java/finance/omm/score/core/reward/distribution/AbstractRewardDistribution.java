package finance.omm.score.core.reward.distribution;

import static finance.omm.utils.constants.TimeConstants.SECOND;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.WorkingBalance;
import finance.omm.score.core.reward.distribution.db.Assets;
import finance.omm.score.core.reward.distribution.db.UserClaimedRewards;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.score.core.reward.distribution.legacy.LegacyRewards;
import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.db.EnumerableDictDB;
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
import scorex.util.HashMap;

public abstract class AbstractRewardDistribution extends AddressProvider implements RewardDistribution {

    public static final String IS_REWARD_CLAIM_ENABLED = "isRewardClaimEnabled";
    public static final String DISABLE_HANDLE_ACTIONS = "disable-handle-actions";

    public static final String B_OMM_REWARD_START_DATE = "bOMMRewardStartDate";
    private static final BigInteger WEIGHT = BigInteger.valueOf(40).multiply(ICX).divide(BigInteger.valueOf(100L));

    public final Assets assets;

    public final UserClaimedRewards userClaimedRewards = new UserClaimedRewards("user-claimed-reward");

    //asset address => total
    public final DictDB<Address, BigInteger> workingTotal;
    //user address = > asset address => total
    public final BranchDB<Address, DictDB<Address, BigInteger>> workingBalance;
    //    public final VarDB<BigInteger> weight;
    protected final EnumerableDictDB<Address, String> transferToContractMap = new EnumerableDictDB<>(
            "transferToContract",
            Address.class, String.class);

    public final VarDB<BigInteger> bOMMRewardStartDate = Context.newVarDB(B_OMM_REWARD_START_DATE, BigInteger.class);

    public final VarDB<Boolean> isRewardClaimEnabled = Context.newVarDB(IS_REWARD_CLAIM_ENABLED, Boolean.class);

    public final VarDB<Boolean> isHandleActionDisabled = Context.newVarDB(DISABLE_HANDLE_ACTIONS, Boolean.class);

    public final LegacyRewards legacyRewards = new LegacyRewards();


    public AbstractRewardDistribution(Address addressProvider) {
        super(addressProvider);
        //TODO pass empty key for c-testnet
        assets = new Assets("assets-temp");
        workingBalance = Context.newBranchDB("workingBalance", BigInteger.class);
        workingTotal = Context.newDictDB("workingTotal", BigInteger.class);
    }


    @External(readonly = true)
    public boolean isHandleActionDisabled() {
        return isHandleActionDisabled.get();
    }

    @External(readonly = true)
    public Map<String, BigInteger> getWorkingBalances(Address user) {
        Map<String, String> assets = this.assets.getAssetName(this.transferToContractMap.keySet());
        Map<String, BigInteger> response = new HashMap<>();
        DictDB<Address, BigInteger> balances = workingBalance.at(user);
        for (Map.Entry<String, String> entry : assets.entrySet()) {
            BigInteger userWorkingBalance = balances.getOrDefault(Address.fromString(entry.getKey()), BigInteger.ZERO);
            response.put(entry.getValue(), userWorkingBalance);
        }
        return response;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getWorkingTotal() {
        Map<String, String> assets = this.assets.getAssetName(this.transferToContractMap.keySet());
        Map<String, BigInteger> response = new HashMap<>();
        for (Map.Entry<String, String> entry : assets.entrySet()) {
            BigInteger assetWorkingTotal = workingTotal.getOrDefault(Address.fromString(entry.getKey()),
                    BigInteger.ZERO);
            response.put(entry.getValue(), assetWorkingTotal);
        }
        return response;
    }


    @External
    public void addType(String key, boolean transferToContract) {
        checkOwner();
        Object[] params = new Object[]{
                key, transferToContract
        };
        if (transferToContract) {
            Address address = getAddress(key);
            if (address == null) {
                throw RewardDistributionException.unknown(
                        "Address required for type if transferToContract is enable (" + key + ")");
            }
            params = new Object[]{
                    key, true, address
            };
            Asset asset = new Asset(address, key);
            asset.name = key;
            asset.lpID = null;
            assets.put(address, asset);
//            this.assets.setLastUpdateTimestamp(address, TimeConstants.getBlockTimestamp().divide(SECOND));
            transferToContractMap.put(address, key);
            AssetAdded(key, key, address, BigInteger.ZERO);
        }
        call(Contracts.REWARD_WEIGHT_CONTROLLER, "addType", params);
        AddType(key, transferToContract);
    }


    @External
    public void addAsset(String type, String name, Address address, @Optional BigInteger poolID) {
        checkOwner();
//        if (address == null && (poolID == null || poolID.equals(BigInteger.ZERO))) {
//            throw RewardDistributionException.invalidAsset("Nor address or poolID provided");
//        }
//
//        if (address != null && (poolID != null && !poolID.equals(BigInteger.ZERO))) {
//            throw RewardDistributionException.invalidAsset("Both address and poolID provided");
//        }

        call(Contracts.REWARD_WEIGHT_CONTROLLER, "addAsset", type, address, name);

        Asset asset = new Asset(address, type);
        asset.name = name;
        asset.lpID = poolID;
        assets.put(address, asset);
//        this.assets.setLastUpdateTimestamp(address, TimeConstants.getBlockTimestamp().divide(SECOND));
        AssetAdded(type, name, address, poolID);
    }

    @External(readonly = true)
    public Map<String, ?> getRewards(Address _user) {
        Map<String, Object> response = new HashMap<>();
        BigInteger totalRewards = BigInteger.ZERO;

        List<Address> assets = this.assets.keySet(this.transferToContractMap.keySet());
        for (Address address : assets) {
            Asset asset = this.assets.get(address);
            if (asset == null) {
                throw RewardDistributionException.invalidAsset("Asset is null (" + address + ")");
            }

            BigInteger reward = getUserReward(address, _user);
            Map<String, BigInteger> entityMap = (Map<String, BigInteger>) response.get(asset.type);
            if (entityMap == null) {
                entityMap = new HashMap<>() {{
                    put("total", BigInteger.ZERO);
                }};
            }
            BigInteger total = entityMap.get("total");
            entityMap.put(asset.name, reward);
            entityMap.put("total", total.add(reward));
            response.put(asset.type, entityMap);
            totalRewards = totalRewards.add(reward);
        }

        response.put("total", totalRewards);
        BigInteger timeInSeconds = TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);
        response.put("now", timeInSeconds);

        return response;

    }

    //    @Override
    @External(readonly = true)
    public boolean isRewardClaimEnabled() {
        return isRewardClaimEnabled.getOrDefault(Boolean.FALSE);
    }


    @External
    public void claimRewards(Address user) {
        if (!isRewardClaimEnabled()) {
            throw RewardDistributionException.rewardClaimDisabled();
        }
        Map<String, BigInteger> boostedBalance = getBoostedBalance(user);
        BigInteger bOMMUserBalance = boostedBalance.get("bOMMUserBalance");
        BigInteger bOMMTotalSupply = boostedBalance.get("bOMMTotalSupply");

        BigInteger accruedReward = BigInteger.ZERO;
        List<Address> assets = this.assets.keySet(this.transferToContractMap.keySet());

        for (Address address : assets) {
            Asset asset = this.assets.get(address);
            if (asset == null) {
                throw RewardDistributionException.invalidAsset("Asset is null (" + address + ")");
            }
            WorkingBalance workingBalance = getUserBalance(user, address, asset.lpID);
            workingBalance.bOMMUserBalance = bOMMUserBalance;
            workingBalance.bOMMTotalSupply = bOMMTotalSupply;

            BigInteger newReward = updateIndexes(asset.address, user);

            accruedReward = accruedReward.add(newReward);

            this.assets.clearAccruedReward(user, asset.address);

            updateWorkingBalance(workingBalance);
        }

        if (BigInteger.ZERO.equals(accruedReward)) {
            return;
        }
        BigInteger rewards = userClaimedRewards.getOrDefault(user, BigInteger.ZERO);
        userClaimedRewards.put(user, rewards.add(accruedReward));

        call(Contracts.OMM_TOKEN, "transfer", user, accruedReward);
        RewardsClaimed(user, accruedReward, "Asset rewards claimed");
    }

    @External(readonly = true)
    public BigInteger getClaimedReward(Address user) {
        return userClaimedRewards.getOrDefault(user, BigInteger.ZERO);
    }

    protected BigInteger updateIndexes(Address assetAddr, Address user) {
        BigInteger userBalance = this.workingBalance.at(user).getOrDefault(assetAddr, BigInteger.ZERO);

        BigInteger userIndex = this.assets.getUserIndex(assetAddr, user);

        BigInteger newIndex = this.getAssetIndex(assetAddr, false);

        BigInteger accruedRewards = this.assets.getAccruedRewards(assetAddr, user);
        if (newIndex.equals(userIndex)) {
            return accruedRewards;
        }

        BigInteger totalReward = calculateReward(userBalance, newIndex, userIndex).add(accruedRewards);

        this.assets.setAccruedRewards(user, assetAddr, totalReward);
        this.assets.setUserIndex(assetAddr, user, newIndex);
        this.UserIndexUpdated(user, assetAddr, userIndex, newIndex);

        return totalReward;
    }


    protected BigInteger getUserReward(Address assetAddr, Address user) {
        BigInteger userBalance = this.workingBalance.at(user).getOrDefault(assetAddr, BigInteger.ZERO);

        BigInteger userIndex = this.assets.getUserIndex(assetAddr, user);

        BigInteger accruedRewards = this.assets.getAccruedRewards(assetAddr, user);

        BigInteger newIndex = this.getAssetIndex(assetAddr, true);

        return calculateReward(userBalance, newIndex, userIndex).add(accruedRewards);
    }

    protected BigInteger getAssetIndex(Address assetAddr, Boolean readonly) {
        BigInteger totalSupply = this.workingTotal.getOrDefault(assetAddr, BigInteger.ZERO);

        return getAssetIndex(assetAddr, totalSupply, readonly);
    }

    protected BigInteger getAssetIndex(Address assetAddr, BigInteger totalSupply, Boolean readonly) {

        BigInteger oldIndex = this.assets.getAssetIndex(assetAddr);
        BigInteger lastUpdateTimestamp = getIndexUpdateTimestamp(assetAddr);
        BigInteger currentTime = TimeConstants.getBlockTimestamp().divide(SECOND);

        if (currentTime.equals(lastUpdateTimestamp)) {
            return oldIndex;
        }
        /*
        reward weight controller store snapshot in micro second
         */
        BigInteger assetIndex = call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getIntegrateIndex",
                assetAddr,
                totalSupply, lastUpdateTimestamp.multiply(SECOND));
        BigInteger newIndex = oldIndex.add(assetIndex);
        if (!readonly) {
            if (!oldIndex.equals(newIndex)) {
                this.assets.setAssetIndex(assetAddr, newIndex);
                this.AssetIndexUpdated(assetAddr, oldIndex, newIndex);
            }
            this.assets.setLastUpdateTimestamp(assetAddr, currentTime);
        }
        return newIndex;
    }

    protected BigInteger getIndexUpdateTimestamp(Address assetAddr) {
        BigInteger lastUpdateTimestamp = this.assets.getIndexUpdateTimestamp(assetAddr);
        if (lastUpdateTimestamp == null) {
            lastUpdateTimestamp = bOMMRewardStartDate.get();
        }
        return lastUpdateTimestamp;
    }

    protected WorkingBalance getUserBalance(Address userAddr, Address assetAddr, BigInteger poolId) {
        WorkingBalance workingBalance = new WorkingBalance();

        SupplyDetails response = fetchUserBalance(userAddr, assetAddr, poolId);
        BigInteger decimals = response.decimals;
        BigInteger principalUserBalance = response.principalUserBalance;
        BigInteger principalTotalSupply = response.principalTotalSupply;
        workingBalance.userBalance = convertToExa(principalUserBalance, decimals);
        workingBalance.totalSupply = convertToExa(principalTotalSupply, decimals);
        workingBalance.userAddr = userAddr;
        workingBalance.assetAddr = assetAddr;

        return workingBalance;
    }

    public SupplyDetails fetchUserBalance(Address user, Address asset, BigInteger poolId) {

        Map<String, BigInteger> response = null;

        if (poolId == null || poolId.compareTo(BigInteger.ZERO) <= 0) {
            response = Context.call(Map.class, asset, "getPrincipalSupply", user);
        } else {
            response = Context.call(Map.class, getAddress(Contracts.STAKED_LP.getKey()),
                    "getLPStakedSupply", poolId, user);
        }

        if (response == null) {
            throw RewardDistributionException.unknown("invalid response from token (" + asset + ")");
        }

        return SupplyDetails.fromMap(response);
    }


    protected Map<String, BigInteger> getBoostedBalance(Address user) {
        BigInteger bOMMUserBalance = call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user);
        BigInteger bOMMTotalSupply = call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");
        return Map.of("bOMMUserBalance", bOMMUserBalance, "bOMMTotalSupply", bOMMTotalSupply);
    }

    protected void updateWorkingBalance(WorkingBalance balance) {
        Address assetAddr = balance.assetAddr;
        Address userAddr = balance.userAddr;

        BigInteger currentWorkingBalance = this.workingBalance.at(userAddr).getOrDefault(assetAddr, BigInteger.ZERO);

        BigInteger userBalance = balance.userBalance;
        BigInteger totalSupply = balance.totalSupply;

        BigInteger bOMMUserBalance = balance.bOMMUserBalance;
        BigInteger bOMMTotalSupply = balance.bOMMTotalSupply;

        BigInteger newWorkingBalance = exaMultiply(userBalance, WEIGHT);

        if (BigInteger.ZERO.compareTo(bOMMTotalSupply) < 0) {
//            supply*0.4+totalSupply*bOMMUserBalance/bOMMTotalSupply*0.6
            BigInteger numerator = exaMultiply(totalSupply, bOMMUserBalance);
            BigInteger total = exaDivide(numerator, bOMMTotalSupply);
            newWorkingBalance = exaMultiply(total, ICX.subtract(WEIGHT)).add(newWorkingBalance);
        }

        newWorkingBalance = userBalance.min(newWorkingBalance);

        this.workingBalance.at(userAddr).set(assetAddr, newWorkingBalance);

        BigInteger workingTotal = this.workingTotal.getOrDefault(assetAddr, BigInteger.ZERO)
                .add(newWorkingBalance)
                .subtract(currentWorkingBalance);
        this.workingTotal.set(assetAddr, workingTotal);

        this.WorkingBalanceUpdated(userAddr, assetAddr, newWorkingBalance, workingTotal);
    }


    protected void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw RewardDistributionException.notOwner();
        }
    }

    protected void checkStakeLp() {
        if (!Context.getCaller()
                .equals(this.getAddress(Contracts.STAKED_LP.getKey()))) {
            throw RewardDistributionException.notStakedLp();
        }
    }

    protected void checkGovernance() {
        if (!Context.getCaller()
                .equals(this.getAddress(Contracts.GOVERNANCE.getKey()))) {
            throw RewardDistributionException.notGovernanceContract();
        }
    }

    protected static BigInteger calculateReward(BigInteger balance, BigInteger assetIndex, BigInteger userIndex) {
        return MathUtils.exaMultiply(balance, assetIndex.subtract(userIndex));
    }

    @EventLog(indexed = 3)
    public void AssetAdded(String type, String name, Address address, BigInteger poolID) {
    }

    @EventLog(indexed = 2)
    public void AddType(String id, boolean transferToContract) {
    }

    @EventLog(indexed = 1)
    public void AssetIndexUpdated(Address assetAddr, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 2)
    public void UserIndexUpdated(Address _user, Address assetAddr, BigInteger _oldIndex, BigInteger _newIndex) {
    }

    @EventLog()
    public void RewardsClaimed(Address _user, BigInteger _rewards, String _msg) {}

    @EventLog(indexed = 2)
    public void WorkingBalanceUpdated(Address user, Address assetAddr, BigInteger newWorkingBalance,
            BigInteger workingTotal) {
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

    public <K> K call(Class<K> kClass, Contracts contract, String method, Object... params) {
        return Context.call(kClass, getAddress(contract.getKey()), method, params);
    }

}
