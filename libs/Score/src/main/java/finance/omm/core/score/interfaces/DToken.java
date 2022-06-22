package finance.omm.core.score.interfaces;

import java.math.BigInteger;

import finance.omm.core.score.interfaces.token.IRC2;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;
import score.annotation.External;

public interface DToken extends AddressProvider, IRC2{

    @External(readonly = true)
    BigInteger getUserBorrowCumulativeIndex(Address _user);

    @External(readonly = true)
    BigInteger principalBalanceOf(Address _user);

    @External(readonly = true)
    BigInteger principalTotalSupply();

    @External(readonly = true)
    SupplyDetails getPrincipalSupply(Address _user);

    @External
    void mintOnBorrow(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    @External
    void  burnOnRepay(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    @External
    void burnOnLiquidation(Address _user, BigInteger _amount, BigInteger _balanceIncrease);

    @External(readonly = true)
    TotalStaked getTotalStaked();
}
