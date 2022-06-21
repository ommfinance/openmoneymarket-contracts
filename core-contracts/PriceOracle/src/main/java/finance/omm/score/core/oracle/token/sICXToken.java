package finance.omm.score.core.oracle.token;

import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class sICXToken extends BaseToken {

    public sICXToken(String name, String priceOracleKey) {
        super(name, priceOracleKey);
    }

    @Override
    public BigInteger convert(Address dataSource, BigInteger amount, BigInteger decimals) {
        BigInteger exaValue = convertToExa(amount, decimals);

        BigInteger todayRate = Context.call(BigInteger.class, dataSource,
                "getPriceByName", "sICX/ICX");
        return exaMultiply(exaValue, todayRate);

    }

}
