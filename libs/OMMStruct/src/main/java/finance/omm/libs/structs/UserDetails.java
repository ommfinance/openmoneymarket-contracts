package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;
import score.annotation.Keep;

public class UserDetails {

    @Keep
    public Address _user;
    @Keep
    public BigInteger _userBalance;
    @Keep
    public BigInteger _totalSupply;

    @Keep
    public BigInteger _decimals;
}
