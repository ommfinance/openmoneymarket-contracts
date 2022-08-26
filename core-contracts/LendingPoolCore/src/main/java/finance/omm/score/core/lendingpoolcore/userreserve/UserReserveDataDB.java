package finance.omm.score.core.lendingpoolcore.userreserve;

import score.Context;
import score.DictDB;

import java.math.BigInteger;

public class UserReserveDataDB {

    public final DictDB<byte[], BigInteger> lastUpdateTimestamp = Context.newDictDB("lastUpdateTimestamp", BigInteger.class);
    public final DictDB<byte[], BigInteger> originationFee = Context.newDictDB("originationFee", BigInteger.class);

    public UserReserveDataDB() {

    }
}
