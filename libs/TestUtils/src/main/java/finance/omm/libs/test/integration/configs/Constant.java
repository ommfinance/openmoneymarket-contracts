package finance.omm.libs.test.integration.configs;

import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;

public class Constant {

    public static final BigInteger LOAN_ORIGINATION_FEE_PERCENTAGE = MathUtils.ICX.divide(BigInteger.valueOf(100));
    public static final BigInteger MINIMUM_OMM_STAKE = MathUtils.ICX;
    public static final BigInteger UNSTAKING_PERIOD = BigInteger.valueOf(3);
    public static final BigInteger BORROW_THRESHOLD = BigInteger.valueOf(9)
            .multiply(MathUtils.ICX)
            .divide(BigInteger.valueOf(100));
}
