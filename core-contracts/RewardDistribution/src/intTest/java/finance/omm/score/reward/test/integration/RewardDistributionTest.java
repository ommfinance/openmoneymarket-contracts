package finance.omm.score.reward.test.integration;

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

            ownerClient.reward.addType("DaoFund", true);

            Map<String, BigInteger> allWeightType = ownerClient.rewardWeightController.getAllTypeWeight(null);
            Assertions.assertEquals(2, allWeightType.size());

            assertEquals(allWeightType.get("reserve"), BigInteger.ZERO);
            assertEquals(allWeightType.get("DaoFund"), BigInteger.ZERO);

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

                assertUserRevert(RewardWeightException.notOwner(),
                        () -> testClient.rewardWeightController.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("reserve", ICX.divide(BigInteger.TWO)),
                                new TypeWeightStruct("DaoFund", ICX.divide(BigInteger.TWO)),
                        }, null), null);

            }

            @DisplayName("should able to set type weight by owner")
            @Test
            @Order(35)
            void should_able_to_set_weight_by_owner() {
                if (STATES.getOrDefault("should_able_to_set_weight_by_owner", false)) {
                    return;
                }
                ownerClient.rewardWeightController.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("reserve", ICX.divide(BigInteger.TWO)),
                        new TypeWeightStruct("DaoFund", ICX.divide(BigInteger.TWO)),
                }, BigInteger.TEN);

                Map<String, BigInteger> allWeightType = ownerClient.rewardWeightController.getAllTypeWeight(null);
                Assertions.assertFalse(allWeightType.isEmpty());

                assertEquals(allWeightType.get("reserve"), ICX.divide(BigInteger.TWO));
                assertEquals(allWeightType.get("DaoFund"), ICX.divide(BigInteger.TWO));
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
                BigInteger weight = ownerClient.rewardWeightController.getTypeWeight("reserve", null);
                assertEquals(weight, ICX.divide(BigInteger.TWO));

                BigInteger aValue = ICX.divide(BigInteger.valueOf(4L));
                BigInteger bValue = ICX.subtract(aValue);

                ownerClient.rewardWeightController.setTypeWeight(new TypeWeightStruct[]{
                        new TypeWeightStruct("reserve", aValue),
                        new TypeWeightStruct("DaoFund", bValue),
                }, BigInteger.valueOf(1000L));

                BigInteger aActualValue = ownerClient.rewardWeightController.getTypeWeight("reserve",
                        BigInteger.valueOf(1001));

                assertEquals(aValue, aActualValue);

                aActualValue = ownerClient.rewardWeightController.getTypeWeight("reserve", BigInteger.valueOf(999));

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
                        () -> ownerClient.rewardWeightController.setTypeWeight(new TypeWeightStruct[]{
                                new TypeWeightStruct("reserve", aValue),
                                new TypeWeightStruct("DaoFund", bValue),
                        }, BigInteger.valueOf(999)), null);


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

                    assertUserRevert(RewardDistributionException.notOwner(),
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
                    ownerClient.reward.addAsset("reserve", "asset-1", assets.get("asset-1"), BigInteger.ZERO);
                    ownerClient.reward.addAsset("reserve", "asset-2", assets.get("asset-2"), BigInteger.ZERO);
                    ownerClient.reward.addAsset("reserve", "asset-3", assets.get("asset-3"), BigInteger.ZERO);

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
                        should_able_to_set_weight_by_owner();
                        should_able_to_add_asset();
                    }

                    @DisplayName("setAssetWeight - should throw unauthorized")
                    @Test
                    @Order(80)
                    void should_throw_unauthorized() {

                        assertUserRevert(RewardWeightException.notOwner(),
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

                        Map<String, BigInteger> type_B_Weights = ownerClient.rewardWeightController.getAssetWeightByTimestamp(
                                "DaoFund",
                                BigInteger.valueOf(System.currentTimeMillis() * 1000));

                        long type_B_Weight = 75 * 100; //type weight * asset weight
                        System.out.println("type_B_Weights = " + type_B_Weights);
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(type_B_Weight)).divide(BigInteger.valueOf(10000L)),
                                type_B_Weights.get(assets.get("DaoFund").toString()));

                    }

                    @DisplayName("should able to set asset weight by owner")
                    @Test
                    @Order(100)
                    void should_able_to_set_asset_weight_by_owner() {
                        if (STATES.getOrDefault("should_able_to_set_asset_weight_by_owner", false)) {
                            return;
                        }
                        ownerClient.rewardWeightController.setAssetWeight("reserve", new WeightStruct[]{
                                new WeightStruct(assets.get("asset-1"), ICX.divide(BigInteger.TWO)),
                                new WeightStruct(assets.get("asset-2"), ICX.divide(BigInteger.valueOf(4L))),
                                new WeightStruct(assets.get("asset-3"), ICX.divide(BigInteger.valueOf(4L))),
                        }, BigInteger.valueOf(1000L));

                        BigInteger assetWeight = ownerClient.rewardWeightController.getAssetWeight(
                                assets.get("asset-1"),
                                BigInteger.valueOf(999L));
                        assertEquals(BigInteger.ZERO, assetWeight);

                        assetWeight = ownerClient.rewardWeightController.getAssetWeight(assets.get("asset-1"),
                                BigInteger.valueOf(1001L));//todo 1000L or 1001L

                        long assetAWeight = 25 * 50; //type weight * asset weight

                        assertEquals(ICX.multiply(BigInteger.valueOf(assetAWeight)).divide(BigInteger.valueOf(10000L)),
                                assetWeight);
                        assetWeight = ownerClient.rewardWeightController.getAssetWeight(assets.get("asset-1"),
                                null);

                        assertEquals(ICX.multiply(BigInteger.valueOf(assetAWeight)).divide(BigInteger.valueOf(10000L)),
                                assetWeight);

                        Map<String, BigInteger> type_A_Weights = ownerClient.rewardWeightController.getAssetWeightByTimestamp(
                                "reserve",
                                BigInteger.valueOf(1001L));

                        long asset_1_Weight = 25 * 50; //type weight * asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_1_Weight)).divide(BigInteger.valueOf(10000L)),
                                type_A_Weights.get(assets.get("asset-1").toString()));

                        long asset_2_Weight = 25 * 25; //type weight * asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_2_Weight)).divide(BigInteger.valueOf(10000L)),
                                type_A_Weights.get(assets.get("asset-2").toString()));

                        long asset_3_Weight = 25 * 25; //type weight * asset weight
                        assertEquals(
                                ICX.multiply(BigInteger.valueOf(asset_3_Weight)).divide(BigInteger.valueOf(10000L)),
                                type_A_Weights.get(assets.get("asset-3").toString()));

                        STATES.put("should_able_to_set_asset_weight_by_owner", true);
                    }
                }

            }

        }


    }


}