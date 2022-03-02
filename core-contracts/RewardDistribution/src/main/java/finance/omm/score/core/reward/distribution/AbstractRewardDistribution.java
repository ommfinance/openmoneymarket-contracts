package finance.omm.score.core.reward.distribution;

import static finance.omm.utils.constants.TimeConstants.SECOND;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.WorkingBalance;
import finance.omm.score.core.reward.distribution.db.Assets;
import finance.omm.score.core.reward.distribution.db.UserClaimedRewards;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.db.EnumerableDictDB;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
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

    public final Assets assets;

    public final UserClaimedRewards userClaimedRewards = new UserClaimedRewards("user-claimed-reward");

    public final DictDB<String, BigInteger> workingTotal;
    public final BranchDB<Address, DictDB<String, BigInteger>> workingBalance;
    public final VarDB<BigInteger> weight;
    protected final EnumerableDictDB<String, String> transferToContractMap = new EnumerableDictDB<>(
            "transferToContract",
            String.class, String.class);


    public AbstractRewardDistribution(BigInteger _weight) {
        super(null);
        assets = new Assets("assets");
        workingBalance = Context.newBranchDB("workingBalance", BigInteger.class);
        workingTotal = Context.newDictDB("workingTotal", BigInteger.class);
        weight = Context.newVarDB("weight", BigInteger.class);
        if (_weight.compareTo(ICX) > 0) {
            throw RewardDistributionException.unknown("invalid weight percentage");
        }
        weight.set(_weight);
    }

    @External
    public void setWeight(BigInteger _weight) {
        checkOwner();
        if (_weight.compareTo(ICX) > 0) {
            throw RewardDistributionException.unknown("invalid weight percentage");
        }
        weight.set(_weight);
    }

    @External
    public void addType(String key, boolean transferToContract, @Optional String addressKey) {
        checkOwner();
        String assetId = call(String.class, Contracts.REWARD_WEIGHT_CONTROLLER, "addType", key, transferToContract);
        if (transferToContract) {
            Address address = getAddress(Contracts.valueOf(addressKey).toString());
            Context.require(address != null, "Address required for type if transferToContract is enable");
            Asset asset = new Asset(assetId, key);
            asset.name = key;
            asset.address = address;
            asset.lpID = null;
            assets.put(address, asset);
            this.assets.setLastUpdateTimestamp(assetId, TimeConstants.getBlockTimestamp().divide(SECOND));
            transferToContractMap.put(assetId, addressKey);
            AssetAdded(assetId, key, key, address, null);
        }
        AddType(key, transferToContract);
    }


    @External
    public void addAsset(String typeId, String name, Address address, @Optional BigInteger poolID) {
        checkOwner();
//        if (address == null && (poolID == null || poolID.equals(BigInteger.ZERO))) {
//            throw RewardDistributionException.invalidAsset("Nor address or poolID provided");
//        }
//
//        if (address != null && (poolID != null && !poolID.equals(BigInteger.ZERO))) {
//            throw RewardDistributionException.invalidAsset("Both address and poolID provided");
//        }

        String id = call(String.class, Contracts.REWARD_WEIGHT_CONTROLLER, "addAsset", typeId, name);

        Asset asset = new Asset(id, typeId);
        asset.name = name;
        asset.address = address;
        asset.lpID = poolID;
        assets.put(address, asset);
        this.assets.setLastUpdateTimestamp(id, TimeConstants.getBlockTimestamp().divide(SECOND));
        AssetAdded(id, typeId, name, address, poolID);
    }

    @External(readonly = true)
    public Map<String, ?> getRewards(Address user) {
        BigInteger totalRewards = BigInteger.ZERO;
        Map<String, Object> response = new HashMap<>();
        for (Address address : this.assets.keySet()) {
            Asset asset = this.assets.get(address);
            if (asset == null) {
                throw RewardDistributionException.invalidAsset("Asset is null (" + address + ")");
            }
            BigInteger reward = getUserReward(asset.id, user, true);
            Map<String, BigInteger> entityMap = (Map<String, BigInteger>) response.get(asset.typeId);
            if (entityMap == null) {
                entityMap = new HashMap<>() {{
                    put("total", BigInteger.ZERO);
                }};
            }
            BigInteger total = entityMap.get("total");
            entityMap.put(asset.name, reward);
            entityMap.put("total", total.add(reward));
            response.put(asset.typeId, entityMap);
            totalRewards = totalRewards.add(reward);
        }

        response.put("total", totalRewards);
        BigInteger timeInSeconds = TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);
        response.put("now", timeInSeconds);
        return response;

    }

    @External
    public void claimRewards(Address user) {
        Map<String, BigInteger> boostedBalance = getBoostedBalance(user);
        BigInteger bOMMUserBalance = boostedBalance.get("bOMMUserBalance");
        BigInteger bOMMTotalSupply = boostedBalance.get("bOMMTotalSupply");

        BigInteger accruedReward = BigInteger.ZERO;
        for (Address address : this.assets.keySet()) {
            Asset asset = this.assets.get(address);
            if (asset == null) {
                throw RewardDistributionException.invalidAsset("Asset is null (" + address + ")");
            }
            BigInteger reward = getUserReward(asset.id, user, false);
            accruedReward = accruedReward.add(reward);
            Map<String, BigInteger> balances = getUserBalance(user, asset);

            WorkingBalance workingBalance = new WorkingBalance();
            workingBalance.assetId = asset.id;
            workingBalance.user = user;
            workingBalance.tokenBalance = balances.get("userBalance");
            workingBalance.tokenTotalSupply = balances.get("totalSupply");
            workingBalance.bOMMUserBalance = bOMMUserBalance;
            workingBalance.bOMMTotalSupply = bOMMTotalSupply;

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


    protected BigInteger getUserReward(String assetId, Address user, Boolean readonly) {
        BigInteger userBalance = this.workingBalance.at(user).getOrDefault(assetId, BigInteger.ZERO);
        BigInteger totalSupply = this.workingTotal.getOrDefault(assetId, BigInteger.ZERO);
        BigInteger userIndex = this.assets.getUserIndex(assetId, user);

        BigInteger accruedRewards = BigInteger.ZERO;

        BigInteger newIndex = this.getAssetIndex(assetId, totalSupply, readonly);

        if (!userIndex.equals(newIndex) && !BigInteger.ZERO.equals(userBalance)) {
            accruedRewards = calculateReward(userBalance, newIndex, userIndex);
            if (!readonly) {
                this.assets.setUserIndex(assetId, user, newIndex);
                this.UserIndexUpdated(user, assetId, userIndex, newIndex);
            }
        }
        return accruedRewards;
    }


    protected BigInteger getAssetIndex(String assetId, BigInteger totalSupply, Boolean readonly) {
        BigInteger oldIndex = this.assets.getAssetIndex(assetId);
        BigInteger lastUpdateTimestamp = this.assets.getLastUpdateTimestamp(assetId);
        BigInteger currentTime = TimeConstants.getBlockTimestamp().divide(SECOND);

        if (currentTime.equals(lastUpdateTimestamp)) {
            return oldIndex;
        }
        /*
        reward weight controller store snapshot in microsecondsx
         */
        BigInteger assetIndex = call(BigInteger.class, Contracts.REWARD_WEIGHT_CONTROLLER, "getIntegrateIndex", assetId,
                totalSupply, lastUpdateTimestamp.multiply(SECOND));
        BigInteger newIndex = oldIndex.add(assetIndex);
        if (!readonly) {
            if (!oldIndex.equals(newIndex)) {
                this.assets.setAssetIndex(assetId, newIndex);
                this.AssetIndexUpdated(assetId, oldIndex, newIndex);
            }
            this.assets.setLastUpdateTimestamp(assetId, currentTime);
        }
        return newIndex;
    }

    protected Map<String, BigInteger> getUserBalance(Address user, Asset asset) {
        BigInteger poolId = asset.lpID;
        Map<String, BigInteger> result = new HashMap<>();
        Map<String, ?> response = fetchUserBalance(user, asset.address, poolId);
        BigInteger decimals = (BigInteger) response.get("decimals");
        BigInteger principalUserBalance = (BigInteger) response.get("principalUserBalance");
        BigInteger principalTotalSupply = (BigInteger) response.get("principalTotalSupply");
        result.put("userBalance", convertToExa(principalUserBalance, decimals));
        result.put("totalSupply", convertToExa(principalTotalSupply, decimals));
        result.put("decimals", decimals);
        return result;
    }

    public Map<String, ?> fetchUserBalance(Address user, Address asset, BigInteger poolId) {
        Map<String, ?> response;
        if (poolId == null || poolId.equals(BigInteger.ZERO)) {
            response = (Map<String, ?>) Context.call(asset, "getPrincipalSupply", user);
        } else {
            response = (Map<String, ?>) Context.call(getAddress(Contracts.STAKED_LP.getKey()),
                    "getLPStakedSupply", poolId, user);
        }
        if (response == null) {
            throw RewardDistributionException.unknown("invalid response from token (" + asset + ")");
        }
        return response;
    }


    protected void updateWorkingBalance(String assetId, Address user, BigInteger tokenBalance,
            BigInteger tokenTotalSupply) {
        Map<String, BigInteger> boostedBalance = getBoostedBalance(user);
        WorkingBalance balance = new WorkingBalance();
        balance.assetId = assetId;
        balance.user = user;
        balance.tokenBalance = tokenBalance;
        balance.tokenTotalSupply = tokenTotalSupply;
        balance.bOMMUserBalance = boostedBalance.get("bOMMUserBalance");
        balance.bOMMTotalSupply = boostedBalance.get("bOMMTotalSupply");

        updateWorkingBalance(balance);

    }

    protected Map<String, BigInteger> getBoostedBalance(Address user) {
        BigInteger bOMMUserBalance = call(BigInteger.class, Contracts.OMM_TOKEN, "balanceOf", user);
        BigInteger bOMMTotalSupply = call(BigInteger.class, Contracts.OMM_TOKEN, "totalSupply");
        return Map.of("bOMMUserBalance", bOMMUserBalance, "bOMMTotalSupply", bOMMTotalSupply);
    }

    private void updateWorkingBalance(WorkingBalance balance) {
        String assetId = balance.assetId;
        Address user = balance.user;
        BigInteger currentWorkingBalance = this.workingBalance.at(user).getOrDefault(assetId, BigInteger.ZERO);

        BigInteger weight = this.weight.getOrDefault(BigInteger.ZERO);
        BigInteger userBalance = balance.tokenBalance;
        BigInteger totalSupply = balance.tokenTotalSupply;

        BigInteger bOMMUserBalance = balance.bOMMUserBalance;
        BigInteger bOMMTotalSupply = balance.bOMMTotalSupply;

        BigInteger newWorkingBalance = exaMultiply(userBalance, weight);
        if (BigInteger.ZERO.compareTo(bOMMTotalSupply) < 0) {
            newWorkingBalance = newWorkingBalance.add(
                    exaMultiply(
                            exaDivide(
                                    exaMultiply(totalSupply, bOMMUserBalance)
                                    , bOMMTotalSupply
                            ),
                            ICX.subtract(weight)
                    )
            );
        }

        newWorkingBalance = userBalance.min(newWorkingBalance);
        this.workingBalance.at(user).set(assetId, newWorkingBalance);
        BigInteger workingTotal = this.workingTotal.getOrDefault(assetId, BigInteger.ZERO)
                .add(newWorkingBalance)
                .subtract(currentWorkingBalance);
        this.workingTotal.set(assetId, workingTotal);

        this.WorkingBalanceUpdated(user, assetId, newWorkingBalance, workingTotal);
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
    public void AssetAdded(String id, String typeId, String name, Address address,
            BigInteger poolID) {
    }

    @EventLog(indexed = 2)
    public void AddType(String id, Boolean transferToContract) {
    }

    @EventLog(indexed = 1)
    public void AssetIndexUpdated(String assetId, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 2)
    public void UserIndexUpdated(Address _user, String assetId, BigInteger _oldIndex, BigInteger _newIndex) {
    }

    @EventLog()
    public void RewardsClaimed(Address _user, BigInteger _rewards, String _msg) {}

    @EventLog(indexed = 2)
    public void WorkingBalanceUpdated(Address user, String assetId, BigInteger newWorkingBalance,
            BigInteger workingTotal) {
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

    public <K> K call(Class<K> kClass, Contracts contract, String method, Object... params) {
        return Context.call(kClass, getAddress(contract.getKey()), method, params);
    }

}
