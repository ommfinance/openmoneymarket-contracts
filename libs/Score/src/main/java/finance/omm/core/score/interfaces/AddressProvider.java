package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AddressDetails;
import java.util.Map;
import score.Address;

public interface AddressProvider {

    void setAddresses(AddressDetails[] _addressDetails);

    Map<String, Address> getAddresses();

    Address getAddress(String _name);

    Address getAddressProvider();
}
