package finance.omm.libs.test.integration.scores;

import java.math.BigInteger;
import score.Address;
import score.annotation.Optional;

public interface sICX {

    BigInteger totalSupply();

    BigInteger balanceOf(Address _owner);

    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);
}
