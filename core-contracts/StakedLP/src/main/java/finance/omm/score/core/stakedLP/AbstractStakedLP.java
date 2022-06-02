package finance.omm.score.core.stakedLP;

import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;

public abstract class AbstractStakedLP extends AddressProvider implements StakedLP,
        Authorization<StakedLPException> {
    public static final String TAG = "Staked Lp";
    public static final BigInteger ZERO = BigInteger.ZERO;
    public final ArrayDB<Integer> supportedPools = Context.newArrayDB("supportedPools", Integer.class);
    public final BranchDB<Address, DictDB<Integer, BigInteger>> poolStakeDetails = Context.newBranchDB(
            "poolStakeDetails", BigInteger.class);
    public final DictDB<Address,BigInteger> totalStaked = Context.newDictDB("totalStaked", BigInteger.class);
    public final DictDB<Integer, Address> addressMap = Context.newDictDB("addressMap", Address.class);
    public final VarDB<BigInteger> minimumStake = Context.newVarDB("minimumStake", BigInteger.class);


    public AbstractStakedLP(Address addressProvider, boolean _update) {
        super(addressProvider, _update);

        if (minimumStake.get() == null) {
            minimumStake.set(ZERO);
        }
    }

    protected BigInteger getAverageDecimals(Integer _id){
        Map<String, ?> poolStats  = call(Map.class, Contracts.DEX,"getPoolStats", _id);

        return null;
    }

    protected void stake(Address user, Integer id, BigInteger value) {
        //
    }



}
