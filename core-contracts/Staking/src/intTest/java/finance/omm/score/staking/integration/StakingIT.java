package finance.omm.score.staking.integration;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.StakingScoreClient;
import finance.omm.libs.test.integration.scores.SystemInterfaceScoreClient;

import finance.omm.score.staking.integration.config.StakingConfig;
import foundation.icon.jsonrpc.Address;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import score.RevertedException;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static finance.omm.libs.test.integration.Environment.godClient;
import static finance.omm.score.staking.utils.Constant.ONE_EXA;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StakingIT implements ScoreIntegrationTest {

    public static OMMClient ownerClient;
    public static OMMClient testClient;
    public static OMMClient readerClient;
    public static OMMClient stakingTestClient;
    public static OMMClient stakingTestClient2;

    public static Map<String, Address> addressMap;
    public score.Address outsidePrep = Address.fromString("hxe7edbaf218b7709f67b7660aad99ae7b96b9008b");
    boolean transfer = false;
    public final BigInteger time = BigInteger.valueOf(System.currentTimeMillis() / 1000);

    SystemInterfaceScoreClient systemScore = new SystemInterfaceScoreClient(godClient);

    public final BigInteger HUNDRED = BigInteger.valueOf(100);

    public static Address lendingPoolCore;


    @BeforeAll
    public static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new StakingConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();

        testClient = omm.testClient();
        readerClient = omm.newClient();
        stakingTestClient = omm.newClient();
        stakingTestClient2 = omm.newClient();

        lendingPoolCore= addressMap.get(Contracts.LENDING_POOL_CORE.getKey());

        ownerClient.staking.setOmmLendingPoolCore(lendingPoolCore);
        ownerClient.sICX.setMinter(addressMap.get(Contracts.STAKING.getKey()));

    }

    @Test
    @Order(1)
    public void name(){
        assertEquals("Staked ICX Manager",readerClient.staking.name());
    }

    @Test
    @Order(1)
    public void check_getters(){
        BigInteger feePercentage = BigInteger.valueOf(10).multiply(ONE_EXA);
        assertEquals(feePercentage,readerClient.staking.getFeePercentage());

        BigInteger prepProductivity = BigInteger.valueOf(90).multiply(ONE_EXA);
        assertEquals(prepProductivity,readerClient.staking.getPrepProductivity());

        ownerClient.staking.setPrepProductivity(BigInteger.ZERO);
    }

    @Test
    @Order(1)
    public void checkStakingStatus() {
        assertTrue(readerClient.staking.getStakingOn());
    }

    @Test
    @Order(2)
    public void checkTopPreps() {

        List<score.Address> topPrep = readerClient.staking.getTopPreps();

        // finding top preps having productivity greater than 90 and bondedAmount more than 0
        Map<String, Object> prepDict = systemScore.getPReps(BigInteger.ONE, BigInteger.valueOf(100));
        List<Map<String, Object>> prepDetails = (List<Map<String, Object>>) prepDict.get("preps");
        List<score.Address> expectedTopPreps = new ArrayList<>();
        for (Map<String, Object> preps : prepDetails) {
            score.Address prepAddress = score.Address.fromString((String) preps.get("address"));
            Map<String, Object> singlePrepInfo = systemScore.getPRep(prepAddress);
            BigInteger totalBlocks = new BigInteger(singlePrepInfo.get("totalBlocks").toString().substring(2), 16);
            BigInteger validatedBlocks = new BigInteger(singlePrepInfo.get("validatedBlocks").toString().substring(2), 16);
            validatedBlocks = validatedBlocks.multiply(ICX);
            totalBlocks = totalBlocks.multiply(ICX);
            if (totalBlocks.compareTo(BigInteger.ZERO) == 0) {
                continue;
            }
            BigInteger prepProductivity = validatedBlocks.multiply(ICX).multiply(HUNDRED).divide(totalBlocks);
            BigInteger bondedAmount = new BigInteger(singlePrepInfo.get("bonded").toString().substring(2), 16);
            if ((!bondedAmount.equals(BigInteger.ZERO)) && prepProductivity.compareTo(BigInteger.valueOf(90L)) > 0) {
                expectedTopPreps.add(prepAddress);
            }
        }

        assertEquals(expectedTopPreps.size(), topPrep.size());
        assertTrue(topPrep.contains(expectedTopPreps.get(0)));
    }


    @Test
    @Order(3)
    public void testStakeByNewUser() {
        // done when omm delegation is zero -> should be delegated to top prep in prepList
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        BigInteger amountToStake = BigInteger.valueOf(100).multiply(ICX);
        ((StakingScoreClient) ownerClient.staking).stakeICX(amountToStake, null, null); // ownerClient staked 100

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        for (score.Address prep : prepList) {
            if (contains(prep, topPreps)) {
                userExpectedDelegations.put(prep.toString(), new BigInteger("100").multiply(ICX));
                expectedNetworkDelegations.put(prep.toString(), new BigInteger("100").multiply(ICX));
                expectedPrepDelegations.put(prep.toString(), new BigInteger("100").multiply(ICX));
            }
        }

        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);


    }

    @Test
    @Order(4)
    public void testSecondStake() {
        // 10 icx to 4 contributors
        userLockOMM(ownerClient);
        // done when omm delegation is 4 -> should be delegated to these 4 contributor
        // 4 contirbutor should have productivity > 90%
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        BigInteger amountToStake = BigInteger.valueOf(200).multiply(ICX);
        ((StakingScoreClient) ownerClient.staking).stakeICX(amountToStake, null, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        // totalStake -> 300 ICX
        // contributor percentage = 25%
        // each contributor 25 % of 300 ICX = 75 ICX
        Map<String,BigInteger> expectedOmmDelgations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                // only one prep of ommContributor exist in topPrep
                expectedOmmDelgations.put(ommContributor.toString(),BigInteger.valueOf(300).multiply(ICX));
            }
        }

        for (score.Address prep : prepList) {
            if (contains(prep, ommContributors) && contains(prep, topPreps)) {
                userExpectedDelegations.put(prep.toString(), new BigInteger("300").multiply(ICX));
                expectedPrepDelegations.put(prep.toString(), new BigInteger("300").multiply(ICX));
                expectedNetworkDelegations.put(prep.toString(), new BigInteger("300").multiply(ICX));
            }

        }

        assertEquals(expectedOmmDelgations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }


    @Test
    @Order(5)
    public void testThirdStake() {
        // stake initiated by ownerClient for stakingClient
        score.Address user = stakingTestClient.getAddress();
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(user);

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        BigInteger amountToStake = BigInteger.valueOf(200).multiply(ICX);
        ((StakingScoreClient) ownerClient.staking).stakeICX(amountToStake, user, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(user);

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(user);
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 500 , 4 contributors each percentage = 25%
         * 25% of 500 = 125 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(500).multiply(ICX));
            }
        }

        for (score.Address prep : prepList) {
            if (contains(prep, ommContributors) && contains(prep, topPreps)) {
                userExpectedDelegations.put(prep.toString(), new BigInteger("200").multiply(ICX));
                expectedPrepDelegations.put(prep.toString(), new BigInteger("500").multiply(ICX));
                expectedNetworkDelegations.put(prep.toString(), new BigInteger("500").multiply(ICX));
            }

        }
        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }

    @Test
    @Order(6)
    public void delegateOutsideTopPrep() {
        // ownerClient delegates to prep not in topPrep
        // prep is an ommContributor
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        PrepDelegations p = new PrepDelegations();
        p._address = outsidePrep;
        p._votes_in_per = new BigInteger("100").multiply(ICX);
        PrepDelegations[] userDelegation = new PrepDelegations[]{p};
        // delegates to one address
        ownerClient.staking.delegate(userDelegation);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake, afterTotalStake);
        assertEquals(previousTotalSupply, afterTotalSupply);
        assertEquals(userBalance, afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 500 , 4 contributors each percentage = 25%
         * 25% of 500 = 125 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(500).multiply(ICX));
            }
        }


        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("300").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("200").multiply(ICX));
            expectedNetworkDelegations.put(outsidePrep.toString(), new BigInteger("200").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("300").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("500").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("500").multiply(ICX));
        }

        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }

    @Test
    @Order(7)
    public void stakeAfterDelegate() {
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        // ownerClient has 100% delegation to outsidePrep from previous case
        BigInteger amountToStake = BigInteger.valueOf(100).multiply(ICX);
        ((StakingScoreClient) ownerClient.staking).stakeICX(amountToStake, null, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 600 , 4 contributors each percentage = 25%
         * 25% of 600 = 150 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(600).multiply(ICX));
            }
        }

        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("400").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("400").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("400").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("600").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("600").multiply(ICX));
        }


        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);
    }

    @Test
    @Order(8)
    public void delegateFirstThenStake() {
        // stakingTestClient delegates to prep not in topPrep
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        PrepDelegations p = new PrepDelegations();
        // omm contributor

        p._address = outsidePrep;
        p._votes_in_per = new BigInteger("100").multiply(ICX);
        PrepDelegations[] userDelegation = new PrepDelegations[]{p};
        // delegates to one address
        stakingTestClient.staking.delegate(userDelegation);


        // stakes
        BigInteger amountToStake = BigInteger.valueOf(50).multiply(ICX);
        ((StakingScoreClient) stakingTestClient.staking).stakeICX(amountToStake, null, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(stakingTestClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();

        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("250").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("450").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("250").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("650").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("650").multiply(ICX));
        }


        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);
    }

    @Test
    @Order(9)
    public void unstakeZero(){
        Address stakingAddress = addressMap.get(Contracts.STAKING.getKey());
        JSONObject data = new JSONObject();
        data.put("method", "unstake");
        BigInteger amountToUnstake = BigInteger.ZERO;

        RevertedException zeroUnstake = assertThrows(RevertedException.class, () ->
                ownerClient.sICX.transfer(stakingAddress, amountToUnstake, data.toString().getBytes()));
    }

    @Test
    @Order(9)
    public void unstakePartial() {
        Address stakingAddress = addressMap.get(Contracts.STAKING.getKey());
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());


        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        // unstake
        JSONObject data = new JSONObject();
        data.put("method", "unstake");
        BigInteger amountToUnstake = BigInteger.valueOf(100).multiply(ICX);
        ownerClient.sICX.transfer(stakingAddress, amountToUnstake, data.toString().getBytes());

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake.subtract(amountToUnstake), afterTotalStake);
        assertEquals(previousTotalSupply.subtract(amountToUnstake), afterTotalSupply);
        assertEquals(userBalance.subtract(amountToUnstake), afterUserBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 550 , 4 contributors each percentage = 25%
         * 25% of 550 = 137.5 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(550).multiply(ICX));
            }
        }

        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("300").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("550").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("300").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("550").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("550").multiply(ICX));
        }


        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

        // unstakeInfo
        List<Map<String, Object>> userUnstakeInfo = readerClient.staking.getUserUnstakeInfo(ownerClient.getAddress());
        assertEquals(ownerClient.getAddress().toString(), userUnstakeInfo.get(0).get("sender"));
        assertEquals(ownerClient.getAddress().toString(), userUnstakeInfo.get(0).get("from"));
        String hexValue = (String) userUnstakeInfo.get(0).get("amount");
        hexValue = hexValue.replace("0x", "");

        assertEquals(amountToUnstake, new BigInteger(hexValue, 16));

        // unstakeInfo for staking address
        List<List<Object>> unstakeStakingInfo = readerClient.staking.getUnstakeInfo();
        List<Object> stakingInfo = unstakeStakingInfo.get(0);

        assertEquals(ownerClient.getAddress().toString(), stakingInfo.get(2).toString());

        // on system network
        Map<String, Object> stakeDetails = systemScore.getStake(stakingAddress);
        List<Map<String, Object>> stakingOnSystemScore = (List<Map<String, Object>>) stakeDetails.get("unstakes");
        String unstakeExpected = (String) stakingOnSystemScore.get(0).get("unstakeBlockHeight");
        Assertions.assertEquals(unstakeExpected, userUnstakeInfo.get(0).get("blockHeight"));

    }

    @Test
    @Order(10)
    public void unstakeFull() {
        // staking client unstakes all stakedICX
        Address stakingAddress = addressMap.get(Contracts.STAKING.getKey());
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        // unstake
        JSONObject data = new JSONObject();
        data.put("method", "unstake");
        BigInteger amountToUnstake = BigInteger.valueOf(250).multiply(ICX);
        stakingTestClient.sICX.transfer(stakingAddress, amountToUnstake, data.toString().getBytes());

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        assertEquals(previousTotalStake.subtract(amountToUnstake), afterTotalStake);
        assertEquals(previousTotalSupply.subtract(amountToUnstake), afterTotalSupply);
        assertEquals(userBalance.subtract(amountToUnstake), afterUserBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(stakingTestClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 300 , 4 contributors each percentage = 25%
         * 25% of 300 = 75 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(300).multiply(ICX));
            }
        }


        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("0").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("300").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("0").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("300").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("300").multiply(ICX));
        }


        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

