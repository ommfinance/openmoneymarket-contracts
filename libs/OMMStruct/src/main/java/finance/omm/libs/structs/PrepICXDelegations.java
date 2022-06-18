package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;
import score.annotation.Keep;

public class PrepICXDelegations {
    @Keep
    public Address _address;
    @Keep
    public BigInteger _votes_in_per;
    @Keep
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

        PrepICXDelegations that = (PrepICXDelegations) o;

        if (_address != null ? !_address.equals(that._address) : that._address != null) {
            return false;
        }
        if (_votes_in_per != null ? !_votes_in_per.equals(that._votes_in_per) : that._votes_in_per != null) {
            return false;
        }
        return _votes_in_icx != null ? _votes_in_icx.equals(that._votes_in_icx) : that._votes_in_icx == null;
    }

    @Override
    public int hashCode() {
        int result = _address != null ? _address.hashCode() : 0;
        result = 31 * result + (_votes_in_per != null ? _votes_in_per.hashCode() : 0);
        result = 31 * result + (_votes_in_icx != null ? _votes_in_icx.hashCode() : 0);
        return result;
    }
}

