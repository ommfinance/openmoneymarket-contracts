package finance.omm.score.core.fee.distribution.integration;

import finance.omm.libs.structs.WeightStruct;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.score.core.fee.distribution.integration.config.feeDistributionConfig;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeeDistributionIT implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient testClient;
    private static OMMClient alice;

    private static Map<String, Address> addressMap;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new feeDistributionConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
    }

    private Map<String, Address> feeAddress = new HashMap<>() {{
        put("fee-1", Faker.address(Address.Type.EOA));
        put("fee-2", Faker.address(Address.Type.CONTRACT));
    }};

    @Test
    public void name() {
        assertEquals("Omm Fee Distribution", ownerClient.feeDistribution.name());
    }


}
