package finance.omm.score.token;

import finance.omm.core.score.interfaces.OMMToken;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.token.enums.Status;
import finance.omm.score.token.exception.OMMTokenException;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.db.EnumerableSet;
import java.math.BigInteger;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;

public abstract class AbstractOMMToken extends AddressProvider implements OMMToken,
        Authorization<OMMTokenException> {

    protected final String TAG = "Omm Token";
    protected static final String TOKEN_NAME = "Omm Token";
    protected static final String SYMBOL_NAME = "OMM";


    public static final String NAME = "name";
    public static final String SYMBOL = "symbol";
    public static final String DECIMALS = "decimals";
    public static final String TOTAL_SUPPLY = "total_supply";
    public static final String BALANCES = "balances";
//    public static final String ADMIN = "admin";
    public static final String LOCK_LIST = "lock_list";

    public static final String MINIMUM_STAKE = "minimum_stake";
    public static final String STAKED_BALANCES = "staked_balances";
    public static final String TOTAL_STAKED_BALANCE = "total_stake_balance";
    public static final String UNSTAKING_PERIOD = "unstaking_period";
//    public static final String SNAPSHOT_STARTED_AT = "snapshot-started-at";
    public static final String STAKERS = "stakers";


    public final VarDB<String> name = Context.newVarDB(NAME, String.class);
    public final VarDB<String> symbol = Context.newVarDB(SYMBOL, String.class);
    public final VarDB<BigInteger> decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    public final VarDB<BigInteger> totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    public final DictDB<Address, BigInteger> balances = Context.newDictDB(BALANCES, BigInteger.class);
    //    public final VarDB<Address> admin = Context.newVarDB(ADMIN, Address.class);
    public final EnumerableSet<Address> lockList = new EnumerableSet<>(LOCK_LIST, Address.class);

    public final VarDB<BigInteger> minimumStake = Context.newVarDB(MINIMUM_STAKE, BigInteger.class);
    public final BranchDB<Address, DictDB<Integer, BigInteger>> stakedBalances = Context.newBranchDB(STAKED_BALANCES,
            BigInteger.class);
    public final VarDB<BigInteger> totalStakedBalance = Context.newVarDB(TOTAL_STAKED_BALANCE, BigInteger.class);
    public final VarDB<BigInteger> unstakingPeriod = Context.newVarDB(UNSTAKING_PERIOD, BigInteger.class);
    public final EnumerableSet<Address> stakers = new EnumerableSet<>(STAKERS, Address.class);

//    public final VarDB<BigInteger> snapshotStartedAt = Context.newVarDB(SNAPSHOT_STARTED_AT, BigInteger.class);

    public AbstractOMMToken(Address addressProvider, String tokenName, String symbolName) {
        super(addressProvider, false);
        if (this.name.get() == null && this.totalSupply.get() == null) {
            this.name.set(tokenName);
            this.symbol.set(symbolName);
            this.decimals.set(BigInteger.valueOf(18));
            this.totalSupply.set(BigInteger.ZERO);
        }
    }


    protected void addStaker(Address staker) {
        this.stakers.add(staker);
    }

    protected void removeStaker(Address staker) {
        this.stakers.remove(staker);
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}

    protected void makeAvailable(Address user) {
        DictDB<Integer, BigInteger> stakeInfo = this.stakedBalances.at(user);
        BigInteger unstakingPeriod = stakeInfo.getOrDefault(Status.UNSTAKING_PERIOD.getKey(), BigInteger.ZERO);
        if (unstakingPeriod.compareTo(TimeConstants.getBlockTimestamp()) <= 0) {
            stakeInfo.set(Status.UNSTAKING.getKey(), BigInteger.ZERO);
        }
    }

    /**
     * Transfers certain amount of tokens from sender to the recipient.
     *
     * @param from  - The account from which the token is to be transferred.
     * @param to    - The account to which the token is to be transferred.
     * @param value - The no. of tokens to be transferred.
     * @param data  -Any information or message
     */
    protected void _transfer(Address from, Address to, BigInteger value, byte[] data) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw OMMTokenException.unknown("Transferring value cannot be less than 0.");
        }
        BigInteger fromBalance = this.balances.getOrDefault(from, BigInteger.ZERO);
        if (fromBalance.compareTo(value) < 0) {
            throw OMMTokenException.insufficientBalance("Insufficient balance");
        }

        checkFeeSharing(from);
        this.makeAvailable(to);
        this.makeAvailable(from);
        BigInteger senderAvailableBalance = this.available_balanceOf(from);

        if (senderAvailableBalance.compareTo(value) < 0) {
            throw OMMTokenException.insufficientBalance(
                    "available balance of user " + senderAvailableBalance + "balance to transfer " + value);
        }
        this.balances.set(from, fromBalance.subtract(value));

        BigInteger toBalance = this.balances.getOrDefault(to, BigInteger.ZERO);
        this.balances.set(to, toBalance.add(value));

        if (to.isContract()) {
            call(to, "tokenFallback", from, value, data);
        }

        this.Transfer(from, to, value, data);
    }

    protected void checkFeeSharing(Address from) {
        Boolean isFeeSharingEnabled = call(Boolean.class, Contracts.LENDING_POOL, "isFeeSharingEnable", from);
        if (isFeeSharingEnabled) {
            Context.setFeeSharingProportion(100);
        }
    }
}
