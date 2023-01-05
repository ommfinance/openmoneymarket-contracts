package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.Constant;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.governance.ReserveAttributes;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ScoreInterface(suffix = "Client")
public interface LendingPoolCore {

    String name();

    void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold);

    void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLTVasCollateral);

    void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold);

    void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus);

    void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled);

    void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled);

    void updateIsFreezed(Address _reserve, boolean _isFreezed);

    void updateIsActive(Address _reserve, boolean _isActive);

    List<Address> getReserves();

    BigInteger getReserveLiquidityCumulativeIndex(Address _reserve);

    BigInteger getReserveBorrowCumulativeIndex(Address _reserve);

    boolean isReserveBorrowingEnabled(Address _reserve);

    void addReserveData(ReserveAttributes _reserve);

    Map<String, Object> getReserveData(Address _reserve);

    Map<String, Object> getReserveValues(Address _reserve);

    Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user);

    BigInteger getNormalizedIncome(Address _reserve);

    BigInteger getNormalizedDebt(Address _reserve);

    BigInteger getReserveAvailableLiquidity(Address _reserve);

    Map<String, Object> getReserveConfiguration(Address _reserve);

    void setReserveConstants(Constant[] _constants);

    Map<String, Object> getReserveConstants(Address _reserve);

    void transferToUser(Address _reserve, Address _user, BigInteger _amount, @Optional byte[] _data);

    void liquidateFee(Address _reserve, BigInteger _amount, Address _destination);

    void updateStateOnDeposit(Address _reserve, Address _user, BigInteger _amount);

    void updateStateOnRedeem(Address _reserve, Address _user, BigInteger _amountRedeemed);

    Map<String, BigInteger> updateStateOnBorrow(Address _reserve, Address _user, BigInteger _amountBorrowed, BigInteger _borrowFee);

    void updateStateOnRepay(Address _reserve, Address _user, BigInteger _paybackAmountMinusFees,
                            BigInteger _originationFeeRepaid, BigInteger _balanceIncrease, boolean _repaidWholeLoan);

    void updateStateOnLiquidation(Address _principalReserve, Address _collateralReserve, Address _user,
                                  BigInteger _amountToLiquidate, BigInteger _collateralToLiquidate, BigInteger _feeLiquidated,
                                  BigInteger _liquidatedCollateralForFee, BigInteger _balanceIncrease);

    Address getReserveOTokenAddress(Address _reserve);

    Address getReserveDTokenAddress(Address _reserve);

    BigInteger getUserUnderlyingAssetBalance(Address _reserve, Address _user);

    BigInteger getUserUnderlyingBorrowBalance(Address _reserve, Address _user);

    BigInteger getUserOriginationFee(Address _reserve, Address _user);

    Map<String, BigInteger> getUserBasicReserveData(Address _reserve, Address _user);

    Map<String, BigInteger> getUserBorrowBalances(Address _reserve, Address _user);

    void updatePrepDelegations(PrepDelegations[] _delegations);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    void fallback();

}
