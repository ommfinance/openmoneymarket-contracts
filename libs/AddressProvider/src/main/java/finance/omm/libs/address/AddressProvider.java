package finance.omm.libs.address;


import finance.omm.libs.structs.AddressDetail;
import finance.omm.utils.exceptions.OMMException;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;

public class AddressProvider {

    public static final String _ADDRESSES = "addresses";
    public static final String _CONTRACTS = "contracts";
    public static final String _ADDRESS_PROVIDER = "addressProvider";

    protected final VarDB<Address> _addressProvider = Context.newVarDB(_ADDRESS_PROVIDER, Address.class);
    protected final DictDB<String, Address> _addresses = Context.newDictDB(_ADDRESSES, Address.class);
    protected final ArrayDB<String> _contracts = Context.newArrayDB(_CONTRACTS, String.class);


    public AddressProvider(Address addressProvider) {
        if (_addressProvider.getOrDefault(null) == null) {
            _addressProvider.set(addressProvider);
        }
    }

    @External
    public void setAddresses(AddressDetail[] _addressDetails) {
        checkAddressProvider();
        for (AddressDetail addressDetail : _addressDetails) {
            if (this._addresses.get(addressDetail.name) == null && addressDetail.address != null) {
                this._contracts.add(addressDetail.name);
            }
            this._addresses.set(addressDetail.name, addressDetail.address);
        }
    }

    @External(readonly = true)
    public Map<String, Address> getAddresses() {
        int size = _contracts.size();
        Map.Entry<String, Address>[] entries = new Map.Entry[size];
        for (int i = 0; i < size; i++) {
            String name = _contracts.get(i);
            Address value = _addresses.get(name);
            entries[i] = Map.entry(name, value);
        }
        return Map.ofEntries(entries);
    }

    @External(readonly = true)
    public Address getAddress(String name) {
        return _addresses.get(name);
    }

    @External(readonly = true)
    public Address getAddressProvider() {
        return this._addressProvider.get();
    }


    protected void checkAddressProvider() {
        if (!Context.getCaller().equals(_addressProvider.get())) {
            throw new OMMException.AddressProviderException(99, "require Address provider contract access");
        }
    }


}
