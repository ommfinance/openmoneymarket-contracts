package finance.omm.score.core.oracle.token;

import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import java.math.BigInteger;
import score.Address;
import score.Context;

public class sICXToken extends BaseToken {

    private Address dataSource;

    public sICXToken(String name, String priceOracleKey, Address dataSource) {
        super(name, priceOracleKey);
        this.dataSource = dataSource;
    }

    @Override
    public BigInteger convert(BigInteger amount, BigInteger decimals) {
        BigInteger exaValue = convertToExa(amount, decimals);

        BigInteger todayRate = Context.call(BigInteger.class, this.dataSource,
                "getPriceByName", "sICX/ICX");
        return exaMultiply(exaValue, todayRate);

    }

}
