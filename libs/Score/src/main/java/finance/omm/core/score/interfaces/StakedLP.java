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

    String  name();

    void setMinimumStake(BigInteger _value);

    BigInteger getMinimumStake();

    TotalStaked getTotalStaked(BigInteger _id);

    Map<Address, BigInteger> balanceOf(Address _owner, int _id);

    List<Map<String,BigInteger>> getBlanceByPool();

    List<Map<String,BigInteger>>  getPoolBalanceByUser(Address _owner);

    void addPool(int _poolID, Address asset); // _id,_pool

    Address getPoolById(int _id);

    void removePool(int _poolID);

    Map<BigInteger, Address> getSupportedPools();

    void unstake(int _id, int _value);

    void onIRC31Received(Address _operator,Address _from, int _id, int _value, byte data);

    SupplyDetails getLPStakedSupply(int _id, Address _user);


}
