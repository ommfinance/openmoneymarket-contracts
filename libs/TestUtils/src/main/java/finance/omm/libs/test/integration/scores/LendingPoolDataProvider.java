package finance.omm.libs.test.integration.scores;

import score.Address;

public interface LendingPoolDataProvider {

    void setSymbol(Address _reserve, String _sym);
}
