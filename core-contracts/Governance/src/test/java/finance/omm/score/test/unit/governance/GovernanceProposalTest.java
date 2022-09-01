package finance.omm.score.test.unit.governance;


import static finance.omm.utils.math.MathUtils.exaDivide;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class GovernanceProposalTest extends AbstractGovernanceTest {

    Account OMM_TOKEN_ACCOUNT = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN);
    BigInteger value = THOUSAND.multiply(ICX);
    BigInteger FIVE_MINUTES = BigInteger.valueOf(5L).multiply(SIXTY).multiply(BigInteger.valueOf(1_000_000L));
    BigInteger snapshot = TimeConstants.getBlockTimestamp().add(FIVE_MINUTES);
    BigInteger voteStart = TimeConstants.getBlockTimestamp().add(FIVE_MINUTES);
    Account fromWallet = sm.createAccount(10);
    Address from = fromWallet.getAddress();
    String name = "Zero Knowledge";
    String forum = "https://forum.omm.finance/random_proposal";
    String description = "Verify your statement is true without having to show how you proved it.";
    String methodName = "defineVote";
    BigInteger duration = TWO.multiply(BigInteger.valueOf(86400 * 1_000_000L));
    BigInteger quorum = THIRTY_THREE.multiply(PERCENT);
    BigInteger ZERO = BigInteger.ZERO;

    private void initialize() {
        score.invoke(owner, "setVoteDefinitionFee", THOUSAND.multiply(ICX));
        score.invoke(owner, "setQuorum", quorum);
        score.invoke(owner, "setVoteDuration", duration);
        score.invoke(owner, "setVoteDefinitionCriteria", BigInteger.ONE.multiply(PERCENT));
    }

    private byte[] createByteArray(String name, String forum, String description,
            BigInteger voteStart, String methodName) {

        JsonObject internalParameters = new JsonObject()
                .add("name", name)
                .add("forum", forum)
                .add("description", description)
                .add("vote_start", voteStart.longValue());

        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        byte[] data = jsonData.toString().getBytes();
        return data;
    }

    private void successfulProposalCreationMocks() {
        BigInteger userStaked = HUNDRED.multiply(ICX);
        BigInteger totalStaked = TWO.multiply(HUNDRED.multiply(ICX));

        doReturn(userStaked).when(bOMM).balanceOfAt(any(), any());
        doReturn(totalStaked).when(bOMM).totalSupplyAt(any());

        doNothing().when(ommToken).transfer(any(), any(), any());
    }

    @Test
    public void getProposalsCount() {
        // no proposals at start
        int count = (int) score.call("getProposalCount");
        assertEquals(0, count);
    }

    @Test
    /*
     * Test the following setter and getter methods
     * setVoteDefinitionFee / getVoteDefinitionFee
     * setQuorum / getQuorum
     * setVoteDuration / getVoteDuration
     * setBoostedVoteDefinitionCriterion / getBoostedOmmVoteDefinitionCriterion
     */
    public void initializeMethods() {
        Account notOwner = sm.createAccount(10);

        BigInteger voteDefinitionFeeExpected = THOUSAND.multiply(ICX);
        BigInteger quorumExpected = THIRTY_THREE.multiply(PERCENT);
        BigInteger voteDurationExpected = TWO;
        BigInteger ommVoteDefinitionCriterion = BigInteger.ONE.multiply(PERCENT);

        score.invoke(owner, "setVoteDefinitionFee", voteDefinitionFeeExpected);
        score.invoke(owner, "setQuorum", quorumExpected);
        score.invoke(owner, "setVoteDuration", voteDurationExpected);
        score.invoke(owner, "setVoteDefinitionCriteria", ommVoteDefinitionCriterion);

        assertEquals(voteDefinitionFeeExpected, score.call("getVoteDefinitionFee"));
        assertEquals(quorumExpected, score.call("getQuorum"));
        assertEquals(ommVoteDefinitionCriterion, score.call("getBoostedOmmVoteDefinitionCriterion"));
        assertEquals(voteDurationExpected, score.call("getVoteDuration"));

        Executable errorMsg = () -> score.invoke(notOwner, "setVoteDefinitionFee", voteDefinitionFeeExpected);
        expectErrorMessage(errorMsg, "require owner access");
        errorMsg = () -> score.invoke(notOwner, "setQuorum", voteDefinitionFeeExpected);
        expectErrorMessage(errorMsg, "require owner access");
        errorMsg = () -> score.invoke(notOwner, "setVoteDuration", voteDefinitionFeeExpected);
        expectErrorMessage(errorMsg, "require owner access");
        errorMsg = () -> score.invoke(notOwner, "setVoteDefinitionCriteria", voteDefinitionFeeExpected);
        expectErrorMessage(errorMsg, "require owner access");
    }


    @Test
    public void defineVote() {
        initialize();

        byte[] data = createByteArray(name, forum, description, voteStart, methodName);

        // general case of proposal creation
        successfulProposalCreationMocks();
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data);
        verify(scoreSpy).ProposalCreated(ONE, name, from);

        // proposals count should increase
        int count = (int) score.call("getProposalCount");
        assertEquals(1, count);

        name += " University";

        // use contract other than OMM Token
        Account notOMM = Account.newScoreAccount(10);
        Executable notOMMToken = () -> score.invoke(notOMM, "tokenFallback", from, value, data);
        expectErrorMessage(notOMMToken, "invalid token sent");
