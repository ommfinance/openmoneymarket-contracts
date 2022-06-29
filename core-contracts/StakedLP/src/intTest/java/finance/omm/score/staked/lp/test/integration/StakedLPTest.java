package finance.omm.score.staked.lp.test.integration;

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
        put(1, Faker.address(Type.CONTRACT));
        put(2, Faker.address(Type.CONTRACT));
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

        assertNull(ownerClient.stakedLP.getPoolById(1));

        assertUserRevert(StakedLPException.unauthorized("error"),
                () -> ownerClient.stakedLP.addPool(1, POOLS.get(1)), null);

        AssetConfig pool1 = new AssetConfig();
        pool1.poolID = 1;
        pool1.assetName = "LP-1";
        pool1.distPercentage = ICX;
        pool1.rewardEntity = "liquidity";
        pool1.asset = POOLS.get(1);

        AssetConfig pool2 = new AssetConfig();
        pool2.poolID = 2;
        pool2.assetName = "LP-2";
        pool2.distPercentage = ICX;
        pool2.rewardEntity = "liquidity";
        pool2.asset = POOLS.get(2);

        ownerClient.governance.addPools(new AssetConfig[]{
                pool1, pool2
        });

        assertEquals(POOLS.get(1), ownerClient.stakedLP.getPoolById(1));

        assertEquals(new HashMap<>() {{
            put("1", POOLS.get(1));
            put("2", POOLS.get(2));
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

        ownerClient.dex.mintTo(BigInteger.ONE, testClient.getAddress(), BigInteger.TEN.pow(19));

        ownerClient.dex.mintTo(BigInteger.ONE, demoClient.getAddress(), BigInteger.TEN.pow(19));

        Function<UserRevertedException, String> supplier = Throwable::getMessage;

        //stake failure as no data passed
//        assertReverted(new RevertedException(1, "UnknownFailure"),
//                () -> demoClient.dex.transferFrom(demoClient.getAddress(), addressMap.get("stakedLP"), BigInteger.ONE,
//                        BigInteger.TEN.pow(19), new byte[0]));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        demoClient.dex.transferFrom(demoClient.getAddress(), addressMap.get("stakedLP"), BigInteger.ONE,
                BigInteger.valueOf(9).pow(19), "{\"method\":\"stake\"}".getBytes());

        Map<String, BigInteger> balanceOfDemo = demoClient.stakedLP.balanceOf(demoClient.getAddress(), 1);

        assertEquals(BigInteger.ONE, balanceOfDemo.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(1).pow(19), balanceOfDemo.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(9).pow(19), balanceOfDemo.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(9).pow(19), balanceOfDemo.get("totalStakedBalance"));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        testClient.dex.transferFrom(testClient.getAddress(), addressMap.get("stakedLP"), BigInteger.ONE,
                BigInteger.valueOf(5).pow(19), "{\"method\":\"stake\"}".getBytes());

        balanceOfDemo = testClient.stakedLP.balanceOf(testClient.getAddress(), 1);

        assertEquals(BigInteger.ONE, balanceOfDemo.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(5).pow(19), balanceOfDemo.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(5).pow(19), balanceOfDemo.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(14).pow(19), balanceOfDemo.get("totalStakedBalance"));
    }
}