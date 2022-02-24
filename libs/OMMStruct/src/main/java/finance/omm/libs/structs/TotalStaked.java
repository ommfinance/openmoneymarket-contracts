package finance.omm.libs.structs;

import java.math.BigInteger;
import java.util.Map;

public class TotalStaked {

    public BigInteger decimals;
    public BigInteger totalStaked;


    public static TotalStaked fromMap(Map<String, ?> map) {
        TotalStaked staked = new TotalStaked();
        staked.decimals = (BigInteger) map.get("decimals");
        staked.totalStaked = (BigInteger) map.get("totalStaked");
        return staked;
    }
}