//
        // insufficient fee check
        BigInteger insufficientValue = HUNDRED.multiply(ICX);
        Executable insufficientFeeSent = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from,
                insufficientValue, data);
        expectErrorMessage(insufficientFeeSent, "Insufficient fee to create proposal");
//
        // method not named defineVote
        String invalidMethodName = "notDefineVote";
        byte[] invalidData1 = createByteArray(name, forum, description, voteStart, invalidMethodName);
        Executable wrongMethodName = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, invalidData1);
        expectErrorMessageIn(wrongMethodName, "No valid method called :: ");

//        // length of description over 500
        String invalidDescription = "Invalid Description".repeat(200);
        byte[] invalidData2 = createByteArray(methodName, forum, invalidDescription, voteStart, methodName);
        Executable invalidDescriptionLength = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value,
                invalidData2);
        expectErrorMessage(invalidDescriptionLength, "Description must be less than or equal to 500 characters.");

        // vote_start not in microseconds
        BigInteger voteStartNotInMicro = TimeConstants.getBlockTimestamp().divide(THOUSAND);
        byte[] invalidData3a = createByteArray(methodName, forum, description, voteStartNotInMicro,
                methodName);
        Executable voteStartNotInMicroSecond = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value,
                invalidData3a);
        expectErrorMessage(voteStartNotInMicroSecond, "vote_start timestamp should be in microseconds");

        // start vote before current timestamp
        BigInteger invalidVoteStartTime = TimeConstants.getBlockTimestamp().subtract(FIVE_MINUTES);
        byte[] invalidData4 = createByteArray(methodName, forum, description, invalidVoteStartTime,
                methodName);
        Executable voteStartBeforeCurrentTime = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value,
                invalidData4);
        expectErrorMessage(voteStartBeforeCurrentTime, "Vote cannot start before the current timestamp");


        // proposal already exists check
        String existingName = "Zero Knowledge";
        String expected = "Proposal name (" + existingName + ") has already been used.";
        byte[] invalidData6 = createByteArray(existingName, forum, description, voteStart, methodName);
        Executable alreadyExists = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, invalidData6);
        expectErrorMessage(alreadyExists, expected);

        // insufficient staking balance
        doReturn(PERCENT).when(bOMM).balanceOfAt(any(), any());
        byte[] insufficentData = createByteArray(name, forum, description, voteStart, methodName);
        Executable insufficientStaked = () -> score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value,
                insufficentData);
        expectErrorMessageIn(insufficientStaked, "User needs at least ");

        // still only one proposal
        count = (int) score.call("getProposalCount");
        assertEquals(1, count);
    }

    @Test
    public void checkVote() {
        initialize();

        successfulProposalCreationMocks();
        byte[] data = createByteArray(name + " 1", forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data);

        Map<String, ?> voteCheck = (Map<String, ?>) score.call("checkVote", 1);

        assertEquals(voteCheck.get("id"), 1);
        assertEquals(voteCheck.get("name"), name + " 1");
        assertEquals(voteCheck.get("proposer"), from);
        assertEquals(voteCheck.get("description"), description);
        assertEquals(voteCheck.get("majority"), new BigInteger("666666666666666667"));
        assertEquals(voteCheck.get("start day"), voteStart);
        assertEquals(voteCheck.get("end day"), voteStart.add(duration));
        assertEquals(voteCheck.get("quorum"), quorum);
        assertEquals(voteCheck.get("for"), ZERO);
        assertEquals(voteCheck.get("against"), ZERO);
        assertEquals(voteCheck.get("for_voter_count"), ZERO);
        assertEquals(voteCheck.get("against_voter_count"), ZERO);
        assertEquals(voteCheck.get("forum"), forum);
        assertEquals(voteCheck.get("status"), "Active");
    }

    @Test
    public void getProposals() {
        initialize();

        successfulProposalCreationMocks();
        byte[] data1 = createByteArray(name + " 10", forum, description, voteStart, methodName);
        byte[] data2 = createByteArray(name + " 20", forum, description, voteStart, methodName);
        byte[] data3 = createByteArray(name + " 30", forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data1);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data2);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data3);

        int count = (int) score.call("getProposalCount");
        assertEquals(3, count);

        List<Map<String, ?>> proposals = (List<Map<String, ?>>) score.call("getProposals", 3, 1);

        // check for one proposal
        Map<String, ?> proposal1 = proposals.get(1);
        assertEquals(proposal1.get("id"), 2);
        assertEquals(proposal1.get("name"), name + " 20");
        assertEquals(proposal1.get("proposer"), from);
        assertEquals(proposal1.get("description"), description);
        assertEquals(proposal1.get("majority"), new BigInteger("666666666666666667"));
        assertEquals(proposal1.get("start day"), voteStart);
        assertEquals(proposal1.get("end day"), voteStart.add(duration));
        assertEquals(proposal1.get("quorum"), quorum);
        assertEquals(proposal1.get("for"), ZERO);
        assertEquals(proposal1.get("against"), ZERO);
        assertEquals(proposal1.get("for_voter_count"), ZERO);
        assertEquals(proposal1.get("against_voter_count"), ZERO);
        assertEquals(proposal1.get("forum"), forum);
        assertEquals(proposal1.get("status"), "Active");
    }

    @Test
    public void updateVoteForum() {
        initialize();

        // null proposal
        String newLink = forum + "123";
        Executable errorMsg = () -> score.invoke(owner, "updateVoteForum", 1, newLink);
        expectErrorMessage(errorMsg, "Proposal not found with index :: 1");

        successfulProposalCreationMocks();
        byte[] data1 = createByteArray(name, forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data1);
        verify(scoreSpy).ProposalCreated(ONE, name, from);

        // non owner
        Account randomUser = sm.createAccount(10);
        errorMsg = () -> score.invoke(randomUser, "updateVoteForum", 1, newLink);
        expectErrorMessage(errorMsg, "require owner access");

        // successful update
        score.invoke(owner, "updateVoteForum", 1, newLink);

        // checks
        Map<String, ?> proposal = (Map<String, ?>) score.call("checkVote", 1);
        assertEquals(proposal.get("forum"), newLink);
    }


    @Test
    public void cancelVote() {
        initialize();

        // try to cancel a proposal which does not exist
        Executable errorMsg = () -> score.invoke(owner, "cancelVote", 10);
        expectErrorMessage(errorMsg, "Proposal not found with index :: 10");

        // general case of proposal creation
        successfulProposalCreationMocks();
        byte[] data = createByteArray(name, forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data);
        verify(scoreSpy).ProposalCreated(ONE, name, from);

        // random user should not be able to cancel proposal
        Account randomUser = sm.createAccount(10);
        Executable randomUserError = () -> score.invoke(randomUser, "cancelVote", 1);
        expectErrorMessage(randomUserError, "Only owner or proposer may call this method.");

        // check status for cancelled proposal
        score.invoke(owner, "cancelVote", 1);
        Map<String, ?> proposal = (Map<String, ?>) score.call("checkVote", 1);
        assertEquals(proposal.get("status"), "Cancelled");

        // create another proposal
        BigInteger ONE_HOUR = FIVE_MINUTES.multiply(BigInteger.valueOf(12));
        BigInteger ONE_DAY = ONE_HOUR.multiply(BigInteger.valueOf(24L));
        voteStart = voteStart.add(ONE_DAY);
        byte[] data1 = createByteArray(name + " abcd", forum, description, voteStart,
                methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data1);
        verify(scoreSpy).ProposalCreated(TWO, name + " abcd", from);

        increaseTimeBy(ONE_DAY.add(FIVE_MINUTES));
        Executable voteStarted = () -> score.invoke(fromWallet, "cancelVote", 2);
        expectErrorMessage(voteStarted, "Only owner can cancel a vote that has started.");

        BigInteger SIX_DAYS = ONE_DAY.multiply(BigInteger.valueOf(6L));
        increaseTimeBy(SIX_DAYS);

        // newly created proposal cannot be cancelled
        Executable pastTime = () -> score.invoke(owner, "cancelVote", 1);
        expectErrorMessage(pastTime, "Omm Governance: Proposal can be cancelled only from active status.");
    }

    @Test
    /* Cast Vote and execute proposal tests */
    public void castVote() {
        initialize();

        Account user1 = sm.createAccount(1);
        Account user2 = sm.createAccount(1);
        Account user3 = sm.createAccount(1);
        Account user4 = sm.createAccount(1);
        Account user5 = sm.createAccount(1);

        Address from = user2.getAddress();
        /*
         * ommToken total supply 1000
         * ommToken total staked 800
         * ommToken user staked 50, 100, 200, 300, 150
         */
        BigInteger FIFTY = BigInteger.valueOf(50).multiply(ICX);
        BigInteger HUNDRED = FIFTY.multiply(TWO);
        BigInteger TWO_HUNDRED = HUNDRED.multiply(TWO);
        BigInteger THREE_HUNDRED = HUNDRED.add(TWO_HUNDRED);
        BigInteger HUNDRED_FIFTY = FIFTY.add(HUNDRED);

        doReturn(THOUSAND.multiply(ICX)).when(bOMM).totalSupplyAt(any());
        doReturn(EIGHTY.multiply(ICX).multiply(TEN)).when(bOMM).totalSupplyAt(any());

        doReturn(FIFTY).when(bOMM).balanceOfAt(eq(user1.getAddress()), any());
        doReturn(HUNDRED).when(bOMM).balanceOfAt(eq(user2.getAddress()), any());
        doReturn(TWO_HUNDRED).when(bOMM).balanceOfAt(eq(user3.getAddress()), any());
        doReturn(THREE_HUNDRED).when(bOMM).balanceOfAt(eq(user4.getAddress()), any());
        doReturn(HUNDRED_FIFTY).when(bOMM).balanceOfAt(eq(user5.getAddress()), any());

        // user2 will create a proposal
        byte[] data = createByteArray(name, forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data);
        verify(scoreSpy).ProposalCreated(ONE, name, from);

        BigInteger ONE_HOUR = FIVE_MINUTES.multiply(BigInteger.valueOf(12));
        increaseTimeBy(ONE_HOUR.multiply(TWO));

        score.invoke(user1, "castVote", 1, true);
        verify(scoreSpy).VoteCast(name, true, user1.getAddress(), FIFTY, FIFTY, ZERO);
        score.invoke(user2, "castVote", 1, false);
        verify(scoreSpy).VoteCast(name, false, user2.getAddress(), HUNDRED, FIFTY, HUNDRED);
        score.invoke(user3, "castVote", 1, true);
        verify(scoreSpy).VoteCast(name, true, user3.getAddress(), TWO_HUNDRED, TWO_HUNDRED.add(FIFTY), HUNDRED);
        score.invoke(user4, "castVote", 1, true);
        BigInteger FIVE_HUNDRED_FIFTY = TWO_HUNDRED.add(THREE_HUNDRED).add(FIFTY);
        verify(scoreSpy).VoteCast(name, true, user4.getAddress(),
                THREE_HUNDRED, FIVE_HUNDRED_FIFTY, HUNDRED);
        score.invoke(user5, "castVote", 1, false);
        verify(scoreSpy).VoteCast(name, false, user5.getAddress(),
                HUNDRED_FIFTY, FIVE_HUNDRED_FIFTY, HUNDRED.add(HUNDRED_FIFTY));

        // check proposal
        Map<String, ?> proposal = (Map<String, ?>) score.call("checkVote", 1);

        // the proposal was created by user 2
        assertEquals(proposal.get("id"), 1);
        assertEquals(proposal.get("name"), name);
        assertEquals(proposal.get("proposer"), from);
        assertEquals(proposal.get("description"), description);
        assertEquals(proposal.get("majority"), new BigInteger("666666666666666667"));
        assertEquals(proposal.get("start day"), voteStart);
        assertEquals(proposal.get("end day"), voteStart.add(duration));
        assertEquals(proposal.get("quorum"), quorum);
        assertEquals(proposal.get("for"), exaDivide(FIVE_HUNDRED_FIFTY, BigInteger.valueOf(800L).multiply(ICX)));
        assertEquals(proposal.get("against"),
                exaDivide(HUNDRED.add(HUNDRED_FIFTY), BigInteger.valueOf(800L).multiply(ICX)));
        assertEquals(proposal.get("for_voter_count"), BigInteger.valueOf(3L));
        assertEquals(proposal.get("against_voter_count"), TWO);
        assertEquals(proposal.get("forum"), forum);
        assertEquals(proposal.get("status"), "Active");

        // voting not over yet, try to execute must mail
        Executable voteNotOver = () -> score.invoke(owner, "execute_proposal", 1);
        expectErrorMessage(voteNotOver, "Voting period has not ended");

        BigInteger TWO_DAYS = BigInteger.valueOf(86400L).multiply(BigInteger.valueOf(1000000L))
                .multiply(BigInteger.valueOf(2L));

        increaseTimeBy(TWO_DAYS);

        Map<String, ?> proposalOver = (Map<String, ?>) score.call("checkVote", 1);
        assertEquals("Succeeded", proposalOver.get("status"));

        // execute proposal not owner
        Executable notOwnerCall = () -> score.invoke(user5, "execute_proposal", 1);
        expectErrorMessage(notOwnerCall, "require owner access");

        // execute proposal owner
        score.invoke(owner, "execute_proposal", 1);
        verify(scoreSpy).ActionExecuted(ONE, "Executed");

        // set proposal status check not owner
        notOwnerCall = () -> score.invoke(user5, "setProposalStatus", 1, "Pending");
        expectErrorMessage(notOwnerCall, "require owner access");

        // set proposal status check owner
        score.invoke(owner, "setProposalStatus", 1, "Failed Execution");
        proposal = (Map<String, ?>) score.call("checkVote", 1);
        assertEquals(proposal.get("status"), "Failed Execution");

        // try to execute proposal which is not active
        Executable notActiveProposal = () -> score.invoke(owner, "execute_proposal", 1);
        expectErrorMessage(notActiveProposal, "This proposal is not active.");
    }

    @Test
    public void differentProposalsStatus() {
        initialize();

        Account user1 = sm.createAccount(1);
        Account user2 = sm.createAccount(1);
        Account user3 = sm.createAccount(1);
        Account user4 = sm.createAccount(1);
        Account user5 = sm.createAccount(1);

        Address from = user3.getAddress();
        /*
         * ommToken total supply 15000
         * ommToken total staked 2000
         * considered 5 users
         * ommToken user staked 50, 100, 200, 300, 150
         * considered users total staked 800
         */
        BigInteger FIFTY = BigInteger.valueOf(50).multiply(ICX);
        BigInteger HUNDRED = FIFTY.multiply(TWO);
        BigInteger TWO_HUNDRED = HUNDRED.multiply(TWO);
        BigInteger THREE_HUNDRED = HUNDRED.add(TWO_HUNDRED);
        BigInteger HUNDRED_FIFTY = FIFTY.add(HUNDRED);
        BigInteger TWO_THOUSAND = BigInteger.valueOf(2000L).multiply(ICX);

        doReturn(BigInteger.valueOf(15_000L).multiply(ICX)).when(bOMM).totalSupply(any());
        doReturn(TWO_THOUSAND).when(bOMM).totalSupplyAt(any());

        doReturn(FIFTY).when(bOMM).balanceOfAt(eq(user1.getAddress()), any());
        doReturn(HUNDRED).when(bOMM).balanceOfAt(eq(user2.getAddress()), any());
        doReturn(TWO_HUNDRED).when(bOMM).balanceOfAt(eq(user3.getAddress()), any());
        doReturn(THREE_HUNDRED).when(bOMM).balanceOfAt(eq(user4.getAddress()), any());
        doReturn(HUNDRED_FIFTY).when(bOMM).balanceOfAt(eq(user5.getAddress()), any());

        // user3 will create a proposal
        byte[] data = createByteArray(name, forum, description, voteStart, methodName);
        score.invoke(OMM_TOKEN_ACCOUNT, "tokenFallback", from, value, data);
        verify(scoreSpy).ProposalCreated(ONE, name, from);

        BigInteger ONE_HOUR = FIVE_MINUTES.multiply(BigInteger.valueOf(12));
        increaseTimeBy(ONE_HOUR);

        score.invoke(user1, "castVote", 1, true);
        verify(scoreSpy).VoteCast(name, true, user1.getAddress(), FIFTY, FIFTY, ZERO);

        // check proposal
        Map<String, ?> proposal = (Map<String, ?>) score.call("checkVote", 1);

        assertEquals(proposal.get("id"), 1);
        assertEquals(proposal.get("for"), exaDivide(FIFTY, TWO_THOUSAND));
        assertEquals(proposal.get("against"), ZERO);
        assertEquals(proposal.get("for_voter_count"), ONE);
        assertEquals(proposal.get("against_voter_count"), ZERO);
        assertEquals(proposal.get("status"), "Active");

        // let the proposal voting end
        BigInteger TWO_DAYS = BigInteger.valueOf(86400L).multiply(BigInteger.valueOf(1000000L))
                .multiply(BigInteger.valueOf(2L));
        increaseTimeBy(TWO_DAYS);

        // check proposal status
        Map<String, ?> proposalAfter = (Map<String, ?>) score.call("checkVote", 1);

        assertEquals(proposalAfter.get("id"), 1);
        assertEquals(proposalAfter.get("for"), exaDivide(FIFTY, TWO_THOUSAND));
        assertEquals(proposalAfter.get("against"), ZERO);
        assertEquals(proposalAfter.get("for_voter_count"), ONE);
        assertEquals(proposalAfter.get("against_voter_count"), ZERO);
        assertEquals(proposalAfter.get("status"), "No Quorum");

        // Set status of proposal to succeeded, lol though it didn't pass quorum
        score.invoke(owner, "setProposalStatus", 1, "Succeeded");
        proposal = (Map<String, ?>) score.call("checkVote", 1);
        assertEquals(proposal.get("status"), "Succeeded");

        // try to execute that proposal now
        score.invoke(owner, "execute_proposal", 1);
        verify(scoreSpy).ActionExecuted(ONE, "Executed");
    }
}