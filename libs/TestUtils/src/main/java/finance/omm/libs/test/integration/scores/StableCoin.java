package finance.omm.libs.test.integration.scores;

import finance.omm.core.score.interfaces.token.IRC2;
import score.Address;

import java.math.BigInteger;

public interface StableCoin extends IRC2 {

    void mintTo(Address _to, BigInteger _value);

    void addIssuer(Address _issuer);

    void approve(Address _issuer, BigInteger _value);

    void transfer(Address _to, BigInteger _value, byte[] _data);



}
