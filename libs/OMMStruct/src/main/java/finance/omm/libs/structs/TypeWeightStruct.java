package finance.omm.libs.structs;

import java.math.BigInteger;

public class TypeWeightStruct {

    public String key;
    public BigInteger weight;

    public TypeWeightStruct() {

    }

    public TypeWeightStruct(String key, BigInteger weight) {
        this.key = key;
        this.weight = weight;
    }
}
