package finance.omm.utils.math;

import java.math.BigInteger;

public class MathUtils {

    public static BigInteger ICX = pow10(18);
    public static BigInteger HALF_ICX = ICX.divide(BigInteger.TWO);
    public static BigInteger MILLION = BigInteger.valueOf(1_000_000L).multiply(pow10(18));
    public static BigInteger HUNDRED_THOUSAND = BigInteger.valueOf(100_000L).multiply(pow10(18));


    public static boolean isLessThan(BigInteger first, BigInteger second) {
        return first.compareTo(second) < 0;
    }

    public static boolean isLesThanEqual(BigInteger first, BigInteger second) {
        return first.compareTo(second) <= 0;
    }

    public static boolean isGreaterThanEqual(BigInteger first, BigInteger second) {
        return first.compareTo(second) >= 0;
    }

    public static boolean isGreaterThan(BigInteger first, BigInteger second) {
        return first.compareTo(second) > 0;
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
}
