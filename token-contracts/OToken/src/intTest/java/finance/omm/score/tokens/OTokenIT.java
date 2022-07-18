package finance.omm.score.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.oTokenConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        Config config = new oTokenConfig(omm.getAddresses());
        omm.runConfig(config);
        ommClient = omm.defaultClient();

    }

    @Test
    void testName() {
        assertEquals("SICX Interest Token", ommClient.oICX.name());
    }
}
