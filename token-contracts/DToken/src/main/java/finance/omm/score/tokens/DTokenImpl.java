package finance.omm.score.tokens;

import finance.omm.libs.address.AddressProvider;
import score.Address;

public class DTokenImpl extends AddressProvider {

    public DTokenImpl(Address addressProvider, boolean _update) {
        super(addressProvider, _update);
    }

}
