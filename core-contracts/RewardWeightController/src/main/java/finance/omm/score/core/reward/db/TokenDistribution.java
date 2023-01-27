package finance.omm.score.core.reward.db;

import finance.omm.score.core.reward.exception.RewardWeightException;
import finance.omm.utils.math.MathUtils;
import score.Context;
import score.DictDB;
import score.VarDB;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.constants.TimeConstants.DAYS_PER_YEAR;
import static finance.omm.utils.math.MathUtils.HUNDRED_THOUSAND;
import static finance.omm.utils.math.MathUtils.MILLION;

public class TokenDistribution {

    private final DictDB<Integer, BigInteger> distributionCheckpoints;
    private final VarDB<Integer> checkpointCount;
    private final DictDB<Integer, BigInteger> dayCheckpoints;

    public TokenDistribution(String key) {
        this.distributionCheckpoints = Context.newDictDB(key + "-distribution-checkpoints", BigInteger.class);
        this.checkpointCount = Context.newVarDB(key + "-checkpoint-count", Integer.class);
        this.dayCheckpoints = Context.newDictDB(key + "-day-checkpoints", BigInteger.class);
    }

    public void init() {
        if (this.getCheckpointCount() > 0) {
            return;
        }
        distributionCheckpoints.set(0, MILLION);
        dayCheckpoints.set(0, BigInteger.ZERO);

        distributionCheckpoints.set(1, BigInteger.valueOf(4L).multiply(HUNDRED_THOUSAND));
        dayCheckpoints.set(1, BigInteger.valueOf(30));

        distributionCheckpoints.set(2, BigInteger.valueOf(3L).multiply(HUNDRED_THOUSAND));
        dayCheckpoints.set(2, DAYS_PER_YEAR);

        distributionCheckpoints.set(3,BigInteger.ZERO);
        dayCheckpoints.set(3,BigInteger.valueOf(535));

        distributionCheckpoints.set(4, BigInteger.valueOf(2L).multiply(HUNDRED_THOUSAND));
        dayCheckpoints.set(4, DAYS_PER_YEAR.multiply(BigInteger.TWO));

        distributionCheckpoints.set(5, HUNDRED_THOUSAND);
        dayCheckpoints.set(5, DAYS_PER_YEAR.multiply(BigInteger.valueOf(3)));

        checkpointCount.set(6);
    }

    public Integer getCheckpointCount() {
        return this.checkpointCount.getOrDefault(0);
    }

    public Map<String, BigInteger> getTokenDistribution(BigInteger day) {
        int _index = searchIndexForDay(day);
        return Map.of("totalDistribution", this.distributionCheckpoints.get(_index),
                "distributionChangedOn", this.dayCheckpoints.get(_index));
    }

    protected int searchIndexForDay(BigInteger day) {
        int low = 0;
        int high = checkpointCount.getOrDefault(0);

        if (day.compareTo(dayCheckpoints.get(0)) < 0) {
            Context.revert("invalid day ");
        }

        if (day.compareTo(dayCheckpoints.get(high - 1)) >= 0) {
            return high - 1;
        }

        while (low < high) {
            int mid = (low + high + 1) / 2;
            var _day = dayCheckpoints.get(mid);
            var _compare = _day.compareTo(day);
            if (_compare > 0) {
                high = mid - 1;
            } else if (_compare < 0) {
                low = mid;
            } else {
                return mid;
            }
        }
        return low;
    }

    public void updateTokenDistribution(BigInteger day, BigInteger value) {
        if (!MathUtils.isGreaterThanEqual(value, BigInteger.ZERO)) {
            throw RewardWeightException.unknown("Value cannot be negative");
        }

        var count = checkpointCount.get();
        var latestDay = dayCheckpoints.get(count - 1);
        if (latestDay.compareTo(day) < 0) {
            checkpointCount.set(count + 1);
            dayCheckpoints.set(count, day);
            distributionCheckpoints.set(count, value);
            return;
        }
        var index = searchIndexForDay(day);

        if (dayCheckpoints.get(index).compareTo(day) != 0) {
            index = index + 1;
        }

        dayCheckpoints.set(index, day);
        distributionCheckpoints.set(index, value);
        checkpointCount.set(index + 1);

    }

}