package finance.omm.score.math;

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

public final class MathUtils {

	private static final BigInteger EXA = pow(TEN, 18);
	private static final BigInteger HALF_EXA = EXA.divide(TWO); 

	public static BigInteger pow(BigInteger base, int exponent) {
		BigInteger result = BigInteger.ONE;
		for (int i = 0; i < exponent; i++) {
			result = result.multiply(base);
		}
		return result;
	}

	public static BigInteger exaMul(BigInteger a, BigInteger b) {
		return HALF_EXA.add( a.multiply(b) ).divide(EXA);
	}

	public static BigInteger exaDiv(BigInteger a, BigInteger b) {
		BigInteger halfB = b.divide(TWO);
		return halfB.add( a.multiply(EXA) ).divide(b);
	}

	//verify max value of n
	public static BigInteger exaPow(BigInteger x, int n){
		BigInteger z = ZERO;
		if (n % 2 != 0) {
			z = x;
		}else {
			z = EXA;
		}

		n = n / 2;
		while (n != 0){
			x = exaMul(x, x);

			if (n % 2 != 0) {
				z = exaMul(z, x);
			}
			n = n / 2;
		}

		return z;
	}
}
