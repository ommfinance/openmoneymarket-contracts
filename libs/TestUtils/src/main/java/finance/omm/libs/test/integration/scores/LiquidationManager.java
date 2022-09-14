package finance.omm.libs.test.integration.scores;

import java.math.BigInteger;

public interface LiquidationManager {

    BigInteger calculateBadDebt(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD,
            BigInteger _totalCollateralBalanceUSD, BigInteger _ltv);
}
