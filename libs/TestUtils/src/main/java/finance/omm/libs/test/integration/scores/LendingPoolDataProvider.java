package finance.omm.libs.test.integration.scores;

import score.Address;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface LendingPoolDataProvider {

    String name();

    void setSymbol(Address _reserve, String _sym);

    Map<String, Object> getUserAccountData(Address _user);

    String getSymbol(Address _reserve);

    Map<String, BigInteger> getReserveAccountData();

    Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user);

    Map<String, Object> getReserveData(Address _reserve);

    Map<String, Map<String, Object>> getAllReserveData();

    Map<String, Map<String, Object>> getAllReserveConfigurationData();

    Map<String, Map<String, BigInteger>> getUserAllReserveData(Address _user);

    Map<String, Object> getUserLiquidationData(Address _user);

    Map<String, Map<String, Object>> liquidationList(BigInteger _index);

    boolean balanceDecreaseAllowed(Address _reserve, Address _user, BigInteger _amount);

    BigInteger calculateCollateralNeededUSD(Address _reserve, BigInteger _amount, BigInteger _fee,
                                            BigInteger _userCurrentBorrowBalanceUSD,
                                            BigInteger _userCurrentFeesUSD, BigInteger _userCurrentLtv);

    Map<String, Object> getReserveConfigurationData(Address _reserve);

    BigInteger getLoanOriginationFeePercentage();

    BigInteger getRealTimeDebt(Address _reserve, Address _user);

    List<Map<String, BigInteger>> getUserUnstakeInfo(Address _address);

}
