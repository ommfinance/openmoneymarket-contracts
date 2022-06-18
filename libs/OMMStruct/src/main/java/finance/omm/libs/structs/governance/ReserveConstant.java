package finance.omm.libs.structs.governance;

import java.math.BigInteger;
import score.Address;

public class ReserveConstant {

    public Address reserve;
    public BigInteger optimalUtilizationRate;
    public BigInteger baseBorrowRate;
    public BigInteger slopeRate1;
    public BigInteger slopeRate2;
}
