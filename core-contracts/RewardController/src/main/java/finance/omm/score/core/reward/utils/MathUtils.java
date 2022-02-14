package finance.omm.score.core.reward.utils;

import java.math.BigInteger;

public class MathUtils {

    public static BigInteger ICX = pow10(18);
    public static BigInteger HALF_ICX = ICX.divide(BigInteger.TWO);
    public static BigInteger MILLION = BigInteger.valueOf(1_000_000L).multiply(pow10(18));


    public static boolean isLesThan(BigInteger first, BigInteger second) {
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
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(BigInteger.TEN);
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
//        halfB = b // 2
//        return (halfB + (a * EXA)) // b
        BigInteger halfSecond = second.divide(BigInteger.TWO);
        return halfSecond.add(first.multiply(ICX)).divide(halfSecond);
    }
}
