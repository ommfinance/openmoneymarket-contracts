package finance.omm.score.core.governance;

import finance.omm.core.score.interfaces.DAOFund;
import finance.omm.core.score.interfaces.DAOFundClient;
import finance.omm.core.score.interfaces.FeeProviderClient;
import finance.omm.core.score.interfaces.Governance;
import finance.omm.core.score.interfaces.LendingPoolCoreClient;
import finance.omm.core.score.interfaces.OMMToken;
import finance.omm.core.score.interfaces.OMMTokenClient;
import finance.omm.core.score.interfaces.StakedLPClient;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.governance.db.ProposalDB;
import finance.omm.score.core.governance.exception.GovernanceException;
import finance.omm.score.core.governance.interfaces.RewardDistributionImplClient;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.constants.TimeConstants.Timestamp;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;

public abstract class AbstractGovernance extends AddressProvider implements Governance,
        Authorization<GovernanceException> {

    public static final String TAG = "Governance Manager";

    public static final BigInteger MAJORITY = BigInteger.valueOf(666666666666666667L);
    public static final BigInteger MAX_ACTIONS = BigInteger.valueOf(5L);

    public final VarDB<BigInteger> voteDuration = Context.newVarDB("vote_duration", BigInteger.class);
    public final VarDB<BigInteger> ommVoteDefinitionCriterion = Context.newVarDB("min_omm", BigInteger.class);
    public final VarDB<BigInteger> voteDefinitionFee = Context.newVarDB("definition_fee", BigInteger.class);
    public final VarDB<BigInteger> quorum = Context.newVarDB("quorum", BigInteger.class);



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
     * @param snapshot
     * @param proposer
     * @param forum
     */
    protected void defineVote(String name, String description, BigInteger voteStart, BigInteger snapshot,
            Address proposer, String forum) {
        if (description.length() > 500) {
            throw GovernanceException.invalidVoteParams("Description must be less than or equal to 500 characters.");
        }
        BigInteger currentTimestamp = TimeConstants.getBlockTimestamp();

        TimeConstants.checkIsValidTimestamp(voteStart, Timestamp.MICRO_SECONDS,
                GovernanceException.invalidVoteParams("vote_start timestamp should be in microseconds"));
        TimeConstants.checkIsValidTimestamp(snapshot, Timestamp.MICRO_SECONDS,
                GovernanceException.invalidVoteParams("snapshot start timestamp should be in microseconds"));

        if (voteStart.compareTo(currentTimestamp) <= 0) {
            throw GovernanceException.invalidVoteParams("Vote cannot start at or before the current timestamp.");
        }

        if (currentTimestamp.compareTo(snapshot) > 0 || snapshot.compareTo(voteStart) >= 0) {
            throw GovernanceException.invalidVoteParams(
                    "The reference snapshot must be in the range: [current_time (" + currentTimestamp
                            + "), start_time  (" + voteStart + ")].");
        }

        int voteIndex = ProposalDB.getProposalId(name);
        if (voteIndex > 0) {
            throw GovernanceException.invalidVoteParams("Proposal name (" + name + ") has already been used.");
        }

        OMMToken ommToken = getInstance(OMMToken.class, Contracts.OMM_TOKEN);

        BigInteger ommTotalSupply = ommToken.totalSupply();
        BigInteger userStakedBalance = ommToken.stakedBalanceOfAt(proposer, snapshot);

        BigInteger ommCriterion = this.ommVoteDefinitionCriterion.get();

        if (MathUtils.exaDivide(userStakedBalance, ommTotalSupply).compareTo(ommCriterion) < 0) {
            throw GovernanceException.insufficientStakingBalance(ommCriterion);
        }

        ProposalDB proposal = new ProposalDB.ProposalBuilder(proposer, name)
                .setDescription(description)
                .setQuorum(quorum.get())
                .setMajority(MAJORITY)
                .setSnapshot(snapshot)
                .setStartVote(voteStart)
                .setEndVote(voteStart.add(voteDuration.get()))
                .setFee(voteDefinitionFee.get())
                .setForum(forum).build();

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
        }
        return null;
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

}
