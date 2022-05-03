package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import score.Address;
import score.annotation.Optional;

public interface DaoFund extends AddressProvider {

    String name();

    void transferOmm(BigInteger _value, Address _address);

    void tokenFallback(Address _from, BigInteger _value, @Optional byte[] _data);

}
