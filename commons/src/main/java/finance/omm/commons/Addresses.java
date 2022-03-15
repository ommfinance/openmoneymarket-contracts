package finance.omm.commons;

import java.util.Map;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;

public class Addresses extends BaseScore{

	// ADDRESSES
	public static final String GOVERNANCE = "governance";
	public static final String OMM_TOKEN = "ommToken";

	private String ADDRESSES = "addresses";
	private String CONTRACTS = "contracts";
	private String ADDRESS_PROVIDER = "addressProvider";

	private DictDB<String, Address> addresses = Context.newDictDB(ADDRESSES, Address.class);
	private VarDB<Address> addressProvider = Context.newVarDB(ADDRESS_PROVIDER, Address.class);
	private ArrayDB<String> contracts = Context.newArrayDB(CONTRACTS, String.class);

	public Addresses(Address _address, @Optional boolean _update) {
		if(_update) {
			Context.println(getTag() + "| on update Addresses event:" + Context.getAddress());
		}else {
			Context.println(getTag() + "| on install Addresses event:" + Context.getAddress());
			this.addressProvider.set(_address);
		}
	}

	@External
	public void setAddresses(AddressDetails[] _addressDetails) {
		onlyAddressProvider();
		for (AddressDetails contract : _addressDetails) {
			Context.println(getTag() + "| contract : " +contract.getName() + " , address: " + contract.getAddress());
			if (!containsInArrayDb(contract.getName(), this.contracts)) {
				this.contracts.add(contract.getName());
			}
			this.addresses.set( contract.getName(), contract.getAddress());
		}
	}

	@External(readonly=true)
	public Map<String, Address> getAddresses() {
		return arrayAndDictDbToMap(contracts, addresses);
	}

	@External(readonly=true)
	public Address getAddress(String _name) {
		return this.addresses.getOrDefault(_name, null);
	}

	@External(readonly=true)
	public Address getAddressProvider() {
		return this.addressProvider.getOrDefault(null);
	}

	@Override
	public String getTag() {
		return "Addresses";
	}
}
