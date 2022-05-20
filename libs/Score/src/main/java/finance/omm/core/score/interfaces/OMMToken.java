package finance.omm.core.score.interfaces;


import finance.omm.core.score.interfaces.token.IRC2;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface OMMToken extends IRC2, AddressProvider {

    SupplyDetails getPrincipalSupply(Address _user);

    TotalStaked getTotalStaked();


    void mint(BigInteger _amount, @Optional byte[] _data);


    BigInteger available_balanceOf(Address _owner);


    BigInteger staked_balanceOf(Address _owner);


    BigInteger unstaked_balanceOf(Address _owner);


    void setUnstakingPeriod(BigInteger _timeInSeconds);


    BigInteger getUnstakingPeriod();


    List<Address> getStakersList(int _start, int _end);


    int totalStakers();


    boolean inStakerList(Address _staker);


    Map<String, ?> details_balanceOf(Address _owner);


    BigInteger total_staked_balance();


    void setMinimumStake(BigInteger _min);


    BigInteger getMinimumStake();


    void addToLockList(Address _user);


    void removeFromLockList(Address _user);

    List<Address> get_locklist_addresses(int _start, int _end);

    void stake(BigInteger _value, Address _user);

    void cancelUnstake(BigInteger _value);

    void unstake(BigInteger _value, Address _user);

    void migrateStakedOMM(BigInteger _amount, BigInteger _lockPeriod);

}
