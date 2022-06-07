package finance.omm.score.reward.test.unit;

import static finance.omm.utils.constants.TimeConstants.DAY_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.reward.distribution.RewardDistributionImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import score.Address;

public abstract class RewardDistributionAbstractTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    public Account owner;
    public Score score;
    public RewardDistributionImpl scoreSpy;
    protected float floatWeight = 0.4f;

    public static final BigInteger TWO = BigInteger.TWO;
    public static final BigInteger THREE = BigInteger.valueOf(3);
    public static final BigInteger FOUR = BigInteger.valueOf(4);
    public static final BigInteger HUNDRED = BigInteger.valueOf(100);
    public static final BigInteger FORTY = BigInteger.valueOf(40L);
    public static final BigInteger SIXTY = BigInteger.valueOf(60L);

    protected Address[] addresses = new Address[]{
            Account.newScoreAccount(201).getAddress(),
            Account.newScoreAccount(202).getAddress(),
            Account.newScoreAccount(203).getAddress(),
            Account.newScoreAccount(204).getAddress(),
            Account.newScoreAccount(205).getAddress(),
            Account.newScoreAccount(206).getAddress(),
            Account.newScoreAccount(207).getAddress(),
            Account.newScoreAccount(208).getAddress(),
            Account.newScoreAccount(209).getAddress(),
            Account.newScoreAccount(210).getAddress(),
            Account.newScoreAccount(211).getAddress(),
    };


    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.REWARD_WEIGHT_CONTROLLER, Account.newScoreAccount(102));
        put(Contracts.DAO_FUND, Account.newScoreAccount(103));
        put(Contracts.WORKER_TOKEN, Account.newScoreAccount(104));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(105));
        put(Contracts.BOOSTED_OMM, Account.newScoreAccount(106));
    }};


    @BeforeAll
    protected static void init() {
        long CURRENT_TIMESTAMP = System.currentTimeMillis() / 1_000L;
        sm.getBlock().increase(CURRENT_TIMESTAMP / 2);
    }

    @BeforeEach
    void setup() throws Exception {

        owner = sm.createAccount(100);

        BigInteger bOMMCutOff = BigInteger.valueOf(sm.getBlock().getTimestamp())
                .divide(SECOND)
                .subtract(DAY_IN_SECONDS.multiply(BigInteger.TWO));

        score = sm.deploy(owner, RewardDistributionImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress(), bOMMCutOff);
        setAddresses();
        RewardDistributionImpl t = (RewardDistributionImpl) score.getInstance();
        scoreSpy = spy(t);
        mockAssets(scoreSpy, Mockito.spy(scoreSpy.assets));
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


    static void mockAssets(RewardDistributionImpl obj, Object value) throws Exception {
        Field field = obj.getClass().getField("assets");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(obj, value);
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }


}
