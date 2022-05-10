package finance.omm.score.core.reward.db;

import java.math.BigInteger;
import score.DictDB;

public interface Searchable {

    default int searchCheckpoint(BigInteger timestamp, Integer checkpointCount,
            DictDB<Integer, BigInteger> timeCheckpoints) {
        BigInteger latestTimestamp = timeCheckpoints.getOrDefault(checkpointCount, BigInteger.ZERO);

        if (latestTimestamp.compareTo(timestamp) < 0) {
            return checkpointCount;
        }

        if (latestTimestamp.compareTo(timestamp) == 0 && checkpointCount != 1) {
            return checkpointCount - 1;
        }
        if (checkpointCount == 1 && latestTimestamp.compareTo(timestamp) >= 0) {
            return 0;
        }

        if (checkpointCount != 1) {
            BigInteger firstTimestamp = timeCheckpoints.get(1);
            if (firstTimestamp != null && firstTimestamp.compareTo(timestamp) >= 0) {
                return 1;
            }
        }
        int lower = 1, upper = checkpointCount;
        while (lower < upper) {
            int mid = (upper + lower + 1) / 2;
            BigInteger midTimestamp = timeCheckpoints.getOrDefault(mid, BigInteger.ZERO);
            int value = midTimestamp.compareTo(timestamp);
            if (value < 0) {
                lower = mid;
            } else if (value > 0) {
                upper = mid - 1;
            } else {
                return mid - 1;
            }
        }

        return lower;
    }
}
