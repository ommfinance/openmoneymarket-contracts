package finance.omm.score.reward.test.integration;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.core.score.interfaces.RewardDistributionScoreClient;
import finance.omm.libs.test.ScoreIntegrationTest;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import java.math.BigInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RewardDistributionTest implements ScoreIntegrationTest {

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    DefaultScoreClient clientWithTester = new DefaultScoreClient(client.endpoint(), client._nid(), tester,
            client._address());

    @ScoreClient
    RewardDistribution scoreClient = new RewardDistributionScoreClient(client);

    RewardDistribution scoreClientWithTester = new RewardDistributionScoreClient(clientWithTester);


    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");

        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");

        System.out.println("afterAll end");
    }

    @DisplayName("addType")
    @Nested
    class TestAddType {

        @DisplayName("should throw unauthorized")
        @Test
        void should_throw_unauthorized() {
            client._transfer(Address.of(tester), BigInteger.valueOf(100).multiply(ICX), "transfer");

            assertUserRevert(RewardDistributionException.notOwner(),
                    () -> scoreClientWithTester.addType("key", false), null);

        }

    }

}