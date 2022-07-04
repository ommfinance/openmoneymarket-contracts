package finance.omm.libs.test.integration.scores;

import score.annotation.Payable;

import java.math.BigInteger;

public interface LendingPool {

    void setFeeSharingTxnLimit(BigInteger _limit);

    @Payable
    void deposit(BigInteger _amount);

}
