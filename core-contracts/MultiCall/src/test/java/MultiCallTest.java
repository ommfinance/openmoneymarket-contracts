import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finanace.omm.score.core.multicall.Multicall;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.Context;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

public class MultiCallTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    protected Account owner;

    protected Score score;
    protected Multicall spyScore;

    protected static MockedStatic<Context> contextMock;

    protected static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.STAKING, Account.newScoreAccount(105));
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

        score = sm.deploy(owner, Multicall.class, MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        Multicall multicall = (Multicall) score.getInstance();
        spyScore = spy(multicall);
        score.setInstance(spyScore);

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

    protected MockedStatic.Verification mockBlockHeight() {
        return Context::getBlockHeight;
    }


    @Test
    public void name(){
        assertEquals("OMM Multicall",score.call("name"));
    }

    @Test
    public void getTopPreps(){
        Address targetAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.STAKING).getAddress();
        String method = "getTopPreps";
        String[] params = new String[]{};

        Multicall.Call calls = new Multicall.Call();
        calls.target = targetAddr;
        calls.method = method;
        calls.params = params;

        List<Address> topPrep = new ArrayList<>();
        topPrep.add(sm.createAccount().getAddress());
        topPrep.add(sm.createAccount().getAddress());

        contextMock.when(mockBlockHeight()).thenReturn(1000L);
        contextMock.when(() -> Context.call(targetAddr,method)).thenReturn(topPrep);

        Map<String,Object> result = (Map<String, Object>) score.call("aggregate",(Object) new Multicall.Call[]{calls});

        assertEquals(result.get("blockNumber"),(long)1000);
        System.out.println(result.get("returnData"));
    }
}
