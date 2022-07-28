package finance.omm.score;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import java.math.BigInteger;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import score.Address;
import score.Context;

public class WorkerTokenTest extends TestBase {

    private static ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();

    private static BigInteger decimals = BigInteger.valueOf(1);
    private static BigInteger initialSupply = BigInteger.valueOf(100);
    private static BigInteger totalSupply = new BigInteger("50000000000");

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.WORKER_TOKEN, Account.newScoreAccount(102));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(103));
        put(Contracts.REWARDS, Account.newScoreAccount(104));
    }};

    public static final Address[] addresses = new Address[]{
            sm.createAccount().getAddress(),
            sm.createAccount().getAddress(),
            sm.createAccount().getAddress(),
            sm.createAccount().getAddress(),
            sm.createAccount().getAddress()
    };

    private Score workerToken;

    @BeforeAll
    public static void init() {
        owner.addBalance(WorkerTokenImpl.WORKER_TOKEN_SYMBOL, totalSupply);
    }

    @BeforeEach
    public void setup() throws Exception {
        workerToken = sm.deploy(owner, WorkerTokenImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress(),
                initialSupply, decimals, false);
    }

    @Test
    void testTransfer() {
        Account receiver = sm.createAccount();
        BigInteger balance = (BigInteger) workerToken.call("balanceOf", receiver.getAddress());

        assertEquals(ZERO, balance);

        BigInteger amount = new BigInteger("500");
        workerToken.invoke(owner, "transfer", receiver.getAddress(), amount, "hotdogs".getBytes());

        balance = (BigInteger) workerToken.call("balanceOf", receiver.getAddress());

        assertEquals(amount, balance);
    }

    @Test
    void testTransferToScore() {
        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {
            Account score = sm.createAccount();
            Address scoreAddress = spy(score.getAddress());
            BigInteger balance = (BigInteger) workerToken.call("balanceOf", scoreAddress);
            BigInteger amount = new BigInteger("500");
            byte[] data = "settlement".getBytes();

            assertEquals(ZERO, balance);

            Mockito.when(scoreAddress.isContract()).thenReturn(true);

            theMock.when(() -> Context.call(scoreAddress, "tokenFallback",
                    owner.getAddress(), amount, data)).thenReturn(null);

            theMock.when(() -> Context.getCaller())
                    .thenReturn(owner.getAddress());

            workerToken.invoke(owner, "transfer", scoreAddress, amount, data);

            balance = (BigInteger) workerToken.call("balanceOf", scoreAddress);

            assertEquals(amount, balance);
            Mockito.verify(scoreAddress).isContract();

            theMock.verify(
                    () -> Context.call(scoreAddress, "tokenFallback",
                            owner.getAddress(), amount, data),
                    Mockito.times(1));
        }
    }

    @Test
    void testTransferZeroAmount() {
        Account receiver = sm.createAccount();
        BigInteger balance = (BigInteger) workerToken.call("balanceOf", receiver.getAddress());

        assertEquals(ZERO, balance);

        try {
            workerToken.invoke(owner, "transfer", receiver.getAddress(), ZERO, "hotdogs".getBytes());
        } catch (AssertionError e) {
            assertEquals(
                    "Reverted(0): Worker Token: Transferring value should be greater than zero",
                    e.getMessage());
            balance = (BigInteger) workerToken.call("balanceOf", receiver.getAddress());
            assertEquals(ZERO, balance);
        }
    }

    @Test
    void testTransferOutOfBalance() {
        Account receiver = sm.createAccount();
        BigInteger balance = (BigInteger) workerToken.call("balanceOf",
                receiver.getAddress());

        assertEquals(ZERO, balance);

        BigInteger amount = totalSupply.add(BigInteger.ONE);
        try {
            workerToken.invoke(owner, "transfer", receiver.getAddress(), amount, "hotdogs".getBytes());
        } catch (AssertionError e) {
            assertEquals(
                    "Reverted(0): Worker Token : Out of balance: " + amount,
                    e.getMessage());
            balance = (BigInteger) workerToken.call("balanceOf", receiver.getAddress());
            assertEquals(ZERO, balance);
        }
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
        workerToken.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }

    @Test
    void tokenFallback() {
        setAddresses();
        Address rewards = MOCK_CONTRACT_ADDRESS.get(Contracts.REWARDS).getAddress();
        Address ommToken = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();
        byte[] data = "".getBytes();

        Executable call = () -> workerToken.invoke(owner, "tokenFallback", rewards, ZERO, data);
        expectErrorMessageIn(call, "Only OMM Token can be distributed to workers.");

        call = () -> workerToken.invoke(owner, "tokenFallback", ommToken, ZERO, data);
        expectErrorMessageIn(call, "Only rewards");
    }

    public void expectErrorMessageIn(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        boolean isInString = e.getMessage().contains(errorMessage);
        assertEquals(true, isInString);
    }
}
