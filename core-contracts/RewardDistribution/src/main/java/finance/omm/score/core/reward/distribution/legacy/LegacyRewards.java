package finance.omm.score.core.reward.distribution.legacy;

import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.libs.structs.WorkingBalance;
import java.math.BigInteger;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;


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

    private BigInteger getAssetIndex(Address assetAddress, BigInteger totalBalance, BigInteger cutOffTimestamp) {

        BigInteger lastUpdateTimestamp = this._lastUpdateTimestamp.getOrDefault(assetAddress, cutOffTimestamp);

        if (totalBalance.equals(BigInteger.ZERO) || cutOffTimestamp.equals(lastUpdateTimestamp)) {
            return BigInteger.ZERO;
        }
        BigInteger _emissionPerSecond = this._rewardConfig.getEmissionPerSecond(assetAddress);
        BigInteger timeDelta = cutOffTimestamp.subtract(lastUpdateTimestamp);

        return exaDivide(_emissionPerSecond.multiply(timeDelta), totalBalance);
    }

    public void clear(Address userAddr, Address assetAddr) {
        this._usersUnclaimedRewards.at(userAddr).set(assetAddr, null);
    }

    public BigInteger accumulateUserRewards(WorkingBalance workingBalance, BigInteger cutOffTimestamp,
            boolean isReadOnly) {
        Address userAddr = workingBalance.userAddr;
        Address assetAddr = workingBalance.assetAddr;
        BigInteger userIndex = this._userIndex.at(userAddr).getOrDefault(assetAddr, BigInteger.ZERO);
        BigInteger userUnclaimedReward = this._usersUnclaimedRewards.at(userAddr)
                .getOrDefault(assetAddr, BigInteger.ZERO);

        BigInteger oldIndex = this._assetIndex.getOrDefault(assetAddr, BigInteger.ZERO);
        BigInteger newIndex = getAssetIndex(assetAddr, workingBalance.totalSupply, cutOffTimestamp).add(oldIndex);
        if (userIndex.equals(newIndex)) {
            return userUnclaimedReward;
        }
        BigInteger newUserReward = getRewards(workingBalance.userBalance, newIndex, userIndex).add(
                userUnclaimedReward);
        if (!isReadOnly) {
            if (!newIndex.equals(oldIndex)) {
                this._assetIndex.set(assetAddr, newIndex);
                this.LegacyAssetIndexUpdated(assetAddr, oldIndex, newIndex);
            }

            this._lastUpdateTimestamp.set(assetAddr, cutOffTimestamp);

            this._usersUnclaimedRewards.at(userAddr).set(assetAddr, newUserReward);
            this._userIndex.at(userAddr).set(assetAddr, newIndex);
            this.LegacyUserIndexUpdated(userAddr, assetAddr, userIndex, newIndex);
        }
        return newUserReward;
    }


    private static BigInteger getRewards(BigInteger _userBalance, BigInteger _assetIndex, BigInteger _userIndex) {
        return exaMultiply(_userBalance, _assetIndex.subtract(_userIndex));
    }


    @EventLog(indexed = 1)
    public void LegacyAssetIndexUpdated(Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 2)
    public void LegacyUserIndexUpdated(Address _user, Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }

}
