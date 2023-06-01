package finance.omm.core.score.interfaces.token;

import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

@ScoreInterface
public interface IRC2Mintable extends IRC2 {
    @External
    void setMinter(Address _address);

    @External(readonly = true)
    Address getMinter();

    @External
    void mint(BigInteger _amount, @Optional byte[] _data);

    @External
    void mintTo(Address _account, BigInteger _amount, @Optional byte[] _data);
}
