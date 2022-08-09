package finance.omm.core.score.interfaces;

import finance.omm.core.score.interfaces.token.IRC2;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;

import java.math.BigInteger;

public interface DToken extends AddressProvider, IRC2 {
    BigInteger getUserBorrowCumulativeIndex(Address _user);

    BigInteger principalBalanceOf(Address _user);

    BigInteger principalTotalSupply();

    SupplyDetails getPrincipalSupply(Address _user);

    void enableHandleAction();

    void disableHandleAction();

    boolean isHandleActionEnabled();

    void mintOnBorrow(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    void burnOnRepay(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    void burnOnLiquidation(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    TotalStaked getTotalStaked();
}
