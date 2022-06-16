package finance.omm.score.tokens;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.score.tokens.utils.IRC2Token;
import java.math.BigInteger;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractBOMMTest extends TestBase {

    protected static final ServiceManager sm = getServiceManager();
    protected static final Account owner = sm.createAccount();

    protected static Score tokenScore;
    private static final BigInteger INITIAL_SUPPLY = BigInteger.valueOf(1000).multiply(ICX);

    @BeforeAll
    public static void init() throws Exception {
        tokenScore = sm.deploy(owner, IRC2Token.class, INITIAL_SUPPLY);
    }


}
