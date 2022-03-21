package finance.omm.score;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import finance.omm.commons.Addresses;
import finance.omm.libs.structs.AddressDetail;

public class FreeProviderTest extends TestBase{

	private static ServiceManager sm = getServiceManager();
	private static Account owner = sm.createAccount();

	private BigInteger decimals = BigInteger.valueOf(10);
	private static BigInteger totalSupply = BigInteger.valueOf(50000000000L);

	private Score freeProvider;
	private Score ommToken;
	private Account accountAddressProvider = sm.createAccount();
	private Account accountGovernance = sm.createAccount();

	private AddressDetail governanceDetails;
	private AddressDetail ommTokenDetails;


	@BeforeAll
	public static void init() {
		owner.addBalance(Addresses.OMM_TOKEN, totalSupply);
	}

	@BeforeEach
	public void setup() throws Exception {
		governanceDetails = new AddressDetail();
		governanceDetails.name =  Addresses.GOVERNANCE;
		governanceDetails.address = accountGovernance.getAddress();

		freeProvider = sm.deploy(owner, FreeProvider.class, accountAddressProvider.getAddress(), false);
		ommToken = sm.deploy(owner, MockOmmToken.class);

		ommTokenDetails = new AddressDetail(); 
		ommTokenDetails.name = Addresses.OMM_TOKEN;
		ommTokenDetails.address = ommToken.getAddress();

		ommToken.getAccount().addBalance(Addresses.OMM_TOKEN, totalSupply);
	}

	@Test
	void testTransferFund() {

		AddressDetail[] addressDetails = {governanceDetails, ommTokenDetails};
		freeProvider.invoke(accountAddressProvider, "setAddresses", (Object)addressDetails);

		BigInteger amount = TEN.pow(decimals.intValue());
		Account receiver = sm.createAccount();

		ommToken.invoke(owner, "setOwner", ommToken.getAccount());
		ommToken.invoke(owner, "addTo", receiver);
		freeProvider.invoke(accountGovernance, "transferFund", ommToken.getAddress(), amount, receiver.getAddress());

		assertEquals(amount, receiver.getBalance(Addresses.OMM_TOKEN));
		assertEquals(totalSupply.subtract(amount), ommToken.getAccount().getBalance(Addresses.OMM_TOKEN));

	}

	@Test
	void testLoanOriginationFeePercentage() {

		var actualFeePercentage = (BigInteger)freeProvider.call("getLoanOriginationFeePercentage");

		assertEquals(BigInteger.ZERO, actualFeePercentage);

		var feePercentage = new BigInteger("8");
		freeProvider.invoke(owner, "setLoanOriginationFeePercentage", feePercentage);

		actualFeePercentage = (BigInteger)freeProvider.call("getLoanOriginationFeePercentage");
		assertEquals(feePercentage, actualFeePercentage);
	}
}