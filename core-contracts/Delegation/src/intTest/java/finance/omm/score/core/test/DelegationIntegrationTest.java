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
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
//import finance.omm.libs.test.integration.configs.DelegationConfig;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.delegation.exception.DelegationException;
import finance.omm.score.core.test.config.DelegationConfig;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
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
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");
        omm.setupOMM();
        addressMap = omm.getAddresses();
        System.out.println("address"+addressMap);
        Config config = new DelegationConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();

        System.out.println(addressMap);

        ((LendingPoolScoreClient)ownerClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX),BigInteger.valueOf(1000).multiply(ICX));


    }

    @Test
    void testName() {
        assertEquals("OMM Delegation", ownerClient.delegation.name());
    }

    @Test // TODO: remaining
    /*
    user votes other than contributors
    on calling the method the vote should be to contributors
     */
    void initializeVoteToContributor(){

        updateDelegation();

//        System.out.println(testClient.delegation.userDefaultDelegation(testClient.getAddress()));

//        Map<String, BigInteger> prepVotes = testClient.delegation.userPrepVotes(testClient.getAddress());
//        System.out.println(prepVotes.entrySet());


        ownerClient.delegation.initializeVoteToContributors();

//        System.out.println(testClient.delegation.userDefaultDelegation(testClient.getAddress()));
//        System.out.println(ownerClient.delegation.userDefaultDelegation(ownerClient.getAddress()));

//        System.out.println(testClient.delegation.userDefaultDelegation(testClient.getAddress()));
//
//        prepVotes = testClient.delegation.userPrepVotes(testClient.getAddress());
//        System.out.println(prepVotes.entrySet());


    }

    @Test
    void voteThreshold(){
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.setVoteThreshold(BigInteger.TEN),null);

        assertEquals(BigInteger.ZERO, ownerClient.delegation.getVoteThreshold());

        ownerClient.delegation.setVoteThreshold(BigInteger.TEN);
        assertEquals(BigInteger.TEN, ownerClient.delegation.getVoteThreshold());
    }

    @Test
    void addContributor(){
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addContributor(contributorAddr.get(0)),null);

        // contributors are the default preps
        List<score.Address> preplist = ownerClient.delegation.getContributors();
        assertEquals(4,preplist.size());

        ownerClient.delegation.addContributor(contributorAddr.get(0));

        preplist = ownerClient.delegation.getContributors();

        assertEquals(5, preplist.size());
    }

    @Test
    void addAllContributor(){
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addAllContributors(contributorAddr.toArray(Address[]::new)),null);

        ownerClient.delegation.addAllContributors(contributorAddr.toArray(Address[]::new));
        for (int i = 0; i < contributorAddr.size(); i++) {

        }
        List<score.Address> preplist = ownerClient.delegation.getContributors();


        assertEquals(7, preplist.size());
    }

    @Test
    /*
    addAllContributor() and remove of the address
    should throw exception when trying to remove same address
     */
    void removeContributor(){// TODO: add owner check
        //        assertUserRevert(DelegationException.notOwner(),
//                () -> testClient.delegation.removeContributor(prep.get(0)),null);
        addAllContributor();
        List<score.Address> preplist = ownerClient.delegation.getContributors();
        assertEquals(7, preplist.size());

        ownerClient.delegation.removeContributor(contributorAddr.get(0));

        preplist = ownerClient.delegation.getContributors();
        assertEquals(6, preplist.size());

        assertUserRevert(DelegationException.unknown("OMM: " + contributorAddr.get(0) +" is not in contributor list"),
                () ->  ownerClient.delegation.removeContributor(contributorAddr.get(0)),null);
    }

    private PrepDelegations[] prepDelegations(int n) {
        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);
        PrepDelegations[] delegations = new PrepDelegations[n];
        int value = 100/n;
        System.out.println("value " + value);
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            score.Address prep = prepSet[i +3];
            BigInteger votesInPer = BigInteger.valueOf((long) value).multiply(BigInteger.valueOf(10).pow(16));
            System.out.println("votes: " + votesInPer);
            total = total.add(votesInPer);
            System.out.println("total " +total);
            PrepDelegations prepDelegation = new PrepDelegations(prep, votesInPer);
            delegations[i] = prepDelegation;
        }
        return delegations;
    }


