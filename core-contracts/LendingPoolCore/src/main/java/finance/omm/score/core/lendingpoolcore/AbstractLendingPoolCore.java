package finance.omm.score.core.lendingpoolcore;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.SECONDS_PER_YEAR;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.exaPow;

import finance.omm.core.score.interfaces.LendingPoolCore;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;

import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.score.core.lendingpoolcore.exception.LendingPoolCoreException;
import finance.omm.score.core.lendingpoolcore.reservedata.ReserveDataDB;
import finance.omm.score.core.lendingpoolcore.userreserve.UserReserveDataDB;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
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

    public AbstractLendingPoolCore(Address addressProvider) {
        super(addressProvider, false);
    }

    @EventLog(indexed = 3)
    public void ReserveUpdated(Address _reserve, BigInteger _liquidityRate, BigInteger _borrowRate,
            BigInteger _liquidityCumulativeIndex, BigInteger _borrowCumulativeIndex) {
    }

    @EventLog(indexed = 3)
    public void InterestTransfer(BigInteger _amount, Address _reserve, Address _initiatiator) {
    }

    protected String reservePrefix(Address reserve) {
        return ("reserve" + "|" + reserve.toString());
    }

    protected String userReservePrefix(Address reserve, Address user) {
        return ("userReserve" + "|" + reserve.toString() + "|" + user.toString());
    }

    protected void updateLastUpdateTimestamp(Address reserveAddress, BigInteger lastUpdateTimestamp) {
        String prefix = reservePrefix(reserveAddress);
        reserve.lastUpdateTimestamp.at(prefix).set(lastUpdateTimestamp);
    }

    protected void updateLiquidityRate(Address reserveAddress, BigInteger liquidityRate) {
        String prefix = reservePrefix(reserveAddress);
        reserve.liquidityRate.at(prefix).set(liquidityRate);
    }

    protected void updateBorrowRate(Address reserveAddress, BigInteger borrowRate) {
        String prefix = reservePrefix(reserveAddress);
        reserve.borrowRate.at(prefix).set(borrowRate);
    }

    protected void updateBorrowCumulativeIndex(Address reserveAddress, BigInteger borrowCumulativeIndex) {
        String prefix = reservePrefix(reserveAddress);
        reserve.borrowCumulativeIndex.at(prefix).set(borrowCumulativeIndex);

    }

    protected void updateLiquidityCumulativeIndex(Address reserveAddress, BigInteger liquidityCumulativeIndex) {
        String prefix = reservePrefix(reserveAddress);
        reserve.liquidityCumulativeIndex.at(prefix).set(liquidityCumulativeIndex);
    }

    protected void updateUserLastUpdateTimestamp(Address reserve, Address user,
            BigInteger lastUpdateTimestamp) {
        String prefix = userReservePrefix(reserve, user);
        userReserve.lastUpdateTimestamp.at(prefix).set(lastUpdateTimestamp);
    }

    protected void updateUserOriginationFee(Address reserve, Address user, BigInteger originationFee) {
        String prefix = userReservePrefix(reserve, user);
        userReserve.originationFee.at(prefix).set(originationFee);
    }

    protected boolean checkReserve(Address reserve) {
        List<Address> reserveList = getReserves();
        return reserveList.contains(reserve);
    }

    protected boolean isValidReserve(Address _reserve) {
        int reserveListSize = reserveList.size();
        for (int i = 0; i < reserveListSize; i++) {
            Address reserve = reserveList.get(i);
            if (_reserve.equals(reserve)) {
                return true;
            }
        }
        return false;
    }

    protected void addNewReserve(Address res) {
        reserveList.add(res);
    }

    protected BigInteger calculateLinearInterest(BigInteger rate, BigInteger lastUpdateTimestamp) {
        BigInteger timeDifference = (TimeConstants.getBlockTimestamp().subtract(lastUpdateTimestamp)).divide(
                TimeConstants.SECOND);
        BigInteger timeDelta = exaDivide(timeDifference, SECONDS_PER_YEAR);
        return exaMultiply(rate, timeDelta).add(ICX);
    }

    protected BigInteger calculateCompoundedInterest(BigInteger rate, BigInteger lastUpdateTimestamp) {
        BigInteger timeDifference = (TimeConstants.getBlockTimestamp().subtract(lastUpdateTimestamp)).divide(
                TimeConstants.SECOND);
        BigInteger ratePerSecond = rate.divide(SECONDS_PER_YEAR);
        return exaPow(ratePerSecond.add(ICX), timeDifference);
    }

    protected void updateCumulativeIndexes(Address _reserve) {

        BigInteger totalBorrows = getReserveTotalBorrows(_reserve);

        if (totalBorrows.compareTo(BigInteger.ZERO) > 0) {
            String prefix = reservePrefix(_reserve);
            BigInteger liquidityRate = reserve.liquidityRate.at(prefix).get();
            BigInteger lastUpdateTimestamp = reserve.lastUpdateTimestamp.at(prefix).get();
            BigInteger liquidityCumulativeIndex = reserve.liquidityCumulativeIndex.at(prefix).get();
            BigInteger borrowRate = reserve.borrowRate.at(prefix).get();
            BigInteger borrowCumulativeIndex = reserve.borrowCumulativeIndex.at(prefix).get();

            BigInteger cumulatedLiquidityInterest = calculateLinearInterest(liquidityRate, lastUpdateTimestamp);
            updateLiquidityCumulativeIndex(_reserve, exaMultiply(cumulatedLiquidityInterest, liquidityCumulativeIndex));
            BigInteger cumulatedBorrowInterest = calculateCompoundedInterest(borrowRate, lastUpdateTimestamp);
            updateBorrowCumulativeIndex(_reserve, exaMultiply(cumulatedBorrowInterest, borrowCumulativeIndex));

        }
    }

    protected BigInteger getReserveTotalLiquidity(Address reserve) {
        return getReserveAvailableLiquidity(reserve).add(getReserveTotalBorrows(reserve));
    }

    protected BigInteger getReserveTotalBorrows(Address reserveAddress) {
        String prefix = reservePrefix(reserveAddress);
        return call(BigInteger.class, reserve.dTokenAddress.at(prefix).get(),
                "principalTotalSupply");
    }

    protected void updateReserveInterestRatesAndTimestampInternal(Address _reserve, BigInteger liquidityAdded,
            BigInteger liquidityTaken) {
        BigInteger totalBorrows = getReserveTotalBorrows(_reserve);
        String prefix = reservePrefix(_reserve);
        BigInteger liquidityCumulativeIndex = reserve.liquidityCumulativeIndex.at(prefix).get();
        BigInteger borrowCumulativeIndex = reserve.borrowCumulativeIndex.at(prefix).get();

        Map<String, BigInteger> rate = calculateInterestRates(_reserve, getReserveAvailableLiquidity(_reserve)
                .add(liquidityAdded)
                .subtract(liquidityTaken), totalBorrows);
        updateLiquidityRate(_reserve, rate.get("liquidityRate"));
        updateBorrowRate(_reserve, rate.get("borrowRate"));
        updateLastUpdateTimestamp(_reserve, TimeConstants.getBlockTimestamp());

        ReserveUpdated(_reserve, rate.get("liquidityRate"), rate.get("borrowRate"),liquidityCumulativeIndex,
                borrowCumulativeIndex);

    }

    protected BigInteger getCurrentBorrowRate(Address _reserve) {
        String prefix = reservePrefix(_reserve);
        return reserve.borrowRate.at(prefix).getOrDefault(BigInteger.ZERO);
    }

    protected void updateUserStateOnBorrowInternal(Address reserve, Address user, BigInteger borrowFee) {
        BigInteger userPreviousOriginationFee = getUserOriginationFeeProxy(reserve,user);
        updateUserOriginationFee(reserve, user, userPreviousOriginationFee.add(borrowFee));
        updateUserLastUpdateTimestamp(reserve, user, TimeConstants.getBlockTimestamp());
    }

    protected void updateUserStateOnRepayInternal(Address reserve, Address user,
            BigInteger originationFeeRepaid) {
        BigInteger originationFee = getUserOriginationFeeProxy(reserve,user);
        updateUserOriginationFee(reserve, user, originationFee.subtract(originationFeeRepaid));
        updateUserLastUpdateTimestamp(reserve, user, TimeConstants.getBlockTimestamp());
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

    protected void updateUserStateOnLiquidationInternal(Address reserve, Address user,
            BigInteger feeLiquidated) {

        if (feeLiquidated.compareTo(BigInteger.ZERO) > 0) {
            BigInteger originationFee = getUserOriginationFee(reserve, user);
            updateUserOriginationFee(reserve, user, originationFee.subtract(feeLiquidated));
        }

        updateUserLastUpdateTimestamp(reserve, user, TimeConstants.getBlockTimestamp());
    }

    protected Map<String, BigInteger> calculateInterestRates(Address reserve, BigInteger availableLiquidity,
            BigInteger totalBorrows) {
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