package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import java.util.Map;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;

public interface OToken {

    String name();
    String symbol();
    BigInteger decimals();
    BigInteger principalTotalSupply();
    BigInteger totalSupply();
    BigInteger getUserLiquidityCumulativeIndex(Address _user);
    BigInteger balanceOf(Address _owner);
    BigInteger principalBalanceOf(Address _user);
    boolean isTransferAllowed(Address _user, BigInteger _amount);
    SupplyDetails getPrincipalSupply(Address _user);
    Map<String, Object> redeem(Address _user, BigInteger _amount);
    void mintOnDeposit(Address _user, BigInteger _amount);
    void burnOnLiquidation(Address _user, BigInteger _value);
    void transfer(Address _to, BigInteger _value, byte[] _data);
    TotalStaked getTotalStaked();

}
