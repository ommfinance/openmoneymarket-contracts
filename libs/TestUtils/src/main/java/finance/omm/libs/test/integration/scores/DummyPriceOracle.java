package finance.omm.libs.test.integration.scores;

import score.annotation.External;

import java.math.BigInteger;
import java.util.Map;

public interface DummyPriceOracle {

    @External
    void set_reference_data(String _base, BigInteger _price);

    @External(readonly = true)
    Map<String, BigInteger> get_reference_data(String _base, String _quote);
}
