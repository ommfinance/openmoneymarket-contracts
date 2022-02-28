package finance.omm.score.core.reward.db;

import finance.omm.utils.db.EnumerableDictDB;

public class TypeDB extends EnumerableDictDB<String, String> {

    public TypeDB(String id) {
        super(id, String.class, String.class);
    }
}
