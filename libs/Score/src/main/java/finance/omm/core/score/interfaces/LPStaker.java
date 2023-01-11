package finance.omm.core.score.interfaces;

import score.Address;
import score.annotation.EventLog;

import java.math.BigInteger;

public interface LPStaker {

    String name();

    void stakeLP(BigInteger poolId, BigInteger value);

    void transferLp(Address to, BigInteger value, BigInteger poolId,byte[] data);

    BigInteger balanceOfLp(BigInteger poolId);

    void transferFunds(Address address, BigInteger value,byte[] data);

    void unstakeLP(BigInteger poolId, BigInteger value);

    void claimRewards();

    void onIRC31Received(Address operator, Address from, BigInteger id, BigInteger value, byte[] data);

    void tokenFallback(Address from, BigInteger value, byte[] data);

    @EventLog(indexed = 2)
    void FundReceived(Address reserve, BigInteger value);

    @EventLog(indexed = 3)
    void LPTokenReceived(BigInteger poolId, BigInteger value, Address from);
}
