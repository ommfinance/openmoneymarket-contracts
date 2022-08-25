package finance.omm.score.core.lendingPool.test;

import static finance.omm.score.core.lendingpool.AbstractLendingPool.START_HEIGHT;
import static finance.omm.score.core.lendingpool.AbstractLendingPool.TXN_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.lendingpool.LendingPoolImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class AbstractLendingPoolTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();

    public Account owner;

    public Account notOwner;


    public Score score;

    public LendingPoolImpl scoreSpy;

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.OMM_TOKEN,Account.newScoreAccount(102));
        put(Contracts.oICX,Account.newScoreAccount(103));
        put(Contracts.sICX,Account.newScoreAccount(104));
        put(Contracts.IUSDC,Account.newScoreAccount(105));
        put(Contracts.LENDING_POOL_CORE,Account.newScoreAccount(106));
        put(Contracts.oIUSDC,Account.newScoreAccount(107));
        put(Contracts.STAKING,Account.newScoreAccount(108));
        put(Contracts.FEE_PROVIDER,Account.newScoreAccount(109));
    }};

    static MockedStatic<Context> contextMock;

    protected BranchDB<Address, DictDB<Integer, BigInteger>> feeSharingDB =
            Mockito.mock(BranchDB.class);
    protected DictDB<String, BigInteger> feeSharingDictDB = Mockito.mock(DictDB.class);

    protected DictDB<Address,BigInteger> depositIndex = Mockito.mock(DictDB.class);

    @BeforeAll
    protected static void init(){
        long CURRENT_TIMESTAMP = System.currentTimeMillis() / 1_000L;
        sm.getBlock().increase(CURRENT_TIMESTAMP / 2);
        contextMock = Mockito.mockStatic(Context.class, Mockito.CALLS_REAL_METHODS);
    }

    @BeforeEach
    void setup() throws Exception{
        contextMock.reset();
        contextMock
                .when(() -> Context.newBranchDB("feeSharingUsers", BigInteger.class))
                .thenReturn(feeSharingDB);

        owner = sm.createAccount(100);
        notOwner = sm.createAccount(100);

        score = sm.deploy(owner,LendingPoolImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        LendingPoolImpl lendingPool = (LendingPoolImpl) score.getInstance();
        scoreSpy = spy(lendingPool);
        score.setInstance(scoreSpy);

//        mockFeeSharing();
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

    protected void mockFeeSharing() {
//        Mockito.reset(feeSharingDB);
//        Mockito.reset(feeSharingDictDB);

        doReturn(feeSharingDictDB).when(feeSharingDB).at(any());
        doReturn(BigInteger.ONE).when(feeSharingDictDB).get(START_HEIGHT);
        doReturn(BigInteger.TWO).when(feeSharingDictDB).get(TXN_COUNT);

    }

    protected void mockDepositIndex(){
        doReturn(BigInteger.valueOf(1)).when(depositIndex).get(any());

    }

}
