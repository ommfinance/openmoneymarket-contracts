package finance.omm.core.score.interfaces.token;

import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;

@ScoreInterface
public interface IRC2BurnableInterface {

    @External
    void burn(BigInteger _amount);

    @External
    void burnFrom(Address _account, BigInteger _amount);
}
