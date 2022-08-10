package finance.omm.score.test.unit.oracle;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.oracle.PriceOracleImpl;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Context;

import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;


public class AbstractPriceOracleTest extends TestBase{

    public static final ServiceManager sm = getServiceManager();
    public Account owner;

    public Account notOwner;
    public Score score;
    public PriceOracleImpl scoreSpy;

    protected static MockedStatic<Context> contextMock;

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.BAND_ORACLE, Account.newScoreAccount(102));
        put(Contracts.DEX, Account.newScoreAccount(103));
    }};

    @BeforeAll
    public static void init() {
        contextMock = Mockito.mockStatic(Context.class, Mockito.CALLS_REAL_METHODS);

        long CURRENT_TIMESTAMP = System.currentTimeMillis() / 1_000L;
        sm.getBlock().increase(CURRENT_TIMESTAMP / 2);
    }


    @BeforeEach
    void setup() throws Exception {

        owner = sm.createAccount(100);
        notOwner =sm.createAccount(50);

        score = sm.deploy(owner, PriceOracleImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();
        PriceOracleImpl t = (PriceOracleImpl) score.getInstance();
        scoreSpy = spy(t);
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

    protected BigInteger getPrice(String name){
        switch (name) {
            case "USDS": {
                BigInteger adjustedPrice = BigInteger.valueOf(1).multiply(ICX);
                BigInteger convertedPrice = exaMultiply(adjustedPrice, ICX);
                BigInteger totalSupply = BigInteger.valueOf(7600_000).multiply(ICX);

                return totalSupply.multiply(convertedPrice);

            }
            case "ICX": {
                BigInteger adjustedPrice = BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.valueOf(100));
                BigInteger todayRate = BigInteger.valueOf(3).multiply(ICX);
                BigInteger convertedPrice = exaMultiply(adjustedPrice, todayRate);
                BigInteger totalSupply = BigInteger.valueOf(8000_000).multiply(ICX);

                return totalSupply.multiply(convertedPrice);
            }
            case "IUSDC": {
                BigInteger price = BigInteger.valueOf(1).multiply(ICX);
                BigInteger adjustedPrice = convertToExa(price, BigInteger.valueOf(6));
                BigInteger todayRate = BigInteger.valueOf(9).multiply(ICX);
                BigInteger convertedPrice = exaMultiply(adjustedPrice, todayRate);

                BigInteger totalSupply = BigInteger.valueOf(7200_000).multiply(ICX);

                return totalSupply.multiply(convertedPrice);
            }
        }

        return null;

    }

    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }


}


