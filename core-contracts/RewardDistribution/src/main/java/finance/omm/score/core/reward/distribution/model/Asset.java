package finance.omm.score.core.reward.distribution.model;


import java.math.BigInteger;
import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class Asset {

    public final String type;
    public String name;
    public BigInteger lpID;
    public Address address;

    public Asset(Address address, String type) {
        this.type = type;
        this.address = address;
    }

    public static void writeObject(ObjectWriter w, Asset a) {
        w.beginList(4);
        w.write(a.address);
        w.write(a.type);
        w.write(a.name);
        w.writeNullable(a.lpID);
        w.end();
    }

    public static Asset readObject(ObjectReader r) {
        r.beginList();
        Asset a = new Asset(r.readAddress(), r.readString());
        a.name = r.readString();
        a.lpID = r.readNullable(BigInteger.class);
        r.end();
        return a;
    }

}