//        // unstakeInfo
        List<Map<String, Object>> userUnstakeInfo = readerClient.staking.getUserUnstakeInfo(stakingTestClient.getAddress());
        assertEquals(stakingTestClient.getAddress().toString(), userUnstakeInfo.get(0).get("sender"));
        assertEquals(stakingTestClient.getAddress().toString(), userUnstakeInfo.get(0).get("from"));
        String hexValue = (String) userUnstakeInfo.get(0).get("amount");
        hexValue = hexValue.replace("0x", "");

        assertEquals(amountToUnstake, new BigInteger(hexValue, 16));

        // unstakeInfo for staking address
        List<List<Object>> unstakeStakingInfo = readerClient.staking.getUnstakeInfo();
        List<Object> stakingInfo = unstakeStakingInfo.get(1);

        assertEquals(stakingTestClient.getAddress().toString(), stakingInfo.get(2).toString());

        // on system network
        Map<String, Object> stakeDetails = systemScore.getStake(stakingAddress);
        List<Map<String, Object>> stakingOnSystemScore = (List<Map<String, Object>>) stakeDetails.get("unstakes");
        String unstakeExpected = (String) stakingOnSystemScore.get(0).get("unstakeBlockHeight");
        Assertions.assertEquals(unstakeExpected, readerClient.staking.getUserUnstakeInfo(ownerClient.getAddress()).get(0).get(
                "blockHeight"));
    }

    @Test
    @Order(11)
    public void stakeAfterUnstake() {
        // ownerClient stakes again
        Address stakingAddress = addressMap.get(Contracts.STAKING.getKey());

        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger userBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        List<score.Address> prepList = readerClient.staking.getPrepList();
        List<score.Address> topPreps = readerClient.staking.getTopPreps();

        BigInteger amountToStake = BigInteger.valueOf(10).multiply(ICX);
        ((StakingScoreClient) ownerClient.staking).stakeICX(amountToStake, null, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterUserBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);
        assertEquals(userBalance.add(amountToStake), afterUserBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> ommContributors = readerClient.delegation.getContributors();
        /* total Staked -> 310 , 4 contributors each percentage = 25%
         * 25% of 310 = 75 ICX */
        Map<String,BigInteger> expectedOmmDelegations = new java.util.HashMap<>();
        for (score.Address ommContributor: ommContributors) {
            if (contains(ommContributor,topPreps)){
                expectedOmmDelegations.put(ommContributor.toString(),BigInteger.valueOf(310).multiply(ICX));
            }
        }

        if (contains(outsidePrep, ommContributors) && contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("310").multiply(ICX));
            expectedPrepDelegations.put(outsidePrep.toString(), new BigInteger("310").multiply(ICX));
        }
        if (contains(outsidePrep, ommContributors) && !contains(outsidePrep, topPreps)) {
            userExpectedDelegations.put(outsidePrep.toString(), new BigInteger("310").multiply(ICX));
            expectedPrepDelegations.put(topPreps.get(0).toString(), new BigInteger("310").multiply(ICX));
            expectedNetworkDelegations.put(topPreps.get(0).toString(), new BigInteger("310").multiply(ICX));
        }


        assertEquals(expectedOmmDelegations, readerClient.staking.getbOMMDelegations());
        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

        // unstakeInfo
        List<Map<String, Object>> userUnstakeInfo = readerClient.staking.getUserUnstakeInfo(ownerClient.getAddress());
        assertEquals(ownerClient.getAddress().toString(), userUnstakeInfo.get(0).get("sender"));
        assertEquals(ownerClient.getAddress().toString(), userUnstakeInfo.get(0).get("from"));
        String hexValue = (String) userUnstakeInfo.get(0).get("amount");
        hexValue = hexValue.replace("0x", "");

        assertEquals(BigInteger.valueOf(90).multiply(ICX), new BigInteger(hexValue, 16));

        // unstakeInfo for staking address
        List<List<Object>> unstakeStakingInfo = readerClient.staking.getUnstakeInfo();
        List<Object> stakingInfo = unstakeStakingInfo.get(0);

        assertEquals(ownerClient.getAddress().toString(), stakingInfo.get(2).toString());

        // on system network
        Map<String, Object> stakeDetails = systemScore.getStake(stakingAddress);
        List<Map<String, Object>> stakingOnSystemScore = (List<Map<String, Object>>) stakeDetails.get("unstakes");
        String unstakeExpected = (String) stakingOnSystemScore.get(0).get("unstakeBlockHeight");
        Assertions.assertEquals(unstakeExpected, userUnstakeInfo.get(0).get("blockHeight"));

    }

    @Test
    @Order(12)
    public void claimUnstakedICX() {
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();

        assertEquals(BigInteger.TEN.multiply(ICX), readerClient.staking.totalClaimableIcx());
        assertEquals(BigInteger.TEN.multiply(ICX), readerClient.staking.claimableICX(ownerClient.getAddress()));
        assertEquals(BigInteger.ZERO, readerClient.staking.claimableICX(stakingTestClient.getAddress()));

        BigInteger amountToStake = BigInteger.valueOf(400).multiply(ICX);
        ((StakingScoreClient) stakingTestClient.staking).stakeICX(amountToStake, null, null);

        //10+90+250
        assertEquals(BigInteger.valueOf(350).multiply(ICX), readerClient.staking.totalClaimableIcx());
        assertEquals(HUNDRED.multiply(ICX), readerClient.staking.claimableICX(ownerClient.getAddress()));
        assertEquals(BigInteger.valueOf(250).multiply(ICX), readerClient.staking.claimableICX(stakingTestClient.getAddress()));

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();

        assertEquals(previousTotalStake.add(amountToStake), afterTotalStake);
        assertEquals(previousTotalSupply.add(amountToStake), afterTotalSupply);

        // calim ICX
        BigInteger beforeBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());
        ownerClient.staking.claimUnstakedICX(ownerClient.getAddress());
        BigInteger afterBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(beforeBalance, afterBalance);

    }

    @Test
    @Order(13)
    public void transferPreferenceToNoPreference() {
        // transfer sicx from ownerClient to a testClient
        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger senderBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());
        BigInteger receiverBalance = readerClient.sICX.balanceOf(testClient.getAddress());

        BigInteger amountToTransfer = BigInteger.valueOf(50).multiply(ICX);

        ownerClient.sICX.transfer(testClient.getAddress(), amountToTransfer, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterSenderBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());
        BigInteger afterReceiverBalance = readerClient.sICX.balanceOf(testClient.getAddress());

        assertEquals(previousTotalStake, afterTotalStake);
        assertEquals(previousTotalSupply, afterTotalSupply);
        assertEquals(receiverBalance.add(amountToTransfer), afterReceiverBalance);
        assertEquals(senderBalance.subtract(amountToTransfer), afterSenderBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(testClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> prepList = readerClient.staking.getPrepList();

        userExpectedDelegations.put(prepList.get(0).toString(), new BigInteger("50").multiply(ICX));
        expectedPrepDelegations.put(prepList.get(0).toString(), new BigInteger("710").multiply(ICX));
        expectedNetworkDelegations.put(prepList.get(0).toString(), new BigInteger("710").multiply(ICX));

        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }

    @Test
    @Order(14)
    public void transferPreferenceToPreference() {
        // ownerClient transfer to StakingClient

        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();
        BigInteger senderBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());
        BigInteger receiverBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        BigInteger amountToTransfer = new BigInteger("50").multiply(ICX);
        ownerClient.sICX.transfer(stakingTestClient.getAddress(), amountToTransfer, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterSenderBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());
        BigInteger afterReceiverBalance = readerClient.sICX.balanceOf(stakingTestClient.getAddress());

        assertEquals(previousTotalStake, afterTotalStake);
        assertEquals(previousTotalSupply, afterTotalSupply);
        assertEquals(receiverBalance.add(amountToTransfer), afterReceiverBalance);
        assertEquals(senderBalance.subtract(amountToTransfer), afterSenderBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(stakingTestClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> prepList = readerClient.staking.getPrepList();

        userExpectedDelegations.put(outsidePrep.toString(), receiverBalance.add(amountToTransfer));
        expectedNetworkDelegations.put(prepList.get(0).toString(), new BigInteger("710").multiply(ICX));
        expectedPrepDelegations.put(prepList.get(0).toString(), new BigInteger("710").multiply(ICX));

        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }


    @Test
    @Order(15)
    public void transferNullToNull() {
        Address receiverAddress = addressMap.get(Contracts.DAO_FUND.getKey());

        BigInteger previousTotalStake = readerClient.staking.getTotalStake();
        BigInteger previousTotalSupply = readerClient.sICX.totalSupply();

        BigInteger receiverBalance = readerClient.sICX.balanceOf(receiverAddress);

        BigInteger amountToTransfer = BigInteger.valueOf(50).multiply(ICX);

        ((StakingScoreClient) stakingTestClient2.staking).stakeICX(HUNDRED.multiply(ICX), null, null);

        BigInteger senderBalance = readerClient.sICX.balanceOf(stakingTestClient2.getAddress());

        stakingTestClient2.sICX.transfer(receiverAddress, amountToTransfer, null);

        BigInteger afterTotalStake = readerClient.staking.getTotalStake();
        BigInteger afterTotalSupply = readerClient.sICX.totalSupply();
        BigInteger afterSenderBalance = readerClient.sICX.balanceOf(stakingTestClient2.getAddress());
        BigInteger afterReceiverBalance = readerClient.sICX.balanceOf(receiverAddress);

        assertEquals(previousTotalStake.add(HUNDRED.multiply(ICX)), afterTotalStake);
        assertEquals(previousTotalSupply.add(HUNDRED.multiply(ICX)), afterTotalSupply);
        assertEquals(senderBalance.subtract(amountToTransfer), afterSenderBalance);
        assertEquals(receiverBalance.add(amountToTransfer), afterReceiverBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(stakingTestClient2.getAddress());
        Map<String, BigInteger> receiverDelegations = readerClient.staking.getAddressDelegations(receiverAddress);
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> receiverExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> prepList = readerClient.staking.getPrepList();

        userExpectedDelegations.put(prepList.get(0).toString(), BigInteger.valueOf(50).multiply(ICX));
        receiverExpectedDelegations.put(prepList.get(0).toString(), BigInteger.valueOf(50).multiply(ICX));
        expectedNetworkDelegations.put(prepList.get(0).toString(), new BigInteger("810").multiply(ICX));
        expectedPrepDelegations.put(prepList.get(0).toString(), BigInteger.valueOf(810).multiply(ICX));

        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(receiverDelegations, receiverExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }


    @Test
    @Order(16)
    public void transferNullToPreference() {

        BigInteger senderBalance = readerClient.sICX.balanceOf(stakingTestClient2.getAddress());
        BigInteger receiverBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        BigInteger amountToTransfer = BigInteger.valueOf(50).multiply(ICX);


        stakingTestClient2.sICX.transfer(ownerClient.getAddress(), amountToTransfer, null);

        BigInteger afterSenderBalance = readerClient.sICX.balanceOf(stakingTestClient2.getAddress());
        BigInteger afterReceiverBalance = readerClient.sICX.balanceOf(ownerClient.getAddress());

        assertEquals(senderBalance.subtract(amountToTransfer), afterSenderBalance);
        assertEquals(receiverBalance.add(amountToTransfer), afterReceiverBalance);

        // delegations
        Map<String, BigInteger> userDelegations = readerClient.staking.getAddressDelegations(stakingTestClient2.getAddress());
        Map<String, BigInteger> receiverDelegations = readerClient.staking.getAddressDelegations(ownerClient.getAddress());
        Map<String, BigInteger> prepDelegations = readerClient.staking.getPrepDelegations();

        Map<String, BigInteger> userExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> receiverExpectedDelegations = new HashMap<>();
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();
        Map<String, BigInteger> expectedNetworkDelegations = new java.util.HashMap<>();

        List<score.Address> prepList = readerClient.staking.getPrepList();

        receiverExpectedDelegations.put(outsidePrep.toString(), receiverBalance.add(BigInteger.valueOf(50).multiply(ICX)));
        expectedNetworkDelegations.put(prepList.get(0).toString(), new BigInteger("810").multiply(ICX));
        expectedPrepDelegations.put(prepList.get(0).toString(), BigInteger.valueOf(810).multiply(ICX));

        assertEquals(userDelegations, userExpectedDelegations);
        assertEquals(receiverDelegations, receiverExpectedDelegations);
        assertEquals(prepDelegations, expectedPrepDelegations);
        checkNetworkDelegations(expectedNetworkDelegations);

    }


    public boolean contains(score.Address target, List<score.Address> addresses) {
        for (score.Address address : addresses) {
            if (address.equals(target)) {
                return true;
            }
        }
        return false;
    }

    void checkNetworkDelegations(Map<String, BigInteger> expected) {
        Address stakingAddress = addressMap.get(Contracts.STAKING.getKey());
        Map<String, Object> delegations = systemScore.getDelegation(stakingAddress);
        Map<String, BigInteger> networkDelegations = new java.util.HashMap<>();
        List<Map<String, Object>> delegationList = (List<Map<String, Object>>) delegations.get("delegations");

        for (Map<String, Object> del : delegationList) {
            String hexValue = del.get("value").toString();
            hexValue = hexValue.replace("0x", "");
            networkDelegations.put(del.get("address").toString(), new BigInteger(hexValue, 16));
        }
        assertEquals(expected, networkDelegations);

    }


    protected void rewardDistribution() {
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000000).multiply(ICX));

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(40000).multiply(ICX), ownerClient.getAddress(),
                "reward".getBytes());
    }

    protected void transferOmm() {
        rewardDistribution();

        ownerClient.ommToken.transfer(testClient.getAddress(), BigInteger.valueOf(20000).multiply(ICX),
                "transfer to testClient".getBytes());
    }

    protected void userLockOMM(OMMClient client) {
        // testClientLocks OMM -> default delagation -> contributors

        if (!transfer) {
            transferOmm();

        }
        transfer = true;
        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger unlockTimeMicro = getTimeAfter(2);

        byte[] data = createByteArray("createLock", unlockTimeMicro);
        client.ommToken.transfer(to, value, data);
    }

    protected BigInteger getTimeAfter(int n) {
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf((long) n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

    }

    protected byte[] createByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime", String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

}