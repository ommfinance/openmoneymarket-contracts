package finance.omm.core.score.interfaces;

import java.math.BigInteger;

public interface PriceOracle {

    String name();

    void setOMMPool(String _value);

    String getOMMPool();

    BigInteger get_reference_data(String _base, String _quote);
}
