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

import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserReverted;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.libs.test.integration.Environment.godClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    @Order(1)
    public void testName() {
        assertEquals("Omm Governance Manager", ownerClient.governance.name());
    }

    @DisplayName("configuration")
    @Test
    @Order(1)
    public void configuration(){
        BigInteger amount = BigInteger.valueOf(10_000).multiply(ICX);
        rewardDistribution();
        transferOmmFromOwner(alice,amount);
        transferOmmFromOwner(bob,amount);

        // totalOmmLocked -> 25_000
        userLockOMM(ownerClient,amount,4);
        userLockOMM(alice,amount,4);
        userLockOMM(bob,amount.divide(BigInteger.TWO),2);
    }

    @DisplayName("setting constants ")
    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    class Constants{

        @Test
        @Order(1)
        public void setVoteDefinitionFee(){
            BigInteger voteFee = BigInteger.valueOf(1000).multiply(ICX);

            assertUserReverted(41, () -> alice.governance.setVoteDefinitionFee(voteFee), null);

            ownerClient.governance.setVoteDefinitionFee(voteFee);

            assertEquals(voteFee,alice.governance.getVoteDefinitionFee());
        }

        @Test
        @Order(1)
        public void setVoteDuration(){
            BigInteger duration = BigInteger.valueOf(20000000); // 20 * 10 ** 6 seconds


            assertUserReverted(41,
                    () -> alice.governance.setVoteDuration(duration), null);

            ownerClient.governance.setVoteDuration(duration);

            assertEquals(duration,ownerClient.governance.getVoteDuration());
        }

        @Test
        @Order(1)
        public void setQuorum(){
            BigInteger quorum = BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100));
            ownerClient.governance.setQuorum(quorum);


            assertUserReverted(41,
                    () -> alice.governance.setQuorum(quorum), null);

            assertUserReverted(40,
                    () -> ownerClient.governance.setQuorum(BigInteger.valueOf(2).multiply(ICX)), null);

            ownerClient.governance.setVoteDuration(quorum);

            assertEquals(quorum,ownerClient.governance.getQuorum());
        }

        @Test
        @Order(1)
        public void setVoteDefinition(){
            BigInteger voteDefinition = BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100));

            assertUserReverted(41,
                    () -> alice.governance.setVoteDefinitionCriteria(voteDefinition), null);

            assertUserReverted(40,
                    () -> ownerClient.governance.setVoteDefinitionCriteria(BigInteger.TWO.multiply(ICX)), null);

            ownerClient.governance.setVoteDefinitionCriteria(voteDefinition);

            assertEquals(voteDefinition,ownerClient.governance.getBoostedOmmVoteDefinitionCriterion());
        }

        @DisplayName("Adding and removing allowed methods ")
        @Nested
        @TestMethodOrder(OrderAnnotation.class)
        class MethodConfiguration{

            Address daoFund = addressMap.get(Contracts.DAO_FUND.getKey());
            Address rewards = addressMap.get(Contracts.REWARD_WEIGHT_CONTROLLER.getKey());
            @Test
            @Order(1)
            public void addMethods(){
                String[] methods = new String[]{"transferOmm","tokenFallback"};


                assertUserReverted(41,
                        () -> bob.governance.addAllowedMethods(daoFund,methods), null);

                ownerClient.governance.addAllowedMethods(daoFund,methods);
                String[] rewardMethods = new String[]{"setTypeWeight"};
                ownerClient.governance.addAllowedMethods(rewards,rewardMethods);

                // querying methods
                List<score.Address> supportedContracts = bob.governance.getSupportedContracts();
                assertEquals(daoFund,supportedContracts.get(0));
                assertEquals(rewards,supportedContracts.get(1));

                List<String> supportedMethods = bob.governance.getSupportedMethodsOfContract(daoFund);
                assertEquals(methods[0],supportedMethods.get(0));
                assertEquals(methods[1],supportedMethods.get(1));

            }

            @Test
            @Order(2)
            public void removeMethods(){
                String[] methods = new String[]{"tokenFallback"};

                assertUserReverted(41,
                        () -> bob.governance.removeAllowedMethods(daoFund,methods), null);

                ownerClient.governance.removeAllowedMethods(daoFund,methods);

                List<String> supportedMethods = bob.governance.getSupportedMethodsOfContract(daoFund);
                assertEquals("transferOmm",supportedMethods.get(0));

            }

            @DisplayName("Creating Proposals")
            @Nested
            @TestMethodOrder(OrderAnnotation.class)
            class ProposalCreation{
                Address governance = addressMap.get(Contracts.GOVERNANCE.getKey());
                Address rewards = addressMap.get(Contracts.REWARD_WEIGHT_CONTROLLER.getKey());
                Address daoFund = addressMap.get(Contracts.DAO_FUND.getKey());
                BigInteger value = BigInteger.valueOf(1000).multiply(ICX);

                @Test
                @Order(1)
                public void insufficientFeeProposal(){
                    assertReverted(new RevertedException(1,"Insufficient fee to create proposal"),
                            ()->ownerClient.ommToken.transfer(governance,BigInteger.ONE,"".getBytes()));

                }

                @Test
                @Order(1)
                public void invalidMethodName(){
                    String transaction = "[]";
                    byte[] data = defineVoteByteArray("invalidName",name,forum,description,transaction);

                    assertReverted(new RevertedException(1,"No valid method called :: " + Arrays.toString(data)),
                            ()->ownerClient.ommToken.transfer(governance,value,data));


                }

                @Test
                @Order(2)
                public void successfulProposals(){
                    assertEquals(0,getProposalCount());

                    BigInteger balance_before_proposal = ommTokenBalance(ownerClient);

                    String transaction = transactions(daoFund,"transferOmm");

                    // owner creates proposal
                    createProposal(ownerClient,transaction,1);
                    createProposal(ownerClient,transaction,2);
                    createProposal(ownerClient,transaction,3);

                    BigInteger balance_after_proposal = ommTokenBalance(ownerClient);
                    assertEquals(balance_before_proposal,
                            balance_after_proposal.add(BigInteger.valueOf(3000).multiply(ICX)));
                    assertEquals(3,getProposalCount());
                }

                @Test
                @Order(3)
                public void proposalsWithEmptyTransaction(){
                    BigInteger balance_before_proposal = ommTokenBalance(bob);

                    String transaction = "[]";

                    createProposal(bob,transaction,4);
                    createProposal(bob,transaction,5);

                    BigInteger balance_after_proposal = ommTokenBalance(bob);
                    assertEquals(balance_before_proposal,
                            balance_after_proposal.add(BigInteger.valueOf(2000).multiply(ICX)));

                    assertEquals(5,getProposalCount());
                }

                @Test
                @Order(4)
                public void proposalsWithStructTransaction(){
                    BigInteger balance_before_proposal = ommTokenBalance(ownerClient);

                    String transaction = transactionsStructArray(rewards,"setTypeWeight");

                    createProposal(ownerClient,transaction,6);

                    BigInteger balance_after_proposal = ommTokenBalance(ownerClient);
                    assertEquals(balance_before_proposal,
                            balance_after_proposal.add(BigInteger.valueOf(1000).multiply(ICX)));

                    assertEquals(6,getProposalCount());
                }


                @DisplayName("Voting on different proposals ")
                @Nested
                @TestMethodOrder(OrderAnnotation.class)
                class VoteProposals{

                    BigInteger blockHeight = godClient._lastBlockHeight();
                    BigInteger aliceVotingWeight = votingWeight(alice,blockHeight);
                    BigInteger bobVotingWeight = votingWeight(bob,blockHeight);

                    @Test
                    @Order(1)
                    public void for_votes(){

                        // checking voteWeight
                        assertTrue(aliceVotingWeight.compareTo(BigInteger.ZERO)>0);
                        assertTrue(bobVotingWeight.compareTo(BigInteger.ZERO)>0);

                        // proposal1 -> accepted
                        alice.governance.castVote(1,true);
                        bob.governance.castVote(1,true);

                        // voters count in proposal 1
                        Map<String,BigInteger> votersCount = ownerClient.governance.getVotersCount(1);
                        assertEquals(BigInteger.TWO,votersCount.get("for_voters"));

                        // voters weight in proposal 1
                        Map<String,?> votes = ownerClient.governance.getVotesOfUser(1,alice.getAddress());
//                        assertEquals(aliceVotingWeight,new BigInteger((votes.get("for")).toString(),16)); TODO


                        // proposal4 -> accepted but no transaction
                        alice.governance.castVote(4,true);
                        bob.governance.castVote(4,true);
                        votersCount = ownerClient.governance.getVotersCount(4);
                        assertEquals(BigInteger.TWO,votersCount.get("for_voters"));

                        // proposal 6 -> accepted
                        alice.governance.castVote(6,true);
                        bob.governance.castVote(6,true);
                        votersCount = ownerClient.governance.getVotersCount(6);
                        assertEquals(BigInteger.TWO,votersCount.get("for_voters"));

                    }

                    @Test
                    @Order(2)
                    public void against_votes(){

                        // proposal2 -> rejected
                        alice.governance.castVote(2,false);
                        bob.governance.castVote(2,false);

                        // voters count in proposal 2
                        Map<String,BigInteger> votersCount = ownerClient.governance.getVotersCount(2);
                        assertEquals(BigInteger.TWO,votersCount.get("against_voters"));

                        // voters weight in proposal 2
                        Map<String,?> votes = ownerClient.governance.getVotesOfUser(2,bob.getAddress());
//                        assertEquals(bobVotingWeight,new BigInteger((votes.get("against")).toString(),16));

                        // proposal5 -> rejected but no transaction
                        alice.governance.castVote(5,false);
                        bob.governance.castVote(5,false);

                        votersCount = ownerClient.governance.getVotersCount(5);
                        assertEquals(BigInteger.TWO,votersCount.get("against_voters"));
                    }

                    @DisplayName("Execute proposal")
                    @Nested
                    @TestMethodOrder(OrderAnnotation.class)
                    class ExecuteProposal{

                        @Test
                        @Order(1)
                        public void execute_before_vote_end(){

                            assertUserReverted(40, ()-> ownerClient.governance.execute_proposal(1));
                        }

                        @Test
                        @Order(1)
                        public void execute_succeeded_proposal() throws InterruptedException {

                            // voting time 30sec
                            Thread.sleep(20001);

                            // check proposal 1 -> succeeded
                            assertEquals("Succeeded",proposalStatus(1));

                            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

                            // executingProposal by owner
                            ownerClient.governance.execute_proposal(1);

                            assertEquals("Executed",proposalStatus(1));
                            // TODO: verify ActionExecuted eventlog

                            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
                            assertEquals(balance_after_execution,
                                    balance_before_execution.add(BigInteger.valueOf(1100).multiply(ICX)));

                        }

                        @Test
                        @Order(2)
                        public void execute_defeated_proposal() {

                            // check proposal 2 -> defeated
                            assertEquals("Defeated",proposalStatus(2));

                            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

                            // executingProposal by alice
                            alice.governance.execute_proposal(2);

                            assertEquals("Defeated",proposalStatus(2));

                            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
                            assertEquals(balance_before_execution, balance_after_execution);

                        }

                        @Test
                        @Order(3)
                        public void execute_no_quorum_proposal() {
                            // check proposal 3 -> no quorum
                            assertEquals("No Quorum",proposalStatus(3));

                            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

                            // executingProposal by bob
                            bob.governance.execute_proposal(3);

                            assertEquals("No Quorum",proposalStatus(3));

                            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
                            assertEquals(balance_before_execution, balance_after_execution);

                        }

                        @Test
                        @Order(2)
                        public void executing_executed_proposal(){
                            assertUserReverted(46, ()-> ownerClient.governance.execute_proposal(1));

                        }

                        @Test
                        @Order(4)
                        public void execute_succeeded_proposal_with_empty_transaction() {

                            // check proposal 4 -> succeeded
                            assertEquals("Succeeded",proposalStatus(4));

                            BigInteger balance_before_execution = ommTokenBalance(bob);

                            // executingProposal by ownerClient
                            ownerClient.governance.execute_proposal(4);

                            assertEquals("Executed",proposalStatus(4));
                            // TODO: verify ActionExecuted eventlog

                            BigInteger balance_after_execution = ommTokenBalance(bob);
                            assertEquals(balance_after_execution,
                                    balance_before_execution.add(BigInteger.valueOf(1000).multiply(ICX)));
                        }

                        @Test
                        @Order(2)
                        public void execute_defeated_proposal_with_empty_transaction() {

                            // check proposal 5 -> defeated
                            assertEquals("Defeated",proposalStatus(5));

                            BigInteger balance_before_execution = ommTokenBalance(bob);

                            // executingProposal by ownerClient
                            ownerClient.governance.execute_proposal(5);

                            assertEquals("Defeated",proposalStatus(5));

                            BigInteger balance_after_execution = ommTokenBalance(bob);
                            assertEquals(balance_before_execution, balance_after_execution);
                        }

                        @Test
                        @Order(3)
                        public void execute_proposal_with_struct_transaction() {
                            BigInteger systemTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
                            BigInteger time = systemTime.add(BigInteger.valueOf(1));

                            // check proposal 6 -> succeeded
                            assertEquals("Succeeded",proposalStatus(6));

                            BigInteger balance_before_execution = ommTokenBalance(ownerClient);

                            // executingProposal by ownerClient
                            ownerClient.governance.execute_proposal(6);

                            assertEquals("Executed",proposalStatus(6));

                            BigInteger balance_after_execution = ommTokenBalance(ownerClient);
                            assertEquals(balance_after_execution,
                                    balance_before_execution.add(BigInteger.valueOf(1000).multiply(ICX)));

                            Map<String,?> after_weight = ownerClient.rewardWeightController.
                                    getAllTypeWeight(time.add(BigInteger.TWO));
                            assertEquals(after_weight.get("reserve"),BigInteger.ZERO);
                            assertEquals(after_weight.get("daoFund"),ICX.divide(BigInteger.TWO));
                            assertEquals(after_weight.get("liquidity"),ICX.divide(BigInteger.TWO));

                        }


                    }


                }

            }
        }


    }

    private BigInteger votingWeight(OMMClient client, BigInteger timestamp){
        return ownerClient.governance.myVotingWeight(client.getAddress(),timestamp);
    }




    private String proposalStatus(int index){
        Map<String,?> proposal = ownerClient.governance.checkVote(index);

        return (String) proposal.get("status");
    }


    private BigInteger ommTokenBalance(OMMClient client){
        return ownerClient.ommToken.balanceOf(client.getAddress());

    }

    private void createProposal(OMMClient client, String transaction, int proposalId){
        Address governance = addressMap.get(Contracts.GOVERNANCE.getKey());
        BigInteger value = BigInteger.valueOf(1000).multiply(ICX);

        String proposalName = name + " "+proposalId;
        byte[] data = defineVoteByteArray(methodName,proposalName,forum,description,transaction);
        client.ommToken.transfer(governance,value,data);
    }


    private int getProposalCount(){
        return ownerClient.governance.getProposalCount();
    }

    private void rewardDistribution(){
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000_000).multiply(ICX));

        // owner has 3000_000 omm
        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(3000_000).multiply(ICX),
                ownerClient.getAddress(),"toOwner".getBytes());

    }

    private  void transferOmmFromOwner(OMMClient client, BigInteger value){
        ownerClient.ommToken.transfer(client.getAddress(),value,
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

    private JsonObject transactionsStruct(score.Address address, String methodName){

        JsonObject param = new JsonObject()
                .add("type", "Struct[]")
                .add("value", structValue());

        JsonArray params = new JsonArray();
        params.add(param);

        JsonObject transactionArray =  new JsonObject()
                .add("address",address.toString())
                .add("method", methodName)
                .add("parameters", params);

        return transactionArray;
    }

    private String transactionsStructArray(Address address,String methodName){
        JsonObject transactions = transactionsStruct(address,methodName);

        JsonArray transactionArray = new JsonArray();
        transactionArray.add(transactions);

        return transactionArray.toString();
    }

    private JsonArray structValue(){

        JsonArray structArray = new JsonArray();

        JsonObject jsonObject =  new JsonObject()
                .add("key",
                        structParam("String", "daoFund"))
                .add("weight", structParam("BigInteger",String.valueOf(ICX.divide(BigInteger.TWO))));

        JsonObject jsonObject2 =  new JsonObject()
                .add("key",
                        structParam("String", "reserve"))
                .add("weight", structParam("BigInteger",String.valueOf(BigInteger.ZERO)));

        JsonObject jsonObject3 =  new JsonObject()
                .add("key",
                        structParam("String", "liquidity"))
                .add("weight", structParam("BigInteger",String.valueOf(ICX.divide(BigInteger.TWO))));

        structArray.add(jsonObject);
        structArray.add(jsonObject2);
        structArray.add(jsonObject3);
        return structArray;

    }

    private JsonObject structParam(String type, String value){
        return new JsonObject()
                .add("type",type)
                .add("value",value);
    }



}
