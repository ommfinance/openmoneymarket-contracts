package finance.omm.score.test.unit.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.core.score.interfaces.DAOFund;
import finance.omm.core.score.interfaces.FeeProvider;
import finance.omm.core.score.interfaces.LendingPoolCore;
import finance.omm.core.score.interfaces.OMMToken;
import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.governance.GovernanceImpl;
import finance.omm.score.core.governance.interfaces.RewardDistributionImpl;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class AbstractGovernanceTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    public Account owner;
    public Score score;
    public GovernanceImpl scoreSpy;

    public static final BigInteger ONE = BigInteger.ONE;
    public static final BigInteger TWO = BigInteger.TWO;
    public static final BigInteger TEN = BigInteger.TEN;
    public static final BigInteger PERCENT = TEN.pow(16);
    public static final BigInteger THIRTY_THREE = BigInteger.valueOf(33L);
    public static final BigInteger SIXTY = BigInteger.valueOf(60L);
    public static final BigInteger EIGHTY = BigInteger.valueOf(80L);
    public static final BigInteger HUNDRED = BigInteger.valueOf(100L);
    public static final BigInteger THOUSAND = BigInteger.valueOf(1000L);


    protected Address[] addresses = new Address[]{
            Account.newScoreAccount(201).getAddress(),
    };


    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.REWARD_WEIGHT_CONTROLLER, Account.newScoreAccount(102));
        put(Contracts.DAO_FUND, Account.newScoreAccount(103));
        put(Contracts.WORKER_TOKEN, Account.newScoreAccount(104));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(105));
        put(Contracts.BOOSTED_OMM, Account.newScoreAccount(106));
    }};

    protected DAOFund daoFund;
    protected RewardDistributionImpl rewardDistribution;
    protected LendingPoolCore lendingPoolCore;
    protected StakedLP stakedLp;
    protected FeeProvider feeProvider;
    protected OMMToken ommToken;

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

        score = sm.deploy(owner, GovernanceImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        GovernanceImpl t = (GovernanceImpl) score.getInstance();
        scoreSpy = spy(t);
        mockScoreClients();
        score.setInstance(scoreSpy);
    }

    /**
     * mock score clients
     */
    private void mockScoreClients() {
        daoFund = spy(DAOFund.class);
        rewardDistribution = spy(RewardDistributionImpl.class);
        lendingPoolCore = spy(LendingPoolCore.class);
        stakedLp = spy(StakedLP.class);
        feeProvider = spy(FeeProvider.class);
        ommToken = spy(OMMToken.class);

        scoreSpy.daoFund = daoFund;
        scoreSpy.rewardDistribution = rewardDistribution;
        scoreSpy.lendingPoolCore = lendingPoolCore;
        scoreSpy.stakedLP = stakedLp;
        scoreSpy.feeProvider = feeProvider;
        scoreSpy.ommToken = ommToken;
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

    @Test
    public void testTransfer() {
        doNothing().when(scoreSpy.daoFund).transferOmm(TestBase.ICX, addresses[0]);
        score.invoke(owner, "transferOmmFromDaoFund", TestBase.ICX, addresses[0]);
    }
}

