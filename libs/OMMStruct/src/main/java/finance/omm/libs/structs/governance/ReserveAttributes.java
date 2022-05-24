package finance.omm.libs.structs.governance;

import java.math.BigInteger;
import score.Address;

public class ReserveAttributes {

    public Address reserveAddress;
    public Address oTokenAddress;
    public Address dTokenAddress;
    public BigInteger lastUpdateTimestamp;
    public BigInteger liquidityRate;
    public BigInteger borrowRate;
    public BigInteger liquidityCumulativeIndex;
    public BigInteger borrowCumulativeIndex;
    public BigInteger baseLTVasCollateral;
    public BigInteger liquidationThreshold;
    public BigInteger liquidationBonus;
    public int decimals;
    public boolean borrowingEnabled;
    public boolean usageAsCollateralEnabled;
    public boolean isFreezed;
    public boolean isActive;
}
