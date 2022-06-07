package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ScoreInterface(suffix = "Client")
public interface StakedLP {

    String name();

    @External
    void setMinimumStake(BigInteger _value);

    @External(readonly = true)
    BigInteger getMinimumStake();

    @External(readonly = true)
    TotalStaked getTotalStaked(int _id);


    Map<String, BigInteger> balanceOf(Address _owner, int _id);

    @External(readonly = true)
    List<Map<String,BigInteger>> getBalanceByPool();

    @External(readonly = true)
    List<Map<String,BigInteger>>  getPoolBalanceByUser(Address _owner);

    @External
    void addPool(int _id, Address _pool);

    @External(readonly = true)
    Address getPoolById(int _id);

    @External
    void removePool(int _poolID);

    @External(readonly = true)
    Map<String, Address> getSupportedPools();

    @External
    void unstake(int _id, BigInteger _value);

    @External
    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] data);

    @External(readonly = true)
    SupplyDetails getLPStakedSupply(int _id, Address _user);



}
