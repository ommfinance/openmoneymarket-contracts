package finance.omm.score.reward.test.integration;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.RewardDistributionConfig;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.score.core.reward.exception.RewardWeightException;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.Address.Type;
import foundation.icon.score.client.RevertedException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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


    private static OMMClient ownerClient;
    private static OMMClient testClient;

    private static Map<String, Address> addressMap;


    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("rewardDistribution/scores.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new RewardDistributionConfig(omm.getAddresses());
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();
    }


    private Map<String, Address> assets = new HashMap<>() {{
        put("asset-1", Faker.address(Type.CONTRACT));
        put("asset-2", Faker.address(Type.CONTRACT));
        put("asset-3", Faker.address(Type.CONTRACT));
    }};

    Map<String, Boolean> STATES = new HashMap<>();


    @DisplayName("addType")
    @Nested
    class TestTypes {

        @DisplayName("addType - should throw unauthorized")
        @Test
        @Order(10)
        void should_throw_unauthorized_addType() {

            assertUserRevert(RewardDistributionException.unauthorized("Only Governance contract is allowed to call " +
                            "addType" + " method"),
                    () -> testClient.reward.addType("key", false), null);

        }

        @DisplayName("addType - should able to add type contract false")
        @Test
        @Order(20)
        void should_add_type_transfer_to_contract_false() {
            if (STATES.getOrDefault("should_add_type_contract_false", false)) {
                return;
            }
            Map<String, BigInteger> allWeightType = ownerClient.rewardWeightController.getAllTypeWeight(null);
            Assertions.assertTrue(allWeightType.isEmpty());

            ownerClient.governance.addType("reserve", false);
            allWeightType = ownerClient.rewardWeightController.getAllTypeWeight(null);
            Assertions.assertFalse(allWeightType.isEmpty());
            Assertions.assertEquals(1, allWeightType.size());

            assertEquals(allWeightType.get("reserve"), BigInteger.ZERO);

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

            ownerClient.governance.addType("daoFund", true);

            Map<String, BigInteger> allWeightType = ownerClient.rewardWeightController.getAllTypeWeight(null);
            Assertions.assertEquals(2, allWeightType.size());

            assertEquals(allWeightType.get("reserve"), BigInteger.ZERO);
            assertEquals(allWeightType.get("daoFund"), BigInteger.ZERO);

            STATES.put("should_add_type_contract_true", true);
        }

        @DisplayName("setTypeWeight")
        @Nested
        class TestSetTypeWeight {

            BigInteger CURRENT_TIME = BigInteger.valueOf(System.currentTimeMillis() / 1_000L);
            BigInteger AFTER_10_SEC;

            @BeforeEach
            void before() {
                should_add_type_transfer_to_contract_true();
            }

            @DisplayName("setTypeWeight - should throw unauthorized")
            @Test
            @Order(30)
            void should_throw_unauthorized_setTypeWeight() {

                assertUserRevert(
                        RewardWeightException.notAuthorized("Only Governance contract can call set type method"),
                        () -> testClient.rewardWeightController.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("reserve", ICX.divide(BigInteger.TWO)),
                                new TypeWeightStruct("daoFund", ICX.divide(BigInteger.TWO)),
                        }, null), null);

            }

            @DisplayName("should able to set type weight by owner")
            @Test
            @Order(35)
            void should_able_to_set_weight_by_owner() {
                if (STATES.getOrDefault("should_able_to_set_weight_by_owner", false)) {
                    return;
                }
                AFTER_10_SEC = CURRENT_TIME.add(BigInteger.TEN);
                ownerClient.governance.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("reserve", ICX.divide(BigInteger.TWO)),
                        new TypeWeightStruct("daoFund", ICX.divide(BigInteger.TWO)),
                }, AFTER_10_SEC);

                Map<String, BigInteger> allWeightType = ownerClient.rewardWeightController.
                        getAllTypeWeight(AFTER_10_SEC.add(BigInteger.ONE));
                Assertions.assertFalse(allWeightType.isEmpty());

                assertEquals(allWeightType.get("reserve"), ICX.divide(BigInteger.TWO));
                assertEquals(allWeightType.get("daoFund"), ICX.divide(BigInteger.TWO));
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
                BigInteger weight = ownerClient.rewardWeightController
                        .getTypeWeight("reserve", CURRENT_TIME.add(BigInteger.valueOf(11)));
                assertEquals(weight, ICX.divide(BigInteger.TWO));

                BigInteger aValue = ICX.divide(BigInteger.valueOf(4L));
                BigInteger bValue = ICX.subtract(aValue);

                BigInteger AFTER_1000_SEC = CURRENT_TIME.add(BigInteger.valueOf(1_000L));

                ownerClient.governance.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("reserve", aValue),
                        new TypeWeightStruct("daoFund", bValue),
                }, AFTER_1000_SEC);

                BigInteger aActualValue = ownerClient.rewardWeightController.getTypeWeight("reserve",
                        AFTER_1000_SEC.add(BigInteger.ONE));

                assertEquals(aValue, aActualValue);

                aActualValue = ownerClient.rewardWeightController.getTypeWeight("reserve", AFTER_1000_SEC);

                assertEquals(ICX.divide(BigInteger.TWO), aActualValue);

                STATES.put("should_able_to_set_weight", true);
            }

            @DisplayName("should not able to weight on old timestamp")
            @Test
            @Order(50)
            void should_not_able_set_old_timestamp() {
                should_able_to_set_weight_by_owner();
                BigInteger aValue = ICX.divide(BigInteger.valueOf(4L));
                BigInteger bValue = ICX.subtract(aValue);

                BigInteger before10Sec = AFTER_10_SEC.subtract(BigInteger.valueOf(20L));

                String expectedError = "latest " + AFTER_10_SEC + " checkpoint exists than " + before10Sec;

                assertReverted(new RevertedException(1, "UnknownFailure"),
                        () -> ownerClient.governance.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("reserve", aValue),
                                new TypeWeightStruct("daoFund", bValue),
                        }, before10Sec));
            }


            @DisplayName("addAsset")
            @Nested
            class TestAsset {

                @BeforeEach
                void before() {
                    should_add_type_transfer_to_contract_true();
                    should_able_to_set_weight();
                }

                @DisplayName("addAsset - should throw unauthorized")
                @Test
                @Order(60)
                void should_throw_unauthorized() {

                    assertUserRevert(RewardDistributionException.unauthorized(
                                    "Only Governance contract is allowed to call addAsset method"),
                            () -> testClient.reward.addAsset("reserve", "oICX", assets.get("asset-1"),
                                    BigInteger.ZERO),
                            null);

                }

                @DisplayName("addAsset - should able to add asset by owner")
                @Test
                @Order(70)
                void should_able_to_add_asset() {
                    if (STATES.getOrDefault("should_able_to_add_asset", false)) {
                        return;
                    }
                    List<score.Address> addresses = ownerClient.reward.getAssets();
                    assertEquals(1, addresses.size());
                    ownerClient.governance.addAsset("reserve", "asset-1", assets.get("asset-1"), BigInteger.ZERO);
                    ownerClient.governance.addAsset("reserve", "asset-2", assets.get("asset-2"), BigInteger.ZERO);
                    ownerClient.governance.addAsset("reserve", "asset-3", assets.get("asset-3"), BigInteger.ZERO);

                    addresses = ownerClient.reward.getAssets();
                    assertEquals(4, addresses.size());

                    STATES.put("should_able_to_add_asset", true);
                }

                @DisplayName("setAssetWeight")
                @Nested
                class TestSetAssetWeight {

                    @BeforeEach
                    void before() {
                        should_add_type_transfer_to_contract_true();
                        should_able_to_set_weight();
                        should_able_to_add_asset();
                    }

                    @DisplayName("setAssetWeight - should throw unauthorized")
                    @Test
                    @Order(80)
                    void should_throw_unauthorized() {

                        assertUserRevert(RewardWeightException.notAuthorized(
                                        "Only Governance contract can call set asset weight method"),
                                () -> testClient.rewardWeightController.setAssetWeight("reserve", new WeightStruct[]{
                                        new WeightStruct(assets.get("asset-1"), ICX.divide(BigInteger.TWO)),
                                        new WeightStruct(assets.get("asset-2"), ICX.divide(BigInteger.valueOf(4L))),
                                        new WeightStruct(assets.get("asset-3"), ICX.divide(BigInteger.valueOf(4L))),
                                }, BigInteger.valueOf(1000L)), null);

                    }

                    @DisplayName("verify weight of transferred to contract type")
                    @Test
                    @Order(90)
                    void verifyWeightOfTransferredToContract() {

                        BigInteger AFTER_11_SEC = CURRENT_TIME.add(BigInteger.valueOf(11));

                        Map<String, BigInteger> type_B_Weights = ownerClient.rewardWeightController.
                                getAssetWeightByTimestamp("daoFund", AFTER_11_SEC);

                        System.out.println("type_B_Weights = " + type_B_Weights);
                        // type daoFund is not split, so 100%
                        assertEquals(ICX, type_B_Weights.get("daoFund"));

                    }

                    @DisplayName("should able to set asset weight by owner")
                    @Test
                    @Order(100)
                    void should_able_to_set_asset_weight_by_owner() {
                        if (STATES.getOrDefault("should_able_to_set_asset_weight_by_owner", false)) {
                            return;
                        }
                        /*
                            For reserve type
                            50 % : 10 sec - 1000 sec
                            25 % : 1000 sec and later
                         */
                        // set asset weight after 10 seconds
                        BigInteger AFTER_10_SEC = CURRENT_TIME.add(BigInteger.valueOf(10L));
                        ownerClient.governance.setAssetWeight("reserve", new WeightStruct[]{
                                new WeightStruct(assets.get("asset-1"), ICX.divide(BigInteger.TWO)),
                                new WeightStruct(assets.get("asset-2"), ICX.divide(BigInteger.valueOf(4L))),
                                new WeightStruct(assets.get("asset-3"), ICX.divide(BigInteger.valueOf(4L))),
                        }, AFTER_10_SEC);

                        // should be zero before 10 sec
                        BigInteger assetWeight = ownerClient.rewardWeightController.getAssetWeight(
                                assets.get("asset-1"), AFTER_10_SEC.subtract(BigInteger.ONE));
                        assertEquals(BigInteger.ZERO, assetWeight);

                        // after 10th second
                        long assetAWeight = 50 * 50; //type weight * asset weight
                        assetWeight = ownerClient.rewardWeightController.getAssetWeight(assets.get("asset-1"),
                                CURRENT_TIME.add(BigInteger.valueOf(11L)));
                        assertEquals(ICX.multiply(BigInteger.valueOf(assetAWeight)).divide(BigInteger.valueOf(10000L)),
                                assetWeight);

                        // after 1001 th second
                        assetAWeight = 25 * 50; //type weight * asset weight
                        BigInteger AFTER_1001_SEC = CURRENT_TIME.add(BigInteger.valueOf(1001L));
                        assetWeight = ownerClient.rewardWeightController.getAssetWeight(assets.get("asset-1"),
                                AFTER_1001_SEC);
                        assertEquals(ICX.multiply(BigInteger.valueOf(assetAWeight)).divide(BigInteger.valueOf(10000L)),
                                assetWeight);

                        Map<String, BigInteger> type_A_Weights = ownerClient.rewardWeightController.getAssetWeightByTimestamp(
                                "reserve",
                                AFTER_1001_SEC);

                        long asset_1_Weight = 50; // asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_1_Weight)).divide(BigInteger.valueOf(100L)),
                                type_A_Weights.get("asset-1"));

                        long asset_2_Weight = 25; // asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_2_Weight)).divide(BigInteger.valueOf(100L)),
                                type_A_Weights.get("asset-2"));

                        long asset_3_Weight = 25; // asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_3_Weight)).divide(BigInteger.valueOf(100L)),
                                type_A_Weights.get("asset-3"));

                        STATES.put("should_able_to_set_asset_weight_by_owner", true);
                    }
                }
            }
        }
    }
}