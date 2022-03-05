package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;

public class WorkingBalance {

    public Address user;
    public Address assetAddr;
    public BigInteger bOMMUserBalance;
    public BigInteger bOMMTotalSupply;
    public BigInteger tokenBalance;
    public BigInteger tokenTotalSupply;
}
