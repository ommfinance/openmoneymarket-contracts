package finance.omm.score.core.fee.distribution.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.StakingScoreClient;
import finance.omm.score.core.fee.distribution.integration.config.feeDistributionConfig;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeeDistributionIT implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient testClient;
    private static OMMClient alice;

    private static Map<String, Address> addressMap;
    private final BigInteger time = BigInteger.valueOf(System.currentTimeMillis()/ 1000);
    Set<Map.Entry<score.Address, String>> contributor = Environment.contributors.entrySet();
    private final Map<String,OMMClient> clientMap = new HashMap<>(){};

    boolean transfer = false;

    private static Address lendingPoolCore;
    private static Address daoFund;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new feeDistributionConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();

        // loading contributors
        for (Object wallet:contributor.toArray()) {
            String privKey = wallet.toString().substring(43);
            alice = omm.customClient(privKey);
            clientMap.put(privKey,alice);
        }

        lendingPoolCore = addressMap.get(Contracts.LENDING_POOL_CORE.getKey());
        daoFund = addressMap.get(Contracts.DAO_FUND.getKey());

    }

    private final Map<String, Address> feeAddress = new HashMap<>() {{
        put("fee-1", Faker.address(Address.Type.EOA));
        put("fee-2", Faker.address(Address.Type.CONTRACT));
    }};

    @Test
    @Order(1)
    public void name() {
        assertEquals("OMM Fee Distribution", ownerClient.feeDistribution.name());
    }

    @Test
    @Order(1)
    public void ownerSetFeeDistribution(){

        BigInteger weight1 = BigInteger.TEN.multiply(ICX).divide(BigInteger.valueOf(100));
        BigInteger weight2 = BigInteger.TEN.multiply(ICX).divide(BigInteger.valueOf(100));
        BigInteger weight3 = BigInteger.valueOf(80).multiply(ICX).divide(BigInteger.valueOf(100));
//        Address lendingPoolCore = addressMap.get(Contracts.LENDING_POOL_CORE.getKey());
        Address[] addresses = new Address[]{feeAddress.get("fee-1"),daoFund,lendingPoolCore};
        BigInteger[] weights = new BigInteger[]{weight1,weight2,weight3};
        ownerClient.feeDistribution.setFeeDistribution(addresses,weights);

        assertEquals(weight1,ownerClient.feeDistribution.getFeeDistributionOf(feeAddress.get("fee-1")));
        assertEquals(weight2,ownerClient.feeDistribution.getFeeDistributionOf(daoFund));
        assertEquals(weight3,ownerClient.feeDistribution.getFeeDistributionOf(lendingPoolCore));
        ownerClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));
    }

    @Test
    @Order(1)
    public void setDistributionFail(){

        Address[] addresses = new Address[]{feeAddress.get("fee-1"),feeAddress.get("fee-2")};
        BigInteger[] weights = new BigInteger[]{BigInteger.TEN.multiply(ICX),BigInteger.valueOf(91).multiply(ICX)};

        assertUserRevert(FeeDistributionException.unknown(
                        "Fee Distribution sum of percentages not equal to 100 101000000000000000000"),
                () -> ownerClient.feeDistribution.setFeeDistribution(addresses,weights), null);

        assertUserRevert(FeeDistributionException.unknown(
                        "Fee Distribution Invalid pair length of arrays"),
                () -> ownerClient.feeDistribution.setFeeDistribution(new Address[]{feeAddress.get("fee-1")},weights), null);

    }

    private void sendSicxInFeeDistribution(Address to, BigInteger amount){
        ((StakingScoreClient)ownerClient.staking).stakeICX(amount,ownerClient.getAddress(),"stake".getBytes());

        ownerClient.sICX.transfer(to,amount,"transfer".getBytes());
    }

    private void setOmmDelegations(OMMClient client, int n){
//        Address lendingPoolCore = addressMap.get(Contracts.LENDING_POOL_CORE.getKey());
        sendSicxInFeeDistribution(lendingPoolCore,BigInteger.TEN.multiply(ICX));
        assertEquals(BigInteger.ZERO,
                ownerClient.sICX.balanceOf(ownerClient.getAddress()));

        PrepDelegations[] prepDelegations = prepDelegations(n);
        client.delegation.updateDelegations(prepDelegations,client.getAddress());
    }

    @Order(2)
    @Test
    public void sicxDistribution(){
        userLockOMM(ownerClient);
//        setOmmDelegations();
        // send feeDistribution -> 1000 sICX
        // address1-> 10% -> 100 sICX
        // address2-> 10% -> 100 sICX
        // address3 -> 80% ->800 sICX -> gets distributed
        BigInteger amount = BigInteger.valueOf(1000).multiply(ICX);
        Address feeDistribution = addressMap.get(Contracts.FEE_DISTRIBUTION.getKey());
        sendSicxInFeeDistribution(feeDistribution,amount);

        System.out.println("daaa " + daoFund);
        assertEquals(BigInteger.valueOf(100).multiply(ICX),
                ownerClient.feeDistribution.getFeeDistributed(daoFund));
        assertEquals(BigInteger.valueOf(100).multiply(ICX),
                ownerClient.sICX.balanceOf(daoFund));
//        Address lendingPoolCore = addressMap.get(Contracts.LENDING_POOL_CORE.getKey());
        assertEquals(BigInteger.ZERO, ownerClient.feeDistribution.getFeeDistributed(lendingPoolCore));
    }


    @Order(3)
    @Test
    public void calimFee(){
        // 800 ICX -> 4 contributors equally
        // each contributor -> 25% of 800ICX -> 200ICX

        // loaded from contributor.json
        OMMClient contributor1 = clientMap.get("393f6548d472787138ebc6ac54ee38ace1b8a4dd46c3edfb3122b35db589286f");
        OMMClient contributor2 = clientMap.get("6736efad6c84269c6615921c43d3885cf2c6be20e8358ada8e776ada6a26a2dd");

        // contributor 1 claims reward in their own address
        contributor1.feeDistribution.claimRewards(contributor1.getAddress());

        // contributor 2 calims reward in testClient
        contributor2.feeDistribution.claimRewards(testClient.getAddress());

        assertEquals(BigInteger.valueOf(200).multiply(ICX),ownerClient.feeDistribution.
                getFeeDistributed(contributor1.getAddress()));
        assertEquals(BigInteger.valueOf(200).multiply(ICX),ownerClient.feeDistribution.
                getFeeDistributed(testClient.getAddress()));
        assertEquals(BigInteger.ZERO,ownerClient.feeDistribution.
                getFeeDistributed(contributor2.getAddress()));

        // can not claim when there is collected amount is zero
        assertUserRevert(FeeDistributionException.unknown(
                        "Fee Distribution :: Caller has no reward to claim"),
                () -> contributor1.feeDistribution.claimRewards(contributor1.getAddress()), null);


        assertEquals(BigInteger.valueOf(200).multiply(ICX),ownerClient.feeDistribution.
                getFeeDistributed(contributor1.getAddress()));

    }

    @Test
    @Order(4)
    public void distributeSicx_again(){
        /*
        * now the delgation of lendingPoolCore has changed
        * another user comes and delegates to only same contributors
        * 800ICX -> 4 contributors -> 200 ICX each
        *
        */
        userLockOMM(testClient);
        setOmmDelegations(testClient,4);

        BigInteger amount = BigInteger.valueOf(1000).multiply(ICX);
        Address feeDistribution = addressMap.get(Contracts.FEE_DISTRIBUTION.getKey());
        sendSicxInFeeDistribution(feeDistribution,amount);

        assertEquals(BigInteger.valueOf(200).multiply(ICX),
                ownerClient.feeDistribution.getFeeDistributed(daoFund));
        assertEquals(BigInteger.valueOf(200).multiply(ICX),
                ownerClient.sICX.balanceOf(daoFund));

        OMMClient contributor1 = clientMap.get("393f6548d472787138ebc6ac54ee38ace1b8a4dd46c3edfb3122b35db589286f");

        // contributor 1 claims reward in their own address
        contributor1.feeDistribution.claimRewards(contributor1.getAddress());

        // 200+200
        assertEquals(BigInteger.valueOf(400).multiply(ICX),ownerClient.feeDistribution.
                getFeeDistributed(contributor1.getAddress()));

    }

    @Test
    @Order(5)
    public void distributeSicx_again_only_one_prep(){
        /*
         * now the delgation of lendingPoolCore has changed
         * test user comes and delegates to only one of the contributors
         * 800ICX -> 3 contributors -> same amount
         * 800ICX -> 1 contributor -> different -> around 226 ICX more
         */
        setOmmDelegations(testClient,1);

        BigInteger amount = BigInteger.valueOf(1000).multiply(ICX);
        Address feeDistribution = addressMap.get(Contracts.FEE_DISTRIBUTION.getKey());
        sendSicxInFeeDistribution(feeDistribution,amount);

        OMMClient contributor1 = clientMap.get("393f6548d472787138ebc6ac54ee38ace1b8a4dd46c3edfb3122b35db589286f");
        OMMClient contributor2 = clientMap.get("f639b497bbf871d4f0bdeb6b86a72282edb1bfb30f6ee7e78cfdc95a6ddc5d43");
        OMMClient contributor3 = clientMap.get("6736efad6c84269c6615921c43d3885cf2c6be20e8358ada8e776ada6a26a2dd");
        OMMClient contributor4 = clientMap.get("f35ff7cf4f5759cb0878088d0887574a896f7f0fc2a73898d88be1fe52977dbd");

        // contributor claimRewards
        contributor1.feeDistribution.claimRewards(contributor1.getAddress());
        contributor2.feeDistribution.claimRewards(contributor2.getAddress());
        contributor3.feeDistribution.claimRewards(contributor3.getAddress());
//        contributor4.feeDistribution.claimValidatorsRewards(contributor4.getAddress());

        System.out.println(ownerClient.feeDistribution.getFeeDistributed(contributor1.getAddress()));
        System.out.println(ownerClient.feeDistribution.getFeeDistributed(contributor2.getAddress()));
        System.out.println(ownerClient.feeDistribution.getFeeDistributed(contributor3.getAddress()));

        assertEquals(BigInteger.valueOf(200).multiply(ICX),
                ownerClient.feeDistribution.getFeeDistributed(testClient.getAddress()));

        // has not claimed
        assertEquals(BigInteger.ZERO,ownerClient.feeDistribution.getFeeDistributed(contributor4.getAddress()));

    }

    private PrepDelegations[] prepDelegations(int n ){
        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);
        PrepDelegations[] delegations = new PrepDelegations[n];
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            BigInteger votesInPer = ICX.divide(BigInteger.valueOf(n));
            score.Address prep = prepSet[i];

            total = total.add(votesInPer);

            BigInteger dustVote = ICX.subtract(total);

            if (dustVote.compareTo(BigInteger.ONE) <= 0){
                votesInPer = votesInPer.add(dustVote);
            }
            PrepDelegations delegation = new PrepDelegations(prep,votesInPer);
            delegations[i]=delegation;

        }
        return delegations;
    }

    private void rewardDistribution(){
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000000).multiply(ICX));

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(40000).multiply(ICX),ownerClient.getAddress(),
                "reward".getBytes());
    }
    private  void transferOmm(){
        rewardDistribution();

        ownerClient.ommToken.transfer(testClient.getAddress(),BigInteger.valueOf(20000).multiply(ICX),
                "transfer to testClient".getBytes());
    }

    private void userLockOMM(OMMClient client){
        // testClientLocks OMM -> default delagation -> contributors

        if (!transfer){
            transferOmm();

        }
        transfer = true;
        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger unlockTimeMicro = getTimeAfter(2);

        byte[] data =createByteArray("createLock",unlockTimeMicro);
        client.ommToken.transfer(to,value,data);
    }

    private BigInteger getTimeAfter(int n){
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf((long) n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

    }

    private byte[] createByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime", String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }





}
