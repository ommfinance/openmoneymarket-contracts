package finance.omm.core.score.interfaces;

import score.Address;
import score.annotation.EventLog;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

public interface LPStaker {

    String name();

    void stake(BigInteger poolId, BigInteger value);

    void transfer(Address to, BigInteger value, BigInteger poolId, @Optional byte[] data);

    Map<String, BigInteger> balanceOfLp(Address owner, BigInteger poolId);

    void unstake(BigInteger poolId, BigInteger value);

    void onIRC31Received(Address operator, Address from, BigInteger id, BigInteger value, byte[] data);

    void tokenFallback(Address from, BigInteger value, byte[] data);

    @EventLog(indexed = 2)
    void TokenReceived(Address reserve, BigInteger value);

    @EventLog(indexed = 3)
    void LPTokenReceived(Address from, BigInteger poolId, BigInteger value);
}
