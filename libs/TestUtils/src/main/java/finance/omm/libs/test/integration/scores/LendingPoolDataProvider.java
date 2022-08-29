package finance.omm.libs.test.integration.scores;

import java.util.Map;
import score.Address;

public interface LendingPoolDataProvider {

    void setSymbol(Address _reserve, String _sym);
    Map<String, Object> getUserLiquidationData(Address _user);
    Map<String, Object> getUserAccountData(Address _user);
}
