package finance.omm.utils.constants;

import finance.omm.utils.math.UnsignedBigInteger;
import java.math.BigInteger;
import score.Context;

public class TimeConstants {

    public static final BigInteger SECOND = BigInteger.valueOf(1000000L);
    public static final BigInteger MINUTE_IN_MICRO_SECONDS = BigInteger.valueOf(60L).multiply(SECOND);
    public static final BigInteger HOUR_IN_MICRO_SECONDS = BigInteger.valueOf(60L).multiply(MINUTE_IN_MICRO_SECONDS);
    public static final BigInteger DAY_IN_MICRO_SECONDS = BigInteger.valueOf(24L).multiply(HOUR_IN_MICRO_SECONDS);
    public static final BigInteger DAY_IN_SECONDS = BigInteger.valueOf(60 * 60 * 24);
    public static final BigInteger WEEK_IN_MICRO_SECONDS = BigInteger.valueOf(7L).multiply(DAY_IN_MICRO_SECONDS);
    public static final BigInteger MONTH_IN_MICRO_SECONDS = BigInteger.valueOf(30L).multiply(DAY_IN_MICRO_SECONDS);

    public static final BigInteger DAYS_PER_YEAR = BigInteger.valueOf(365L);
    public static final BigInteger YEAR_IN_MICRO_SECONDS = DAYS_PER_YEAR.multiply(DAY_IN_MICRO_SECONDS);

    public static final UnsignedBigInteger U_WEEK_IN_MICRO_SECONDS = new UnsignedBigInteger(WEEK_IN_MICRO_SECONDS);

    /**
     * get current block timestamp in microseconds
     *
     * @return - BigInteger
     */
    public static BigInteger getBlockTimestamp() {
        return BigInteger.valueOf(Context.getBlockTimestamp());
    }
}
