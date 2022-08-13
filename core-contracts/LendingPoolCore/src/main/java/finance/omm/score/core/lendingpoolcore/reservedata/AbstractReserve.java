package finance.omm.score.core.lendingpoolcore.reservedata;

import finance.omm.libs.structs.governance.ReserveAttributes;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public class AbstractReserve {

    public static Map<String, Object> getDataFromReserve(byte[] prefix, ReserveDataDB reserve) {
        Map<String, Object> reserveData = new HashMap<>();
        reserveData.put("reserveAddress", reserve.getItem(prefix).reserveAddress.get());
        reserveData.put("oTokenAddress", reserve.getItem(prefix).oTokenAddress.get());
        reserveData.put("dTokenAddress", reserve.getItem(prefix).dTokenAddress.get());
        reserveData.put("lastUpdateTimestamp", reserve.getItem(prefix).lastUpdateTimestamp.getOrDefault(BigInteger.ZERO));
        reserveData.put("liquidityRate", reserve.getItem(prefix).liquidityRate.getOrDefault(BigInteger.ZERO));
        reserveData.put("borrowRate", reserve.getItem(prefix).borrowRate.getOrDefault(BigInteger.ZERO));
        reserveData.put("borrowThreshold", reserve.getItem(prefix).borrowThreshold.getOrDefault(BigInteger.ZERO));
        reserveData.put("liquidityCumulativeIndex", reserve.getItem(prefix).liquidityCumulativeIndex.getOrDefault(BigInteger.ZERO));
        reserveData.put("borrowCumulativeIndex", reserve.getItem(prefix).borrowCumulativeIndex.getOrDefault(BigInteger.ZERO));
        reserveData.put("baseLTVasCollateral", reserve.getItem(prefix).baseLTVasCollateral.getOrDefault(BigInteger.ZERO));
        reserveData.put("liquidationThreshold", reserve.getItem(prefix).liquidationThreshold.getOrDefault(BigInteger.ZERO));
        reserveData.put("liquidationBonus", reserve.getItem(prefix).liquidationBonus.getOrDefault(BigInteger.ZERO));
        reserveData.put("decimals", reserve.getItem(prefix).decimals.getOrDefault(BigInteger.ZERO));
        reserveData.put("borrowingEnabled", reserve.getItem(prefix).borrowingEnabled.get());
        reserveData.put("usageAsCollateralEnabled", reserve.getItem(prefix).usageAsCollateralEnabled.get());
        reserveData.put("isFreezed", reserve.getItem(prefix).isFreezed.get());
        reserveData.put("isActive", reserve.getItem(prefix).isActive.get());

        return reserveData;
    }

    public static void addDataToReserve(byte[] prefix, ReserveDataDB reserve, ReserveDataObject reserveData) {
        reserve.getItem(prefix).dTokenAddress.set(reserveData.dTokenAddress);
        reserve.getItem(prefix).reserveAddress.set(reserveData.reserveAddress);
        reserve.getItem(prefix).oTokenAddress.set(reserveData.oTokenAddress);
        reserve.getItem(prefix).lastUpdateTimestamp.set(reserveData.lastUpdateTimestamp);
        reserve.getItem(prefix).liquidityRate.set(reserveData.liquidityRate);
        reserve.getItem(prefix).borrowRate.set(reserveData.borrowRate);
        reserve.getItem(prefix).liquidityCumulativeIndex.set(reserveData.liquidityCumulativeIndex);
        reserve.getItem(prefix).borrowCumulativeIndex.set(reserveData.borrowCumulativeIndex);
        reserve.getItem(prefix).baseLTVasCollateral.set(reserveData.baseLTVasCollateral);
        reserve.getItem(prefix).liquidationThreshold.set(reserveData.liquidationThreshold);
        reserve.getItem(prefix).liquidationBonus.set(reserveData.liquidationBonus);
        reserve.getItem(prefix).decimals.set(reserveData.decimals);
        reserve.getItem(prefix).borrowingEnabled.set(reserveData.borrowingEnabled);
        reserve.getItem(prefix).usageAsCollateralEnabled.set(reserveData.usageAsCollateralEnabled);
        reserve.getItem(prefix).isFreezed.set(reserveData.isFreezed);
        reserve.getItem(prefix).isActive.set(reserveData.isActive);
        reserve.getItem(prefix).borrowThreshold.set(ICX);
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
