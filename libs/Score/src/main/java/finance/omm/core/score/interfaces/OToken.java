package finance.omm.core.score.interfaces;

import finance.omm.core.score.interfaces.token.IRC2;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;

import java.math.BigInteger;
import java.util.Map;

public interface OToken extends IRC2, AddressProvider {

    BigInteger principalTotalSupply();

    BigInteger getUserLiquidityCumulativeIndex(Address _user);

    BigInteger principalBalanceOf(Address _user);

    boolean isTransferAllowed(Address _user, BigInteger _amount);

    SupplyDetails getPrincipalSupply(Address _user);

    void enableHandleAction();

    void disableHandleAction();

    boolean isHandleActionEnabled();

    Map<String, ?> redeem(Address _user, BigInteger _amount);

    void mintOnDeposit(Address _user, BigInteger _amount);

    void burnOnLiquidation(Address _user, BigInteger _value);

    TotalStaked getTotalStaked();

}
