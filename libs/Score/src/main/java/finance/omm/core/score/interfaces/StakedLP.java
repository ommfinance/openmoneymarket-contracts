package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import foundation.icon.score.client.ScoreInterface;
import score.Address;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ScoreInterface(suffix = "Client")
public interface StakedLP {

    String name();

    void setMinimumStake(BigInteger _value);

    BigInteger getMinimumStake();

    TotalStaked getTotalStaked(int _id);

    Map<String, BigInteger> balanceOf(Address _owner, int _id);

    List<Map<String,BigInteger>> getBalanceByPool();

    List<Map<String,BigInteger>>  getPoolBalanceByUser(Address _owner);

    void addPool(int _id, Address _pool);

    Address getPoolById(int _id);

    void removePool(int _poolID);

    Map<String, Address> getSupportedPools();

    void unstake(int _id, BigInteger _value);

    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] data); //balance bata biginteger

    SupplyDetails getLPStakedSupply(int _id, Address _user);

    BigInteger totalStaked(int _id);

}
