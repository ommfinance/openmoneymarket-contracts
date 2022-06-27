package finance.omm.score.core.test;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.score.core.delegation.DelegationImpl.TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.DelegationConfig;
import finance.omm.score.core.delegation.exception.DelegationException;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DelegationIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;


    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("delegation/scores.json");

        omm.setupOMM();
        Config config = new DelegationConfig(omm.getAddresses());
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();

    }

    List<score.Address> preps = new ArrayList<>(){{
        add(Faker.address(Address.Type.EOA));
        add(Faker.address(Address.Type.EOA));
        add(Faker.address(Address.Type.EOA));
    }};


    @Test
    void testName() {
        assertEquals("OMM Delegation", ommClient.delegation.name());
    }

    @Test
    void set_and_get_voteThreshold(){
        ommClient.delegation.setVoteThreshold(BigInteger.valueOf(10));
        BigInteger voteThreshold = ommClient.delegation.getVoteThreshold();
        assertEquals(BigInteger.valueOf(10),voteThreshold);

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.setVoteThreshold(BigInteger.valueOf(10)), null);

    }

    @Test
    void add_remove_get_contributer(){
        ommClient.delegation.addContributor(preps.get(0));
        ommClient.delegation.addContributor(preps.get(1));

        List<score.Address> expected = new ArrayList<>(){{
            add(preps.get(0));
            add(preps.get(1));
        }};
        List<score.Address> contributors = ommClient.delegation.getContributors();
        assertEquals(expected,contributors);

        ommClient.delegation.removeContributor(preps.get(0));
        expected.remove(preps.get(0));
        contributors = ommClient.delegation.getContributors();
        assertEquals(expected,contributors);

        assertUserRevert(DelegationException.unknown(" "),
                () -> testClient.delegation.removeContributor(preps.get(0)), null);

        //carried out with testclient

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addContributor(preps.get(0)), null);

        testClient.delegation.removeContributor(preps.get(1));//to be updated
        expected.remove(preps.get(1));
        contributors = testClient.delegation.getContributors();
        assertEquals(expected,contributors);
    }
}