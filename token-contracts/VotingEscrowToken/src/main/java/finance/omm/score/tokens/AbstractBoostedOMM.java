package finance.omm.score.tokens;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.core.score.interfaces.BoostedToken;
import finance.omm.libs.address.AddressProvider;
import finance.omm.score.tokens.model.LockedBalance;
import finance.omm.score.tokens.model.Point;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.db.EnumerableSet;
import finance.omm.utils.math.UnsignedBigInteger;
import java.math.BigInteger;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public abstract class AbstractBoostedOMM extends AddressProvider implements BoostedToken {

    public static final BigInteger MAX_TIME = BigInteger.valueOf(4L).multiply(TimeConstants.YEAR_IN_MICRO_SECONDS);
    protected static final UnsignedBigInteger MULTIPLIER = UnsignedBigInteger.pow10(18);

    protected static final int DEPOSIT_FOR_TYPE = 0;
    protected static final int CREATE_LOCK_TYPE = 1;
    protected static final int INCREASE_LOCK_AMOUNT = 2;
    protected static final int INCREASE_UNLOCK_TIME = 3;

    protected final VarDB<String> name = Context.newVarDB("name", String.class);
    protected final VarDB<String> symbol = Context.newVarDB("symbol", String.class);
    protected final VarDB<Integer> decimals = Context.newVarDB("decimals", Integer.class);

    protected final VarDB<Address> tokenAddress = Context.newVarDB("tokenAddress", Address.class);

    protected final NonReentrant nonReentrant = new NonReentrant("Boosted_Omm_Reentrancy");


    protected final VarDB<BigInteger> supply = Context.newVarDB("Boosted_Omm_Supply", BigInteger.class);

    protected final DictDB<Address, LockedBalance> locked = Context.newDictDB("Boosted_Omm_locked",
            LockedBalance.class);

    protected final VarDB<BigInteger> epoch = Context.newVarDB("Boosted_Omm_epoch", BigInteger.class);
    protected final DictDB<BigInteger, Point> pointHistory = Context.newDictDB("Boosted_Omm_point_history",
            Point.class);
    protected final BranchDB<Address, DictDB<BigInteger, Point>> userPointHistory = Context.newBranchDB(
            "Boosted_Omm_user_point_history", Point.class);
    protected final DictDB<Address, BigInteger> userPointEpoch = Context.newDictDB("Boosted_Omm_user_point_epoch",
            BigInteger.class);
    protected final DictDB<BigInteger, BigInteger> slopeChanges = Context.newDictDB("Boosted_Omm_slope_changes",
            BigInteger.class);

    protected final VarDB<Address> admin = Context.newVarDB("Boosted_Omm_admin", Address.class);
    protected final VarDB<Address> futureAdmin = Context.newVarDB("Boosted_Omm_future_admin", Address.class);
    protected final EnumerableSet<Address> users = new EnumerableSet<>("users_list", Address.class);

    protected final VarDB<BigInteger> minimumLockingAmount = Context.newVarDB(KeyConstants.bOMM_MINIMUM_LOCKING_AMOUNT,
            BigInteger.class);


    public AbstractBoostedOMM(Address addressProvider, Address tokenAddress, String name, String symbol) {
        super(addressProvider, false);

        this.tokenAddress.set(tokenAddress);

        if (this.name.get() == null) {
            int decimals = ((BigInteger) callToken("decimals")).intValue();
            this.decimals.set(decimals);
            this.name.set(name);
            this.symbol.set(symbol);
        }
        this.admin.set(Context.getCaller());

        if (this.pointHistory.get(BigInteger.ZERO) == null) {
            Point point = new Point();
            point.block = UnsignedBigInteger.valueOf(Context.getBlockHeight());
            point.timestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
            this.pointHistory.set(BigInteger.ZERO, point);
        }

        if (this.supply.get() == null) {
            this.supply.set(BigInteger.ZERO);
        }

        if (this.epoch.get() == null) {
            this.epoch.set(BigInteger.ZERO);
        }

        if (this.minimumLockingAmount.get() == null) {
            this.minimumLockingAmount.set(ICX);
        }

    }

    @EventLog
    public void CommitOwnership(Address admin) {
    }

    @EventLog
    public void ApplyOwnership(Address admin) {
    }

    @EventLog(indexed = 2)
    public void Deposit(Address provider, BigInteger locktime, BigInteger value, int type, BigInteger timestamp) {
    }

    @EventLog(indexed = 1)
    public void Withdraw(Address provider, BigInteger value, BigInteger timestamp) {
    }

    @EventLog
    public void Supply(BigInteger prevSupply, BigInteger supply) {
    }


    @External(readonly = true)
    public int decimals() {
        return this.decimals.get();
    }

    @External(readonly = true)
    public String name() {
        return this.name.get();
    }

    @External(readonly = true)
    public String symbol() {
        return this.symbol.get();
    }

    public Object callToken(String method, Object... params) {
        return Context.call(this.tokenAddress.get(), method, params);
    }
}
