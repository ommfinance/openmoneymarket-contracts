package finance.omm.score.core.reward.distribution.db;

import finance.omm.utils.db.EnumerableDictDB;
import java.math.BigInteger;
import score.Address;

public class UserClaimedRewards extends EnumerableDictDB<Address, BigInteger> {

    public UserClaimedRewards(String id) {
        super(id, Address.class, BigInteger.class);
    }
}

