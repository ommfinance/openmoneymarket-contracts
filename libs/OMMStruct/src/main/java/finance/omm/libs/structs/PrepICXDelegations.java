package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;
import score.Address;

public class PrepICXDelegations {
    public Address _address;
    public BigInteger _votes_in_per;
    public BigInteger _votes_in_icx;

    public static PrepICXDelegations fromMap(Map<String, ?> map) {
        PrepICXDelegations prepDelegations = new PrepICXDelegations();
        prepDelegations._address = (Address) map.get("_address");
        prepDelegations._votes_in_per = (BigInteger) map.get("_votes_in_per");
        prepDelegations._votes_in_icx = (BigInteger) map.get("_votes_in_icx");
        return prepDelegations;
    }
}

