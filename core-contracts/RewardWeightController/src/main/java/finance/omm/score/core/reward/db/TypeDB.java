package finance.omm.score.core.reward.db;

import finance.omm.utils.db.EnumerableDictDB;

public class TypeDB extends EnumerableDictDB<String, Boolean> {

    public TypeDB(String id) {
        super(id, String.class, Boolean.class);
    }
}
