package finance.omm.core.score.interfaces;

import score.Address;

import java.math.BigInteger;
import java.util.Map;

import static java.math.BigInteger.ZERO;

public interface LiquidationManager {

    public String name();

    public BigInteger calculateBadDebt(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger _totalCollateralBalanceUSD, BigInteger _ltv);

    public Map<String,BigInteger> calculateAvailableCollateralToLiquidate(Address _collateral, Address _reserve, BigInteger _purchaseAmount, BigInteger _userCollateralBalance, Boolean _fee);

//    public static BigInteger calculateCurrentLiquidationThreshold(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger _totalCollateralBalanceUSD){
//        if (_totalCollateralBalanceUSD.compareTo(ZERO)==0){
//            return ZERO;
//        }
//        return exaDivide();
//    }

    public Map<String,BigInteger> liquidationCall(Address _collateral, Address _reserve, Address _user, BigInteger _purchaseAmount);

}
