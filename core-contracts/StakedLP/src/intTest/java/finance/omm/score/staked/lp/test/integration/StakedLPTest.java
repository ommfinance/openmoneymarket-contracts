package finance.omm.score.staked.lp.test.integration;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.SupplyDetails;
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
import java.util.List;
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
import score.UserRevertException;
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

        assertUserRevert(StakedLPException.unauthorized("not governance"),
                () -> ownerClient.stakedLP.removePool(6),null);

//        Address pool = Faker.address(Type.CONTRACT);
//        assertUserRevert(StakedLPException.unknown("pool null"),
//                ()-> ownerClient.governance.removePool(pool),null);

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

        ownerClient.dex.mintTo(BigInteger.valueOf(8), demoClient.getAddress(), BigInteger.TEN.pow(20));

        ownerClient.dex.mintTo(BigInteger.valueOf(6), demoClient.getAddress(), BigInteger.valueOf(50).multiply(ICX));

        ownerClient.dex.mintTo(BigInteger.valueOf(6), testClient.getAddress(), BigInteger.valueOf(50).multiply(ICX));

        Function<UserRevertedException, String> supplier = Throwable::getMessage;

        //stake failure as no data passed
        assertReverted(new RevertedException(1, "UnknownFailure"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        BigInteger.TEN.pow(19), new byte[0]));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                BigInteger.TEN.pow(19), "{\"method\":\"stake\"}".getBytes());

        demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(6),
                BigInteger.valueOf(20).multiply(ICX), "{\"method\":\"stake\"}".getBytes());


        assertReverted(new RevertedException(1, "No valid method called :: " + "abcd"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        BigInteger.TEN.pow(19), "{\"method\":\"abcd\"}".getBytes()));

        assertReverted(new RevertedException(1,"pool with id: " + 8 + " is not supported"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(8),
                        BigInteger.TEN.pow(19), "{\"method\":\"stake\"}".getBytes()));

        assertUserRevert(new UserRevertException("Invalid amount"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        BigInteger.ZERO.pow(19), "{\"method\":\"stake\"}".getBytes()),null);

        assertUserRevert(new UserRevertException("Insufficient funds"),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        BigInteger.ONE.pow(19).negate(), "{\"method\":\"stake\"}".getBytes()),null);

        BigInteger minValue = ICX.subtract(BigInteger.ONE);
        assertReverted(new RevertedException(1,"Amount to stake: " + minValue +" is smaller the minimum stake: " +ICX),
                () -> demoClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                        minValue, "{\"method\":\"stake\"}".getBytes()));


        Map<String, BigInteger> balanceOfDemo = demoClient.stakedLP.balanceOf(demoClient.getAddress(), 7);

        assertEquals(BigInteger.valueOf(7), balanceOfDemo.get("poolID"));
        assertEquals(BigInteger.valueOf(90).multiply(ICX), balanceOfDemo.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(10).pow(19), balanceOfDemo.get("totalStakedBalance"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfDemo.get("userTotalBalance"));

        Map<String, BigInteger> balanceOfDemo6 = demoClient.stakedLP.balanceOf(demoClient.getAddress(), 6);

        assertEquals(BigInteger.valueOf(6), balanceOfDemo6.get("poolID"));
        assertEquals(BigInteger.valueOf(30).multiply(ICX), balanceOfDemo6.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(20).multiply(ICX), balanceOfDemo6.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(20).multiply(ICX), balanceOfDemo6.get("totalStakedBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfDemo6.get("userTotalBalance"));

        //stake BigInteger.valueOf(9).pow(19) out of BigInteger.valueOf(10).pow(19) LP token
        testClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(7),
                BigInteger.valueOf(50).multiply(ICX), "{\"method\":\"stake\"}".getBytes());

        testClient.dex.transfer(addressMap.get("stakedLP"), BigInteger.valueOf(6),
                BigInteger.valueOf(10).multiply(ICX), "{\"method\":\"stake\"}".getBytes());

        Map<String, BigInteger> balanceOfTest = testClient.stakedLP.balanceOf(testClient.getAddress(), 7);

        assertEquals(BigInteger.valueOf(7), balanceOfTest.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfTest.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTest.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTest.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(60).multiply(ICX), balanceOfTest.get("totalStakedBalance"));

        Map<String, BigInteger> balanceOfTest6 = testClient.stakedLP.balanceOf(testClient.getAddress(), 6);

        assertEquals(BigInteger.valueOf(6), balanceOfTest6.get("poolID"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTest6.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(40).multiply(ICX), balanceOfTest6.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(10).multiply(ICX), balanceOfTest6.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(30).multiply(ICX), balanceOfTest6.get("totalStakedBalance"));


        assertEquals(BigInteger.valueOf(60).multiply(ICX), testClient.stakedLP.getTotalStaked(7).totalStaked);
        assertEquals(BigInteger.valueOf(18),testClient.stakedLP.getTotalStaked(7).decimals);
        assertEquals(BigInteger.valueOf(60).multiply(ICX), testClient.stakedLP.totalStaked(7));

        assertEquals(BigInteger.valueOf(30).multiply(ICX), testClient.stakedLP.getTotalStaked(6).totalStaked);
        assertEquals(BigInteger.valueOf(12),testClient.stakedLP.getTotalStaked(6).decimals);
        assertEquals(BigInteger.valueOf(30).multiply(ICX), testClient.stakedLP.totalStaked(6));

        //getBalanceByPool
        List<Map<String, BigInteger>> balanceByPool = testClient.stakedLP.getBalanceByPool();

        BigInteger[] id = {BigInteger.valueOf(6),BigInteger.valueOf(7)};
        BigInteger[] amount = {BigInteger.valueOf(30).multiply(ICX),BigInteger.valueOf(60).multiply(ICX)};

        for (int i = 0; i < balanceByPool.size(); i++) {
            assertEquals(balanceByPool.get(i).get("poolID"),id[i]);
            assertEquals(balanceByPool.get(i).get("totalStakedBalance"),amount[i]);
        }

        //getPoolBalanceByUser
        List<Map<String, BigInteger>> demoUser = (List<Map<String, BigInteger>>) testClient.stakedLP.getPoolBalanceByUser(demoClient.getAddress());

        int i = 0;
        assertEquals(demoUser.get(i).get("poolID"),BigInteger.valueOf(6));
        assertEquals(demoUser.get(i).get("userTotalBalance"),BigInteger.valueOf(50).multiply(ICX));
        assertEquals(demoUser.get(i).get("userAvailableBalance"),BigInteger.valueOf(30).multiply(ICX));
        assertEquals(demoUser.get(i).get("totalStakedBalance"),BigInteger.valueOf(30).multiply(ICX));
        assertEquals(demoUser.get(i).get("userStakedBalance"),BigInteger.valueOf(20).multiply(ICX));

        i = 1;
        assertEquals(demoUser.get(i).get("poolID"),BigInteger.valueOf(7));
        assertEquals(demoUser.get(i).get("userTotalBalance"),BigInteger.valueOf(100).multiply(ICX));
        assertEquals(demoUser.get(i).get("userAvailableBalance"),BigInteger.valueOf(90).multiply(ICX));
        assertEquals(demoUser.get(i).get("totalStakedBalance"),BigInteger.valueOf(60).multiply(ICX));
        assertEquals(demoUser.get(i).get("userStakedBalance"),BigInteger.valueOf(10).multiply(ICX));


        List<Map<String, BigInteger>> testUser = (List<Map<String, BigInteger>>) testClient.stakedLP.getPoolBalanceByUser(testClient.getAddress());

        assertEquals(testUser.get(0).get("poolID"),BigInteger.valueOf(6));
        assertEquals(testUser.get(0).get("userTotalBalance"),BigInteger.valueOf(50).multiply(ICX));
        assertEquals(testUser.get(0).get("userAvailableBalance"),BigInteger.valueOf(40).multiply(ICX));
        assertEquals(testUser.get(0).get("totalStakedBalance"),BigInteger.valueOf(30).multiply(ICX));
        assertEquals(testUser.get(0).get("userStakedBalance"),BigInteger.valueOf(10).multiply(ICX));

        assertEquals(testUser.get(1).get("poolID"),BigInteger.valueOf(7));
        assertEquals(testUser.get(1).get("userTotalBalance"),BigInteger.valueOf(100).multiply(ICX));
        assertEquals(testUser.get(1).get("userAvailableBalance"),BigInteger.valueOf(50).multiply(ICX));
        assertEquals(testUser.get(1).get("totalStakedBalance"),BigInteger.valueOf(60).multiply(ICX));
        assertEquals(testUser.get(1).get("userStakedBalance"),BigInteger.valueOf(50).multiply(ICX));

        //getLPStakedSupply
        SupplyDetails supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(7,demoClient.getAddress());

        assertEquals(BigInteger.valueOf(18),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(60).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(10).multiply(ICX),supplyDetails.principalUserBalance);

        //getLPStakedSupply
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(7,testClient.getAddress());

        assertEquals(BigInteger.valueOf(18),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(60).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(50).multiply(ICX),supplyDetails.principalUserBalance);

        //of pool id 6
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(6,demoClient.getAddress());

        assertEquals(BigInteger.valueOf(12),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(30).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(20).multiply(ICX),supplyDetails.principalUserBalance);

        //getLPStakedSupply
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(6,testClient.getAddress());

        assertEquals(BigInteger.valueOf(12),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(30).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(10).multiply(ICX),supplyDetails.principalUserBalance);

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

        //unstake error when value is passed is zero or less than zero
        assertUserRevert(StakedLPException.unknown("Cannot unstake less than zero value to stake" + 0),
                () -> testClient.stakedLP.unstake(7, BigInteger.valueOf(0).multiply(ICX)), null);

        assertUserRevert(StakedLPException.unknown("Cannot unstake less than zero value to stake" + 0),
                () -> testClient.stakedLP.unstake(7, BigInteger.valueOf(20).multiply(ICX).negate()), null);

        //wnstake error when value passed is more than staked value
        assertUserRevert(StakedLPException.unknown("Cannot unstake,user dont have enough staked balance" +
                        "amount to unstake " + 60 +
                        "staked balance of user:" + testClient.getAddress()  + "is" + 50),
                () -> testClient.stakedLP.unstake(7, BigInteger.valueOf(60).multiply(ICX)), null);

        // unstake by testClient
        testClient.stakedLP.unstake(7, BigInteger.TEN.multiply(ICX));
        demoClient.stakedLP.unstake(7, BigInteger.valueOf(5).multiply(ICX));

        testClient.stakedLP.unstake(6, BigInteger.valueOf(5).multiply(ICX));
        demoClient.stakedLP.unstake(6, BigInteger.valueOf(15).multiply(ICX));

        // check balances after
        Map<String, BigInteger> balanceOfTestAfter = testClient.stakedLP.balanceOf(testClient.getAddress(), 7);
        assertEquals(BigInteger.valueOf(7), balanceOfTestAfter.get("poolID"));
        assertEquals(BigInteger.valueOf(10).pow(20), balanceOfTestAfter.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(60).multiply(ICX), balanceOfTestAfter.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(40).multiply(ICX), balanceOfTestAfter.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(45).multiply(ICX), balanceOfTestAfter.get("totalStakedBalance"));

        // check balances after
        Map<String, BigInteger> balanceOfDemoAfter = testClient.stakedLP.balanceOf(demoClient.getAddress(), 7);
        assertEquals(BigInteger.valueOf(7), balanceOfDemoAfter.get("poolID"));
        assertEquals(BigInteger.valueOf(100).multiply(ICX), balanceOfDemoAfter.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(95).multiply(ICX), balanceOfDemoAfter.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(5).multiply(ICX), balanceOfDemoAfter.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(45).multiply(ICX), balanceOfDemoAfter.get("totalStakedBalance"));

        // check balances after of pool ID 6
        balanceOfTestAfter = testClient.stakedLP.balanceOf(testClient.getAddress(), 6);
        assertEquals(BigInteger.valueOf(6), balanceOfTestAfter.get("poolID"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfTestAfter.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(45).multiply(ICX), balanceOfTestAfter.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(5).multiply(ICX), balanceOfTestAfter.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(10).multiply(ICX), balanceOfTestAfter.get("totalStakedBalance"));

        // check balances after of pool ID 6
        balanceOfDemoAfter = testClient.stakedLP.balanceOf(demoClient.getAddress(), 6);
        assertEquals(BigInteger.valueOf(6), balanceOfDemoAfter.get("poolID"));
        assertEquals(BigInteger.valueOf(50).multiply(ICX), balanceOfDemoAfter.get("userTotalBalance"));
        assertEquals(BigInteger.valueOf(45).multiply(ICX), balanceOfDemoAfter.get("userAvailableBalance"));
        assertEquals(BigInteger.valueOf(5).multiply(ICX), balanceOfDemoAfter.get("userStakedBalance"));
        assertEquals(BigInteger.valueOf(10).multiply(ICX), balanceOfDemoAfter.get("totalStakedBalance"));

        //getTotalstaked
        assertEquals(BigInteger.valueOf(45).multiply(ICX), testClient.stakedLP.getTotalStaked(7).totalStaked);
        assertEquals(BigInteger.valueOf(18),testClient.stakedLP.getTotalStaked(7).decimals);
        assertEquals(BigInteger.valueOf(45).multiply(ICX), testClient.stakedLP.totalStaked(7));

        assertEquals(BigInteger.valueOf(10).multiply(ICX), testClient.stakedLP.getTotalStaked(6).totalStaked);
        assertEquals(BigInteger.valueOf(12),testClient.stakedLP.getTotalStaked(6).decimals);
        assertEquals(BigInteger.valueOf(10).multiply(ICX), testClient.stakedLP.totalStaked(6));

        //getBalanceByPool after unstake
        List<Map<String, BigInteger>> balanceByPool = testClient.stakedLP.getBalanceByPool();

        BigInteger[] id = {BigInteger.valueOf(6),BigInteger.valueOf(7)};
        BigInteger[] amount = {BigInteger.TEN.multiply(ICX),BigInteger.valueOf(45).multiply(ICX)};

        for (int i = 0; i < balanceByPool.size(); i++) {
            assertEquals(balanceByPool.get(i).get("poolID"),id[i]);
            assertEquals(balanceByPool.get(i).get("totalStakedBalance"),amount[i]);
        }

        //getPoolBalanceByUser after unstake
        List<Map<String, BigInteger>> demoUser = (List<Map<String, BigInteger>>) testClient.stakedLP.getPoolBalanceByUser(demoClient.getAddress());

        int i = 0;
        assertEquals(demoUser.get(0).get("poolID"),BigInteger.valueOf(6));
        assertEquals(demoUser.get(0).get("userTotalBalance"),BigInteger.valueOf(50).multiply(ICX));
        assertEquals(demoUser.get(0).get("userAvailableBalance"),BigInteger.valueOf(45).multiply(ICX));
        assertEquals(demoUser.get(0).get("totalStakedBalance"),BigInteger.valueOf(10).multiply(ICX));
        assertEquals(demoUser.get(0).get("userStakedBalance"),BigInteger.valueOf(5).multiply(ICX));

        i = 1;
        assertEquals(demoUser.get(i).get("poolID"),BigInteger.valueOf(7));
        assertEquals(demoUser.get(i).get("userTotalBalance"),BigInteger.valueOf(100).multiply(ICX));
        assertEquals(demoUser.get(i).get("userAvailableBalance"),BigInteger.valueOf(95).multiply(ICX));
        assertEquals(demoUser.get(i).get("totalStakedBalance"),BigInteger.valueOf(45).multiply(ICX));
        assertEquals(demoUser.get(i).get("userStakedBalance"),BigInteger.valueOf(5).multiply(ICX));


        List<Map<String, BigInteger>> testUser = (List<Map<String, BigInteger>>) testClient.stakedLP.getPoolBalanceByUser(testClient.getAddress());

        assertEquals(testUser.get(0).get("poolID"),BigInteger.valueOf(6));
        assertEquals(testUser.get(0).get("userTotalBalance"),BigInteger.valueOf(50).multiply(ICX));
        assertEquals(testUser.get(0).get("userAvailableBalance"),BigInteger.valueOf(45).multiply(ICX));
        assertEquals(testUser.get(0).get("totalStakedBalance"),BigInteger.valueOf(10).multiply(ICX));
        assertEquals(testUser.get(0).get("userStakedBalance"),BigInteger.valueOf(5).multiply(ICX));

        assertEquals(testUser.get(1).get("poolID"),BigInteger.valueOf(7));
        assertEquals(testUser.get(1).get("userTotalBalance"),BigInteger.valueOf(100).multiply(ICX));
        assertEquals(testUser.get(1).get("userAvailableBalance"),BigInteger.valueOf(60).multiply(ICX));
        assertEquals(testUser.get(1).get("totalStakedBalance"),BigInteger.valueOf(45).multiply(ICX));
        assertEquals(testUser.get(1).get("userStakedBalance"),BigInteger.valueOf(40).multiply(ICX));

        //getLPStakedSupply of democlient after unstake
        SupplyDetails supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(7,demoClient.getAddress());

        assertEquals(BigInteger.valueOf(18),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(45).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(5).multiply(ICX),supplyDetails.principalUserBalance);

        //getLPStakedSupply of testclient
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(7,testClient.getAddress());

        assertEquals(BigInteger.valueOf(18),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(45).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(40).multiply(ICX),supplyDetails.principalUserBalance);

        //LPStakedSupply of pool Id 6
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(6,demoClient.getAddress());

        assertEquals(BigInteger.valueOf(12),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(10).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(5).multiply(ICX),supplyDetails.principalUserBalance);

        //getLPStakedSupply of testclient
        supplyDetails = (SupplyDetails) ownerClient.stakedLP.getLPStakedSupply(6,testClient.getAddress());

        assertEquals(BigInteger.valueOf(12),supplyDetails.decimals);
        assertEquals(BigInteger.valueOf(10).multiply(ICX),supplyDetails.principalTotalSupply);
        assertEquals(BigInteger.valueOf(5).multiply(ICX),supplyDetails.principalUserBalance);

        status.put("testUnstake", true);
    }
}