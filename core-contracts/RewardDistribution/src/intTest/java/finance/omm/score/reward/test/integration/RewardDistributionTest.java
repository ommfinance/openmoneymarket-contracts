package finance.omm.score.reward.test.integration;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.core.score.interfaces.RewardDistributionScoreClient;
import finance.omm.libs.test.ScoreIntegrationTest;
import finance.omm.score.core.reward.distribution.utils.RewardDistributionException;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import org.junit.jupiter.api.*;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;

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


    @DisplayName("asset not found")
    @Test
    void invalidSetAsset() {
        Address asset = Faker.address(Address.Type.CONTRACT);
        assertUserRevert(RewardDistributionException.invalidAsset("Asset not found"),
                () -> scoreClient.setAssetName(asset, "_temp"), null);
    }

    @Disabled("unauthorized set asset")
    @Test
    void unauthorizedSetAsset() {
        assertUserRevert(RewardDistributionException.notOwner(),
                () -> scoreClient.setAssetName(Faker.address(Address.Type.CONTRACT), "_temp"), null);
    }

}