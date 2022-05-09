package finance.omm.score.core.governance;

import finance.omm.core.score.interfaces.DAOFund;
import finance.omm.core.score.interfaces.DAOFundClient;
import finance.omm.core.score.interfaces.FeeProvider;
import finance.omm.core.score.interfaces.FeeProviderClient;
import finance.omm.core.score.interfaces.Governance;
import finance.omm.core.score.interfaces.LendingPoolCore;
import finance.omm.core.score.interfaces.LendingPoolCoreClient;
import finance.omm.core.score.interfaces.OMMToken;
import finance.omm.core.score.interfaces.OMMTokenClient;
import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.core.score.interfaces.StakedLPClient;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.governance.db.ProposalDB;
import finance.omm.score.core.governance.exception.GovernanceException;
import finance.omm.score.core.governance.interfaces.GovernanceEventLogs;
import finance.omm.score.core.governance.interfaces.RewardDistributionImpl;
import finance.omm.score.core.governance.interfaces.RewardDistributionImplClient;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.constants.TimeConstants.Timestamp;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.VarDB;

public abstract class AbstractGovernance extends AddressProvider implements Governance, GovernanceEventLogs,
        Authorization<GovernanceException> {

    public static final String TAG = "Governance Manager";

    public static final BigInteger MAJORITY = BigInteger.valueOf(666666666666666667L);
    public static final BigInteger MAX_ACTIONS = BigInteger.valueOf(5L);

    public final VarDB<BigInteger> voteDuration = Context.newVarDB("vote_duration", BigInteger.class);
    public final VarDB<BigInteger> ommVoteDefinitionCriterion = Context.newVarDB("min_omm", BigInteger.class);
    public final VarDB<BigInteger> voteDefinitionFee = Context.newVarDB("definition_fee", BigInteger.class);
    public final VarDB<BigInteger> quorum = Context.newVarDB("quorum", BigInteger.class);


    public LendingPoolCore lendingPoolCore;
    public RewardDistributionImpl rewardDistribution;
    public StakedLP stakedLP;
    public DAOFund daoFund;
    public FeeProvider feeProvider;
    public OMMToken ommToken;

    public AbstractGovernance(Address addressProvider, boolean _update) {
        super(addressProvider, _update);

        lendingPoolCore = instanceFactory(LendingPoolCoreClient.class, Contracts.LENDING_POOL_CORE);
        rewardDistribution = instanceFactory(RewardDistributionImplClient.class, Contracts.REWARDS);
        stakedLP = instanceFactory(StakedLPClient.class, Contracts.STAKED_LP);
        daoFund = instanceFactory(DAOFundClient.class, Contracts.DAO_FUND);
        feeProvider = instanceFactory(FeeProviderClient.class, Contracts.FEE_PROVIDER);
        ommToken = instanceFactory(OMMTokenClient.class, Contracts.OMM_TOKEN);
    }

    protected void refundVoteDefinitionFee(ProposalDB proposal) {
        if (!proposal.feeRefunded.getOrDefault(Boolean.FALSE)) {
            proposal.feeRefunded.set(Boolean.TRUE);
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

        if (snapshot.compareTo(currentTimestamp) < 0 || voteStart.compareTo(snapshot) < 0) {
            throw GovernanceException.invalidVoteParams(
                    "The reference snapshot must be in the range: [current_time (" + currentTimestamp
                            + "), start_time  (" + voteStart + ")].");
        }

        int voteIndex = ProposalDB.getProposalId(name);
        if (voteIndex > 0) {
            throw GovernanceException.invalidVoteParams("Proposal name (" + name + ") has already been used.");
        }

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


    protected <T> T instanceFactory(Class<T> clazz, Contracts contract) {
        switch (contract) {
            case LENDING_POOL_CORE:
                return clazz.cast(new LendingPoolCoreClient(
                        this.getAddress(Contracts.LENDING_POOL_CORE.getKey())));
            case REWARDS:
                return clazz.cast(new RewardDistributionImplClient(
                        this.getAddress(Contracts.REWARDS.getKey())));
            case OMM_TOKEN:
                return clazz.cast(new OMMTokenClient(
                        this.getAddress(Contracts.OMM_TOKEN.getKey())));
            case FEE_PROVIDER:
                return clazz.cast(new FeeProviderClient(
                        this.getAddress(Contracts.FEE_PROVIDER.getKey())));
            case STAKED_LP:
                return clazz.cast(new StakedLPClient(
                        this.getAddress(Contracts.STAKED_LP.getKey())));
            case DAO_FUND:
                return clazz.cast(new DAOFundClient(
                        this.getAddress(Contracts.DAO_FUND.getKey())));
        }
        return null;
    }

    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

}
