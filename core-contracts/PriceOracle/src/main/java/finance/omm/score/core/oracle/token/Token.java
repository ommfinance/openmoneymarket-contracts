package finance.omm.score.core.oracle.token;

import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import java.math.BigInteger;
import score.Address;
import score.Context;

public class Token {
    public String name;
    public String priceOracleKey;

    public Token(String name, String priceOracleKey) {
        this.name = name;
        this.priceOracleKey = priceOracleKey;
    }

    public BigInteger convert(Address dataSource, BigInteger amount, BigInteger decimals) {
        BigInteger exaValue = convertToExa(amount, decimals);
        if (this.name.equals("sICX")) {
            BigInteger todayRate = Context.call(BigInteger.class, dataSource,
                    "getPriceByName", "sICX/ICX");
            return exaMultiply(exaValue, todayRate);
        }
        return exaValue;
    }

}
