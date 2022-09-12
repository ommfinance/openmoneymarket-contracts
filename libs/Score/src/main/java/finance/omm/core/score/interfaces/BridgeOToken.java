package finance.omm.core.score.interfaces;

import score.Address;

import java.math.BigInteger;

public interface BridgeOToken {
    BigInteger balanceOf(Address _owner);
}
