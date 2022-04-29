package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;
import score.Address;

public class PrepICXDelegations {
    public Address _address;
    public BigInteger _votes_in_per;
    public BigInteger _votes_in_icx;
    public  PrepICXDelegations() {}

    public PrepICXDelegations(Address address, BigInteger votes_in_per, BigInteger votes_in_icx) {
        _address = address;
        _votes_in_per = votes_in_per;
        _votes_in_icx = votes_in_icx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrepDelegations that = (PrepDelegations) o;

        if (_address != null ? !_address.equals(that._address) : that._address != null) {
            return false;
        }
        return _votes_in_per != null ? _votes_in_per.equals(that._votes_in_per) : that._votes_in_per == null;
    }
}

