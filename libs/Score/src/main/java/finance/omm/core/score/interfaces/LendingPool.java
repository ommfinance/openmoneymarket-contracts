package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import java.util.List;
import score.Address;
import score.annotation.Optional;
import score.annotation.Payable;

public interface LendingPool {
    String name();

    void setBridgeFeeThreshold(BigInteger _amount);

    BigInteger getBridgeFeeThreshold();

    void setFeeSharingTxnLimit(BigInteger _limit);

    BigInteger getFeeSharingTxnLimit();

    boolean isFeeSharingEnable(Address _user);

    List<Address> getDepositWallets(int _index);

    List<Address> getBorrowWallets(int _index);

    @Payable
    void deposit(BigInteger _amount);

    void redeem(Address _oToken, BigInteger _amount, @Optional boolean _waitForUnstaking);

    void claimRewards();

    void stake(BigInteger _value);

    void unstake(BigInteger _value);

    void borrow(Address _reserve, BigInteger _amount);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);
}
