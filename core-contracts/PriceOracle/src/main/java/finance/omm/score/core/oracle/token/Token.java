package finance.omm.score.core.oracle.token;

import java.math.BigInteger;

public interface Token {

    String getName();

    String getPriceOracleKey();

    BigInteger convert(BigInteger amount, BigInteger decimals);
}
