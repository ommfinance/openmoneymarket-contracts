package finance.omm.libs.test.integration.scores;

import score.Address;
import score.annotation.Payable;

import java.math.BigInteger;

public interface LendingPool {

    @Payable
    void deposit(BigInteger _amount);

    void setFeeSharingTxnLimit(BigInteger _limit);

    void borrow(Address _reserve, BigInteger _amount);

    void redeem(Address _oToken, BigInteger _amount, boolean _waitForUnstaking);
    
}
