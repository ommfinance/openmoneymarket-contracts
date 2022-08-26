package finance.omm.score.core.lendingpoolcore.reservedata;

import finance.omm.libs.structs.governance.ReserveAttributes;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public class AbstractReserve {

    public static Map<String, Object> getDataFromReserve(byte[] prefix, ReserveDataDB reserve) {
        Map<String, Object> reserveData = new HashMap<>();
        reserveData.put("reserveAddress", reserve.reserveAddress.get(prefix));
        reserveData.put("oTokenAddress", reserve.oTokenAddress.get(prefix));
        reserveData.put("dTokenAddress", reserve.dTokenAddress.get(prefix));
        reserveData.put("lastUpdateTimestamp", reserve.lastUpdateTimestamp.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("liquidityRate", reserve.liquidityRate.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("borrowRate", reserve.borrowRate.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("borrowThreshold", reserve.borrowThreshold.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("liquidityCumulativeIndex", reserve.liquidityCumulativeIndex.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("borrowCumulativeIndex", reserve.borrowCumulativeIndex.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("baseLTVasCollateral", reserve.baseLTVasCollateral.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("liquidationThreshold", reserve.liquidationThreshold.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("liquidationBonus", reserve.liquidationBonus.getOrDefault(prefix, BigInteger.ZERO));
        reserveData.put("decimals", reserve.decimals.getOrDefault(prefix, 0));
        reserveData.put("borrowingEnabled", reserve.borrowingEnabled.get(prefix));
        reserveData.put("usageAsCollateralEnabled", reserve.usageAsCollateralEnabled.get(prefix));
        reserveData.put("isFreezed", reserve.isFreezed.get(prefix));
        reserveData.put("isActive", reserve.isActive.get(prefix));

        return reserveData;
    }

    public static void addDataToReserve(byte[] prefix, ReserveDataDB reserve, ReserveDataObject reserveData) {
        reserve.dTokenAddress.set(prefix, reserveData.dTokenAddress);
        reserve.reserveAddress.set(prefix, reserveData.reserveAddress);
        reserve.oTokenAddress.set(prefix, reserveData.oTokenAddress);
        reserve.lastUpdateTimestamp.set(prefix, reserveData.lastUpdateTimestamp);
        reserve.liquidityRate.set(prefix, reserveData.liquidityRate);
        reserve.borrowRate.set(prefix, reserveData.borrowRate);
        reserve.liquidityCumulativeIndex.set(prefix, reserveData.liquidityCumulativeIndex);
        reserve.borrowCumulativeIndex.set(prefix, reserveData.borrowCumulativeIndex);
        reserve.baseLTVasCollateral.set(prefix, reserveData.baseLTVasCollateral);
        reserve.liquidationThreshold.set(prefix, reserveData.liquidationThreshold);
        reserve.liquidationBonus.set(prefix, reserveData.liquidationBonus);
        reserve.decimals.set(prefix, reserveData.decimals);
        reserve.borrowingEnabled.set(prefix, reserveData.borrowingEnabled);
        reserve.usageAsCollateralEnabled.set(prefix, reserveData.usageAsCollateralEnabled);
        reserve.isFreezed.set(prefix, reserveData.isFreezed);
        reserve.isActive.set(prefix, reserveData.isActive);
        reserve.borrowThreshold.set(prefix, ICX);
    }

    public static ReserveDataObject createReserveDataObject(ReserveAttributes reserveData) {
        Map<String, Object> reserveDataDetails = new HashMap<>();
        reserveDataDetails.put("reserveAddress", reserveData.reserveAddress);
        reserveDataDetails.put("oTokenAddress", reserveData.oTokenAddress);
        reserveDataDetails.put("dTokenAddress", reserveData.dTokenAddress);
        reserveDataDetails.put("lastUpdateTimestamp", reserveData.lastUpdateTimestamp);
        reserveDataDetails.put("liquidityRate", reserveData.liquidityRate);
        reserveDataDetails.put("borrowRate", reserveData.borrowRate);
        reserveDataDetails.put("liquidityCumulativeIndex", reserveData.liquidityCumulativeIndex);
        reserveDataDetails.put("borrowCumulativeIndex", reserveData.borrowCumulativeIndex);
        reserveDataDetails.put("baseLTVasCollateral", reserveData.baseLTVasCollateral);
        reserveDataDetails.put("liquidationThreshold", reserveData.liquidationThreshold);
        reserveDataDetails.put("liquidationBonus", reserveData.liquidationBonus);
        reserveDataDetails.put("decimals", reserveData.decimals);
        reserveDataDetails.put("borrowingEnabled", reserveData.borrowingEnabled);
        reserveDataDetails.put("usageAsCollateralEnabled", reserveData.usageAsCollateralEnabled);
        reserveDataDetails.put("isFreezed", reserveData.isFreezed);
        reserveDataDetails.put("isActive", reserveData.isActive);
        reserveDataDetails.put("borrowThreshold", ICX);

        return new ReserveDataObject(reserveDataDetails);
    }
}
