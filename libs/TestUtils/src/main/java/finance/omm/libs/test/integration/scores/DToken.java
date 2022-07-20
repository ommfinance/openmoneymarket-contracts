package finance.omm.libs.test.integration.scores;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public interface DToken {

    String name();

    @External(readonly = true)
    BigInteger balanceOf(Address _owner);

    @External(readonly = true)
    BigInteger principalTotalSupply();

    BigInteger getUserBorrowCumulativeIndex(Address _user);
    BigInteger principalBalanceOf(Address _user);

    SupplyDetails getPrincipalSupply(Address _user);

    BigInteger totalSupply();

    void transfer(Address _to, BigInteger _value, @Optional byte[] _data);

    TotalStaked getTotalStaked();


}
