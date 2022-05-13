package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import score.Address;

@ScoreInterface(suffix = "Client")
public interface FeeProvider {

    void transferFund(Address _token, BigInteger _value, Address _to);
}
