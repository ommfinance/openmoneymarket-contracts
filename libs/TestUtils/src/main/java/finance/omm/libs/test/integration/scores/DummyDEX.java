package finance.omm.libs.test.integration.scores;

import finance.omm.core.score.interfaces.token.IRC31;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

public interface DummyDEX extends IRC31 {

    void mint(BigInteger _id, BigInteger _supply);

    void mintTo(BigInteger _id, Address _account, BigInteger _supply);

    Map<String, ?> getPoolStats(BigInteger _id);

    BigInteger getPriceByName(String _name);

    BigInteger lookupPid(String _name);

    BigInteger getBalnPrice();

    void transfer(Address _to, BigInteger _id, BigInteger _value, @Optional byte[] _data);
}
