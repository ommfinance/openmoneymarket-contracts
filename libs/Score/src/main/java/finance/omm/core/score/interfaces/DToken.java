package finance.omm.core.score.interfaces;

import java.math.BigInteger;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;
import score.annotation.External;

public interface DToken extends AddressProvider {

    @External(readonly = true)
    String name();
    
    @External(readonly = true)
    String symbol();    
    
    @External(readonly = true)
    BigInteger decimals();
    
    @External(readonly = true)
    BigInteger getUserBorrowCumulativeIndex(Address _user);
    
    @External(readonly = true)
    BigInteger balanceOf(Address _owner);
    
    @External(readonly = true)
    BigInteger principalBalanceOf(Address _user);
    
    @External(readonly = true)
    BigInteger principalTotalSupply();
    
    @External(readonly = true)
    SupplyDetails getPrincipalSupply(Address _user);
    
    @External(readonly = true)
    BigInteger totalSupply();
    
    @External(readonly = true)
    void mintOnBorrow(Address _user, BigInteger _amount, BigInteger _balanceIncrease);
    
    @External(readonly = true)
    void  burnOnRepay(Address _user, BigInteger _amount, BigInteger _balanceIncrease);
    
    @External(readonly = true)
    void burnOnLiquidation(Address _user, BigInteger _amount, BigInteger _balanceIncrease);
    
    @External(readonly = true)
    void transfer(Address _to, BigInteger _value, byte[] _data);
    
    @External(readonly = true)
    TotalStaked getTotalStaked();
}
