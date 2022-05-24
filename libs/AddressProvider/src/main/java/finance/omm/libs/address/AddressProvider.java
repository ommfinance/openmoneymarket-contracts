package finance.omm.libs.address;


import finance.omm.libs.structs.AddressDetails;
import finance.omm.utils.exceptions.OMMException;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;

public class AddressProvider {

    public static final String TAG = "AddressProvider";
    public static final String _ADDRESSES = "addresses";
    public static final String _CONTRACTS = "contracts";
    public static final String _ADDRESS_PROVIDER = "addressProvider";

    protected final VarDB<Address> _addressProvider = Context.newVarDB(_ADDRESS_PROVIDER, Address.class);
    protected final DictDB<String, Address> _addresses = Context.newDictDB(_ADDRESSES, Address.class);
    protected final ArrayDB<String> _contracts = Context.newArrayDB(_CONTRACTS, String.class);


    public AddressProvider(Address addressProvider, @Optional boolean _update) {
        if (_update) {
            onUpdate();
            return;
        }

        if (_addressProvider.get() == null) {
            _addressProvider.set(addressProvider);
        }
    }

    public void onUpdate() {
        Context.println(TAG + " | on update");
    }

    @External
    public void setAddresses(AddressDetails[] _addressDetails) {
        checkAddressProvider();
        for (AddressDetails addressDetail : _addressDetails) {
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

    public void call(Address address, String method, Object... params) {
        Context.call(address, method, params);
    }

    public void call(Contracts contract, String method, Object... params) {
        call(getAddress(contract.getKey()), method, params);
    }

    public  <K> K call(Class<K> kClass, Contracts contract, String method, Object... params) {
        return Context.call(kClass, getAddress(contract.getKey()), method, params);
    }

    protected void checkAddressProvider() {
        if (!Context.getCaller().equals(_addressProvider.get())) {
            throw OMMException.unknown("require Address provider contract access");
        }
    }

    public void onlyOrElseThrow(Contracts contract, OMMException ommException) {
        if (!Context.getCaller()
                .equals(this.getAddress(contract.getKey()))) {
            throw ommException;
        }
    }

}
