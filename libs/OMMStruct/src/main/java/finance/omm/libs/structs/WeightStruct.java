package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;

public class WeightStruct {

    public Address address;
    public BigInteger weight;

    public WeightStruct() {
    }

    public WeightStruct(Address address, BigInteger weight) {
        this.address = address;
        this.weight = weight;
    }
}
