package finance.omm.score.core.stakedLP;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class StakedLPImpl extends AbstractStakedLP {

    public StakedLPImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setMinimumStake(BigInteger _value) {
        onlyOwnerOrElseThrow(StakedLPException.notOwner());
        if (_value.compareTo(ZERO) < 0) {
            throw StakedLPException.unknown("Minimum stake value must be positive, " + _value);
        }
        minimumStake.set(_value);
    }

    @External(readonly = true)
    public BigInteger getMinimumStake() {
        return minimumStake.get();
    }

    @External(readonly = true)
    public TotalStaked getTotalStaked(int _id) {
        return null;
    }

    @External(readonly = true)
    public Map<Address, BigInteger> balanceOf(Address _owner, int _id) {
        return null;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getBalanceByPool() {
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

    @External(readonly = true)
    public Map<String, Address> getSupportedPools() {
        // cast poolId to string
        return null;
    }

    @External
    public void unstake(int _id, BigInteger _value) {

    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte data) {

    }

    @External(readonly = true)
    public SupplyDetails getLPStakedSupply(int _id, Address _user) {
        return null;
    }
}
