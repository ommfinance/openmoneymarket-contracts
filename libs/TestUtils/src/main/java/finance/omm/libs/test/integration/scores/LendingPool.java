package finance.omm.libs.test.integration.scores;

import score.Address;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;

public interface LendingPool {

    @External(readonly = true)
    String name();

    @Payable
    void deposit(BigInteger _amount);

    void setFeeSharingTxnLimit(BigInteger _limit);

    void borrow(Address _reserve, BigInteger _amount);
}
