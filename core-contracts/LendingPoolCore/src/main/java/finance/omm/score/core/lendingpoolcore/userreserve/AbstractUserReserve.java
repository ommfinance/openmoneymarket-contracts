package finance.omm.score.core.lendingpoolcore.userreserve;

import java.math.BigInteger;
import java.util.Map;

public class AbstractUserReserve {

    public static Map<String, BigInteger> getDataFromUserReserve(String prefix, UserReserveDataDB userReserve) {
        return Map.of(
                "lastUpdateTimestamp", userReserve.lastUpdateTimestamp.at(prefix).getOrDefault(BigInteger.ZERO),
                "originationFee", userReserve.originationFee.at(prefix).getOrDefault(BigInteger.ZERO)
        );
    }
}
