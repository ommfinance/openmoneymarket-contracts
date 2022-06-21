package finance.omm.score.core.oracle.token;

import java.math.BigInteger;
import score.Address;

public interface Token {
    String getName();
    String getPriceOracleKey();
    BigInteger convert(Address dataSource, BigInteger amount, BigInteger decimals);
}
