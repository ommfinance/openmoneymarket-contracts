package finance.omm.score.core.lendingpoolcore.userreserve;

import java.math.BigInteger;
import java.util.Map;

public class AbstractUserReserve {

    public static Map<String, BigInteger> getDataFromUserReserve(byte[] prefix, UserReserveDataDB userReserve) {
        return Map.of(
                "lastUpdateTimestamp", userReserve.lastUpdateTimestamp.getOrDefault(prefix, BigInteger.ZERO),
                "originationFee", userReserve.originationFee.getOrDefault(prefix, BigInteger.ZERO)
        );
    }
}
