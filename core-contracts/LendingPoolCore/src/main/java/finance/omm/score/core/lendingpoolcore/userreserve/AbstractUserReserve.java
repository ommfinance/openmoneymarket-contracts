package finance.omm.score.core.lendingpoolcore.userreserve;

import java.math.BigInteger;
import java.util.Map;

public class AbstractUserReserve {

    protected Map<String, BigInteger> getDataFromUserReserve(Byte prefix, UserReserveDataDB userReserve) {
        return Map.of(
                "lastUpdateTimestamp", userReserve.getItem(prefix).lastUpdateTimestamp.getOrDefault(BigInteger.ZERO),
                "originationFee", userReserve.getItem(prefix).originationFee.getOrDefault(BigInteger.ZERO)
        );
    }
}
