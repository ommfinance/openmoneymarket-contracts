package finance.omm.score.core.reward.distribution.deleteme;

import java.math.BigInteger;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;

/**
 * delete me
 */
public class DataToMigrate {

    public static final String TAG = "Omm Reward Distribution Manager";
    public static final String REWARD_CONFIG = "rewardConfig";
    public static final String LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String TIMESTAMP_AT_START = "timestampAtStart";
    public static final String ASSET_INDEX = "assetIndex";
    public static final String USER_INDEX = "userIndex";
    public static final String RESERVE_ASSETS = "reserveAssets";

    public final RewardConfigurationDB _rewardConfig = new RewardConfigurationDB(REWARD_CONFIG);

    public final DictDB<Address, BigInteger> _lastUpdateTimestamp = Context.newDictDB(LAST_UPDATE_TIMESTAMP,
            BigInteger.class);
    public final DictDB<Address, BigInteger> _assetIndex = Context.newDictDB(ASSET_INDEX, BigInteger.class);
    public final BranchDB<Address, DictDB<Address, BigInteger>> _userIndex = Context.newBranchDB(USER_INDEX,
            BigInteger.class);

    //    public final ArrayDB<Address> _reserveAssets = Context.newArrayDB(RESERVE_ASSETS, Address.class);
    public final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);


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


}
