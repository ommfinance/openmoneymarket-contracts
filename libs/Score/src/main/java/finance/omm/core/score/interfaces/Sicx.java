package finance.omm.core.score.interfaces;

import finance.omm.core.score.interfaces.token.IRC2BurnableInterface;
import finance.omm.core.score.interfaces.token.IRC2Mintable;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;

@ScoreInterface
public interface Sicx extends IRC2BurnableInterface, IRC2Mintable {

    @External
    void setEmergencyManager(Address _address);

    @External(readonly = true)
    Address getEmergencyManager();

    @External(readonly = true)
    String getPeg();

    @External(readonly = true)
    BigInteger priceInLoop();

    @External(readonly = true)
    BigInteger lastPriceInLoop();

    @External
    void setStaking(Address _address);

    @External(readonly = true)
    Address getStaking();
}
