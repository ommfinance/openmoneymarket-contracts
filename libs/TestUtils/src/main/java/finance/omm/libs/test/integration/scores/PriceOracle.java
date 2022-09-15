package finance.omm.libs.test.integration.scores;

import java.math.BigInteger;

public interface PriceOracle {

    BigInteger get_reference_data(String _base, String _quote);
}
