package finance.omm.score.core.governance;

import finance.omm.core.score.interfaces.BoostedToken;
import finance.omm.core.score.interfaces.BoostedTokenClient;
import finance.omm.core.score.interfaces.DAOFund;
import finance.omm.core.score.interfaces.DAOFundClient;
import finance.omm.core.score.interfaces.FeeProviderClient;
import finance.omm.core.score.interfaces.Governance;
import finance.omm.core.score.interfaces.LendingPoolCoreClient;
import finance.omm.core.score.interfaces.OMMTokenClient;
import finance.omm.core.score.interfaces.RewardWeightControllerClient;
import finance.omm.core.score.interfaces.StakedLPClient;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.governance.db.ProposalDB;
import finance.omm.score.core.governance.exception.GovernanceException;
import finance.omm.score.core.governance.interfaces.RewardDistributionImplClient;
import finance.omm.score.core.governance.utils.ArbitraryCallManager;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.constants.TimeConstants.Timestamp;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;

public abstract class AbstractGovernance extends AddressProvider implements Governance,
        Authorization<GovernanceException> {

    public static final String TAG = "Governance Manager";

    public static final int SUCCESSFUL_VOTE_EXECUTION_REVERT_ID = 25;

    public static final BigInteger MAJORITY = BigInteger.valueOf(666666666666666667L);
    public static final BigInteger MAX_ACTIONS = BigInteger.valueOf(5L);

    public final VarDB<BigInteger> voteDuration = Context.newVarDB("vote_duration", BigInteger.class);
    public final VarDB<BigInteger> boostedOmmVoteDefinitionCriterion = Context.newVarDB("min_boosted_omm", BigInteger.class);
    public final VarDB<BigInteger> voteDefinitionFee = Context.newVarDB("definition_fee", BigInteger.class);
    public final VarDB<BigInteger> quorum = Context.newVarDB("quorum", BigInteger.class);

    public ArbitraryCallManager callManager = new ArbitraryCallManager();


    public AbstractGovernance(Address addressProvider, boolean _update) {
        super(addressProvider, _update);

    }

    @EventLog(indexed = 2)
    public void ProposalCreated(BigInteger vote_index, String name, Address proposer) {}

    @EventLog(indexed = 2)
    public void VoteCast(String vote_name, boolean vote, Address voter, BigInteger stake, BigInteger total_for,
            BigInteger total_against) {}

    @EventLog(indexed = 2)
    public void ActionExecuted(BigInteger vote_index, String vote_status) {}


    protected void refundVoteDefinitionFee(ProposalDB proposal) {
        if (!proposal.feeRefunded.getOrDefault(Boolean.FALSE)) {
            proposal.feeRefunded.set(Boolean.TRUE);

            DAOFund daoFund = getInstance(DAOFund.class, Contracts.DAO_FUND);
            daoFund.transferOmm(proposal.fee.getOrDefault(BigInteger.ZERO), proposal.proposer.get());
        }
    }

    /**
     * Defines a new vote and which actions are to be executed if it is successful.
     *
     * @param name
     * @param description
     * @param voteStart
     * @param proposer
     * @param forum
     */
    protected void defineVote(String name, String description, BigInteger voteStart, Address proposer, String forum, String transactions) {
        if (description.length() > 500) {
            throw GovernanceException.invalidVoteParams("Description must be less than or equal to 500 characters.");
        }
        BigInteger snapshot = BigInteger.valueOf(Context.getBlockHeight()); // self.block_height
        BigInteger currentTimestamp = TimeConstants.getBlockTimestamp();

        TimeConstants.checkIsValidTimestamp(voteStart, Timestamp.MICRO_SECONDS,
                GovernanceException.invalidVoteParams("vote_start timestamp should be in microseconds"));

        if (voteStart.compareTo(currentTimestamp) < 0) {
            throw GovernanceException.invalidVoteParams("Vote cannot start before the current timestamp");
        }


        int voteIndex = ProposalDB.getProposalId(name);
        if (voteIndex > 0) {
            throw GovernanceException.invalidVoteParams("Proposal name (" + name + ") has already been used.");
        }


        BoostedToken boostedToken = getInstance(BoostedToken.class,Contracts.BOOSTED_OMM);
        BigInteger userBommBalance = boostedToken.balanceOfAt(proposer,snapshot);
        BigInteger bommTotal = boostedToken.totalSupplyAt(snapshot);
        BigInteger bommCriterion = getBoostedOmmVoteDefinitionCriterion();

        if (MathUtils.exaDivide(userBommBalance, bommTotal).compareTo(bommCriterion) < 0) {
            throw GovernanceException.insufficientbOMMBalance(bommCriterion);
        }
        verifyTransactions(transactions);

        ProposalDB proposal = new ProposalDB.ProposalBuilder(proposer, name)
                .setDescription(description)
                .setQuorum(quorum.get())
                .setMajority(MAJORITY)
                .setSnapshot(snapshot)
                .setStartVote(voteStart)
                .setEndVote(voteStart.add(voteDuration.get()))
                .setTotalVotingWeight(bommTotal)
                .setFee(voteDefinitionFee.get())
                .setForum(forum)
                .setTransaction(transactions).build();

        this.ProposalCreated(BigInteger.valueOf(proposal.id.get(name)), name, proposer);
    }


    public <T> T getInstance(Class<T> clazz, Contracts contract) {
        switch (contract) {
            case LENDING_POOL_CORE:
                return clazz.cast(new LendingPoolCoreClient(
                        this.getAddress(contract.getKey())));
            case REWARDS:
                return clazz.cast(new RewardDistributionImplClient(
                        this.getAddress(contract.getKey())));
            case REWARD_WEIGHT_CONTROLLER:
                return clazz.cast(new RewardWeightControllerClient(
                        this.getAddress(contract.getKey())));
            case OMM_TOKEN:
                return clazz.cast(new OMMTokenClient(
                        this.getAddress(contract.getKey())));
            case FEE_PROVIDER:
                return clazz.cast(new FeeProviderClient(
                        this.getAddress(contract.getKey())));
            case STAKED_LP:
                return clazz.cast(new StakedLPClient(
                        this.getAddress(contract.getKey())));
            case DAO_FUND:
                return clazz.cast(new DAOFundClient(
                        this.getAddress(contract.getKey())));
            case BOOSTED_OMM:
                return clazz.cast(new BoostedTokenClient(
                        this.getAddress(contract.getKey())));
        }
        return null;
    }

    private static void verifyTransactions(String transactions) {
        try {
            Context.call(Context.getAddress(), "tryExecuteTransactions", transactions);
        } catch (UserRevertedException e) {
            Context.require(e.getCode() == SUCCESSFUL_VOTE_EXECUTION_REVERT_ID, "Vote execution failed");
        }
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

}
