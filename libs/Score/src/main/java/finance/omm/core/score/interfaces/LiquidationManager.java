package finance.omm.core.score.interfaces;

import score.Address;

import java.math.BigInteger;
import java.util.Map;

import static java.math.BigInteger.ZERO;

public interface LiquidationManager {

    String name();

    Map<String,BigInteger> calculateAvailableCollateralToLiquidate(Address _collateral, Address _reserve, BigInteger
            _purchaseAmount, BigInteger _userCollateralBalance, Boolean _fee);

    Map<String,BigInteger> liquidationCall(Address _collateral, Address _reserve, Address _user, BigInteger
            _purchaseAmount);

}
