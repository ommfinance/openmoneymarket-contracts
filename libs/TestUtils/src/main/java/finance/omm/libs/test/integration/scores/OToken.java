package finance.omm.libs.test.integration.scores;

import score.Address;
import score.annotation.External;

import java.math.BigInteger;

public interface OToken {
    @External(readonly = true)
    BigInteger balanceOf(Address _owner);
}