//    @Test // not working
//    void updateDelegation(){
//        // user delegates to new Preps
//        PrepDelegations[] delegations = prepDelegations(5);
//        List<PrepDelegations> expectedList = new ArrayList<>();
//        for (int i = 0; i < delegations.length; i++) {
//            expectedList.add(delegations[i]);
//
//        }
//        System.out.println("hello");
//
//
//        testClient.delegation.updateDelegations(delegations,testClient.getAddress());
//
//        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
//        for (int i = 0; i < prepDelegatedList.size(); i++) {
//            System.out.println(prepDelegatedList.get(i));
//        }
////        assertEquals(expectedList.size(),prepDelegatedList.size());
////        for (int i = 0; i < 5; i++) {
////            assertEquals(expectedList.get(i),prepDelegatedList.get(i));
////        }
////
////
////        PrepDelegations[] delegationDetails = testClient.delegation.getUserDelegationDetails(testClient.getAddress());
////        assertEquals(delegations,delegationDetails);
//
//    }



    @Test
    void addType(){
        ownerClient.governance.addType("daoFund",true);
        ownerClient.governance.addType("workerToken",true);

        ownerClient.governance.setTypeWeight(new TypeWeightStruct[]{
                new TypeWeightStruct("daoFund", BigInteger.valueOf(40).multiply(ICX.divide(BigInteger.valueOf(100)))),
                new TypeWeightStruct("workerToken", BigInteger.valueOf(60).multiply(ICX.divide(BigInteger.valueOf(100))))
        },time.add(BigInteger.valueOf(100)));


    }

    @Test
    void rewardDistribution(){
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000000).multiply(ICX));

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(40000).multiply(ICX),ownerClient.getAddress());


    }

    @Test
    void transferOmm(){
        rewardDistribution();

        ownerClient.ommToken.transfer(testClient.getAddress(),BigInteger.valueOf(20000).multiply(ICX),
                "transfer to testClient".getBytes());
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

    @Test
    void userLockOMM(){
        transferOmm();

        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger unlockTimeMicro = getTimeAfter(2);

        byte[] data =createByteArray("createLock",unlockTimeMicro);
        testClient.ommToken.transfer(to,value,data);
    }

    @Test
    /*
    check if user has default delegation after locking omm
     */
    void checkUserDefaultDelegation(){
        userLockOMM();
        boolean expected = testClient.delegation.userDefaultDelegation(testClient.getAddress());
        assertTrue(expected);

        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(4,prepDelegatedList.size());

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());

//        Map<String, BigInteger> prepVotes = testClient.delegation.userPrepVotes(testClient.getAddress());
//        System.out.println(prepVotes.entrySet());

        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);

        for (int i = 0; i < prepDelegatedList.size(); i++) {
            assertEquals(prepSet[i],userDelegations[i]._address);
        }


    }


    @Test
    /*
    after user locks OMM
    user updates delegation to their desired prep
     */
    void updateDelegation(){
        userLockOMM();

        // default contributors
        List<score.Address> prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(4,prepDelegatedList.size());

        PrepDelegations[] delegations = prepDelegations(5);
        List<PrepDelegations> expectedList = new ArrayList<>(Arrays.asList(delegations));

        testClient.delegation.updateDelegations(delegations,testClient.getAddress());

        prepDelegatedList=  testClient.delegation.getPrepList();
        assertEquals(8,prepDelegatedList.size());

        PrepDelegations[] computedDelegation = testClient.delegation.computeDelegationPercentages();
        assertEquals(expectedList.size(),computedDelegation.length);

        for (int i = 0; i < expectedList.size(); i++) {
            assertEquals(expectedList.get(i)._address,computedDelegation[i]._address);
            assertEquals(expectedList.get(i)._votes_in_per.multiply(BigInteger.valueOf(100L)),
                    computedDelegation[i]._votes_in_per);
        }

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());
        assertEquals(delegations.length,userDelegations.length);
        for (int i = 0; i < delegations.length; i++) {
            assertEquals(delegations[i]._address,userDelegations[i]._address);
            assertEquals(delegations[i]._votes_in_per,userDelegations[i]._votes_in_per);
        }

//        Map<String, BigInteger> prepVotes = testClient.delegation.userPrepVotes(testClient.getAddress());
//        System.out.println(prepVotes.entrySet());

    }

    @Test
    /*
    delegate votes to different preps
    clearPrevious set delegation to default
     */
    void clearPrevious(){
        updateDelegation();

        assertUserRevert(DelegationException.unknown(TAG+" :You are not authorized to clear others delegation preference"),
                () -> ownerClient.delegation.clearPrevious(testClient.getAddress()),null);

        testClient.delegation.clearPrevious(testClient.getAddress());


        score.Address[] contributors = Environment.contributors.keySet().toArray(score.Address[]::new);

        PrepDelegations[] userDelegations = testClient.delegation.getUserDelegationDetails(testClient.getAddress());
//        for (PrepDelegations delegation: userDelegations) {
//            System.out.println(delegation._address);
//            System.out.println(delegation._votes_in_per);
//        }
//        System.out.println("user delegation length " + userDelegations.length);

        assertEquals(contributors.length,userDelegations.length);
        for (int i = 0; i < contributors.length; i++) {
            assertEquals(contributors[i],userDelegations[i]._address);
        }

    }

    @Test
    void onBalanceUpdate(){ // done from locking omm
        userLockOMM();
        assertUserRevert(DelegationException.unauthorized("Only bOMM contract is allowed to call onBalanceUpdate method"),
                () -> ownerClient.delegation.onBalanceUpdate(ownerClient.getAddress()),null);


    }




    // update Delegation -> add other 3 prep to it
    //                  -> check if the prep list is 5 or not
    //                  -> clear previous method and check if it works or not
    //                  -> getPrepList
    //
    // onKick -> lock some OMM
    //          -> call the method
    //          -> update bhayo ki nai check

    // onBalanceUpdate -> lock OMM
    // increaseUnlockTime -> depositFor
    // getWorkingBalance


}

/*
Reward distribution deploy
Reward weight controller ma governence through addType and setType for doaFund and Worker Token

Start reward distribution
Enable Reward claim
Distribute Omm token -> doa fund and worker token gets omm

Transfer omm from woker token to user's address or you can use diaFund to transfer some OMM

Once the user has OMM call transfer method -> tokenfallback ->make sure the data is there ->
 */

