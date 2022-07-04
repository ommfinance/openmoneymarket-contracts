package finance.omm.score.core.test;

import static finance.omm.score.core.delegation.DelegationImpl.TAG;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static foundation.icon.jsonrpc.Address.Type;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DelegationIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient testClient;

    private static Map<String, Address> addressMap;

    private List<Address> prep = new ArrayList<>(){{
        add(Faker.address(Type.EOA));
        add(Faker.address(Type.EOA));
        add(Faker.address(Type.EOA));

    }};

    private BigInteger time = BigInteger.valueOf(System.currentTimeMillis()/ 1000);



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

    @Test
    void initializeVoteToContributor(){
        ownerClient.delegation.initializeVoteToContributors();
    }

    @Test
    void voteThreshold(){ // why set this vote
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.setVoteThreshold(BigInteger.TEN),null);

        assertEquals(BigInteger.ZERO, ownerClient.delegation.getVoteThreshold());

        ownerClient.delegation.setVoteThreshold(BigInteger.TEN);
        assertEquals(BigInteger.TEN, ownerClient.delegation.getVoteThreshold());
    }

    @Test
    void addContributor(){ // check for more than
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addContributor(prep.get(0)),null);

        // 4 prep are added while configuring
        List<score.Address> preplist = ownerClient.delegation.getContributors();
        assertEquals(4,preplist.size());

        ownerClient.delegation.addContributor(prep.get(0));

        preplist = ownerClient.delegation.getContributors();

        assertEquals(5, preplist.size());
    }

    @Test
    void addAllContributor(){
        assertUserRevert(DelegationException.notOwner(),
                () -> testClient.delegation.addAllContributors(prep.toArray(Address[]::new)),null);

        ownerClient.delegation.addAllContributors(prep.toArray(Address[]::new));
        for (int i = 0; i < prep.size(); i++) {

        }
        List<score.Address> preplist = ownerClient.delegation.getContributors();


        assertEquals(7, preplist.size());
    }

    @Test
    void removeContributor(){// TODO: add owner check
        addAllContributor();

//        assertUserRevert(DelegationException.notOwner(),
//                () -> testClient.delegation.removeContributor(prep.get(0)),null);


        ownerClient.delegation.removeContributor(prep.get(0));

        List<score.Address> preplist = ownerClient.delegation.getContributors();
        for (int i = 0; i < preplist.size(); i++) {
            System.out.println(preplist.get(i));
        }

        assertEquals(4, preplist.size());

        assertEquals(prep.get(1),preplist.get(3));

        assertUserRevert(DelegationException.unknown("OMM: " + prep.get(0) +" is not in contributor list"),
                () ->  ownerClient.delegation.removeContributor(prep.get(0)),null);

    }





    @Test
    void clearPrevious(){// distributeVoteToContributors,updateDelegations
        addContributor();
        assertUserRevert(DelegationException.unknown(TAG+" :You are not authorized to clear others delegation preference"),
                () -> ownerClient.delegation.clearPrevious(testClient.getAddress()),null);

        ownerClient.delegation.clearPrevious(ownerClient.getAddress());


    }

    @Test
    void userDefaultDelegation(){ // checks if that user has delegated to the particular prep or not
        // call matra or clearPrevious testo pachi use garne?
        testClient.delegation.userDefaultDelegation(prep.get(0));
    }

    @Test
    void onKick(){

    }

    @Test
    void prepVotes(){
        ownerClient.delegation.prepVotes(ownerClient.getAddress());
    }

    @Test
    void userPrepVotes(){
        ownerClient.delegation.userPrepVotes(testClient.getAddress());

    }

    private PrepDelegations[] prepDelegations(int n) {
        PrepDelegations[] delegations = new PrepDelegations[n];
        int value = 100/n;
        System.out.println("value " + value);
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            Address prep = Faker.address(Type.EOA);
            System.out.println("address " + prep);
            BigInteger votesInPer = BigInteger.valueOf(value).multiply(BigInteger.valueOf(10).pow(16));
            System.out.println("votes " + votesInPer);
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


//        BigInteger subtractTime = time.subtract(BigInteger.ONE);
//        System.out.println(ownerClient.rewardWeightController.getEmissionRate(subtractTime));
//
        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000000).multiply(ICX));
        Address aad = addressMap.get(Contracts.DAO_FUND.getKey());
        System.out.println("balance of daoFund" + ownerClient.ommToken.balanceOf(aad));

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(40000).multiply(ICX),ownerClient.getAddress());
        System.out.println("balance of owner" + ownerClient.ommToken.balanceOf(ownerClient.getAddress()));


//        BigInteger amount =  ownerClient.ommToken.balanceOf(ownerClient.getAddress());
//        System.out.println("THE ampunt is: "+ amount);
    }

    @Test
    void transferOmm(){
        rewardDistribution();
        System.out.println("balance of owner" + ownerClient.ommToken.balanceOf(ownerClient.getAddress()));

        ownerClient.ommToken.transfer(testClient.getAddress(),BigInteger.valueOf(20000).multiply(ICX),"transfer to testClient".getBytes());
        System.out.println("balance of owner" + ownerClient.ommToken.balanceOf(testClient.getAddress()));

//        BigInteger value = BigInteger.valueOf(20000).multiply(ICX);
//        BigInteger amount =  ownerClient.ommToken.balanceOf(ownerClient.getAddress());
//        System.out.println("THE ampunt is: "+ amount);
//
//        ownerClient.ommToken.transfer(testClient.getAddress(),value,"transfer to testClient".getBytes());
//        BigInteger amount2 =  ownerClient.ommToken.balanceOf(testClient.getAddress());
//        System.out.println("THE ampunt is: "+ amount2);

//        ownerClient.governance.transferOmmFromDaoFund(value,ownerClient.getAddress());
//        ownerClient.workesrToken.transfer(ownerClient.getAddress(),value,null);
//        ownerClient.ommToken.transfer(testClient.getAddress(),BigInteger.valueOf(10),
//                "transfer to test".getBytes());
    }

    private byte[] createByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime",String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        byte[] data = jsonData.toString().getBytes();
        return data;

    }

    private BigInteger getTimeAfter(int n){
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf(n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

    }

    @Test
    void userLockOMM(){
        transferOmm();
        System.out.println("balance of user" + ownerClient.ommToken.balanceOf(testClient.getAddress()));

        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger timeInMicro = time.multiply((BigInteger.TEN.pow(6)));
        BigInteger unlockTimeMicro = getTimeAfter(2);


        byte[] data =createByteArray("createLock",unlockTimeMicro);
        System.out.println("data "+data);
        testClient.ommToken.transfer(to,value,data);
    }

    @Test
    void onBalanceUpdate(){
        assertUserRevert(DelegationException.unauthorized("Only bOMM contract is allowed to call onBalanceUpdate method"),
                () -> ownerClient.delegation.onBalanceUpdate(ownerClient.getAddress()),null);


        ownerClient.bOMM.increaseUnlockTime(BigInteger.valueOf(1000000));
    }

///////////lendingPoolCCcccore ma set delegation//// staking cpntract ma delegate ko fix



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

