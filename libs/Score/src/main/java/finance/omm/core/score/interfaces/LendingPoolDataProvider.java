package finance.omm.core.score.interfaces;

import score.Address;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface LendingPoolDataProvider {

    String name();

    void setSymbol(Address _reserve, String _sym);

    String getSymbol(Address _reserve);

    String[] getRecipients();

    Map<String, BigInteger> getDistPercentages();

    Map<String, BigInteger> getReserveAccountData();

    Map<String, Object> getUserAccountData(Address _user);

    Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user);

    boolean balanceDecreaseAllowed(Address _reserve, Address _user, BigInteger _amount);

    BigInteger calculateCollateralNeededUSD(Address _reserve, BigInteger _amount, BigInteger _fee,
                                            BigInteger _userCurrentBorrowBalanceUSD,
                                            BigInteger _userCurrentFeesUSD, BigInteger _userCurrentLtv);

    Map<String, Map<String, BigInteger>> getUserAllReserveData(Address _user);

    Map<String, Object> getUserLiquidationData(Address _user);

    Map<String, Object> getReserveData(Address _reserve);

    Map<String, Map<String, Object>> getAllReserveData();

    Map<String, Object> getReserveConfigurationData(Address _reserve);

    Map<String, Map<String, Object>> getAllReserveConfigurationData();

    List<Map<String, BigInteger>> getUserUnstakeInfo(Address _address);

    BigInteger getLoanOriginationFeePercentage();

    BigInteger getRealTimeDebt(Address _reserve, Address _user);
}
