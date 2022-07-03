package finance.omm.score.tokens;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.tokens.utils.IRC2Token;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractBOMMTest extends TestBase {

    protected static final ServiceManager sm = getServiceManager();
    protected static final Account owner = sm.createAccount();

    protected static Score tokenScore;
    private static final BigInteger INITIAL_SUPPLY = BigInteger.valueOf(1000).multiply(ICX);


    protected Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.GOVERNANCE, Account.newScoreAccount(102));
    }};

    @BeforeAll
    public static void init() throws Exception {
        tokenScore = sm.deploy(owner, IRC2Token.class, INITIAL_SUPPLY);
    }


    protected void setAddresses(Score score) {
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

}
