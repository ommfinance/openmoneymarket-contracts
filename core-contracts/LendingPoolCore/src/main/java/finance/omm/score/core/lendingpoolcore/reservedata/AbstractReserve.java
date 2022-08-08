package finance.omm.score.core.lendingpoolcore.reservedata;

import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public class AbstractReserve {

    protected Map<String, Object> getDataFromUserReserve(Byte prefix, ReserveDataDB reserve) {
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

    protected void addDataToReserve(Byte prefix, ReserveDataDB reserve, ReserveDataObject reserveData){
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
    }

    protected ReserveDataObject createReserveDataObject(Map<String, Object> reserveData) {
        Map<String, Object> reserveDataDetails = new HashMap<>();
        reserveDataDetails.put("reserveAddress", reserveData.get("reserveAddress"));
        reserveDataDetails.put("oTokenAddress", reserveData.get("oTokenAddress"));
        reserveDataDetails.put("dTokenAddress", reserveData.get("dTokenAddress"));
        reserveDataDetails.put("lastUpdateTimestamp", reserveData.get("lastUpdateTimestamp"));
        reserveDataDetails.put("liquidityRate", reserveData.get("liquidityRate"));
        reserveDataDetails.put("borrowRate", reserveData.get("borrowRate"));
        reserveDataDetails.put("liquidityCumulativeIndex", reserveData.get("liquidityCumulativeIndex"));
        reserveDataDetails.put("borrowCumulativeIndex", reserveData.get("borrowCumulativeIndex"));
        reserveDataDetails.put("baseLTVasCollateral", reserveData.get("baseLTVasCollateral"));
        reserveDataDetails.put("liquidationThreshold", reserveData.get("liquidationThreshold"));
        reserveDataDetails.put("liquidationBonus", reserveData.get("liquidationBonus"));
        reserveDataDetails.put("decimals", reserveData.get("decimals"));
        reserveDataDetails.put("borrowingEnabled", reserveData.get("borrowingEnabled"));
        reserveDataDetails.put("usageAsCollateralEnabled", reserveData.get("usageAsCollateralEnabled"));
        reserveDataDetails.put("isFreezed", reserveData.get("isFreezed"));
        reserveDataDetails.put("isActive", reserveData.get("isActive"));
        reserveDataDetails.put("borrowThreshold", ICX);

        return new ReserveDataObject(reserveDataDetails);
    }
}
