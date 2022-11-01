package finance.omm.score.intTest.governance;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.score.intTest.governance.config.GovernanceConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import score.UserRevertedException;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class GovernanceTest implements ScoreIntegrationTest {

    private static OMMClient ownerClient;
    private static OMMClient alice;
    private static OMMClient bob;

    private static Map<String, Address> addressMap;

    private final BigInteger time = BigInteger.valueOf(System.currentTimeMillis()/ 1000);

    String name = "Arbitrary Call";
    String forum = "https://forum.omm.finance/random_proposal";
    String description = "Any user can call execute proposal after success";
    String methodName = "defineVote";

    BigInteger systemTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);

    Map<String, Boolean> STATES = new HashMap<>();


    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();

        Config config = new GovernanceConfig();
        omm.runConfig(config);

        ownerClient = omm.defaultClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
        bob = omm.newClient(BigInteger.TEN.pow(24));
    }

    @DisplayName("Test name")
    @Test
    public void testName() {
        assertEquals("Omm Governance", ownerClient.governance.name());
    }

    @Test
    @Order(1)
    public void configuration(){

        BigInteger amount = BigInteger.valueOf(10_000).multiply(ICX);
        transferOmmFromOwner(alice,amount);
        transferOmmFromOwner(bob,amount);

        assertEquals(BigInteger.ZERO,votingWeight(alice,systemTime));
        assertEquals(BigInteger.ZERO,votingWeight(bob,systemTime));
        // totalOmmLocked -> 15_000
        userLockOMM(alice,amount,4);
        userLockOMM(bob,amount.divide(BigInteger.TWO),4);

        // check voting weight after this
//        assertEquals(0,votingWeight(alice,systemTime));
//        assertEquals(0,votingWeight(bob,systemTime));

    }

    private BigInteger votingWeight(OMMClient client, BigInteger timestamp){
        return ownerClient.governance.myVotingWeight(client.getAddress(),timestamp);
    }

    @Test
    @Order(2)
    public void owner_puts_proposal(){

        assertEquals(0,getProposalCount());

        Address daoFund = addressMap.get(Contracts.DAO_FUND.getKey());

        BigInteger balance_before_proposal = ommTokenBalance(ownerClient);
        // owner creates proposal
        createProposal(ownerClient,daoFund,"transferOmm");
        createProposal(ownerClient,daoFund,"transferOmm");
        createProposal(ownerClient,daoFund,"transferOmm");

        BigInteger balance_after_proposal = ommTokenBalance(ownerClient);

        assertEquals(balance_before_proposal,
                balance_after_proposal.add(BigInteger.valueOf(3000).multiply(ICX)));
        assertEquals(3,getProposalCount());
    }

    @Test
    @Order(3)
    public void vote_proposal() {

        // proposal1 -> accepted
        alice.governance.castVote(1,true);
        bob.governance.castVote(1,true);

        // proposal2 -> defeated
        alice.governance.castVote(2,false);
        bob.governance.castVote(2,false);

        //proposal3 -> no quorum
        bob.governance.castVote(3,true);

        STATES.put("Voted on all proposal",true);

        // voting time 1 min(10000ms)
//        Thread.sleep(10000);
    }

    @DisplayName("Execute proposal")
    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    class ExecuteProposal{

        @Test
        @Order(1)
        public void execute_before_vote_end(){
            UserRevertedException voteNotEnded = assertThrows(UserRevertedException.class, () ->
                    ownerClient.governance.execute_proposal(1));
        }

        @Test
        @Order(1)
        public void execute_succeeded_proposal() throws InterruptedException {
            if (STATES.getOrDefault("Voted on all proposal",false)){
                return;
            }
            // voting time 1 min(10000ms)
            Thread.sleep(10000);

            // check proposal 1 -> succeeded
            assertEquals("Succeeded",proposalStatus(1));

            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

            // executingProposal by owner
            ownerClient.governance.execute_proposal(1);

            assertEquals("Executed",proposalStatus(1));
            // TODO: verify ActionExecuted eventlog

            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
            assertEquals(balance_before_execution,
                    balance_after_execution.add(BigInteger.valueOf(100).multiply(ICX)));

            STATES.put("executed proposal 1",true);
        }

        @Test
        @Order(1)
        public void execute_defeated_proposal() throws InterruptedException {
            if (STATES.getOrDefault("Voted on all proposal",false)){
                return;
            }
            Thread.sleep(10000);
            // check proposal 2 -> defeated
            assertEquals("Defeated",proposalStatus(2));

            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

            // executingProposal by alice
            alice.governance.execute_proposal(2);

            assertEquals("Executed",proposalStatus(2));

            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
            assertEquals(balance_before_execution, balance_after_execution);

            STATES.put("executed proposal 2",true);
        }

        @Test
        @Order(1)
        public void execute_no_quorum_proposal() throws InterruptedException {
            if (STATES.getOrDefault("Voted on all proposal",false)){
                return;
            }
            Thread.sleep(10000);
            // check proposal 3 -> no quorum
            assertEquals("No Quorum",proposalStatus(3));

            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

            // executingProposal by bob
            bob.governance.execute_proposal(3);

            assertEquals("Executed",proposalStatus(3));

            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
            assertEquals(balance_before_execution, balance_after_execution);

            STATES.put("executed proposal 3",true);
        }

        @Test
        @Order(2)
        public void executing_executed_proposal(){
            if (STATES.getOrDefault("executed proposal 1",false)){
                return;
            }

            UserRevertedException notActive = assertThrows(UserRevertedException.class, () ->
                   ownerClient.governance.execute_proposal(1));
            // try printing the exception and check the result
        }
    }

    private String proposalStatus(int index){
        Map<String,?> proposal = ownerClient.governance.checkVote(index);

        return (String) proposal.get("status");
    }


    private BigInteger ommTokenBalance(OMMClient client){
        return ownerClient.ommToken.balanceOf(client.getAddress());

    }

    private void createProposal(OMMClient client, Address transactionAddress, String transactionMethodName){
        Address governance = addressMap.get(Contracts.GOVERNANCE.getKey());
        BigInteger value = BigInteger.valueOf(1000).multiply(ICX);

        // check method names is present in that contract or not
        String transaction = transactions(transactionAddress,transactionMethodName);

        byte[] data = defineVoteByteArray(methodName,name,forum,description,transaction);
        client.ommToken.transfer(governance,value,data);
    }



    @Test
    @Order(3)
    public void vote(){

    }

    private int getProposalCount(){
        return ownerClient.governance.getProposalCount();
    }

    private void rewardDistribution(){
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000_000).multiply(ICX));

        // owner has 4000_000 omm
        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(4000_000).multiply(ICX),ownerClient.getAddress());

    }

    private  void transferOmmFromOwner(OMMClient client, BigInteger value){
        rewardDistribution();

        ownerClient.ommToken.transfer(client.getAddress(),value.multiply(ICX),
                "transfer to client".getBytes());
    }

    private void userLockOMM(OMMClient client, BigInteger value, int time){

        Address bOmm = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger unlockTimeMicro = getTimeAfter(time);
        byte[] data = lockTimeByteArray("createLock",unlockTimeMicro);

        client.ommToken.transfer(bOmm,value,data);
    }

    private BigInteger getTimeAfter(int n){
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf((long) n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

    }

    private byte[] lockTimeByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime",String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();

    }

    private byte[] defineVoteByteArray(String methodName, String name, String forum, String description,
                                       String transactions) {

        JsonObject internalParameters = new JsonObject()
                .add("name",String.valueOf(name))
                .add("forum",String.valueOf(forum))
                .add("description",String.valueOf(description))
                .add("transactions",String.valueOf(transactions));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();

    }

    private JsonObject transactionsArray(score.Address address, String methodName){

        JsonObject param1 = new JsonObject()
                .add("type", "BigInteger")
                .add("value", "100000000000000000000");

        JsonObject param2 = new JsonObject()
                .add("type", "Address")
                .add("value", ownerClient.getAddress().toString());

        JsonArray params = new JsonArray();
        params.add(param1);
        params.add(param2);

        return new JsonObject()
                .add("address",address.toString())
                .add("method", methodName)
                .add("parameters", params);
    }

    private String transactions(Address address,String methodName){
        JsonObject transactions = transactionsArray(address,methodName);

        JsonArray transactionArray = new JsonArray();
        transactionArray.add(transactions);

        return transactionArray.toString();
    }






}
