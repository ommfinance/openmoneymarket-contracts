package finance.omm.libs.test.integration.scores;

import score.Address;
import score.annotation.External;

import java.math.BigInteger;

public interface OToken {

    String name();

    @External(readonly = true)
    BigInteger balanceOf(Address _owner);

    @External(readonly = true)
    BigInteger principalTotalSupply();

    @External(readonly = true)
    BigInteger principalBalanceOf(Address _user);

    @External(readonly = true)
    BigInteger totalSupply();

    @External
    void transfer(Address _to, BigInteger _value, byte[] _data);

    BigInteger getUserLiquidityCumulativeIndex(Address _user);

}
