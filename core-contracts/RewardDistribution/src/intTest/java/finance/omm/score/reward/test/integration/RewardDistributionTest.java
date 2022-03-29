package finance.omm.score.reward.test.integration;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.core.score.interfaces.RewardDistributionScoreClient;
import finance.omm.core.score.interfaces.RewardWeightController;
import finance.omm.core.score.interfaces.RewardWeightControllerScoreClient;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.test.ScoreIntegrationTest;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.score.core.reward.exception.RewardWeightException;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.Address.Type;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class RewardDistributionTest implements ScoreIntegrationTest {

    private final DefaultScoreClient client;
    private final DefaultScoreClient addressProviderClient;
    private final DefaultScoreClient rewardWeightClient;
    private final DefaultScoreClient testClient;

    @ScoreClient
    private final RewardDistribution scoreClient;
    private final RewardDistribution scoreClientWithTester;


    @ScoreClient
    private final RewardWeightController weightController;
    private final RewardWeightController weightControllerWithTester;


    private Map<String, Address> assets = new HashMap<>() {{
        put("asset-1", Faker.address(Type.CONTRACT));
        put("asset-2", Faker.address(Type.CONTRACT));
        put("asset-3", Faker.address(Type.CONTRACT));
        put("type-b", Faker.address(Type.CONTRACT));
    }};

    Map<String, Boolean> STATES = new HashMap<>();


    RewardDistributionTest() {
        Properties properties = System.getProperties();

        addressProviderClient = DefaultScoreClient.of("addressProvider.", properties);

        properties.setProperty("params.addressProvider", addressProviderClient._address().toString());

        client = DefaultScoreClient.of(properties);

        properties.setProperty("rewardWeight.params.addressProvider", addressProviderClient._address().toString());

        rewardWeightClient = DefaultScoreClient.of("rewardWeight.", properties);

        testClient = new DefaultScoreClient(client.endpoint(), client._nid(), tester,
                client._address());

        scoreClient = new RewardDistributionScoreClient(client);

        scoreClientWithTester = new RewardDistributionScoreClient(testClient);

        weightController = new RewardWeightControllerScoreClient(rewardWeightClient);
        weightControllerWithTester = new RewardWeightControllerScoreClient(
                new DefaultScoreClient(rewardWeightClient.endpoint(), rewardWeightClient._nid(), tester,
                        rewardWeightClient._address()));

        init();
    }


    void init() {

        AddressDetails[] params = new AddressDetails[]{
                new AddressDetails(Contracts.REWARD_WEIGHT_CONTROLLER.getKey(), rewardWeightClient._address()),
                new AddressDetails(Contracts.REWARDS.getKey(), client._address()),
                new AddressDetails("asset-1", assets.get("asset-1")),
                new AddressDetails("asset-2", assets.get("asset-2")),
                new AddressDetails("asset-3", assets.get("asset-3")),
                new AddressDetails("type-b", assets.get("type-b")),
        };

        addressProviderClient._send("setAddresses", Map.of(
                "_addressDetails", params
        ));

        addressProviderClient._send("addAddressToScore", Map.of(
                "_to", Contracts.REWARDS.getKey(),
                "_names", new String[]{
                        Contracts.REWARD_WEIGHT_CONTROLLER.getKey(), "asset-1", "asset-2", "asset-3", "type-b"
                }
        ));

        addressProviderClient._send("addAddressToScore", Map.of(
                "_to", Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                "_names", new String[]{
                        Contracts.REWARDS.getKey(), "asset-1", "asset-2", "asset-3", "type-b"
                }
        ));

    }

    @DisplayName("addType")
    @Nested
    class TestTypes {

        @DisplayName("addType - should throw unauthorized")
        @Test
        @Order(10)
        void should_throw_unauthorized_addType() {
            client._transfer(Address.of(tester), BigInteger.valueOf(100).multiply(ICX), "transfer");

            assertUserRevert(RewardDistributionException.notOwner(),
                    () -> scoreClientWithTester.addType("key", false), null);

        }

        @DisplayName("addType - should able to add type contract false")
        @Test
        @Order(20)
        void should_add_type_transfer_to_contract_false() {
            if (STATES.getOrDefault("should_add_type_contract_false", false)) {
                return;
            }
            Map<String, BigInteger> allWeightType = weightController.getALlTypeWeight(null);
            Assertions.assertTrue(allWeightType.isEmpty());

            scoreClient.addType("type-a", false);

            allWeightType = weightController.getALlTypeWeight(null);
            Assertions.assertFalse(allWeightType.isEmpty());
            Assertions.assertEquals(1, allWeightType.size());

            assertEquals(allWeightType.get("type-a"), BigInteger.ZERO);

            STATES.put("should_add_type_contract_false", true);
        }


        @DisplayName("addType - should able to add type contract true")
        @Test
        @Order(25)
        void should_add_type_transfer_to_contract_true() {
            if (STATES.getOrDefault("should_add_type_contract_true", false)) {
                return;
            }
            should_add_type_transfer_to_contract_false();

            scoreClient.addType("type-b", true);

            Map<String, BigInteger> allWeightType = weightController.getALlTypeWeight(null);
            Assertions.assertEquals(2, allWeightType.size());

            assertEquals(allWeightType.get("type-a"), BigInteger.ZERO);
            assertEquals(allWeightType.get("type-b"), BigInteger.ZERO);

            STATES.put("should_add_type_contract_true", true);
        }

        @DisplayName("setTypeWeight")
        @Nested
        class TestSetTypeWeight {

            @BeforeEach
            void before() {
                should_add_type_transfer_to_contract_true();
            }

            @DisplayName("setTypeWeight - should throw unauthorized")
            @Test
            @Order(30)
            void should_throw_unauthorized_setTypeWeight() {
                client._transfer(Address.of(tester), BigInteger.valueOf(100).multiply(ICX), "transfer");

                assertUserRevert(RewardWeightException.notOwner(),
                        () -> weightControllerWithTester.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("type-a", ICX.divide(BigInteger.TWO)),
                                new TypeWeightStruct("type-b", ICX.divide(BigInteger.TWO)),
                        }, null), null);

            }

            @DisplayName("should able to set type weight by owner")
            @Test
            @Order(35)
            void should_able_to_set_weight_by_owner() {
                if (STATES.getOrDefault("should_able_to_set_weight_by_owner", false)) {
                    return;
                }
                weightController.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("type-a", ICX.divide(BigInteger.TWO)),
                        new TypeWeightStruct("type-b", ICX.divide(BigInteger.TWO)),
                }, BigInteger.TEN);

                Map<String, BigInteger> allWeightType = weightController.getALlTypeWeight(null);
                Assertions.assertFalse(allWeightType.isEmpty());

                assertEquals(allWeightType.get("type-a"), ICX.divide(BigInteger.TWO));
                assertEquals(allWeightType.get("type-b"), ICX.divide(BigInteger.TWO));
                STATES.put("should_able_to_set_weight_by_owner", true);
            }

            @DisplayName("should able to set type weight")
            @Test
            @Order(40)
            void should_able_to_set_weight() {
                if (STATES.getOrDefault("should_able_to_set_weight", false)) {
                    return;
                }
                should_able_to_set_weight_by_owner();
                BigInteger weight = weightController.getTypeWeight("type-a", null);
                assertEquals(weight, ICX.divide(BigInteger.TWO));

                BigInteger aValue = ICX.divide(BigInteger.valueOf(4L));
                BigInteger bValue = ICX.subtract(aValue);

                weightController.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("type-a", aValue),
                        new TypeWeightStruct("type-b", bValue),
                }, BigInteger.valueOf(1000L));

                BigInteger aActualValue = weightController.getTypeWeight("type-a", BigInteger.valueOf(1001));

                assertEquals(aValue, aActualValue);

                aActualValue = weightController.getTypeWeight("type-a", BigInteger.valueOf(999));

                assertEquals(ICX.divide(BigInteger.TWO), aActualValue);

                STATES.put("should_able_to_set_weight", true);
            }

            @DisplayName("should not able to weight on old timestamp")
            @Test
            @Order(50)
            void should_not_able_set_old_timestamp() {
                should_able_to_set_weight();

                BigInteger aValue = ICX.divide(BigInteger.valueOf(4L));
                BigInteger bValue = ICX.subtract(aValue);

                assertUserRevert(RewardWeightException.unknown("latest " + 1000L + " checkpoint exists than " + 999L),
                        () -> weightController.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("type-a", aValue),
                                new TypeWeightStruct("type-b", bValue),
                        }, BigInteger.valueOf(999)), null);


            }


            @DisplayName("addAsset")
            @Nested
            class TestAsset {

                @BeforeEach
                void before() {
                    should_add_type_transfer_to_contract_true();
                    should_able_to_set_weight_by_owner();
                }

                @DisplayName("addAsset - should throw unauthorized")
                @Test
                @Order(60)
                void should_throw_unauthorized() {
                    client._transfer(Address.of(tester), BigInteger.valueOf(100).multiply(ICX), "transfer");

                    assertUserRevert(RewardDistributionException.notOwner(),
                            () -> scoreClientWithTester.addAsset("type-a", "asset-1", assets.get("asset-1"),
                                    BigInteger.ZERO),
                            null);

                }

                @DisplayName("addAsset - should able to add asset by owner")
                @Test
                @Order(70)
                void should_able_to_add_asset() {
                    List<score.Address> addresses = scoreClient.getAssets();
                    assertEquals(1, addresses.size());
                    scoreClient.addAsset("type-a", "asset-1", assets.get("asset-1"), BigInteger.ZERO);
                    scoreClient.addAsset("type-a", "asset-2", assets.get("asset-2"), BigInteger.ZERO);
                    scoreClient.addAsset("type-a", "asset-3", assets.get("asset-3"), BigInteger.ZERO);

                    addresses = scoreClient.getAssets();
                    assertEquals(4, addresses.size());
                }

            }

        }


    }


}