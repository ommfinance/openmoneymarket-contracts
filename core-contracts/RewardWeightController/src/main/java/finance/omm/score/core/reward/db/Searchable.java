package finance.omm.score.core.reward.db;

import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import score.DictDB;

public interface Searchable {

    default int searchCheckpoint(BigInteger timestamp, Integer checkpointCount,
            DictDB<Integer, BigInteger> timeCheckpoints) {
        int compareWithLatestTimestamp = timeCheckpoints.getOrDefault(checkpointCount, BigInteger.ZERO)
                .compareTo(timestamp);

        if (compareWithLatestTimestamp < 0) {
            return checkpointCount;
        }

        /*
        return previous checkpoint if latest timestamp and search timestamp is equals
         */
        if (compareWithLatestTimestamp == 0) {
            return checkpointCount - 1;
        }

        int lower = 0, upper = checkpointCount;
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

    default BigInteger getBlockTimestampInSecond() {
        return TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);
    }
}
