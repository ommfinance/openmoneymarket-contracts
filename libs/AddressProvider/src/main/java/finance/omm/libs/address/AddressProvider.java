package finance.omm.libs.address;


import finance.omm.libs.structs.AddressDetail;
import score.*;
import score.annotation.External;

import java.util.Map;

public class AddressProvider {
    public static final Integer ERROR_NOT_ADDRESS_PROVIDER = 1;

    public static final String _ADDRESSES = "addresses";
    public static final String _CONTRACTS = "contracts";
    public static final String _ADDRESS_PROVIDER = "addressProvider";

    private final VarDB<Address> _addressProvider = Context.newVarDB(_ADDRESS_PROVIDER, Address.class);
    private final DictDB<String, Address> _addresses = Context.newDictDB(_ADDRESSES, Address.class);
    private final ArrayDB<String> _contracts = Context.newArrayDB(_CONTRACTS, String.class);


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


    private void checkAddressProvider() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throwError(ERROR_NOT_ADDRESS_PROVIDER, "Caller is not Address provider");
        }
    }


    private void throwError(Integer errorCode, String message) {
        Context.revert(errorCode, message);
    }


}
