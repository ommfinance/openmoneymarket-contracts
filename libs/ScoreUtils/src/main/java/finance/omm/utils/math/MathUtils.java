package finance.omm.utils.math;

import com.eclipsesource.json.JsonValue;
import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;

public class MathUtils {

    public static BigInteger ICX = pow10(18);
    public static BigInteger HUNDRED_PERCENT = ICX;
    public static BigInteger HALF_ICX = ICX.divide(BigInteger.TWO);
    public static BigInteger MILLION = BigInteger.valueOf(1_000_000L).multiply(ICX);
    public static BigInteger HUNDRED_THOUSAND = BigInteger.valueOf(100_000L).multiply(ICX);


    public static boolean isLessThan(BigInteger first, BigInteger second) {
        return first.compareTo(second) < 0;
    }

    public static boolean isLessThanEqual(BigInteger first, BigInteger second) {
        return first.compareTo(second) <= 0;
    }

    public static boolean isGreaterThanEqual(BigInteger first, BigInteger second) {
        return first.compareTo(second) >= 0;
    }

    public static boolean isGreaterThan(BigInteger first, BigInteger second) {
        return first.compareTo(second) > 0;
    }

    public static boolean isValidPercentage(BigInteger value) {
        return value != null && value.compareTo(BigInteger.ZERO) >= 0 && value.compareTo(ICX) <= 0;
    }


    public static BigInteger pow10(int exponent) {
        return pow(BigInteger.TEN, exponent);
    }

    public static BigInteger pow(BigInteger num, int exponent) {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(num);
        }
        return result;
    }

    public static BigInteger min(BigInteger[] list) {
        BigInteger min = list[0];
        for (BigInteger num : list) {
            if (num.compareTo(min) < 0) {
                min = num;
            }
        }
        return min;

    }

    public static BigInteger exaMultiply(BigInteger first, BigInteger second) {
        return HALF_ICX.add(first.multiply(second)).divide(ICX);
    }

    public static BigInteger exaDivide(BigInteger first, BigInteger second) {
        BigInteger halfSecond = second.divide(BigInteger.TWO);
        return halfSecond.add(first.multiply(ICX)).divide(second);
    }

    /**
     * If a=8*EXA and b=3*EXA ,it  returns 2666666666666666666
     * If a=100*EXA and b=6*EXA , it returns 16666666666666666666
     */
    public static BigInteger exaDivideFloor(BigInteger first, BigInteger second) {
        return first.multiply(ICX).divide(second);
    }

    public static BigInteger convertToExa(BigInteger _amount, BigInteger _decimals) {
        Integer decimal = _decimals.intValue();
        if (decimal.equals(18)) {
            return _amount;
        }
        if (decimal >= 0) {
            return _amount.multiply(ICX).divide(pow10(decimal));
        }
        return _amount;
    }


    public static BigInteger convertExaToOther(BigInteger _amount, Integer _decimals) {
        if (_decimals >= 0) {
            return _amount.multiply(pow10(_decimals)).divide(ICX);
        }
        return _amount;
    }

    public static double percentageInHundred(BigInteger value) {
        return 100*value.doubleValue()/ICX.doubleValue();
    }

    public static BigInteger convertToNumber(JsonValue value) {
        return convertToNumber(value, null);
    }


    public static BigInteger convertToNumber(JsonValue value, BigInteger defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value.isString()) {
            String number = value.asString();
            try {
                if (number.startsWith("0x") || number.startsWith("-0x")) {
                    return new BigInteger(number.replace("0x", ""), 16);
                } else {
                    return new BigInteger(number);
                }
            } catch (NumberFormatException e) {
                throw OMMException.unknown("Invalid numeric value: " + number);
            }
        } else if (value.isNumber()) {
            return new BigInteger(value.toString());
        }
        throw OMMException.unknown("Invalid value format for minimum receive amount: " + value);
    }
}
