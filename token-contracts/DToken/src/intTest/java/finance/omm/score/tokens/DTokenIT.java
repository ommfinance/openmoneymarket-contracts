package finance.omm.score.tokens;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.dTokenConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    @BeforeAll
    static void setup() throws  Exception{
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        Config config = new dTokenConfig(omm.getAddresses());
        omm.runConfig(config);
        ommClient = omm.defaultClient();
    }

    @Test
    void ShouldGetDecimals() {

//        BigInteger decimals = scoreClient.decimals();
//        assertNotNull(decimals);
//        assertEquals(BigInteger.valueOf(18), decimals);

    }

    @Test
    void testName() {
        assertEquals("SICX Debt Token", ommClient.dICX.name());
    }

}
