package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface DAOFund extends AddressProvider {

    String name();

    void transferOmm(BigInteger _value, Address _address);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

}
