package finance.omm.score.core.lendingpoolcore.userreserve;

import score.Context;
import score.VarDB;

import java.math.BigInteger;

public class UserReserveData {

    public final VarDB<BigInteger> lastUpdateTimestamp = Context.newVarDB("lastUpdateTimestamp", BigInteger.class);
    public final VarDB<BigInteger> originationFee = Context.newVarDB("originationFee", BigInteger.class);

    public UserReserveData(Byte prefix) {

    }
}
