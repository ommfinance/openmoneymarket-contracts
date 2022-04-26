package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;
import score.Address;

public class PrepDelegations {
    public Address _address;
    public BigInteger _votes_in_per;

    public static PrepDelegations fromMap(Map<String, ?> map) {
        PrepDelegations prepDelegations = new PrepDelegations();
        prepDelegations._address = (Address) map.get("_address");
        prepDelegations._votes_in_per = (BigInteger) map.get("_votes_in_per");
        return prepDelegations;
    }

}
