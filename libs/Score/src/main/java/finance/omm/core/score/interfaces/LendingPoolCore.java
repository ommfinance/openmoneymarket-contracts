package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.Map;

import score.Address;

@ScoreInterface(suffix = "Client")
public interface LendingPoolCore {

    void updateIsActive(Address _reserve, boolean _status);

    void setReserveConstants(ReserveConstant[] _constants);

    void updateIsFreezed(Address _reserve, boolean _status);

    void addReserveData(ReserveAttributes _reserve);

    void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLtv);

    void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold);

    void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold);

    void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus);

    void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled);

    void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled);

    Map<String, Object> getReserveConfiguration(Address _reserve);

    Map<String,Object> getReserveConstants(Address _reserve);
}
