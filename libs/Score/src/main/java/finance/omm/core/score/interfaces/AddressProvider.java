package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AddressDetail;
import java.util.Map;
import score.Address;

public interface AddressProvider {

    void setAddresses(AddressDetail[] _addressDetails);

    Map<String, Address> getAddresses();

    Address getAddress(String _name);

    Address getAddressProvider();
}
