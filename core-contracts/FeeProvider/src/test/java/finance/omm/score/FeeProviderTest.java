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

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;

public class FeeProviderTest extends TestBase {

    private static ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();

    private BigInteger decimals = BigInteger.valueOf(10);
    private static BigInteger totalSupply = BigInteger.valueOf(50000000000L);

    private Score feeProvider;
    private Score ommToken;
    private Account accountAddressProvider = sm.createAccount();
    private Account accountGovernance = sm.createAccount();

    private AddressDetails governanceDetails;
    private AddressDetails ommTokenDetails;

    @BeforeAll
    public static void init() {
        owner.addBalance(Contracts.OMM_TOKEN.getKey(), totalSupply);
    }

    @BeforeEach
    public void setup() throws Exception {
        governanceDetails = new AddressDetails();
        governanceDetails.name = Contracts.GOVERNANCE.getKey();
        governanceDetails.address = accountGovernance.getAddress();

        feeProvider = sm.deploy(owner, FeeProviderImpl.class, accountAddressProvider.getAddress(), false);
        ommToken = sm.deploy(owner, MockOmmToken.class);

        ommTokenDetails = new AddressDetails();
        ommTokenDetails.name = Contracts.OMM_TOKEN.getKey();
        ommTokenDetails.address = ommToken.getAddress();

        ommToken.getAccount().addBalance(Contracts.OMM_TOKEN.getKey(), totalSupply);
    }

    @Test
    void testTransferFund() {

        AddressDetails[] addressDetails = {governanceDetails, ommTokenDetails};
        feeProvider.invoke(accountAddressProvider, "setAddresses", (Object) addressDetails);

        BigInteger amount = TEN.pow(decimals.intValue());
        Account receiver = sm.createAccount();

        ommToken.invoke(owner, "setOwner", ommToken.getAccount());
        ommToken.invoke(owner, "addTo", receiver);
        feeProvider.invoke(accountGovernance, "transferFund",
                ommToken.getAddress(), amount, receiver.getAddress());

        assertEquals(amount, receiver.getBalance(Contracts.OMM_TOKEN.getKey()));
        assertEquals(totalSupply.subtract(amount),
                ommToken.getAccount().getBalance(Contracts.OMM_TOKEN.getKey()));

    }

    @Test
    void testLoanOriginationFeePercentage() {

        var actualFeePercentage = (BigInteger) feeProvider.call("getLoanOriginationFeePercentage");

        assertEquals(BigInteger.ZERO, actualFeePercentage);

        var feePercentage = new BigInteger("8");
        feeProvider.invoke(owner, "setLoanOriginationFeePercentage", feePercentage);

        actualFeePercentage = (BigInteger) feeProvider.call("getLoanOriginationFeePercentage");
        assertEquals(feePercentage, actualFeePercentage);
    }
}
