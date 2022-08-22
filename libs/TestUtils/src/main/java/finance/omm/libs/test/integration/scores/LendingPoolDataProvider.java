package finance.omm.libs.test.integration.scores;

import score.Address;

import java.math.BigInteger;
import java.util.Map;

public interface LendingPoolDataProvider {

    String name();
    void setSymbol(Address _reserve, String _sym);

    Map<String, BigInteger> getReserveAccountData();

    Map<String, Object> getUserAccountData(Address _user);

    Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user);

    Map<String, Object> getReserveData(Address _reserve);

    Map<String, Map<String, Object>> getAllReserveData();

    Map<String,Object> test(Address reserve);

    Map<String, Map<String, Object>> getAllReserveConfigurationData();
}
