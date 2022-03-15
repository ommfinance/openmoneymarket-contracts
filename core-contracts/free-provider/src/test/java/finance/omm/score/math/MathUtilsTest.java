package finance.omm.score.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static finance.omm.score.math.MathUtils.*;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class MathUtilsTest {

	@Test
	void testExaMul() {

		assertEquals(exaMul(new BigInteger("8888888888888888889") , new BigInteger("8888888888888888889")), new BigInteger("79012345679012345681"));

		// 2 * 10**18 exaMul 2 * 10**18  ==  4 * 10**18
		assertEquals(exaMul( TWO.multiply( pow(TEN, 18)), TWO.multiply( pow(TEN, 18)) ),  new BigInteger("4").multiply(pow(TEN, 18) ));
	}

	@Test
	void testExaDiv() {
		assertEquals(exaDiv( TWO.multiply( pow(TEN, 18)), TWO.multiply( pow(TEN, 18)) ),  ONE.multiply(pow(TEN, 18) ));
	}

	@Test
	void testExaPow() {
		assertEquals(exaPow(TWO.multiply( pow(TEN, 18)), 3), new BigInteger("8").multiply( pow(TEN, 18)));
	}

	@Test
	void testPow() {
		assertEquals( pow(new BigInteger("7"), 18) , new BigInteger("1628413597910449") );
	}
}
