package finance.omm.core.score.interfaces;

import score.Address;
import score.annotation.EventLog;

import java.math.BigInteger;

public interface LPStaker {

    String name();

    void stakeLP(BigInteger poolId, BigInteger value);

    void transferLp(Address to, BigInteger value, BigInteger poolId);

    BigInteger balanceOfLp(BigInteger poolId);

    void transferFunds(Address _address, BigInteger _value);

    void unstakeLP(BigInteger poolId, BigInteger value);

    void claimRewards();

    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    @EventLog(indexed = 2)
    void FundReceived(Address reserve, BigInteger value);

    @EventLog(indexed = 3)
    void LPTokenReceived(BigInteger poolId, BigInteger value, Address from);
}
