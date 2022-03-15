package finance.omm.commons;

import score.Address;

public class AddressDetails {

	private String name;
	private Address address;

	public AddressDetails() {}
	public AddressDetails(String name, Address address) {
		this.name = name;
		this.address = address;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

}
