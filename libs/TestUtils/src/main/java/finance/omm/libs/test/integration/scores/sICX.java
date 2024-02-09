package finance.omm.libs.test.integration.scores;

import java.math.BigInteger;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface sICX {

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

    @External(readonly = true)
    String symbol();

    @External(readonly = true)
    BigInteger totalSupply();

    @External(readonly = true)
    BigInteger balanceOf(Address _owner);

    @External
    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);

    @External
    void setMinter(Address _address);

    @External(readonly = true)
    Address getMinter();

    @External
    void mint(BigInteger _amount, @Optional byte[] _data);
    @External
    void mintTo(Address _account, BigInteger _amount, @Optional byte[] _data);

    @External
    void burnFrom(Address _account, BigInteger _amount);
}
