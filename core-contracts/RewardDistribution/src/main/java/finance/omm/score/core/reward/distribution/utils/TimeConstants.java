package finance.omm.score.core.reward.distribution.utils;

import score.Context;

import java.math.BigInteger;

public class TimeConstants {
    public static final BigInteger SECOND = BigInteger.valueOf(1000000L);
    public static final BigInteger MINUTE_IN_SECONDS = BigInteger.valueOf(60L);
    public static final BigInteger HOUR_IN_SECONDS = BigInteger.valueOf(60L).multiply(MINUTE_IN_SECONDS);
    public static final BigInteger DAY_IN_SECONDS = BigInteger.valueOf(24L).multiply(HOUR_IN_SECONDS);
    public static final BigInteger WEEK = BigInteger.valueOf(7L).multiply(DAY_IN_SECONDS);
    public static final BigInteger MONTH_IN_SECONDS = BigInteger.valueOf(30L).multiply(DAY_IN_SECONDS);

    public static final BigInteger DAYS_PER_YEAR = BigInteger.valueOf(365L);
    public static final BigInteger YEAR_IN_SECONDS = DAYS_PER_YEAR.multiply(DAY_IN_SECONDS);


    /**
     * get current block timestamp in seconds
     *
     * @return - BigInteger
     */
    public static BigInteger getBlockTimestamp() {
        return BigInteger.valueOf(Context.getBlockTimestamp()).divide(SECOND);
    }
}
