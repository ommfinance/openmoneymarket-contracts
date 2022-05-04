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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressDetails that = (AddressDetails) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return address != null ? address.equals(that.address) : that.address == null;
    }

}
