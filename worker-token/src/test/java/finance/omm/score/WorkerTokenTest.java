package finance.omm.score;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import score.Address;
import score.Context;

public class WorkerTokenTest extends TestBase{

	private static ServiceManager sm = getServiceManager();
	private static Account owner = sm.createAccount();

	private static BigInteger decimals = BigInteger.valueOf(10);
	private static BigInteger initialSupply = BigInteger.valueOf(5);
	private static BigInteger totalSupply = new BigInteger("50000000000");

	private Score workerToken;

	@BeforeAll
	public static void init() {
		owner.addBalance(WorkerToken.WORKER_TOKEN_SYMBOL, totalSupply);
	}

	@BeforeEach
	public void setup() throws Exception {
		workerToken = sm.deploy(owner, WorkerToken.class, initialSupply, decimals, false);
	}

	@Test
	void testTransfer() {
		Account receiver = sm.createAccount();
		BigInteger balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());

		assertEquals(ZERO, balance);

		BigInteger amount = new BigInteger("500");
		workerToken.invoke(owner, "transfer", receiver.getAddress(), amount, "hotdogs".getBytes());

		balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());

		assertEquals(amount, balance);
	}

	@Test
	void testTransferToScore() {
		try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){
			Account score = sm.createAccount();
			Address scoreAddress = spy(score.getAddress());
			BigInteger balance = (BigInteger)workerToken.call("balanceOf", scoreAddress);
			BigInteger amount = new BigInteger("500");
			byte[] data = "settlement".getBytes();

			assertEquals(ZERO, balance);

			Mockito.when(scoreAddress.isContract()).thenReturn(true);

			theMock
			.when(() -> Context.call(scoreAddress, "tokenFallback", owner.getAddress(), amount, data))
			.thenReturn(null);

			theMock
			.when(() -> Context.getCaller())
			.thenReturn(owner.getAddress());

			workerToken.invoke(owner, "transfer", scoreAddress, amount, data);

			balance = (BigInteger)workerToken.call("balanceOf", scoreAddress);

			assertEquals(amount, balance);
			Mockito.verify(scoreAddress).isContract();

			theMock
			.verify(() -> Context.call(scoreAddress, "tokenFallback", owner.getAddress(), amount, data),
					Mockito.times(1));
		}
	}

	@Test
	void testTransferZeroAmount() {
		Account receiver = sm.createAccount();
		BigInteger balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());

		assertEquals(ZERO, balance);

		try {
			workerToken.invoke(owner, "transfer", receiver.getAddress(), ZERO, "hotdogs".getBytes());
		}catch (AssertionError e) {
			assertEquals("Reverted(0): Worker Token: Transferring value should be greater than zero", e.getMessage());
			balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());
			assertEquals(ZERO, balance);
		}
	}

	@Test
	void testTransferOutOfBalance() {
		Account receiver = sm.createAccount();
		BigInteger balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());

		assertEquals(ZERO, balance);

		BigInteger amount = totalSupply.add(BigInteger.ONE);
		try {
			workerToken.invoke(owner, "transfer", receiver.getAddress(), amount, "hotdogs".getBytes());
		}catch (AssertionError e) {
			assertEquals("Reverted(0): Worker Token : Out of balance: "+ amount, e.getMessage());
			balance = (BigInteger)workerToken.call("balanceOf", receiver.getAddress());
			assertEquals(ZERO, balance);
		}
	}
}
