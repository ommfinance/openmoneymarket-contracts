package finance.omm.score.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.DelegationConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DelegationIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ommClient;


    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("delegation/scores.json");

        omm.setupOMM();
        Config config = new DelegationConfig(omm.getAddresses());
        omm.runConfig(config);
        ommClient = omm.defaultClient();

    }

    @Test
    void testName() {
        assertEquals("OMM Delegation", ommClient.delegation.name());
    }
}