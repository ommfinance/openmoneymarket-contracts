package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;

public class SupplyDetails {

    public BigInteger decimals;
    public BigInteger principalUserBalance;
    public BigInteger principalTotalSupply;

    public static SupplyDetails fromMap(Map<String, BigInteger> map) {
        SupplyDetails supplyDetails = new SupplyDetails();
        supplyDetails.decimals = map.get("decimals");
        supplyDetails.principalUserBalance = map.get("principalUserBalance");
        supplyDetails.principalTotalSupply = map.get("principalTotalSupply");
        return supplyDetails;
    }
}
