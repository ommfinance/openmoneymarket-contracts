package finance.omm.score.core.lendingpoolcore.reservedata;

import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.utils.math.MathUtils;
import score.Address;

import java.math.BigInteger;

public class ReserveDataObject {

    public Address reserveAddress;
    public Address oTokenAddress;
    public Address dTokenAddress;
    public BigInteger lastUpdateTimestamp;
    public BigInteger liquidityRate;
    public BigInteger borrowRate;
    public BigInteger borrowThreshold;
    public BigInteger liquidityCumulativeIndex;
    public BigInteger borrowCumulativeIndex;
    public BigInteger baseLTVasCollateral;
    public BigInteger liquidationThreshold;
    public BigInteger liquidationBonus;
    public int decimals;
    public Boolean borrowingEnabled;
    public Boolean usageAsCollateralEnabled;
    public Boolean isFreezed;
    public Boolean isActive;

    ReserveDataObject(ReserveAttributes reserveData) {
        this.reserveAddress = reserveData.reserveAddress;
        this.oTokenAddress = reserveData.oTokenAddress;
        this.dTokenAddress = reserveData.dTokenAddress;
        this.lastUpdateTimestamp = reserveData.lastUpdateTimestamp;
        this.liquidityRate = reserveData.liquidityRate;
        this.borrowRate = reserveData.borrowRate;
        this.liquidityCumulativeIndex = reserveData.liquidityCumulativeIndex;
        this.borrowCumulativeIndex = reserveData.borrowCumulativeIndex;
        this.baseLTVasCollateral = reserveData.baseLTVasCollateral;
        this.liquidationThreshold = reserveData.liquidationThreshold;
        this.liquidationBonus = reserveData.liquidationBonus;
        this.decimals = reserveData.decimals;
        this.borrowingEnabled = reserveData.borrowingEnabled;
        this.usageAsCollateralEnabled = reserveData.usageAsCollateralEnabled;
        this.isFreezed = reserveData.isFreezed;
        this.isActive = reserveData.isActive;
        this.borrowThreshold = MathUtils.ICX;
    }

}
