package finance.omm.score.core.test;

import static finance.omm.score.core.delegation.DelegationImpl.TAG;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static foundation.icon.jsonrpc.Address.Type;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.delegation.exception.DelegationException;
import finance.omm.score.core.test.config.DelegationConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import foundation.icon.jsonrpc.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DelegationIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient testClient;

    private static Map<String, Address> addressMap;

    private final List<Address> contributorAddr = new ArrayList<>(){{
        add(Faker.address(Type.EOA));
        add(Faker.address(Type.EOA));
        add(Faker.address(Type.EOA));

    }};

    private final BigInteger time = BigInteger.valueOf(System.currentTimeMillis()/ 1000);

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");
        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new DelegationConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();

        ownerClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));



    }

    @Test
    @Order(1)
    void testName() {
        assertEquals("OMM Delegation", ownerClient.delegation.name());
    }

    @Test
    @Order(2)
    void voteThreshold(){
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.setVoteThreshold(BigInteger.TEN),null);

        assertEquals(BigInteger.ZERO, ownerClient.delegation.getVoteThreshold());

        ownerClient.delegation.setVoteThreshold(BigInteger.TEN);
        assertEquals(BigInteger.TEN, ownerClient.delegation.getVoteThreshold());
    }

    @Test
    @Order(3)
    /*
    check if user has default delegation after locking omm
     */
    void checkUserDefaultDelegation(){
        ((LendingPoolScoreClient)ownerClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX),BigInteger.valueOf(1000).multiply(ICX));
        userLockOMM();
        boolean expected = testClient.delegation.userDefaultDelegation(testClient.getAddress());
        assertTrue(expected);

        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(4,prepDelegatedList.size());

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());

        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);

        for (int i = 0; i < prepDelegatedList.size(); i++) {
            assertEquals(prepSet[i],userDelegations[i]._address);
        }
    }

    @Test
    @Order(4)
    /*
    after user locks OMM
    user updates delegation to their desired prep
     */
    void updateDelegation(){

        // default contributors
        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(4,prepDelegatedList.size());

        PrepDelegations[] delegations = prepDelegations(10);
        List<PrepDelegations> expectedList = new ArrayList<>(Arrays.asList(delegations));

        testClient.delegation.updateDelegations(delegations,testClient.getAddress());

        prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(10,prepDelegatedList.size());

        PrepDelegations[] computedDelegation = testClient.delegation.computeDelegationPercentages();
        assertEquals(expectedList.size(),computedDelegation.length);

        for (int i = 0; i < expectedList.size(); i++) {
            assertEquals(expectedList.get(i)._address,computedDelegation[i]._address);
            float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
            assertEquals(expectedList.get(i)._votes_in_per.multiply(BigInteger.valueOf(100L)).longValue()/ICX.longValue(),
                    computedDelegation[i]._votes_in_per.longValue()/ICX.longValue(),delta);
        }

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());
        assertEquals(delegations.length,userDelegations.length);
        for (int i = 0; i < delegations.length; i++) {
            assertEquals(delegations[i]._address,userDelegations[i]._address);
            assertEquals(delegations[i]._votes_in_per,userDelegations[i]._votes_in_per);
        }
    }

    @Test
    @Order(5)
    void updateDelagationHundredPrep(){

        // testClient has delegated to 10 preps
        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(10,prepDelegatedList.size());

        // now to 100 preps
        PrepDelegations[] delegations = prepDelegations(100);
        List<PrepDelegations> expectedList = new ArrayList<>(Arrays.asList(delegations));

        testClient.delegation.updateDelegations(delegations,testClient.getAddress());

        prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(100,prepDelegatedList.size());

        PrepDelegations[] computedDelegation = testClient.delegation.computeDelegationPercentages();
        assertEquals(expectedList.size(),computedDelegation.length);

        for (int i = 0; i < expectedList.size(); i++) {
            assertEquals(expectedList.get(i)._address,computedDelegation[i]._address);
            float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
            assertEquals(expectedList.get(i)._votes_in_per.multiply(BigInteger.valueOf(100L)).longValue()/ICX.longValue(),
                    computedDelegation[i]._votes_in_per.longValue()/ICX.longValue(),delta);
        }

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());
        assertEquals(delegations.length,userDelegations.length);
        for (int i = 0; i < delegations.length; i++) {
            assertEquals(delegations[i]._address,userDelegations[i]._address);
            assertEquals(delegations[i]._votes_in_per,userDelegations[i]._votes_in_per);
        }
        assertEquals(100,userDelegations.length);
    }

    @Test
    @Order(5)
    void updateDelegationShouldFail(){

        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);

        PrepDelegations[] delegation = new PrepDelegations[]{
                new PrepDelegations(prepSet[0],ICX.divide(BigInteger.TWO)),
                new PrepDelegations(prepSet[1],ICX.divide(BigInteger.valueOf(3)))
        };

        BigInteger totalVotes = ICX.divide(BigInteger.TWO).add(ICX.divide(BigInteger.valueOf(3)));

        assertUserRevert(DelegationException.unknown(TAG + ": updating delegation unsuccessful,sum of percentages not equal to 100 " +
                        "sum total of percentages " + totalVotes +
                        " delegation preferences " + delegation.length),
                () ->   testClient.delegation.updateDelegations(delegation,testClient.getAddress()),null);


        PrepDelegations[] delegations = prepDelegations(101);

        assertUserRevert(DelegationException.unknown(TAG + "updating delegation unsuccessful, more than 5 preps provided by user" +
                        "delegations provided" + delegations.length),
                () ->  testClient.delegation.updateDelegations(delegations,testClient.getAddress()),null);

    }

    @Test
    @Order(6)
    /*
    delegate votes to different preps
    clearPrevious set delegation to default
     */
    void clearPrevious(){

        assertUserRevert(DelegationException.unknown(TAG+" :You are not authorized to clear others delegation preference"),
                () -> ownerClient.delegation.clearPrevious(testClient.getAddress()),null);

        testClient.delegation.clearPrevious(testClient.getAddress());


        score.Address[] contributors = Environment.contributors.keySet().toArray(score.Address[]::new);

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());

        assertEquals(contributors.length,userDelegations.length);
        for (int i = 0; i < contributors.length; i++) {
            assertEquals(contributors[i],userDelegations[i]._address);
        }

    }

    @Test
    @Order(7)
    /*
    this method is called from Boosted OMM
     */
    void onKick(){

        assertUserRevert(DelegationException.unauthorized(TAG+" :You are not authorized to clear others delegation preference"),
                () -> ownerClient.delegation.onKick(testClient.getAddress(),BigInteger.ONE,new byte[]{}),null);

    }
    @Test
    @Order(7)
    /*
    onBalanceUpdate is called on bOMM when omm is locked
     */
    void onBalanceUpdate(){
        assertUserRevert(DelegationException.unauthorized("Only bOMM contract is allowed to call onBalanceUpdate method"),
                () -> ownerClient.delegation.onBalanceUpdate(ownerClient.getAddress()),null);


    }

    @Test
    @Order(8)
    /*
    user votes other than contributors
    on calling initializeVoteToContributor the vote should be to contributors
     */
    void initializeVoteToContributor(){

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.initializeVoteToContributors(),null);

        ownerClient.delegation.initializeVoteToContributors();

        assertUserRevert(DelegationException.unknown(TAG+" : This method cannot be called again."),
                () -> ownerClient.delegation.initializeVoteToContributors(),null);
    }

    @Test
    @Order(9)
    void addContributor(){
        // contributors are the default preps
        score.Address[] contributorSet = Environment.contributors.keySet().toArray(score.Address[]::new);
        List<score.Address> contributors = new ArrayList<>(Arrays.asList(contributorSet));

        List<score.Address> preplist = ownerClient.delegation.getContributors();
        assertEquals(4,preplist.size());

        ownerClient.delegation.addContributor(contributorAddr.get(0));

        contributors.add(contributorAddr.get(0));

        preplist = ownerClient.delegation.getContributors();

        assertEquals(5, preplist.size());

        for (int i = 0; i < preplist.size(); i++) {
            assertEquals(contributors.get(i),preplist.get(i));
        }

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addContributor(contributorAddr.get(0)),null);
    }

    @Test
    @Order(10)
    void addAllContributor(){

        score.Address[] contributorSet = Environment.contributors.keySet().toArray(score.Address[]::new);
        List<score.Address> contributors = new ArrayList<>(Arrays.asList(contributorSet));

        ownerClient.delegation.addAllContributors(contributorAddr.toArray(Address[]::new));

        contributors.add(contributorAddr.get(0));
        contributors.add(contributorAddr.get(1));
        contributors.add(contributorAddr.get(2));

        List<score.Address> preplist = ownerClient.delegation.getContributors();

        assertEquals(7, preplist.size());

        for (int i = 0; i < preplist.size(); i++) {
            assertEquals(contributors.get(i),preplist.get(i));
        }

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addAllContributors(contributorAddr.toArray(Address[]::new)),null);

    }

    @Test
    @Order(11)
    /*
    addAllContributor() and remove of the address
    should throw exception when trying to remove same address
     */
    void removeContributor(){

        List<score.Address> preplist = ownerClient.delegation.getContributors();
        assertEquals(7, preplist.size());

        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.removeContributor(contributorAddr.get(0)),null);

        ownerClient.delegation.removeContributor(contributorAddr.get(0));

        preplist = ownerClient.delegation.getContributors();
        assertEquals(6, preplist.size());

        assertUserRevert(DelegationException.unknown("OMM: " + contributorAddr.get(0) +" is not in contributor list"),
                () ->  ownerClient.delegation.removeContributor(contributorAddr.get(0)),null);
    }


    private PrepDelegations[] prepDelegations(int n) {
        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);
        PrepDelegations[] delegations = new PrepDelegations[n];
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            score.Address prep = prepSet[i];

            BigInteger votesInPer = ICX.divide(BigInteger.valueOf(n));

            total = total.add(votesInPer);

            BigInteger dustVote = ICX.subtract(total);

            if (dustVote.compareTo(BigInteger.ONE) <= 0){
                votesInPer = votesInPer.add(dustVote);
            }
            PrepDelegations prepDelegation = new PrepDelegations(prep, votesInPer);
            delegations[i] = prepDelegation;
        }
        return delegations;
    }

    private byte[] createByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime",String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();

    }

    private BigInteger getTimeAfter(int n){
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf((long) n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

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

    private void userLockOMM(){
        transferOmm();

        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger unlockTimeMicro = getTimeAfter(2);

        byte[] data =createByteArray("createLock",unlockTimeMicro);
        testClient.ommToken.transfer(to,value,data);
    }


}


