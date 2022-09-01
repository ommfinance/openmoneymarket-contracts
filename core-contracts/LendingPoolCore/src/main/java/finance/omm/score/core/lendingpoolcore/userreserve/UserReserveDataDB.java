package finance.omm.score.core.lendingpoolcore.userreserve;

import score.BranchDB;
import score.Context;

import java.math.BigInteger;
import score.VarDB;

public class UserReserveDataDB {

    public final BranchDB<String, VarDB<BigInteger>> lastUpdateTimestamp = Context.newBranchDB("lastUpdateTimestamp", BigInteger.class);
    public final BranchDB<String, VarDB<BigInteger>> originationFee = Context.newBranchDB("originationFee", BigInteger.class);

    public UserReserveDataDB() {

    }
}
