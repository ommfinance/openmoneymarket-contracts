package finance.omm.score.core.reward.distribution.legacy;

import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.libs.structs.WorkingBalance;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import scorex.util.HashMap;


@Deprecated
public class LegacyRewards {

    public static final String REWARD_CONFIG = "rewardConfig";
    public static final String LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String ASSET_INDEX = "assetIndex";
    public static final String USER_INDEX = "userIndex";
    public static final String USERS_UNCLAIMED_REWARDS = "usersUnclaimedRewards";


    //user -> asset -> reward value
    public final BranchDB<Address, DictDB<Address, BigInteger>> _usersUnclaimedRewards = Context.newBranchDB(
            USERS_UNCLAIMED_REWARDS, BigInteger.class);

    public final RewardConfigurationDB _rewardConfig = new RewardConfigurationDB(REWARD_CONFIG);

    public final DictDB<Address, BigInteger> _lastUpdateTimestamp = Context.newDictDB(LAST_UPDATE_TIMESTAMP,
            BigInteger.class);
    public final DictDB<Address, BigInteger> _assetIndex = Context.newDictDB(ASSET_INDEX, BigInteger.class);
    //user -> asset -> index value
    public final BranchDB<Address, DictDB<Address, BigInteger>> _userIndex = Context.newBranchDB(USER_INDEX,
            BigInteger.class);


    public void updateAssetIndex(Address assetAddr, BigInteger totalBalance, BigInteger cutOffTimestamp) {
        BigInteger oldIndex = this._assetIndex.getOrDefault(assetAddr, BigInteger.ZERO);
        BigInteger lastUpdateTimestamp = this._lastUpdateTimestamp.getOrDefault(assetAddr, cutOffTimestamp);

        if (totalBalance.equals(BigInteger.ZERO) || cutOffTimestamp.equals(lastUpdateTimestamp)) {
            return;
        }

        BigInteger _emissionPerSecond = this._rewardConfig.getEmissionPerSecond(assetAddr);
        BigInteger timeDelta = cutOffTimestamp.subtract(lastUpdateTimestamp);

        BigInteger newIndex = exaDivide(_emissionPerSecond.multiply(timeDelta), totalBalance).add(oldIndex);

        this._assetIndex.set(assetAddr, newIndex);
        this.LegacyAssetIndexUpdated(assetAddr, oldIndex, newIndex);
        this._lastUpdateTimestamp.set(assetAddr, cutOffTimestamp);
    }

    public BigInteger accumulateUserRewards(WorkingBalance workingBalance) {
        Address userAddr = workingBalance.userAddr;
        Address assetAddr = workingBalance.assetAddr;
        BigInteger userIndex = this._userIndex.at(userAddr).getOrDefault(assetAddr, BigInteger.ZERO);
        BigInteger userUnclaimedReward = this._usersUnclaimedRewards.at(userAddr)
                .getOrDefault(assetAddr, BigInteger.ZERO);

        BigInteger assetIndex = this._assetIndex.getOrDefault(assetAddr, BigInteger.ZERO);
        if (userIndex.equals(assetIndex)) {
            return userUnclaimedReward;
        }
        BigInteger newUserReward = getRewards(workingBalance.userBalance, assetIndex, userIndex).add(
                userUnclaimedReward);

        this._usersUnclaimedRewards.at(userAddr).set(assetAddr, newUserReward);
        this._userIndex.at(userAddr).set(assetAddr, assetIndex);
        this.LegacyUserIndexUpdated(userAddr, assetAddr, userIndex, assetIndex);

        return newUserReward;
    }


    private static BigInteger getRewards(BigInteger _userBalance, BigInteger _assetIndex, BigInteger _userIndex) {
        return exaMultiply(_userBalance, _assetIndex.subtract(_userIndex));
    }

    public Map<String, Map<String, BigInteger>> getAllAssetIndexes() {
        List<Address> assets = this._rewardConfig.getAssets();
        Map<String, Map<String, BigInteger>> assetIndexes = new HashMap<>();
        for (Address asset : assets) {
            BigInteger index = this._assetIndex.getOrDefault(asset, BigInteger.ZERO);
            BigInteger lastUpdatedTimestamp = this._lastUpdateTimestamp.getOrDefault(asset, BigInteger.ZERO);

            Map<String, BigInteger> value = Map.of("index", index, "lastUpdateTimestamp", lastUpdatedTimestamp);
            assetIndexes.put(this._rewardConfig.getAssetName(asset), value);
        }
        return assetIndexes;
    }

    public Map<String, Map<String, BigInteger>> getUserAllIndexes(Address _user) {
        List<Address> assets = this._rewardConfig.getAssets();
        Map<String, Map<String, BigInteger>> assetIndexes = new HashMap<>();
        DictDB<Address, BigInteger> userUnclaimedRewards = _usersUnclaimedRewards.at(_user);

        for (Address asset : assets) {
            BigInteger index = this._userIndex.at(_user).getOrDefault(asset, BigInteger.ZERO);
            BigInteger reward = userUnclaimedRewards.getOrDefault(asset, BigInteger.ZERO);

            Map<String, BigInteger> value = Map.of("index", index, "reward", reward);
            assetIndexes.put(this._rewardConfig.getAssetName(asset), value);
        }
        return assetIndexes;
    }

    public Map<String, BigInteger> getLegacyUnclaimedRewards(Address _user) {
        Map<String, BigInteger> rewards = new HashMap<>();
        List<Address> assets = this._rewardConfig.getAssets();
        DictDB<Address, BigInteger> userUnclaimedRewards = _usersUnclaimedRewards.at(_user);
        BigInteger totalRewards = BigInteger.ZERO;
        for (Address asset : assets) {
            BigInteger reward = userUnclaimedRewards.getOrDefault(asset, BigInteger.ZERO);
            rewards.put(this._rewardConfig.getAssetName(asset), reward);
            totalRewards = totalRewards.add(reward);
        }

        rewards.put("total", totalRewards);

        return rewards;

    }

    @EventLog(indexed = 1)
    public void LegacyAssetIndexUpdated(Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 2)
    public void LegacyUserIndexUpdated(Address _user, Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }

    public List<Address> getAssets() {
        return this._rewardConfig.getAssets();
    }

    public Integer getPoolID(Address assetAddr) {
        return this._rewardConfig.getPoolID(assetAddr);
    }
}
