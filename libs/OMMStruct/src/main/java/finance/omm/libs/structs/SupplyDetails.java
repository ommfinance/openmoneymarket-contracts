package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;

public class SupplyDetails {

    public BigInteger decimals;
    public BigInteger principalUserBalance;
    public BigInteger principalTotalSupply;

    public static SupplyDetails fromMap(Map<String, ?> map) {
        SupplyDetails supplyDetails = new SupplyDetails();
        supplyDetails.decimals = (BigInteger) map.get("decimals");
        supplyDetails.principalUserBalance = (BigInteger) map.get("principalUserBalance");
        supplyDetails.principalTotalSupply = (BigInteger) map.get("principalTotalSupply");
        return supplyDetails;
    }
}
