package finance.omm.libs.structs;

import score.Address;

public class AddressDetails {

    public String name;
    public Address address;

    public AddressDetails() {
    }

    public AddressDetails(String name, Address address) {
        this.name = name;
        this.address = address;
    }


}
