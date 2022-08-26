package finance.omm.score.core.lendingpoolcore;

import static finance.omm.score.core.lendingpoolcore.reservedata.AbstractReserve.getDataFromReserve;
import static finance.omm.utils.math.MathUtils.*;

import finance.omm.core.score.interfaces.LendingPoolCore;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.score.core.lendingpoolcore.exception.LendingPoolCoreException;
import finance.omm.score.core.lendingpoolcore.reservedata.ReserveDataDB;
import finance.omm.score.core.lendingpoolcore.userreserve.UserReserveDataDB;
import score.*;
import score.annotation.EventLog;
import scorex.util.HashMap;

public abstract class AbstractLendingPoolCore extends AddressProvider
        implements LendingPoolCore, Authorization<LendingPoolCoreException> {

    public static final String TAG = "Lending Pool Core";
    public static String CONSTANTS = "constants";
    public static String RESERVE_LIST = "reserveList";

    public static ReserveDataDB reserve = new ReserveDataDB();
    public static UserReserveDataDB userReserve = new UserReserveDataDB();
    public BranchDB<Address, DictDB<String, BigInteger>> constants = Context.newBranchDB(CONSTANTS,
            BigInteger.class);
    public static ArrayDB<Address> reserveList = Context.newArrayDB(RESERVE_LIST,
            Address.class);

    public AbstractLendingPoolCore(Address addressProvider, boolean _update) {
        super(addressProvider, _update);
    }

    @EventLog(indexed = 3)
    public void ReserveUpdated(Address _reserve, BigInteger _liquidityRate, BigInteger _borrowRate,
                               BigInteger _liquidityCumulativeIndex, BigInteger _borrowCumulativeIndex) {
    }

    @EventLog(indexed = 3)
    public void InterestTransfer(BigInteger _amount, Address _reserve, Address _initiatiator) {
    }

    protected byte[] reservePrefix(Address reserve) {
        return ("reserve" + "|" + reserve.toString()).getBytes();
    }

    protected byte[] userReservePrefix(Address reserve, Address user) {
        return ("userReserve" + "|" + reserve.toString() + user.toString()).getBytes();
    }

    protected void updateDToken(Address reserveAddress, Address dToken) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.dTokenAddress.set(prefix, dToken);
    }

    protected void updateLastUpdateTimestamp(Address reserveAddress, BigInteger lastUpdateTimestamp) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.lastUpdateTimestamp.set(prefix, lastUpdateTimestamp);
    }

    protected void updateLiquidityRate(Address reserveAddress, BigInteger liquidityRate) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.liquidityRate.set(prefix, liquidityRate);
    }

    protected void updateBorrowRate(Address reserveAddress, BigInteger borrowRate) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.borrowRate.set(prefix, borrowRate);
    }

    protected void updateBorrowCumulativeIndex(Address reserveAddress, BigInteger borrowCumulativeIndex) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.borrowCumulativeIndex.set(prefix, borrowCumulativeIndex);

    }

    protected void updateLiquidityCumulativeIndex(Address reserveAddress, BigInteger liquidityCumulativeIndex) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.liquidityCumulativeIndex.set(prefix, liquidityCumulativeIndex);
    }

    protected void updateDecimals(Address reserveAddress, int decimals) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.decimals.set(prefix, decimals);
    }

    protected void updateOtokenAddress(Address reserveAddress, Address oTokenAddress) {
        byte[] prefix = reservePrefix(reserveAddress);
        reserve.oTokenAddress.set(prefix, oTokenAddress);
    }

    protected void updateUserLastUpdateTimestamp(Address reserve, Address user,
                                                 BigInteger lastUpdateTimestamp) {
        byte[] prefix = userReservePrefix(reserve, user);
        userReserve.lastUpdateTimestamp.set(prefix, lastUpdateTimestamp);
    }

    protected void updateUserOriginationFee(Address reserve, Address user, BigInteger originationFee) {
        byte[] prefix = userReservePrefix(reserve, user);
        userReserve.originationFee.set(prefix, originationFee);
    }

    protected boolean checkReserve(Address reserve) {
        List<Address> reserveList = getReserves();
        return reserveList.contains(reserve);
    }

    protected void addNewReserve(Address res) {
        reserveList.add(res);
    }

    protected BigInteger calculateLinearInterest(BigInteger rate, BigInteger lastUpdateTimestamp) {
        BigInteger timeDifference = (BigInteger.valueOf(Context.getBlockTimestamp()).subtract(lastUpdateTimestamp)).divide(pow10(6));
        BigInteger timeDelta = exaDivide(timeDifference, SECONDS_PER_YEAR);
        return exaMultiply(rate, timeDelta).add(ICX);
    }

    protected BigInteger calculateCompoundedInterest(BigInteger rate, BigInteger lastUpdateTimestamp) {
        BigInteger timeDifference = (BigInteger.valueOf(Context.getBlockTimestamp()).subtract(lastUpdateTimestamp)).divide(pow10(6));
        BigInteger ratePerSecond = rate.divide(SECONDS_PER_YEAR);
        return exaPow(ratePerSecond.add(ICX), timeDifference);
    }

    protected void updateCumulativeIndexes(Address reserve) {
        Map<String, Object> reserveData = getReserveData(reserve);
        BigInteger totalBorrows = (BigInteger) reserveData.get("totalBorrows");

        if (totalBorrows.compareTo(BigInteger.ZERO) > 0) {
            BigInteger cumulatedLiquidityInterest = calculateLinearInterest((BigInteger) reserveData.get("liquidityRate"),
                    (BigInteger) reserveData.get("lastUpdateTimestamp"));
            updateLiquidityCumulativeIndex(reserve, exaMultiply(cumulatedLiquidityInterest,
                    (BigInteger) reserveData.get("liquidityCumulativeIndex")));
            BigInteger cumulatedBorrowInterest = calculateCompoundedInterest((BigInteger) reserveData.get("borrowRate"),
                    (BigInteger) reserveData.get("lastUpdateTimestamp"));
            updateBorrowCumulativeIndex(reserve, exaMultiply(cumulatedBorrowInterest, (BigInteger) reserveData.get("borrowCumulativeIndex")));
        }
    }

    protected BigInteger getReserveTotalLiquidity(Address reserve) {
        return getReserveAvailableLiquidity(reserve).add(getReserveTotalBorrows(reserve));
    }

    protected BigInteger getReserveTotalBorrows(Address reserveAddress) {
        byte[] prefix = reservePrefix(reserveAddress);
        Map<String, Object> reserveData = getDataFromReserve(prefix, reserve);
        return call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "principalTotalSupply");

    }

    protected BigInteger getReserveUtilizationRate(Address reserve) {
        Map<String, Object> reserveData = getReserveData(reserve);
        BigInteger totalBorrows = (BigInteger) reserveData.get("totalBorrows");

        if (totalBorrows.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        BigInteger totalLiquidity = getReserveTotalLiquidity(reserve);
        return exaDivide(totalBorrows, totalLiquidity);
    }

    protected void updateReserveInterestRatesAndTimestampInternal(Address reserve, BigInteger liquidityAdded,
                                                                  BigInteger liquidityTaken) {
        Map<String, Object> reserveData = getReserveData(reserve);
        Map<String, BigInteger> rate = calculateInterestRates(reserve, getReserveAvailableLiquidity(reserve)
                .add(liquidityAdded)
                .subtract(liquidityTaken), (BigInteger) reserveData.get("totalBorrows"));
        updateLiquidityRate(reserve, rate.get("liquidityRate"));
        updateBorrowRate(reserve, rate.get("borrowRate"));
        updateLastUpdateTimestamp(reserve, BigInteger.valueOf(Context.getBlockTimestamp()));

        ReserveUpdated(reserve, rate.get("liquidityRate"), rate.get("borrowRate"), (BigInteger) reserveData.get("liquidityCumulativeIndex")
                , (BigInteger) reserveData.get("borrowCumulativeIndex"));
    }

    protected BigInteger getCurrentBorrowRate(Address reserve) {
        Map<String, Object> reserveData = getReserveData(reserve);
        return (BigInteger) reserveData.get("borrowRate");
    }

    protected void updateUserStateOnBorrowInternal(Address reserve, Address user, BigInteger amountBorrowed,
                                                   BigInteger balanceIncrease, BigInteger borrowFee) {
        Map<String, BigInteger> userReserveData = getUserReserveData(reserve, user);
        BigInteger userPreviousOriginationFee = userReserveData.get("originationFee");
        updateUserOriginationFee(reserve, user, userPreviousOriginationFee.add(borrowFee));
        updateUserLastUpdateTimestamp(reserve, user, BigInteger.valueOf(Context.getBlockTimestamp()));
    }

    protected void updateUserStateOnRepayInternal(Address reserve, Address user, BigInteger paybackAmountMinusFees,
                                                  BigInteger originationFeeRepaid, BigInteger balanceIncrease, boolean repaidWholeLoan) {
        Map<String, BigInteger> userReserveData = getUserReserveData(reserve, user);
        BigInteger originationFee = userReserveData.get("originationFee");
        updateUserOriginationFee(reserve, user, originationFee.subtract(originationFeeRepaid));
        updateUserLastUpdateTimestamp(reserve, user, BigInteger.valueOf(Context.getBlockTimestamp()));
    }

    protected void updatePrincipalReserveStateOnLiquidationInternal(Address principalReserve, Address user,
                                                                    BigInteger amountToLiquidate, BigInteger balanceIncrease) {
        updateCumulativeIndexes(principalReserve);
        Address dTokenAddress = getReserveDTokenAddress(principalReserve);
        call(dTokenAddress,
                "burnOnLiquidation", user, amountToLiquidate, balanceIncrease);
    }

    protected void updateCollateralReserveStateOnLiquidationInternal(Address collateralReserve) {
        updateCumulativeIndexes(collateralReserve);
    }

    protected void updateUserStateOnLiquidationInternal(Address reserve, Address user, BigInteger amountToLiquidate,
                                                        BigInteger feeLiquidated, BigInteger balanceIncrease) {
        Map<String, BigInteger> userReserveData = getUserReserveData(reserve, user);
        BigInteger originationFee = userReserveData.get("originationFee");

        if (feeLiquidated.compareTo(BigInteger.ZERO) > 0) {
            updateUserOriginationFee(reserve, user, originationFee.subtract(feeLiquidated));
        }

        updateUserLastUpdateTimestamp(reserve, user, BigInteger.valueOf(Context.getBlockTimestamp()));
    }

    protected Map<String, BigInteger> calculateInterestRates(Address reserve, BigInteger availableLiquidity, BigInteger totalBorrows) {
        Map<String, Object> constants = getReserveConstants(reserve);
        Map<String, BigInteger> rate = new HashMap<>();
        BigInteger utilizationRate;

        if (totalBorrows.equals(BigInteger.ZERO) && availableLiquidity.equals(BigInteger.ZERO)) {
            utilizationRate = BigInteger.ZERO;
        } else {
            utilizationRate = exaDivide(totalBorrows, (totalBorrows.add(availableLiquidity)));
        }

        BigInteger optimalUtilizationRate = (BigInteger) constants.get("optimalUtilizationRate");
        BigInteger baseBorrowRate = (BigInteger) constants.get("baseBorrowRate");
        BigInteger slopeRate1 = (BigInteger) constants.get("slopeRate1");
        BigInteger slopeRate2 = (BigInteger) constants.get("slopeRate2");

        if (utilizationRate.compareTo(optimalUtilizationRate) < 0) {
            rate.put("borrowRate", baseBorrowRate.
                    add(exaMultiply(
                            exaDivide(utilizationRate, optimalUtilizationRate),
                            slopeRate1)));
        } else {
            rate.put("borrowRate", baseBorrowRate
                    .add(slopeRate1)
                    .add(exaMultiply(
                            exaDivide(utilizationRate.subtract(optimalUtilizationRate),
                                    ICX.subtract(optimalUtilizationRate)), slopeRate2)));
        }
        rate.put("liquidityRate", exaMultiply(
                exaMultiply(rate.get("borrowRate"), utilizationRate)
                , ICX.multiply(BigInteger.valueOf(9)).divide(BigInteger.TEN)));
        return rate;

    }

    protected <K> K call(Class<K> kClass, Address contract, String method, Object... params) {
        return Context.call(kClass, contract, method, params);
    }
}