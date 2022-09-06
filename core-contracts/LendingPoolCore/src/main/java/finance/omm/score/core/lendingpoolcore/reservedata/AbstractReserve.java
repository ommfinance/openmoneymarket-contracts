package finance.omm.score.core.lendingpoolcore.reservedata;

import finance.omm.libs.structs.governance.ReserveAttributes;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public class AbstractReserve {

    public static Map<String, Object> getDataFromReserve(String prefix, ReserveDataDB reserve) {
        Map<String, Object> reserveData = new HashMap<>();
        reserveData.put("reserveAddress", reserve.reserveAddress.at(prefix).get());
        reserveData.put("oTokenAddress", reserve.oTokenAddress.at(prefix).get());
        reserveData.put("dTokenAddress", reserve.dTokenAddress.at(prefix).get());
        reserveData.put("lastUpdateTimestamp", reserve.lastUpdateTimestamp.at(prefix).get());
        reserveData.put("liquidityRate", reserve.liquidityRate.at(prefix).get());
        reserveData.put("borrowRate", reserve.borrowRate.at(prefix).get());
        reserveData.put("borrowThreshold", reserve.borrowThreshold.at(prefix).get());
        reserveData.put("liquidityCumulativeIndex", reserve.liquidityCumulativeIndex.at(prefix).get());
        reserveData.put("borrowCumulativeIndex", reserve.borrowCumulativeIndex.at(prefix).get());
        reserveData.put("baseLTVasCollateral", reserve.baseLTVasCollateral.at(prefix).get());
        reserveData.put("liquidationThreshold", reserve.liquidationThreshold.at(prefix).get());
        reserveData.put("liquidationBonus", reserve.liquidationBonus.at(prefix).get());
        reserveData.put("decimals", reserve.decimals.at(prefix).get());
        reserveData.put("borrowingEnabled", reserve.borrowingEnabled.at(prefix).get());
        reserveData.put("usageAsCollateralEnabled", reserve.usageAsCollateralEnabled.at(prefix).get());
        reserveData.put("isFreezed", reserve.isFreezed.at(prefix).get());
        reserveData.put("isActive", reserve.isActive.at(prefix).get());

        return reserveData;
    }

    public static void addDataToReserve(String prefix, ReserveDataDB reserve, ReserveDataObject reserveData) {
        reserve.dTokenAddress.at(prefix).set(reserveData.dTokenAddress);
        reserve.reserveAddress.at(prefix).set(reserveData.reserveAddress);
        reserve.oTokenAddress.at(prefix).set(reserveData.oTokenAddress);
        reserve.lastUpdateTimestamp.at(prefix).set(reserveData.lastUpdateTimestamp);
        reserve.liquidityRate.at(prefix).set(reserveData.liquidityRate);
        reserve.borrowRate.at(prefix).set(reserveData.borrowRate);
        reserve.liquidityCumulativeIndex.at(prefix).set(reserveData.liquidityCumulativeIndex);
        reserve.borrowCumulativeIndex.at(prefix).set(reserveData.borrowCumulativeIndex);
        reserve.baseLTVasCollateral.at(prefix).set(reserveData.baseLTVasCollateral);
        reserve.liquidationThreshold.at(prefix).set(reserveData.liquidationThreshold);
        reserve.liquidationBonus.at(prefix).set(reserveData.liquidationBonus);
        reserve.decimals.at(prefix).set(reserveData.decimals);
        reserve.borrowingEnabled.at(prefix).set(reserveData.borrowingEnabled);
        reserve.usageAsCollateralEnabled.at(prefix).set(reserveData.usageAsCollateralEnabled);
        reserve.isFreezed.at(prefix).set(reserveData.isFreezed);
        reserve.isActive.at(prefix).set(reserveData.isActive);
        reserve.borrowThreshold.at(prefix).set(ICX);
    }

    public static ReserveDataObject createReserveDataObject(ReserveAttributes reserveData) {
        return new ReserveDataObject(reserveData);
    }
}
