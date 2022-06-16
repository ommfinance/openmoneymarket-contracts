package finance.omm.core.score.interfaces;

import score.Address;

import java.math.BigInteger;
import java.util.Map;

import static java.math.BigInteger.ZERO;

public interface LiquidationManager {

    String name();

    BigInteger calculateBadDebt(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger
            _totalCollateralBalanceUSD, BigInteger _ltv);

    Map<String,BigInteger> liquidationCall(Address _collateral, Address _reserve, Address _user,
                                           BigInteger _purchaseAmount);

}
