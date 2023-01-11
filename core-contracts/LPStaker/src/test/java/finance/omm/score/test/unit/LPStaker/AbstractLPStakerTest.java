package finance.omm.score.test.unit.LPStaker;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.lp.LPStakerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Context;


import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

public class AbstractLPStakerTest extends TestBase {
    public static final ServiceManager sm = getServiceManager();
    protected Account owner;

    protected Account alice;
    protected Score score;
    protected LPStakerImpl scoreSpy;

    protected static MockedStatic<Context> contextMock;

    protected final BigInteger TEN = BigInteger.TEN;
    protected final BigInteger TWO = BigInteger.TWO;

    protected final Account GOVERNANCE_SCORE = MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE);

    protected static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.STAKED_LP, Account.newScoreAccount(102));
        put(Contracts.DEX, Account.newScoreAccount(103));
        put(Contracts.GOVERNANCE, Account.newScoreAccount(104));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(105));
    }};

    @BeforeAll
    protected static void init() {
        long CURRENT_TIMESTAMP = System.currentTimeMillis() / 1_000L;
        sm.getBlock().increase(CURRENT_TIMESTAMP / 2);
        contextMock = Mockito.mockStatic(score.Context.class, Mockito.CALLS_REAL_METHODS);
    }

    @BeforeEach
    void setup() throws Exception {
        contextMock.reset();
        owner = sm.createAccount(100);
        alice = sm.createAccount(100);

        score = sm.deploy(owner, LPStakerImpl.class, MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        LPStakerImpl lpStaker = (LPStakerImpl) score.getInstance();
        scoreSpy = spy(lpStaker);
        score.setInstance(scoreSpy);

    }

    private void setAddresses() {
        AddressDetails[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetails ad = new AddressDetails();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetails[]::new);

        Object[] params = new Object[]{addressDetails};
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }

    protected void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

    protected MockedStatic.Verification mockCaller() {
        return Context::getCaller;
    }

    protected MockedStatic.Verification mockScoreAddress() {
        return Context::getAddress;
    }

}
