package finance.omm.score.core.lendingpoolcore;


import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.Constant;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.score.core.lendingpoolcore.exception.LendingPoolCoreException;
import finance.omm.score.core.lendingpoolcore.reservedata.ReserveDataObject;
import score.Context;
import score.Address;
import score.DictDB;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import static finance.omm.score.core.lendingpoolcore.reservedata.AbstractReserve.*;
import static finance.omm.score.core.lendingpoolcore.userreserve.AbstractUserReserve.getDataFromUserReserve;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaMultiply;

public class LendingPoolCoreImpl extends AbstractLendingPoolCore {

    public LendingPoolCoreImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        if (_borrowThreshold.compareTo(BigInteger.ZERO) < 0 || _borrowThreshold.compareTo(ICX) > 0) {
            Context.revert(TAG + " : Invalid borrow threshold value)");
        }
        reserve.getItem(prefix).borrowThreshold.set(_borrowThreshold);
    }

    @External
    public void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLTVasCollateral) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).baseLTVasCollateral.set(_baseLTVasCollateral);
    }

    @External
    public void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).liquidationThreshold.set(_liquidationThreshold);
    }

    @External
    public void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).liquidationBonus.set(_liquidationBonus);
    }

    @External
    public void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).borrowingEnabled.set(_borrowingEnabled);
    }

    @External
    public void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).usageAsCollateralEnabled.set(_usageAsCollateralEnabled);
    }

    @External
    public void updateIsFreezed(Address _reserve, boolean _isFreezed) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).isFreezed.set(_isFreezed);
    }

    @External
    public void updateIsActive(Address _reserve, boolean _isActive) {
        onlyGovernance();
        byte[] prefix = reservePrefix(_reserve);
        reserve.getItem(prefix).isActive.set(_isActive);
    }

    @External(readonly = true)
    public List<Address> getReserves() {
        List<Address> reserves = new ArrayList<>();
        int reserveListSize = reserveList.size();
        for (int i = 0; i < reserveListSize; i++) {
            Address reserve = reserveList.get(i);
            reserves.add(reserve);
        }
        return reserves;
    }

    @External(readonly = true)
    public BigInteger getReserveLiquidityCumulativeIndex(Address _reserve) {
        byte[] prefix = reservePrefix(_reserve);
        return reserve.getItem(prefix).liquidityCumulativeIndex.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getReserveBorrowCumulativeIndex(Address _reserve) {
        byte[] prefix = reservePrefix(_reserve);
        return reserve.getItem(prefix).borrowCumulativeIndex.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public boolean isReserveBorrowingEnabled(Address _reserve) {
        return (boolean) getReserveData(_reserve).get("borrowingEnabled");
    }

    @External
    public void addReserveData(ReserveAttributes _reserve) {
        onlyGovernance();
        ReserveDataObject reserveDataObj = createReserveDataObject(_reserve);
        if (!checkReserve(reserveDataObj.reserveAddress)) {
            addNewReserve(reserveDataObj.reserveAddress);
        }
        byte[] prefix = reservePrefix(reserveDataObj.reserveAddress);
        addDataToReserve(prefix, reserve, reserveDataObj);
    }

    @External(readonly = true)
    public Map<String, Object> getReserveData(Address _reserve) {
        Map<String, Object> response = new HashMap<>();
        if (checkReserve(_reserve)) {
            byte[] prefix = reservePrefix(_reserve);
            response = getDataFromReserve(prefix, reserve);
            response.put("totalLiquidity", getReserveTotalLiquidity(_reserve));
            response.put("availableLiquidity", getReserveAvailableLiquidity(_reserve));
            response.put("totalBorrows", getReserveTotalBorrows(_reserve));

            BigInteger totalLiquidity = (BigInteger) response.get("totalLiquidity");
            BigInteger totalBorrows = (BigInteger) response.get("totalBorrows");
            BigInteger borrowThreshold = (BigInteger) response.get("borrowThreshold");

            BigInteger availableBorrows = exaMultiply(borrowThreshold,
                    totalLiquidity.subtract(totalBorrows));
            response.put("availableBorrows", availableBorrows.max(BigInteger.ZERO));
        }
        return response;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user) {
        Map<String, BigInteger> response = new HashMap<>();
        if (checkReserve(_reserve)) {
            byte[] prefix = userReservePrefix(_reserve, _user);
            response = getDataFromUserReserve(prefix, userReserve);
        }
        return response;

    }

    @External(readonly = true)
    public BigInteger getNormalizedIncome(Address _reserve) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        BigInteger interest = calculateLinearInterest((BigInteger) reserveData.get("liquidityRate"),
                (BigInteger) reserveData.get("lastUpdateTimestamp"));
        return exaMultiply(interest, (BigInteger) reserveData.get("liquidityCumulativeIndex"));
    }

    @External(readonly = true)
    public BigInteger getNormalizedDebt(Address _reserve) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        BigInteger interest = calculateCompoundedInterest((BigInteger) reserveData.get("borrowRate"),
                (BigInteger) reserveData.get("lastUpdateTimestamp"));
        return exaMultiply(interest, (BigInteger) reserveData.get("borrowCumulativeIndex"));
    }

    @External(readonly = true)
    public BigInteger getReserveAvailableLiquidity(Address _reserve) {
        return call(BigInteger.class, _reserve,
                "balanceOf", Context.getAddress());
    }

    @External(readonly = true)
    public Map<String, Object> getReserveConfiguration(Address _reserve) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        return Map.of(
                "decimals", reserveData.get("decimals"),
                "baseLTVasCollateral", reserveData.get("baseLTVasCollateral"),
                "liquidationThreshold", reserveData.get("liquidationThreshold"),
                "usageAsCollateralEnabled", reserveData.get("usageAsCollateralEnabled"),
                "isActive", reserveData.get("isActive"),
                "borrowingEnabled", reserveData.get("borrowingEnabled"),
                "liquidationBonus", reserveData.get("liquidationBonus")
        );
    }

    @External
    public void setReserveConstants(Constant[] _constants) {
        onlyGovernance();
        List<Address> reserveList = getReserves();
        for (Constant constant : _constants) {
            Address reserve = constant.reserve;
            if (!reserveList.contains(reserve)) {
                Context.revert(TAG + " invalid reserve ");
            }
            updateCumulativeIndexes(reserve);
            DictDB<String, BigInteger> dictDB = this.constants.at(reserve);
            dictDB.set("optimalUtilizationRate", constant.optimalUtilizationRate);
            dictDB.set("baseBorrowRate", constant.baseBorrowRate);
            dictDB.set("slopeRate1", constant.slopeRate1);
            dictDB.set("slopeRate2", constant.slopeRate2);
            updateReserveInterestRatesAndTimestampInternal(reserve, BigInteger.ZERO, BigInteger.ZERO);
        }
    }

    @External(readonly = true)
    public Map<String, Object> getReserveConstants(Address _reserve) {
        DictDB<String, BigInteger> dictDB = this.constants.at(_reserve);
        return Map.of(
                "reserve", _reserve,
                "optimalUtilizationRate", dictDB.getOrDefault("optimalUtilizationRate", BigInteger.ZERO),
                "baseBorrowRate", dictDB.getOrDefault("baseBorrowRate", BigInteger.ZERO),
                "slopeRate1", dictDB.getOrDefault("slopeRate1", BigInteger.ZERO),
                "slopeRate2", dictDB.getOrDefault("slopeRate2", BigInteger.ZERO)
        );

    }

    @External
    public void transferToUser(Address _reserve, Address _user, BigInteger _amount, @Optional byte[] _data) {
        onlyLendingPool();
        call(_reserve,"transfer", _user, _amount, _data);
    }

    @External
    public void liquidateFee(Address _reserve, BigInteger _amount, Address _destination) {
        onlyLiquidationManager();
        call(_reserve, "transfer", _destination, _amount);
    }

    @External
    public void updateStateOnDeposit(Address _reserve, Address _user, BigInteger _amount) {
        onlyLendingPool();
        updateCumulativeIndexes(_reserve);
        updateReserveInterestRatesAndTimestampInternal(_reserve, _amount, BigInteger.ZERO);
    }

    @External
    public void updateStateOnRedeem(Address _reserve, Address _user, BigInteger _amountRedeemed) {
        onlyLendingPool();
        updateCumulativeIndexes(_reserve);
        updateReserveInterestRatesAndTimestampInternal(_reserve, BigInteger.ZERO, _amountRedeemed);
    }

    @External
    public Map<String, BigInteger> updateStateOnBorrow(Address _reserve,
            Address _user, BigInteger _amountBorrowed, BigInteger _borrowFee) {
        onlyLendingPool();
        BigInteger balanceIncrease = getUserBorrowBalances(_reserve, _user).get("borrowBalanceIncrease");
        if (balanceIncrease.compareTo(BigInteger.ZERO) > 0) {
            BigInteger division = balanceIncrease.divide(BigInteger.TEN);
            call(_reserve,
                    "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            InterestTransfer(division, _reserve, _user);
        }
        updateCumulativeIndexes(_reserve);
        call(getReserveDTokenAddress(_reserve), "mintOnBorrow", _user, _amountBorrowed, balanceIncrease);
        updateUserStateOnBorrowInternal(_reserve, _user, _amountBorrowed, balanceIncrease, _borrowFee);

        updateReserveInterestRatesAndTimestampInternal(_reserve, BigInteger.ZERO, _amountBorrowed);

        BigInteger currentBorrowRate = getCurrentBorrowRate(_reserve);

        return Map.of(
                "currentBorrowRate", currentBorrowRate,
                "balanceIncrease", balanceIncrease
        );

    }

    @External
    public void updateStateOnRepay(Address _reserve, Address _user, BigInteger _paybackAmountMinusFees,
        if (_balanceIncrease.compareTo(BigInteger.ZERO) > 0) {
            BigInteger division = _balanceIncrease.divide(BigInteger.TEN);
            call(_reserve,
                    "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            BigInteger _originationFeeRepaid, BigInteger _balanceIncrease, boolean _repaidWholeLoan) {
        onlyLendingPool();
            InterestTransfer(division, _reserve, _user);
        }
        updateCumulativeIndexes(_reserve);
        call((Address) getReserveData(_reserve).get("dTokenAddress"),
                "burnOnRepay", _user, _paybackAmountMinusFees, _balanceIncrease);
        updateUserStateOnRepayInternal(_reserve, _user, _paybackAmountMinusFees, _originationFeeRepaid,
                _balanceIncrease, _repaidWholeLoan);
        updateReserveInterestRatesAndTimestampInternal(_reserve, _paybackAmountMinusFees, BigInteger.ZERO);
    }

    @External(readonly = true)
    public Address getReserveOTokenAddress(Address _reserve) {
        byte[] prefix = reservePrefix(_reserve);
        return reserve.getItem(prefix).oTokenAddress.get();
    }

    @External(readonly = true)
    public Address getReserveDTokenAddress(Address _reserve) {
        byte[] prefix = reservePrefix(_reserve);
        return reserve.getItem(prefix).dTokenAddress.get();
    }

    @External
    public void updateStateOnLiquidation(Address _principalReserve, Address _collateralReserve, Address _user,
        if (_balanceIncrease.compareTo(BigInteger.ZERO) > 0) {
            BigInteger division = _balanceIncrease.divide(BigInteger.TEN);
            call(_principalReserve,
                    "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            BigInteger _amountToLiquidate, BigInteger _collateralToLiquidate, BigInteger _feeLiquidated,
            BigInteger _liquidatedCollateralForFee, BigInteger _balanceIncrease) {

        onlyLiquidationManager();
            InterestTransfer(division, _principalReserve, _user);
        }
        updatePrincipalReserveStateOnLiquidationInternal(_principalReserve, _user, _amountToLiquidate,
                _balanceIncrease);

        updateCollateralReserveStateOnLiquidationInternal(_collateralReserve);
        updateUserStateOnLiquidationInternal(_principalReserve, _user, _amountToLiquidate, _feeLiquidated,
                _balanceIncrease);
        updateReserveInterestRatesAndTimestampInternal(_principalReserve, _amountToLiquidate, BigInteger.ZERO);
        updateReserveInterestRatesAndTimestampInternal(_collateralReserve, BigInteger.ZERO,
                _collateralToLiquidate.add(_liquidatedCollateralForFee));

    }

    @External(readonly = true)
    public BigInteger getUserUnderlyingAssetBalance(Address _reserve, Address _user) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        return call(BigInteger.class, (Address) reserveData.get("oTokenAddress"),
                "balanceOf", _user);
    }

    @External(readonly = true)
    public BigInteger getUserUnderlyingBorrowBalance(Address _reserve, Address _user) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        return call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "balanceOf", _user);
    }

    @External(readonly = true)
    public BigInteger getUserOriginationFee(Address _reserve, Address _user) {
        Map<String, BigInteger> userReserveData = getUserReserveData(_reserve, _user);
        return userReserveData.get("originationFee");
    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserBasicReserveData(Address _reserve, Address _user) {
        Map<String, BigInteger> userReserveData = getUserReserveData(_reserve, _user);
        BigInteger underlyingBalance = getUserUnderlyingAssetBalance(_reserve, _user);
        BigInteger compoundedBorrowBalance = getUserUnderlyingBorrowBalance(_reserve, _user);
        return Map.of(
                "underlyingBalance", underlyingBalance,
                "compoundedBorrowBalance", compoundedBorrowBalance,
                "originationFee", userReserveData.get("originationFee")
        );
    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserBorrowBalances(Address _reserve, Address _user) {
        Map<String, Object> reserveData = getReserveData(_reserve);
        BigInteger principalBorrowBalance = call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "principalBalanceOf", _user);

        if (principalBorrowBalance.equals(BigInteger.ZERO)) {
            return Map.of(
                    "principalBorrowBalance", BigInteger.ZERO,
                    "compoundedBorrowBalance", BigInteger.ZERO,
                    "borrowBalanceIncrease", BigInteger.ZERO
            );
        }
        BigInteger compoundedBorrowBalance = call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "balanceOf", _user);
        BigInteger borrowBalanceIncrease = compoundedBorrowBalance.subtract(principalBorrowBalance);
        return Map.of(
                "principalBorrowBalance", principalBorrowBalance,
                "compoundedBorrowBalance", compoundedBorrowBalance,
                "borrowBalanceIncrease", borrowBalanceIncrease
        );

    }

    @External
    public void updatePrepDelegations(PrepDelegations[] _delegations) {
        onlyContractOrElseThrow(Contracts.DELEGATION, LendingPoolCoreException.unauthorized(
                "SenderNotAuthorized: (sender) " +
                        Context.getCaller() + " delegation " + getAddress(Contracts.DELEGATION.getKey())));
        call(getAddress(Contracts.STAKING.getKey()), "delegate", (Object) _delegations);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
    }

    @Payable
    public void fallback() {
    }

    private void onlyGovernance() {
        onlyContractOrElseThrow(Contracts.GOVERNANCE, LendingPoolCoreException.unauthorized(
                "SenderNotAuthorized: (sender) " +
                        Context.getCaller() + " governance " + getAddress(Contracts.GOVERNANCE.getKey())));
    }

    private void onlyLendingPool() {
        onlyContractOrElseThrow(Contracts.LENDING_POOL, LendingPoolCoreException.unauthorized(
                "SenderNotAuthorized: (sender) " +
                        Context.getCaller() + " lendingPool " + getAddress(Contracts.LENDING_POOL.getKey())));
    }

    private void onlyLiquidationManager() {
        onlyContractOrElseThrow(Contracts.LIQUIDATION_MANAGER, LendingPoolCoreException.unauthorized(
                "SenderNotAuthorized: (sender) " +
                        Context.getCaller() + " liquidation " + getAddress(Contracts.LIQUIDATION_MANAGER.getKey())));
    }
}
