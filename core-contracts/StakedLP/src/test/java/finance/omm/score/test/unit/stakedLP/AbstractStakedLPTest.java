package finance.omm.score.test.unit.stakedLP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.stakedLP.StakedLPImpl;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class AbstractStakedLPTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    public Account owner;
    public Score score;
    public StakedLPImpl scoreSpy;

    public static final BigInteger ONE = BigInteger.ONE;
    public static final BigInteger TEN = BigInteger.TEN;
    public static final BigInteger ZERO = BigInteger.ZERO;
    public static final BigInteger THOUSAND = BigInteger.valueOf(1000L);

    public static final BigInteger NEGATIVE = BigInteger.ONE.negate();


    protected Address[] addresses = new Address[]{
            Account.newScoreAccount(201).getAddress(),
            Account.newScoreAccount(202).getAddress(),
            Account.newScoreAccount(203).getAddress()
    };


    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.GOVERNANCE, Account.newScoreAccount(102));
        put(Contracts.DEX, Account.newScoreAccount(103));
        put(Contracts.REWARDS, Account.newScoreAccount(104));
    }};

    @BeforeAll
    protected static void init() {
        long CURRENT_TIMESTAMP = System.currentTimeMillis() / 1_000L;
        sm.getBlock().increase(CURRENT_TIMESTAMP / 2);
    }

    public void increaseTimeBy(BigInteger increaseBy) {
        // increaseBy is in microseconds
        long blocks = increaseBy.divide(BigInteger.valueOf(1_000_000L)).intValue()/2;
        sm.getBlock().increase(blocks);
    }

    @BeforeEach
    void setup() throws Exception {

        owner = sm.createAccount(100);

        score = sm.deploy(owner, StakedLPImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        StakedLPImpl t = (StakedLPImpl) score.getInstance();
        scoreSpy = spy(t);
//        mockScoreClients();
        score.setInstance(scoreSpy);
    }

    private void setAddresses() {
        AddressDetails[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetails ad = new AddressDetails();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetails[]::new);

        Object[] params = new Object[]{
                addressDetails
        };
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

    public void expectErrorMessageIn(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        boolean isInString = e.getMessage().contains(errorMessage);
        assertEquals(true, isInString);
    }

    protected byte[] createByteArray(String methodName){

        JsonObject jsonData = new JsonObject()
                .add("method",methodName);

        byte[] data = jsonData.toString().getBytes();
        return data;
    }

    @Test
    public void testTransfer() {

//        doNothing().when(daoFund).transferOmm(TestBase.ICX, addresses[0]);
//
//        score.invoke(owner, "transferOmmFromDaoFund", TestBase.ICX, addresses[0]);
    }
}


