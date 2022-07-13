package finance.omm.score.tokens;

import static finance.omm.utils.constants.AddressConstant.ZERO_ADDRESS;
import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.core.score.interfaces.BoostedToken;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.score.tokens.exception.BoostedOMMException;
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

    protected final EnumerableSet<Address> users = new EnumerableSet<>("users_list", Address.class);

    protected final VarDB<BigInteger> minimumLockingAmount = Context.newVarDB(KeyConstants.bOMM_MINIMUM_LOCKING_AMOUNT,
            BigInteger.class);


    protected final EnumerableSet<Address> allowedContracts = new EnumerableSet<>(KeyConstants.bOMM_ALLOWED_CONTRACTS,
            Address.class);

    protected final VarDB<BigInteger> ommTokenBalance = Context.newVarDB(KeyConstants.bOMM_OMM_BALANCE,
            BigInteger.class);


    public AbstractBoostedOMM(Address addressProvider, Address tokenAddress, String name, String symbol) {
        super(addressProvider, false);
        onInstall(tokenAddress, name, symbol);
    }

    private void onInstall(Address tokenAddress, String name, String symbol) {
        if (this.tokenAddress.get() != null) {
            return;
        }
        this.tokenAddress.set(tokenAddress);

        int decimals = ((BigInteger) callToken("decimals")).intValue();
        this.decimals.set(decimals);
        this.name.set(name);
        this.symbol.set(symbol);

        Point point = new Point();
        point.block = UnsignedBigInteger.valueOf(Context.getBlockHeight());
        point.timestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
        this.pointHistory.set(BigInteger.ZERO, point);

        this.ommTokenBalance.set(BigInteger.ZERO);
        this.supply.set(BigInteger.ZERO);
        this.epoch.set(BigInteger.ZERO);
        this.minimumLockingAmount.set(ICX);
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

    protected BigInteger findBlockEpoch(BigInteger block, BigInteger maxEpoch) {
        BigInteger min = BigInteger.ZERO;
        BigInteger max = maxEpoch;

        for (int index = 0; index < 256 && min.compareTo(max) < 0; ++index) {
            BigInteger mid = min.add(max).add(BigInteger.ONE).divide(BigInteger.TWO);
            Point point = this.pointHistory.getOrDefault(mid, new Point());
            if (point.block.compareTo(block) <= 0) {
                min = mid;
            } else {
                max = mid.subtract(BigInteger.ONE);
            }
        }

        return min;
    }

    protected BigInteger findUserPointHistory(Address address, BigInteger block) {
        BigInteger min = BigInteger.ZERO;
        BigInteger max = this.userPointEpoch.getOrDefault(address, BigInteger.ZERO);

        for (int index = 0; index < 256 && min.compareTo(max) < 0; ++index) {
            BigInteger mid = min.add(max).add(BigInteger.ONE).divide(BigInteger.TWO);
            if (getUserPointHistory(address, mid).block.compareTo(block) <= 0) {
                min = mid;
            } else {
                max = mid.subtract(BigInteger.ONE);
            }
        }
        return min;
    }


    protected BigInteger supplyAt(Point point, BigInteger time) {
        Point lastPoint = point.newPoint();
        UnsignedBigInteger timestampIterator = lastPoint.timestamp.divide(TimeConstants.U_WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
        UnsignedBigInteger uTime = new UnsignedBigInteger(time);
        for (int index = 0; index < 255; ++index) {
            timestampIterator = timestampIterator.add(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
            BigInteger dSlope = BigInteger.ZERO;
            if (timestampIterator.compareTo(time) > 0) {
                timestampIterator = uTime;
            } else {
                dSlope = this.slopeChanges.getOrDefault(timestampIterator.toBigInteger(), BigInteger.ZERO);
            }
            UnsignedBigInteger delta = timestampIterator.subtract(lastPoint.timestamp);
            lastPoint.bias = lastPoint.bias.subtract(lastPoint.slope.multiply(delta.toBigInteger()));
            if (timestampIterator.equals(uTime)) {
                break;
            }

            lastPoint.slope = lastPoint.slope.add(dSlope);
            lastPoint.timestamp = timestampIterator;
        }

        if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
            lastPoint.bias = BigInteger.ZERO;
        }
        return lastPoint.bias;
    }

    protected LockedBalance getLockedBalance(Address user) {
        return locked.getOrDefault(user, new LockedBalance());
    }

    protected Point getUserPointHistory(Address user, BigInteger epoch) {
        return this.userPointHistory.at(user).getOrDefault(epoch, new Point());
    }

    protected void checkpoint(Address address, LockedBalance oldLocked, LockedBalance newLocked) {
        Point uOld = new Point();
        Point uNew = new Point();
        BigInteger oldDSlope = BigInteger.ZERO;
        BigInteger newDSlope = BigInteger.ZERO;
        BigInteger epoch = this.epoch.get();

        UnsignedBigInteger blockTimestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
        UnsignedBigInteger blockHeight = UnsignedBigInteger.valueOf(Context.getBlockHeight());

        if (!address.equals(ZERO_ADDRESS)) {
            //            Calculate slopes and biases
            //            Kept at zero when they have to
            if (oldLocked.end.compareTo(blockTimestamp) > 0 && oldLocked.amount.compareTo(BigInteger.ZERO) > 0) {
                uOld.slope = oldLocked.amount.divide(MAX_TIME);
                UnsignedBigInteger delta = oldLocked.end.subtract(blockTimestamp);
                uOld.bias = uOld.slope.multiply(delta.toBigInteger());
            }

            if (newLocked.end.compareTo(blockTimestamp) > 0 && newLocked.amount.compareTo(BigInteger.ZERO) > 0) {
                uNew.slope = newLocked.amount.divide(MAX_TIME);
                UnsignedBigInteger delta = newLocked.end.subtract(blockTimestamp);
                uNew.bias = uNew.slope.multiply(delta.toBigInteger());
            }

            //          Read values of scheduled changes in the slope
            //          oldLocked.end can be in the past and in the future
            //          newLocked.end can ONLY be in the FUTURE unless everything expired: than zeros
            oldDSlope = this.slopeChanges.getOrDefault(oldLocked.getEnd(), BigInteger.ZERO);
            if (!newLocked.end.equals(UnsignedBigInteger.ZERO)) {
                if (newLocked.end.equals(oldLocked.end)) {
                    newDSlope = oldDSlope;
                } else {
                    newDSlope = this.slopeChanges.getOrDefault(newLocked.getEnd(), BigInteger.ZERO);
                }
            }
        }

        Point lastPoint = new Point(BigInteger.ZERO, BigInteger.ZERO, blockTimestamp.toBigInteger(),
                blockHeight.toBigInteger());
        if (epoch.compareTo(BigInteger.ZERO) > 0) {
            lastPoint = this.pointHistory.getOrDefault(epoch, new Point());
        }
        UnsignedBigInteger lastCheckPoint = lastPoint.timestamp;

        //      initialLastPoint is used for extrapolation to calculate block number
        //      (approximately, for *At methods) and save them
        //      as we cannot figure that out exactly from inside the contract
        Point initialLastPoint = lastPoint.newPoint();
        UnsignedBigInteger blockSlope = UnsignedBigInteger.ZERO;
        if (blockTimestamp.compareTo(lastPoint.timestamp) > 0) {
            blockSlope = MULTIPLIER.multiply(blockHeight.subtract(lastPoint.block))
                    .divide(blockTimestamp.subtract(lastPoint.timestamp));
            //          If last point is already recorded in this block, slope = 0
            //          But that's ok because we know the block in such case
        }

        //      Go over week's to fill history and calculate what the current point is
        UnsignedBigInteger timeIterator = lastCheckPoint.divide(TimeConstants.U_WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.U_WEEK_IN_MICRO_SECONDS);

        for (int index = 0; index < 255; ++index) {
            timeIterator = timeIterator.add(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
            BigInteger dSlope = BigInteger.ZERO;
            if (timeIterator.compareTo(blockTimestamp) > 0) {
                timeIterator = blockTimestamp;
            } else {
                dSlope = this.slopeChanges.getOrDefault(timeIterator.toBigInteger(), BigInteger.ZERO);
            }

            lastPoint.bias = lastPoint.bias.subtract(lastPoint.slope.multiply(timeIterator.subtract(lastCheckPoint)
                    .toBigInteger()));
            lastPoint.slope = lastPoint.slope.add(dSlope);

            if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.bias = BigInteger.ZERO;
            }

            if (lastPoint.slope.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.slope = BigInteger.ZERO;
            }

            lastCheckPoint = timeIterator;
            lastPoint.timestamp = timeIterator;
            UnsignedBigInteger dtime = timeIterator.subtract(initialLastPoint.timestamp);
            lastPoint.block = initialLastPoint.block.add(blockSlope.multiply(dtime).divide(MULTIPLIER));
            epoch = epoch.add(BigInteger.ONE);

            if (timeIterator.equals(blockTimestamp)) {
                lastPoint.block = blockHeight;
                break;
            } else {
                pointHistory.set(epoch, lastPoint);
            }
        }

        this.epoch.set(epoch);
        if (!address.equals(ZERO_ADDRESS)) {
            lastPoint.slope = lastPoint.slope.add(uNew.slope.subtract(uOld.slope));
            lastPoint.bias = lastPoint.bias.add(uNew.bias.subtract(uOld.bias));

            if (lastPoint.slope.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.slope = BigInteger.ZERO;
            }
            if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.bias = BigInteger.ZERO;
            }
        }

        this.pointHistory.set(epoch, lastPoint);

        if (!address.equals(ZERO_ADDRESS)) {
            if (oldLocked.end.compareTo(blockTimestamp) > 0) {
                oldDSlope = oldDSlope.add(uOld.slope);
                if (newLocked.end.equals(oldLocked.end)) {
                    oldDSlope = oldDSlope.subtract(uNew.slope);
                }
                this.slopeChanges.set(oldLocked.getEnd(), oldDSlope);
            }

            if (newLocked.end.compareTo(blockTimestamp) > 0 && newLocked.end.compareTo(oldLocked.end) > 0) {
                newDSlope = newDSlope.subtract(uNew.slope);
                this.slopeChanges.set(newLocked.getEnd(), newDSlope);
            }

            BigInteger userEpoch = this.userPointEpoch.getOrDefault(address, BigInteger.ZERO).add(BigInteger.ONE);
            this.userPointEpoch.set(address, userEpoch);
            uNew.timestamp = blockTimestamp;
            uNew.block = blockHeight;
            this.userPointHistory.at(address).set(userEpoch, uNew);
        }
    }

    protected void depositFor(Address address, BigInteger value, BigInteger unlockTime, LockedBalance lockedBalance,
            int type) {
        LockedBalance locked = lockedBalance.newLockedBalance();
        BigInteger supplyBefore = this.supply.get();
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());

        this.supply.set(supplyBefore.add(value));
        LockedBalance oldLocked = locked.newLockedBalance();

        locked.amount = locked.amount.add(value);
        if (!unlockTime.equals(BigInteger.ZERO)) {
            locked.end = new UnsignedBigInteger(unlockTime);
        }

        this.locked.set(address, locked);
        if (value.compareTo(BigInteger.ZERO) > 0) {
            this.ommTokenBalance.set(this.ommTokenBalance.get().add(value));
        }
        this.checkpoint(address, oldLocked, locked);

        Deposit(address, value, locked.getEnd(), type, blockTimestamp);
        Supply(supplyBefore, supplyBefore.add(value));

        onBalanceUpdate(address);
    }

    protected void createLock(Address sender, BigInteger value, BigInteger unlockTime) {
        this.nonReentrant.updateLock(true);
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        this.assertNotContract(sender);

        unlockTime = unlockTime.divide(TimeConstants.WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.WEEK_IN_MICRO_SECONDS);
        LockedBalance locked = getLockedBalance(sender);

        Context.require(value.compareTo(BigInteger.ZERO) > 0, "Create Lock: Need non zero value");
        Context.require(locked.amount.equals(BigInteger.ZERO), "Create Lock: Withdraw old tokens first");
        Context.require(unlockTime.compareTo(blockTimestamp) > 0, "Create Lock: Can only lock until time in the " +
                "future");
        Context.require(unlockTime.compareTo(blockTimestamp.add(MAX_TIME)) <= 0,
                "Create Lock: Voting Lock can be 4 years max");

        users.add(sender);
        this.depositFor(sender, value, unlockTime, locked, CREATE_LOCK_TYPE);
        this.nonReentrant.updateLock(false);
    }

    protected void increaseAmount(Address sender, BigInteger value, BigInteger unlockTime) {
        this.nonReentrant.updateLock(true);
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        this.assertNotContract(sender);
        LockedBalance locked = getLockedBalance(sender);

        Context.require(value.compareTo(BigInteger.ZERO) > 0, "Increase amount: Need non zero value");
        Context.require(locked.amount.compareTo(BigInteger.ZERO) > 0, "Increase amount: No existing lock found");
        Context.require(locked.getEnd()
                .compareTo(blockTimestamp) > 0, "Increase amount: Cannot add to expired lock.");

        if (!unlockTime.equals(BigInteger.ZERO)) {
            unlockTime = unlockTime.divide(TimeConstants.WEEK_IN_MICRO_SECONDS)
                    .multiply(TimeConstants.WEEK_IN_MICRO_SECONDS);
            Context.require(unlockTime.compareTo(locked.end.toBigInteger()) >= 0,
                    "Increase unlock time: Can only increase lock duration");
            Context.require(unlockTime.compareTo(blockTimestamp.add(MAX_TIME)) <= 0,
                    "Increase unlock time: Voting lock can be 4 years max");
        }

        this.depositFor(sender, value, unlockTime, locked, INCREASE_LOCK_AMOUNT);
        this.nonReentrant.updateLock(false);
    }

    protected void assertNotContract(Address address) {
        if (address.isContract() && !allowedContracts.contains(address)) {
            throw BoostedOMMException.unknown("Only whitelisted contracts are allowed to deposit.");
        }
    }

    protected void onKick(Address user, BigInteger bOMMBalance, byte[] data) {
        call(Contracts.DELEGATION, "onKick", user, bOMMBalance, data);
        call(Contracts.REWARDS, "onKick", user, bOMMBalance, data);
    }

    protected void onBalanceUpdate(Address user) {
        call(Contracts.DELEGATION, "onBalanceUpdate", user);
        call(Contracts.REWARDS, "onBalanceUpdate", user);
    }

    public Object callToken(String method, Object... params) {
        return Context.call(this.tokenAddress.get(), method, params);
    }
}
