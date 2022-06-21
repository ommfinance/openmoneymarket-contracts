package finance.omm.score.core.oracle.token;

import static finance.omm.utils.math.MathUtils.convertToExa;

import java.math.BigInteger;
import score.Address;

public class BaseToken implements Token {
    public String name;
    public String priceOracleKey;

    public BaseToken(String name, String priceOracleKey) {
        this.name = name;
        this.priceOracleKey = priceOracleKey;
    }

    public String getName() {
        return this.name;
    }

    public String getPriceOracleKey() {
        return this.priceOracleKey;
    }

    @Override
    public BigInteger convert(Address dataSource, BigInteger amount, BigInteger decimals) {
        return convertToExa(amount, decimals);
    }
}
