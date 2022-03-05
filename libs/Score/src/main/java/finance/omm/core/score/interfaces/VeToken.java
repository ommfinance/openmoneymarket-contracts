package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface VeToken {

    @External
    void commitTransferOwnership(Address address);

    @External
    void applyTransferOwnership();

    @External(readonly = true)
    Map<String, BigInteger> getLocked(Address _owner);

    @External(readonly = true)
    BigInteger getTotalLocked();

    @External(readonly = true)
    List<Address> getUsers(int start, int end);

    @External(readonly = true)
    BigInteger getLastUserSlope(Address address);

    @External(readonly = true)
    BigInteger userPointHistoryTimestamp(Address address, BigInteger index);

    @External(readonly = true)
    BigInteger lockedEnd(Address address);

    @External
    void checkpoint();

    @External
    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    @External
    void increaseUnlockTime(BigInteger unlockTime);

    @External
    void withdraw();

    @External(readonly = true)
    BigInteger balanceOf(Address address, @Optional BigInteger timestamp);

    @External(readonly = true)
    BigInteger balanceOfAt(Address address, BigInteger block);

    @External(readonly = true)
    BigInteger totalSupply(@Optional BigInteger time);

    @External(readonly = true)
    BigInteger totalSupplyAt(BigInteger block);

    @External(readonly = true)
    Address admin();

    @External(readonly = true)
    Address futureAdmin();

    @External(readonly = true)
    String name();

    @External(readonly = true)
    String symbol();

    @External(readonly = true)
    BigInteger userPointEpoch(Address address);

    @External(readonly = true)
    int decimals();

}
