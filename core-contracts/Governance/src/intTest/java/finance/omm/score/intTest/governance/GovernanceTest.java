package finance.omm.score.intTest.governance;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.score.intTest.governance.config.GovernanceConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class GovernanceTest implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient alice;
    private static OMMClient bob;
    private static Map<String, Address> addressMap;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();

        Config config = new GovernanceConfig();
        omm.runConfig(config);

        ownerClient = omm.defaultClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
        bob = omm.newClient(BigInteger.TEN.pow(24));
    }

    @DisplayName("Test name")
    @Test
    public void testName() {
        assertEquals("Omm Governance", ownerClient.governance.name());
    }
}
