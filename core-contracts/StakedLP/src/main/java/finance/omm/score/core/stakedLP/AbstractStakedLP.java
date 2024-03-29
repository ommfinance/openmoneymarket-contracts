package finance.omm.score.core.stakedLP;

import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.UserDetails;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;

public abstract class AbstractStakedLP extends AddressProvider implements StakedLP,
        Authorization<StakedLPException> {

    public static final String TAG = "Staked Lp";
    protected static final String HANDLE_ACTION_ENABLED = "handle_action_enabled";
    public static final BigInteger ZERO = BigInteger.ZERO;
    public static final BigInteger ONE = BigInteger.ONE;
    public final ArrayDB<Integer> supportedPools = Context.newArrayDB("supportedPools", Integer.class);
    public final BranchDB<Address, BranchDB<Integer, DictDB<Integer, BigInteger>>> poolStakeDetails = Context.newBranchDB(
            "poolStakeDetails", BigInteger.class);
    public final DictDB<Integer, BigInteger> totalStaked = Context.newDictDB("totalStaked", BigInteger.class);
    public final DictDB<Integer, Address> addressMap = Context.newDictDB("addressMap", Address.class);
    public final VarDB<BigInteger> minimumStake = Context.newVarDB("minimumStake", BigInteger.class);
    protected final VarDB<Boolean> _handleActionEnabled = Context.newVarDB(HANDLE_ACTION_ENABLED, Boolean.class);

    public static final Integer STAKED = 1;


    public AbstractStakedLP(Address addressProvider, boolean _update) {
        super(addressProvider, _update);

        if (minimumStake.get() == null) {
            minimumStake.set(ZERO);
        }

        if (_handleActionEnabled.get() == null) {
            _handleActionEnabled.set(true);
        }
    }


    @EventLog(indexed = 2)
    public void LPStaked(Address user, int poolId, BigInteger value) {}

    @EventLog(indexed = 2)
    public void LPUnstaked(Address user, int poolId, BigInteger value) {}

    protected BigInteger getAverageDecimals(int _id) {
        Map<String, ?> poolStats = call(Map.class, Contracts.DEX, "getPoolStats", BigInteger.valueOf(_id));
        BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
        BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
        return (quoteDecimals.add(baseDecimals)).divide(BigInteger.valueOf(2));
    }

    protected boolean inSupportedPools(int poolId) {
        int size = supportedPools.size();
        for (int i = 0; i < size; i++) {
            if (supportedPools.get(i).equals(poolId)) {
                return true;
            }
        }
        return false;
    }

    protected void stake(Address _user, int _id, BigInteger _value) {
        if (!_handleActionEnabled.get()) {
            throw StakedLPException.unknown("staking/unstaking of LP token is disabled");
        }

        boolean isSupported = inSupportedPools(_id);
        if (!isSupported) {
            throw StakedLPException.unknown("pool with id: " + _id + " is not supported");
        }

        if (_value.compareTo(ZERO) <= 0) {
            throw StakedLPException.unknown("Cannot stake less than zero ,value to stake " + _value);
        }
        if (_value.compareTo(minimumStake.get()) < 0) {
            throw StakedLPException.unknown("Amount to stake: " + _value + " is smaller the minimum stake: "
                    + minimumStake.get());

        }

        DictDB<Integer, BigInteger> userPoolDetails = poolStakeDetails.at(_user).at(_id);

        BigInteger previousUserStaked = userPoolDetails.getOrDefault(STAKED, ZERO);
        BigInteger previousTotalStaked = this.totalStaked.getOrDefault(_id, ZERO);

        BigInteger afterUserStaked = previousUserStaked.add(_value);
        BigInteger afterTotalStaked = previousTotalStaked.add(_value);

        userPoolDetails.set(STAKED, afterUserStaked);
        totalStaked.set(_id, afterTotalStaked);

        BigInteger decimals = getAverageDecimals(_id);
        UserDetails userDetails = new UserDetails();
        userDetails._user = _user;
        userDetails._decimals = decimals;
        userDetails._userBalance = previousUserStaked;
        userDetails._totalSupply = previousTotalStaked;

        call(Contracts.REWARDS, "handleLPAction", addressMap.get(_id), userDetails);
        LPStaked(_user, _id, _value);
    }
}
