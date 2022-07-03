package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface BoostedToken {

    void setMinimumLockingAmount(BigInteger value);

    BigInteger getMinimumLockingAmount();

    Map<String, BigInteger> getLocked(Address _owner);

    BigInteger getTotalLocked();

    List<Address> getUsers(int start, int end);

    int activeUsersCount();

    boolean hasLocked(Address _owner);

    BigInteger getLastUserSlope(Address address);


    BigInteger userPointHistoryTimestamp(Address address, BigInteger index);


    BigInteger lockedEnd(Address address);


    void checkpoint();


    void tokenFallback(Address _from, BigInteger _value, byte[] _data);


    void increaseUnlockTime(BigInteger unlockTime);


    void withdraw();


    BigInteger balanceOf(Address _owner, @Optional BigInteger timestamp);


    BigInteger balanceOfAt(Address _owner, BigInteger block);


    BigInteger totalSupply(@Optional BigInteger time);


    BigInteger totalSupplyAt(BigInteger block);


    void kick(Address _user);

    String name();


    String symbol();


    BigInteger userPointEpoch(Address _owner);


    int decimals();

    void addContractToWhitelist(Address address);

    void removeContractFromWhitelist(Address address);

    List<Address> getContractWhitelist();
}
