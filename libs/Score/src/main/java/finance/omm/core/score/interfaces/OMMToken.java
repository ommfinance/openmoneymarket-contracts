package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface OMMToken {


    BigInteger totalSupply();

    BigInteger stakedBalanceOfAt(Address _proposer, BigInteger _timestamp);

    BigInteger totalStakedBalanceOfAt(BigInteger _timestamp);

    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);
}
