package finance.omm.score.core.stakedLP;

import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.score.core.stakedLP.exception.StakeLpException;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class StakedLPImpl extends AddressProvider implements StakedLP {

    public static final String TAG = "Staked LP";

    private static final BigInteger ZERO = BigInteger.ZERO;

    private ArrayDB<BigInteger> supportedPools = Context.newArrayDB("supportedPools",BigInteger.class);
    private DictDB<Address,BigInteger> totalStaked = Context.newDictDB("totalStaked",BigInteger.class);
    private DictDB<BigInteger,Address> addressMap = Context.newDictDB("addressMap", Address.class);
    private VarDB<BigInteger> minimumStake = Context.newVarDB("minimumStake", BigInteger.class);

    // poolStakeDetails
//    private BranchDB<BigInteger,BranchDB<BigInteger,  DictDB<>
//    private BranchDB<BigInteger, DictDB<BigInteger,String>> 2i
//    private DictDB<BigInteger,String > 3;

    public StakedLPImpl(Address addressProvider) {
        super(addressProvider, false);
        minimumStake.set(ZERO);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setMinimumStake(BigInteger _value) {
        checkOwner();
        Context.require(_value.compareTo(ZERO) >= 0 ,"Minimum stake value must be positive," + _value);
        minimumStake.set(_value);
    }

    @External(readonly = true)
    public BigInteger getMinimumStake() {
        return minimumStake.get();
    }

    @External(readonly = true)
    public TotalStaked getTotalStaked(BigInteger _id) {
        return null;
    }

    private BigInteger getAverageDecimals(BigInteger _id){
        call(Contracts.DEX,"getPoolStats",_id);

        return null;
    }

    @External(readonly = true)
    public Map<Address, BigInteger> balanceOf(Address _owner, int _id) {
        return null;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getBlanceByPool() {
        return null;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getPoolBalanceByUser(Address _owner) {
        return null;
    }

    @External
    public void addPool(int _poolID, Address asset) {

    }

    @External(readonly = true)
    public Address getPoolById(int _id) {
        return null;
    }

    @External
    public void removePool(int _poolID) {

    }

    @Override
    public Map<BigInteger, Address> getSupportedPools() {
        return null;
    }

    @Override
    public void unstake(int _id, int _value) {

    }

    @External
    public void onIRC31Received(Address _operator, Address _from, int _id, int _value, byte data) {

    }

    @External
    public SupplyDetails getLPStakedSupply(int _id, Address _user) {
        return null;
    }

    protected void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw StakeLpException.notOwner();
        }
    }

    public void call(Address contract, String method, Object... params) {
        Context.call(contract, method, params);
    }
}
