package finance.omm.score.core.reward.model;

import score.ObjectReader;
import score.ObjectWriter;

public class Asset {

    public final String id;
    public final String typeId;
    public String name;

    public Asset(String id, String typeId) {
        this.id = id;
        this.typeId = typeId;
    }

    public static void writeObject(ObjectWriter w, Asset a) {
        w.beginList(4);
        w.write(a.id);
        w.write(a.typeId);
        w.write(a.name);
        w.end();
    }

    public static Asset readObject(ObjectReader r) {
        r.beginList();
        Asset a = new Asset(r.readString(), r.readString());
        a.name = r.readString();
        r.end();
        return a;
    }

}
