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

import static finance.omm.score.core.lendingpoolcore.reservedata.AbstractReserve.addDataToReserve;
import static finance.omm.score.core.lendingpoolcore.reservedata.AbstractReserve.createReserveDataObject;
import static finance.omm.score.core.lendingpoolcore.reservedata.AbstractReserve.getDataFromReserve;
import static finance.omm.score.core.lendingpoolcore.userreserve.AbstractUserReserve.getDataFromUserReserve;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaMultiply;

public class LendingPoolCoreImpl extends AbstractLendingPoolCore {

    public LendingPoolCoreImpl(Address addressProvider) {
        super(addressProvider);
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        if (_borrowThreshold.compareTo(BigInteger.ZERO) < 0 || _borrowThreshold.compareTo(ICX) > 0) {
            Context.revert(TAG + " : Invalid borrow threshold value)");
        }
        reserve.borrowThreshold.at(prefix).set(_borrowThreshold);
    }

    @External
    public void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLTVasCollateral) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.baseLTVasCollateral.at(prefix).set(_baseLTVasCollateral);
    }

    @External
    public void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.liquidationThreshold.at(prefix).set(_liquidationThreshold);
    }

    @External
    public void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.liquidationBonus.at(prefix).set(_liquidationBonus);
    }

    @External
    public void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.borrowingEnabled.at(prefix).set(_borrowingEnabled);
    }

    @External
    public void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.usageAsCollateralEnabled.at(prefix).set(_usageAsCollateralEnabled);
    }

    @External
    public void updateIsFreezed(Address _reserve, boolean _isFreezed) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.isFreezed.at(prefix).set(_isFreezed);
    }

    @External
    public void updateIsActive(Address _reserve, boolean _isActive) {
        onlyGovernance();
        String prefix = reservePrefix(_reserve);
        reserve.isActive.at(prefix).set(_isActive);
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
        String prefix = reservePrefix(_reserve);
        return reserve.liquidityCumulativeIndex.at(prefix).getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getReserveBorrowCumulativeIndex(Address _reserve) {
        String prefix = reservePrefix(_reserve);
        return reserve.borrowCumulativeIndex.at(prefix).getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public boolean isReserveBorrowingEnabled(Address _reserve) {
        String prefix = reservePrefix(_reserve);
        return reserve.borrowingEnabled.at(prefix).get();
    }

    @External
    public void addReserveData(ReserveAttributes _reserve) {
        onlyGovernance();
        ReserveDataObject reserveDataObj = createReserveDataObject(_reserve);
        if (!isValidReserve(reserveDataObj.reserveAddress)) {
            addNewReserve(reserveDataObj.reserveAddress);
        }
        String prefix = reservePrefix(reserveDataObj.reserveAddress);
        addDataToReserve(prefix, reserve, reserveDataObj);
    }

    @External(readonly = true)
    public Map<String, Object> getReserveData(Address _reserve) {
        Map<String, Object> response = new HashMap<>();
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);
            response = getDataFromReserve(prefix, reserve);
            BigInteger availableLiquidity = getReserveAvailableLiquidity(_reserve);
            BigInteger totalBorrows = getReserveTotalBorrows(_reserve);
            BigInteger totalLiquidity = availableLiquidity.add(totalBorrows);

            response.put("totalLiquidity", totalLiquidity);
            response.put("availableLiquidity", availableLiquidity);
            response.put("totalBorrows", totalBorrows);

            BigInteger borrowThreshold = (BigInteger) response.get("borrowThreshold");

            BigInteger availableBorrows = exaMultiply(borrowThreshold,
                    totalLiquidity.subtract(totalBorrows));
            response.put("availableBorrows", availableBorrows.max(BigInteger.ZERO));
        }
        return response;
    }

    // for borrow only
    @External(readonly = true)
    public Map<String,Object> getReserveBorrowData(Address _reserve){
        Map<String,Object> response = new HashMap<>();
        if (isValidReserve(_reserve)){
            String prefix = reservePrefix(_reserve);
            response.put("isActive",reserve.isActive.at(prefix).get());
            response.put("isFreezed",reserve.isFreezed.at(prefix).get());
            response.put("borrowingEnabled",reserve.borrowingEnabled.at(prefix).get());
            response.put("decimals",reserve.decimals.at(prefix).get());

            BigInteger availableLiquidity = getReserveAvailableLiquidity(_reserve);
            BigInteger totalBorrows = getReserveTotalBorrows(_reserve);

            BigInteger totalLiquidity = availableLiquidity.add(totalBorrows);

            BigInteger borrowThreshold = reserve.borrowThreshold.at(prefix).get();

            response.put("totalLiquidity", totalLiquidity);
            response.put("availableLiquidity", availableLiquidity);
            response.put("totalBorrows", totalBorrows);

            BigInteger availableBorrows = exaMultiply(borrowThreshold,
                    totalLiquidity.subtract(totalBorrows));
            response.put("availableBorrows", availableBorrows.max(BigInteger.ZERO));
        }
        return response;
    }


    @External(readonly = true)
    // deposit
    public Map<String,Object> getReserveValues(Address _reserve){
        Map<String, Object> response = new HashMap<>();
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);
            response.put("isActive",reserve.isActive.at(prefix).get());
            response.put("isFreezed",reserve.isFreezed.at(prefix).get());
            response.put("oTokenAddress",reserve.oTokenAddress.at(prefix).get());
            response.put("isReserveBorrowingEnabled",reserve.borrowingEnabled.at(prefix).get());
        }
        return response;
    }


    @External(readonly = true)
    public Map<String, Object> getReserveDataProxy(Address _reserve) {
        Map<String, Object> response = new HashMap<>();
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);

            BigInteger availableLiquidity = getReserveAvailableLiquidity(_reserve);
            BigInteger totalBorrows = getReserveTotalBorrows(_reserve);
            BigInteger totalLiquidity = availableLiquidity.add(totalBorrows);

            response.put("totalLiquidity", totalLiquidity);
            response.put("availableLiquidity", availableLiquidity);
            response.put("totalBorrows", totalBorrows);

            BigInteger borrowThreshold = reserve.borrowThreshold.at(prefix).get();

            BigInteger availableBorrows = exaMultiply(borrowThreshold,
                    totalLiquidity.subtract(totalBorrows));
            response.put("availableBorrows", availableBorrows.max(BigInteger.ZERO));
        }
        return response;
    }


    @External(readonly = true)
    public Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user) {
        Map<String, BigInteger> response = new HashMap<>();
        if (isValidReserve(_reserve)) {
            String prefix = userReservePrefix(_reserve, _user);
            response = getDataFromUserReserve(prefix, userReserve);
        }
        return response;
    }

    @External(readonly = true)
    public BigInteger getNormalizedIncome(Address _reserve) {
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);
            BigInteger liquidityRate = reserve.liquidityRate.at(prefix).get();
            BigInteger lastUpdateTimestamp = reserve.lastUpdateTimestamp.at(prefix).get();
            BigInteger liquidityCumulativeIndex = reserve.liquidityCumulativeIndex.at(prefix).get();
            BigInteger interest = calculateLinearInterest(liquidityRate,
                    lastUpdateTimestamp);
            return exaMultiply(interest, liquidityCumulativeIndex);
        }
        return null;
    }

    @External(readonly = true)
    public BigInteger getNormalizedDebt(Address _reserve) {
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);
            BigInteger borrowRate = reserve.borrowRate.at(prefix).get();
            BigInteger lastUpdateTimestamp = reserve.lastUpdateTimestamp.at(prefix).get();
            BigInteger borrowCumulativeIndex = reserve.borrowCumulativeIndex.at(prefix).get();
            BigInteger interest = calculateCompoundedInterest(borrowRate, lastUpdateTimestamp);

            return exaMultiply(interest, borrowCumulativeIndex);
        }

        return null;
    }

    @External(readonly = true)
    public BigInteger getReserveAvailableLiquidity(Address _reserve) {
        return call(BigInteger.class, _reserve,
                "balanceOf", Context.getAddress());
    }

    @External(readonly = true)
    public Map<String, Object> getReserveConfiguration(Address _reserve) {
        Map<String, Object> response = new HashMap<>();
        if (isValidReserve(_reserve)) {
            String prefix = reservePrefix(_reserve);
            response.put("decimals",reserve.decimals.at(prefix).get());
            response.put("baseLTVasCollateral", reserve.baseLTVasCollateral.at(prefix).get());
            response.put("liquidationThreshold", reserve.liquidationThreshold.at(prefix).get());
            response.put("usageAsCollateralEnabled", reserve.usageAsCollateralEnabled.at(prefix).get());
            response.put("isActive",reserve.isActive.at(prefix).get());
            response.put("borrowingEnabled", reserve.borrowingEnabled.at(prefix).get());
            response.put("liquidationBonus",reserve.liquidationBonus.at(prefix).get());

        }
        return  response;
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
        call(_reserve, "transfer", _user, _amount, _data);
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
        BigInteger division = balanceIncrease.divide(BigInteger.TEN);
        if (division.compareTo(BigInteger.ZERO) > 0) {
            call(_reserve, "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            InterestTransfer(division, _reserve, _user);
        }
        updateCumulativeIndexes(_reserve);
        call(getReserveDTokenAddress(_reserve), "mintOnBorrow", _user, _amountBorrowed, balanceIncrease);
        updateUserStateOnBorrowInternal(_reserve, _user, _borrowFee);

        updateReserveInterestRatesAndTimestampInternal(_reserve, BigInteger.ZERO, _amountBorrowed);

        BigInteger currentBorrowRate = getCurrentBorrowRate(_reserve);
        call(_reserve, "transfer", _user, _amountBorrowed, "userBorrow".getBytes());
        return Map.of(
                "currentBorrowRate", currentBorrowRate,
                "balanceIncrease", balanceIncrease
        );

    }

    @External
    public void updateStateOnRepay(Address _reserve, Address _user, BigInteger _paybackAmountMinusFees,
            BigInteger _originationFeeRepaid, BigInteger _balanceIncrease, boolean _repaidWholeLoan) {
        onlyLendingPool();
        BigInteger division = _balanceIncrease.divide(BigInteger.TEN);
        if (division.compareTo(BigInteger.ZERO) > 0) {
            call(_reserve, "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            InterestTransfer(division, _reserve, _user);
        }
        updateCumulativeIndexes(_reserve);
        String prefix = reservePrefix(_reserve);
        call(reserve.dTokenAddress.at(prefix).get(),
                "burnOnRepay", _user, _paybackAmountMinusFees, _balanceIncrease);
        updateUserStateOnRepayInternal(_reserve, _user, _originationFeeRepaid);
        updateReserveInterestRatesAndTimestampInternal(_reserve, _paybackAmountMinusFees, BigInteger.ZERO);
    }

    @External(readonly = true)
    public Address getReserveOTokenAddress(Address _reserve) {
        String prefix = reservePrefix(_reserve);
        return reserve.oTokenAddress.at(prefix).get();
    }

    @External(readonly = true)
    public Address getReserveDTokenAddress(Address _reserve) {
        String prefix = reservePrefix(_reserve);
        return reserve.dTokenAddress.at(prefix).get();
    }

    @External
    public void updateStateOnLiquidation(Address _principalReserve, Address _collateralReserve, Address _user,
            BigInteger _amountToLiquidate, BigInteger _collateralToLiquidate, BigInteger _feeLiquidated,
            BigInteger _liquidatedCollateralForFee, BigInteger _balanceIncrease) {

        onlyLiquidationManager();
        BigInteger division = _balanceIncrease.divide(BigInteger.TEN);
        if (division.compareTo(BigInteger.ZERO) > 0) {
            call(_principalReserve, "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), division);
            InterestTransfer(division, _principalReserve, _user);
        }
        updatePrincipalReserveStateOnLiquidationInternal(_principalReserve, _user, _amountToLiquidate,
                _balanceIncrease);

        updateCollateralReserveStateOnLiquidationInternal(_collateralReserve);
        updateUserStateOnLiquidationInternal(_principalReserve, _user, _feeLiquidated);
        updateReserveInterestRatesAndTimestampInternal(_principalReserve, _amountToLiquidate, BigInteger.ZERO);
        updateReserveInterestRatesAndTimestampInternal(_collateralReserve, BigInteger.ZERO,
                _collateralToLiquidate.add(_liquidatedCollateralForFee));

    }

    @External(readonly = true)
    public BigInteger getUserUnderlyingAssetBalance(Address _reserve, Address _user) {
        String prefix = reservePrefix(_reserve);
        return call(BigInteger.class, reserve.oTokenAddress.at(prefix).get(),
                "balanceOf", _user);
    }

    @External(readonly = true)
    public BigInteger getUserUnderlyingBorrowBalance(Address _reserve, Address _user) {
        String prefix = reservePrefix(_reserve);
        return call(BigInteger.class, reserve.dTokenAddress.at(prefix).get(),
                "balanceOf", _user);
    }

    @External(readonly = true)
    public BigInteger getUserOriginationFee(Address _reserve, Address _user) {
        Map<String, BigInteger> userReserveData = getUserReserveData(_reserve, _user);
        return userReserveData.get("originationFee");
    }
    @External(readonly = true)
    public BigInteger getUserOriginationFeeProxy(Address _reserve, Address _user) {
        if (isValidReserve(_reserve)) {
            String prefix = userReservePrefix(_reserve, _user);
            return userReserve.originationFee.at(prefix).getOrDefault(BigInteger.ZERO);
        }
        return null;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserBasicReserveData(Address _reserve, Address _user) {
        BigInteger underlyingBalance = getUserUnderlyingAssetBalance(_reserve, _user);
        BigInteger compoundedBorrowBalance = getUserUnderlyingBorrowBalance(_reserve, _user);
        return Map.of(
                "underlyingBalance", underlyingBalance,
                "compoundedBorrowBalance", compoundedBorrowBalance,
                "originationFee", getUserOriginationFee(_reserve, _user)
        );
    }

    // for borrow only
    @External(readonly = true)
    public Map<String,Object> getUserBasicReserveDataProxy(Address _reserve, Address _user){
        BigInteger underlyingBalance = getUserUnderlyingAssetBalance(_reserve, _user);
        BigInteger compoundedBorrowBalance = getUserUnderlyingBorrowBalance(_reserve, _user);
        String prefix = reservePrefix(_reserve);
        return Map.of(
                "underlyingBalance", underlyingBalance,
                "compoundedBorrowBalance", compoundedBorrowBalance,
                "originationFee", getUserOriginationFeeProxy(_reserve, _user),
                "decimals", reserve.decimals.at(prefix).get(),
                "baseLTVasCollateral", reserve.baseLTVasCollateral.at(prefix).get(),
                "liquidationThreshold", reserve.liquidationThreshold.at(prefix).get(),
                "usageAsCollateralEnabled", reserve.usageAsCollateralEnabled.at(prefix).get()
        );
    }

    // method made for repay only
    @External(readonly = true)
    public Map<String,Object> getUserAndReserveBasicData(Address _reserve, Address _user){
        Map<String, Object> reserveData = new HashMap<>() ;
        if (isValidReserve(_reserve)){
            String prefix = reservePrefix(_reserve);
            reserveData.put("isActive", reserve.isActive.at(prefix).get());
            reserveData.put("originationFee",getUserOriginationFeeProxy(_reserve,_user));
            reserveData.put("borrowBalances",getUserBorrowBalances(_reserve,_user));
        }
        return  reserveData;
    }


    @External(readonly = true)
    public Map<String, BigInteger> getUserBorrowBalances(Address _reserve, Address _user) {
        String prefix = reservePrefix(_reserve);
        Address dTokenAddr = reserve.dTokenAddress.at(prefix).get();
        BigInteger principalBorrowBalance = call(BigInteger.class, dTokenAddr,
                "principalBalanceOf", _user);

        if (principalBorrowBalance.equals(BigInteger.ZERO)) {
            return Map.of(
                    "principalBorrowBalance", BigInteger.ZERO,
                    "compoundedBorrowBalance", BigInteger.ZERO,
                    "borrowBalanceIncrease", BigInteger.ZERO
            );
        }
        BigInteger compoundedBorrowBalance = call(BigInteger.class, dTokenAddr,
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
