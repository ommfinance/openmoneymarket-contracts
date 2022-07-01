package finance.omm.score.staked.lp.test.integration;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import finance.omm.score.staked.lp.test.integration.config.StakedLPConfig;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.Address.Type;
import foundation.icon.score.client.RevertedException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import score.UserRevertedException;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class StakedLPTest implements ScoreIntegrationTest {


    private static OMMClient ownerClient;
    private static OMMClient testClient;
    private static OMMClient demoClient;

    private static Map<String, Address> addressMap;

    private Map<String, Boolean> status = new HashMap<>();

    private final Map<Integer, Address> POOLS = new HashMap<>() {{
        put(6, Faker.address(Type.CONTRACT));
        put(7, Faker.address(Type.CONTRACT));
    }};


    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new StakedLPConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();
        demoClient = omm.newClient(BigInteger.TEN.pow(24));
    }


    @DisplayName("Test name")
    @Test
    void testName() {
        assertEquals("OMM Staked Lp", ownerClient.stakedLP.name());
    }

    @DisplayName("Minimum stake")
    @Test
    @Order(10)
    void testMinimumStake() {
        assertEquals(BigInteger.ZERO, ownerClient.stakedLP.getMinimumStake());
        assertUserRevert(StakedLPException.notOwner(),
                () -> testClient.stakedLP.setMinimumStake(ICX), null);

        BigInteger negateValue = ICX.negate();
        assertUserRevert(StakedLPException.unknown("Minimum stake value must be positive, " + negateValue),
                () -> ownerClient.stakedLP.setMinimumStake(negateValue), null);

        ownerClient.stakedLP.setMinimumStake(ICX);

        assertEquals(ICX, ownerClient.stakedLP.getMinimumStake());

        status.put("testMinimumStake", true);
    }

    @DisplayName("Add pools")
    @Test
    @Order(20)
    void testAddPools() {

        assertEquals(new HashMap<>(), ownerClient.stakedLP.getSupportedPools());

        assertNull(ownerClient.stakedLP.getPoolById(7));

        assertUserRevert(StakedLPException.unauthorized("error"),
                () -> ownerClient.stakedLP.addPool(1, POOLS.get(7)), null);

        ownerClient.governance.addAsset("liquidity", "OMM/sICX", POOLS.get(7), BigInteger.valueOf(7));
        ownerClient.governance.addAsset("liquidity", "OMM/IUSDC", POOLS.get(6), BigInteger.valueOf(6));

        AssetConfig pool1 = new AssetConfig();
        pool1.poolID = 6;
        pool1.assetName = "OMM/IUSDC";
        pool1.distPercentage = ICX.divide(BigInteger.TWO);
        pool1.rewardEntity = "liquidity";
        pool1.asset = POOLS.get(6);
        ;

        AssetConfig pool2 = new AssetConfig();
        pool2.poolID = 7;
        pool2.assetName = "OMM/sICX";
        pool2.distPercentage = ICX.divide(BigInteger.TWO);
        pool2.rewardEntity = "liquidity";
        pool2.asset = POOLS.get(7);
        ;

        ownerClient.governance.addPools(new AssetConfig[]{
                pool1, pool2
        });

        assertEquals(POOLS.get(7), ownerClient.stakedLP.getPoolById(7));

        assertEquals(new HashMap<>() {{
            put("6", POOLS.get(6));
            put("7", POOLS.get(7));
        }}, ownerClient.stakedLP.getSupportedPools());

        status.put("testAddPools", true);
    }


    @DisplayName("test onIRC31Received")
    @Test
    @Order(30)
    void testOnIRC31Received() {
        if (!status.getOrDefault("testMinimumStake", false)) {
            testMinimumStake();
        }
        if (!status.getOrDefault("testAddPools", false)) {
            testAddPools();
        }

        ownerClient.dex.mintTo(BigInteger.valueOf(7), testClient.getAddress(), BigInteger.TEN.pow(20));

        ownerClient.dex.mintTo(BigInteger.valueOf(7), demoClient.getAddress(), BigInteger.TEN.pow(20));

        Function<UserRevertedException, String> supplier = Throwable::getMessage;

        //stake failure as no data passed
        assertReverted(new RevertedException(1, "UnknownFailure"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        BigInteger.TEN.pow(19), new byte[0]));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                BigInteger.TEN.pow(19), "{\"method\":\"stake\"}".getBytes());

        Map<String, BigInteger> balanceOfDemo = demoClient.stakedLP.balanceOf(demoClient.getAddress(), 7);

        assertEquals(BigInteger.valueOf(7), balanceOfDemo.get("poolID"));
        assertEquals(BigInteger.valueOf(90).multiply(ICX), balanceOfDemo.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("totalStakedBalance"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfDemo.get("userTotalBalance"));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        testClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                BigInteger.valueOf(50).multiply(ICX), "{\"method\":\"stake\"}".getBytes());

        Map<String, BigInteger> balanceOfTest = testClient.stakedLP.balanceOf(testClient.getAddress(), 7);

        assertEquals(BigInteger.valueOf(7), balanceOfTest.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfTest.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTest.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTest.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(60).multiply(ICX), balanceOfTest.get("totalStakedBalance"));

        status.put("testOnIRC31Received", true);
    }

    @DisplayName("test unstake")
    @Test
    @Order(40)
    void testUnstake() {
        if (!status.getOrDefault("testOnIRC31Received", false)) {
            testOnIRC31Received();
        }

        // test and demo users have some lp tokens staked
        Map<String, BigInteger> balanceOfTestBefore = testClient.stakedLP.balanceOf(testClient.getAddress(), 7);

        assertUserRevert(StakedLPException.unknown("pool with id: " + 8 + "is not supported"),
                () -> testClient.stakedLP.unstake(8, BigInteger.valueOf(200).multiply(ICX)), null);

        // unstake
        testClient.stakedLP.unstake(7, BigInteger.TEN.multiply(ICX));

        // check balances after
        Map<String, BigInteger> balanceOfTestAfter = testClient.stakedLP.balanceOf(testClient.getAddress(), 7);
        assertEquals(BigInteger.valueOf(7), balanceOfTestAfter.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfTestAfter.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(60).multiply(ICX), balanceOfTestAfter.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(40).multiply(ICX), balanceOfTestAfter.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTestAfter.get("totalStakedBalance"));

        status.put("testUnstake", true);
    }
}