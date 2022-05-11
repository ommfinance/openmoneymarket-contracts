package finance.omm.score.tokens;

import finance.omm.libs.address.AddressProvider;
import score.Address;

public class OTokenImpl extends AddressProvider {

    public OTokenImpl(Address addressProvider, boolean _update) {
        super(addressProvider, _update);
    }

}
