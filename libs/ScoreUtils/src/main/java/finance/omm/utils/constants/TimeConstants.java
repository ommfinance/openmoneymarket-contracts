package finance.omm.utils.constants;

import finance.omm.utils.math.UnsignedBigInteger;
import java.math.BigInteger;
import score.Context;

public class TimeConstants {

    public enum Timestamp {
        SECONDS, MILLI_SECONDS, MICRO_SECONDS
    }

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

    /**
     * check if length of value for timestamp validity
     * <pre>
     * 10 : seconds
     * 13 : milliseconds
     * 16 : microseconds
     * </pre>
     *
     * @param value  - BigInteger timestamp value
     * @param format - SECONDS, MILLI_SECONDS, MICRO_SECONDS
     */
    public static void checkIsValidTimestamp(BigInteger value, Timestamp format) {
        boolean isValid = false;
        switch (format) {
            case SECONDS:
                isValid = value.toString().length() == 10;
                break;
            case MILLI_SECONDS:
                isValid = value.toString().length() == 13;
                break;
            case MICRO_SECONDS:
                isValid = value.toString().length() == 16;
                break;
        }
        if (!isValid) {
            Context.revert("Invalid timestamp value " + value + " (" + format + ")");
        }
    }
}
