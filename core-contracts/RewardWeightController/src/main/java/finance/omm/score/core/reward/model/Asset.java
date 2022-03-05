package finance.omm.score.core.reward.model;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

public class Asset {

    public final Address address;
    public final String type;
    public String name;

    public Asset(Address address, String type) {
        this.address = address;
        this.type = type;
    }

    public static void writeObject(ObjectWriter w, Asset a) {
        w.beginList(3);
        w.write(a.address);
        w.write(a.type);
        w.write(a.name);
        w.end();
    }

    public static Asset readObject(ObjectReader r) {
        r.beginList();
        Asset a = new Asset(r.readAddress(), r.readString());
        a.name = r.readString();
        r.end();
        return a;
    }

}
