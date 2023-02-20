package finance.omm.libs.test.integration.scores;


import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;

public interface LendingPool {

    String name();

    @Payable
    void deposit(BigInteger _amount);

    void setFeeSharingTxnLimit(BigInteger _limit);

    void borrow(Address _reserve, BigInteger _amount);

    void redeem(Address _reserve, BigInteger _amount, @Optional boolean _waitForUnstaking);

    void claimRewards();

    void stake(BigInteger _value);

    void unstake(BigInteger _value);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    List<Address> getDepositWallets(int _index);

    List<Address> getBorrowWallets(int _index);

    void setLiquidationStatus(boolean _status);

    boolean isLiquidationEnabled();
    
}
