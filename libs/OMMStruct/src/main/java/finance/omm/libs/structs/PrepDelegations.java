package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;

public class PrepDelegations {

    public Address _address;
    public BigInteger _votes_in_per;

    public PrepDelegations() {}

    public PrepDelegations(Address address, BigInteger votes_in_per) {
        _address = address;
        _votes_in_per = votes_in_per;
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

    @Override
    public int hashCode() {
        int result = _address != null ? _address.hashCode() : 0;
        result = 31 * result + (_votes_in_per != null ? _votes_in_per.hashCode() : 0);
        return result;
    }
}
