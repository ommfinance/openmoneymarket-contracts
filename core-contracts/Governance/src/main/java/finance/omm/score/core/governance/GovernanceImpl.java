package finance.omm.score.core.governance;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertToNumber;
import static finance.omm.utils.math.MathUtils.isValidPercentage;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
import finance.omm.score.core.governance.db.ProposalDB;
import finance.omm.score.core.governance.enums.ProposalStatus;
import finance.omm.score.core.governance.exception.GovernanceException;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;
import scorex.util.ArrayList;

public class GovernanceImpl extends AbstractGovernance {

    public GovernanceImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void setReserveActiveStatus(Address _reserve, boolean _status) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateIsActive(_reserve, _status);
    }

    @External
    public void setReserveFreezeStatus(Address _reserve, boolean _status) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateIsFreezed(_reserve, _status);
    }

    @External
    public void setReserveConstants(ReserveConstant[] _constants) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.setReserveConstants(_constants);
    }

    @External
    public void initializeReserve(ReserveAttributes _reserve) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.addReserveData(_reserve);
    }

    @External
    public void updateBaseLTVasCollateral(Address _reserve, BigInteger _baseLtv) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateBaseLTVasCollateral(_reserve, _baseLtv);
    }

    @External
    public void updateLiquidationThreshold(Address _reserve, BigInteger _liquidationThreshold) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateLiquidationThreshold(_reserve, _liquidationThreshold);
    }

    @External
    public void updateBorrowThreshold(Address _reserve, BigInteger _borrowThreshold) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateBorrowThreshold(_reserve, _borrowThreshold);
    }

    @External
    public void updateLiquidationBonus(Address _reserve, BigInteger _liquidationBonus) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateLiquidationBonus(_reserve, _liquidationBonus);
    }

    @External
    public void updateBorrowingEnabled(Address _reserve, boolean _borrowingEnabled) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateBorrowingEnabled(_reserve, _borrowingEnabled);
    }

    @External
    public void updateUsageAsCollateralEnabled(Address _reserve, boolean _usageAsCollateralEnabled) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        lendingPoolCore.updateUsageAsCollateralEnabled(_reserve, _usageAsCollateralEnabled);
    }

    @External
    public void enableRewardClaim() {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        rewardDistribution.enableRewardClaim();
    }

    @External
    public void disableRewardClaim() {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        rewardDistribution.disableRewardClaim();
    }

    @External
    public void addPools(AssetConfig[] _assetConfigs) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());

        for (AssetConfig assetConfig : _assetConfigs) {
            this.addPool(assetConfig);
        }
    }

    @External
    public void addPool(AssetConfig _assetConfig) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());

        int poolId = _assetConfig.poolID;
        if (poolId > 0) {
            Address asset = _assetConfig.asset;
            stakedLP.addPool(poolId, asset);
        }
        rewardDistribution.configureAssetConfig(_assetConfig);
    }

    @External
    public void removePool(Address _asset) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());

        int poolId = rewardDistribution.getPoolIDByAsset(_asset).intValue();
        if (poolId > 0) {
            stakedLP.removePool(poolId);
        }
        rewardDistribution.removeAssetConfig(_asset);
    }

    @External
    public void transferOmmToDaoFund(BigInteger _value) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        rewardDistribution.transferOmmToDaoFund(_value);
    }

    @External
    public void transferOmmFromDaoFund(BigInteger _value, Address _address) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        daoFund.transferOmm(_value, _address);
    }

    @External
    public void transferFundFromFeeProvider(Address _token, BigInteger _value, Address _to) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        feeProvider.transferFund(_token, _value, _to);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getVotersCount(int vote_index) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        return Map.of(
                "for_voters", proposal.forVotersCount.getOrDefault(BigInteger.ZERO),
                "against_voters", proposal.againstVotersCount.getOrDefault(BigInteger.ZERO)
        );
    }

    /**
     * Sets the vote duration.
     *
     * @param duration - number of days a vote will be active once started
     */
    @External
    public void setVoteDuration(BigInteger duration) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        this.voteDuration.set(duration);
    }

    @External(readonly = true)
    public BigInteger getVoteDuration() {
        return this.voteDuration.getOrDefault(BigInteger.ZERO);
    }

    /**
     * Sets the percentage of the total eligible omm which must participate in a vote for a vote to be valid.
     *
     * @param quorum -  percentage of the total eligible omm required for a vote to be valid
     */
    @External
    public void setQuorum(BigInteger quorum) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        //TODO > 0 and < 1 or >=0 and <=1
        if (!isValidPercentage(quorum)) {
            throw GovernanceException.unknown("Quorum must be between 0 and " + ICX + ".");
        }
        this.quorum.set(quorum);
    }

    @External
    public BigInteger getQuorum() {
        return this.quorum.getOrDefault(BigInteger.ZERO);
    }

    /**
     * Sets the fee for defining votes. Fee in Omm.
     *
     * @param fee - fee for defining votes
     */
    @External
    public void setVoteDefinitionFee(BigInteger fee) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        this.voteDefinitionFee.set(fee);
    }

    @External
    public BigInteger getVoteDefinitionFee() {
        return this.voteDefinitionFee.getOrDefault(BigInteger.ZERO);
    }

    /**
     * Sets the minimum percentage of omm's total supply which a user must have staked in order to define a vote.
     *
     * @param percentage - percent represented in basis points
     */
    @External
    public void setOmmVoteDefinitionCriterion(BigInteger percentage) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        if (!isValidPercentage(percentage)) {
            throw GovernanceException.unknown("vote definition criteria must be between 0 and " + ICX + ".");
        }
        this.ommVoteDefinitionCriterion.set(percentage);
    }

    @External
    public BigInteger getOmmVoteDefinitionCriterion() {
        return this.ommVoteDefinitionCriterion.getOrDefault(BigInteger.ZERO);
    }

    /**
     * Cancels a vote, in case a mistake was made in its definition.
     *
     * @param vote_index - vote index
     */
    @External
    public void cancelVote(int vote_index) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }
        Address owner = Context.getOwner();
        Address sender = Context.getCaller();

        List<Address> eligibleAddresses = List.of(
                proposal.proposer.get(), owner
        );
        if (!eligibleAddresses.contains(sender)) {
            throw GovernanceException.unauthorized("Only owner or proposer may call this method.");
        }
        if (proposal.startSnapshot.get().compareTo(TimeConstants.getBlockTimestamp()) <= 0
                && !owner.equals(sender)) {
            throw GovernanceException.unauthorized("Only owner can cancel a vote that has started.");
        }

        if (!proposal.status.get().equals(ProposalStatus.ACTIVE.getStatus())) {
            throw GovernanceException.unknown("Omm Governance: Proposal can be cancelled only from active status.");
        }

        this.refundVoteDefinitionFee(proposal);
        proposal.active.set(Boolean.FALSE);
        proposal.status.set(ProposalStatus.CANCELLED.getStatus());
    }

    @External(readonly = true)
    public BigInteger maxActions() {
        return MAX_ACTIONS;
    }

    @External(readonly = true)
    public int getProposalCount() {
        return ProposalDB.getProposalCount();
    }

    @External(readonly = true)
    public List<Map<String, ?>> getProposals(int batch_size, int offset) {
        List<Map<String, ?>> proposals = new ArrayList<>();
        int start = Math.max(1, offset);
        int end = Math.min(batch_size + start - 1, getProposalCount());
        for (int i = start; i <= end; i++) {
            proposals.add(checkVote(i));
        }
        return proposals;
    }

    @External
    public void updateVoteForum(int vote_index, String forum) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }
        proposal.forumLink.set(forum);
    }

    /**
     * Casts a vote in the named poll.
     *
     * @param vote_index
     * @param vote
     */
    @External
    public void castVote(int vote_index, boolean vote) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }
        BigInteger start = proposal.startSnapshot.get();
        BigInteger end = proposal.endSnapshot.get();
        BigInteger now = TimeConstants.getBlockTimestamp();

        if ((now.compareTo(start) <= 0 || now.compareTo(end) >= 0) && !proposal.active.get()) {
            throw GovernanceException.proposalNotActive(vote_index);
        }
        Address sender = Context.getCaller();
        BigInteger snapshot = proposal.voteSnapshot.get();

        BigInteger stake = ommToken.stakedBalanceOfAt(sender, snapshot);
        if (stake.equals(BigInteger.ZERO)) {
            throw GovernanceException.unknown("Omm tokens need to be staked to cast the vote.");

        }
        BigInteger priorForVote = proposal.forVotesOfUser.getOrDefault(sender, BigInteger.ZERO);
        BigInteger priorAgainstVote = proposal.againstVotesOfUser.getOrDefault(sender, BigInteger.ZERO);

        BigInteger totalForVotes = proposal.totalForVotes.getOrDefault(BigInteger.ZERO);
        BigInteger totalAgainstVotes = proposal.totalAgainstVotes.getOrDefault(BigInteger.ZERO);

        BigInteger forVotersCount = proposal.forVotersCount.getOrDefault(BigInteger.ZERO);
        BigInteger againstVotersCount = proposal.againstVotersCount.getOrDefault(BigInteger.ZERO);

        BigInteger totalFor;
        BigInteger totalAgainst;

        boolean isFirstTimeVote = priorForVote.equals(BigInteger.ZERO) && priorAgainstVote.equals(BigInteger.ZERO);
        if (vote) {
            proposal.forVotesOfUser.set(sender, stake);
            proposal.againstVotesOfUser.set(sender, BigInteger.ZERO);

            totalFor = totalForVotes.add(stake).subtract(priorForVote);
            totalAgainst = totalAgainstVotes.subtract(priorAgainstVote);

            if (isFirstTimeVote) {
                proposal.forVotersCount.set(forVotersCount.add(BigInteger.ONE));
            } else if (!priorAgainstVote.equals(BigInteger.ZERO)) {
                proposal.forVotersCount.set(forVotersCount.add(BigInteger.ONE));
                proposal.againstVotersCount.set(againstVotersCount.subtract(BigInteger.ONE));
            }

        } else {
            proposal.againstVotesOfUser.set(sender, stake);
            proposal.forVotesOfUser.set(sender, BigInteger.ZERO);

            totalAgainst = totalAgainstVotes.add(stake).subtract(priorAgainstVote);
            totalFor = totalForVotes.subtract(priorForVote);

            if (isFirstTimeVote) {
                proposal.againstVotersCount.set(againstVotersCount.add(BigInteger.ONE));
            } else if (!priorAgainstVote.equals(BigInteger.ZERO)) {
                proposal.againstVotersCount.set(againstVotersCount.add(BigInteger.ONE));
                proposal.forVotersCount.set(forVotersCount.subtract(BigInteger.ONE));
            }
        }
        proposal.totalForVotes.set(totalFor);
        proposal.totalAgainstVotes.set(totalAgainst);
        VoteCast(proposal.name.get(), vote, sender, stake, totalFor, totalAgainst);
    }


    /**
     * Evaluates a vote after the voting period is done. If the vote passed, any actions included in the proposal are
     * executed. The vote definition fee is also refunded to the proposer if the vote passed.
     *
     * @param vote_index
     * @return ProposalDB
     */

    private ProposalDB evaluateVote(int vote_index) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }

        BigInteger end = proposal.endSnapshot.get();

        if (TimeConstants.getBlockTimestamp().compareTo(end) < 0) {
            throw GovernanceException.unknown("Voting period has not ended");
        }

        if (!proposal.active.get()) {
            throw GovernanceException.unknown("This proposal is not active.");
        }

        Map<String, ?> result = this.checkVote(vote_index);
        String status = (String) result.get("status");

        proposal.status.set(status);
        if (status.equals(ProposalStatus.SUCCEEDED.getStatus())) {
            this.refundVoteDefinitionFee(proposal);
        }
        proposal.active.set(Boolean.FALSE);
        return proposal;
    }

    @External
    public void execute_proposal(int vote_index) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        ProposalDB proposal = evaluateVote(vote_index);
        String status = proposal.status.get();
        if (ProposalStatus.SUCCEEDED.getStatus().equals(status)) {
            status = ProposalStatus.EXECUTED.getStatus();
            proposal.status.set(status);
        }
        ActionExecuted(BigInteger.valueOf(vote_index), status);
    }

    @External
    public void setProposalStatus(int vote_index, String _status) {
        onlyOwnerOrElseThrow(GovernanceException.notOwner());
        ProposalStatus status = ProposalStatus.get(_status);
        if (status == null) {
            throw GovernanceException.unknown("invalid proposal status " + _status);
        }
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }
        proposal.status.set(status.getStatus());
    }

    @External
    public int getVoteIndex(String _name) {
        return ProposalDB.getProposalId(_name);
    }

    @External(readonly = true)
    public Map<String, ?> checkVote(int _vote_index) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(_vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(_vote_index);
        }
        BigInteger totalOMMStaked = ommToken.totalStakedBalanceOfAt(proposal.voteSnapshot.get());

        BigInteger totalForVoted = proposal.totalForVotes.getOrDefault(BigInteger.ZERO);
        BigInteger totalAgainstVotes = proposal.totalAgainstVotes.getOrDefault(BigInteger.ZERO);

        BigInteger _for = MathUtils.exaDivide(totalForVoted, totalOMMStaked);
        BigInteger _against = MathUtils.exaDivide(totalAgainstVotes, totalOMMStaked);

        String status = proposal.status.get();
        BigInteger majority = proposal.majority.get();
        BigInteger quorum = proposal.quorum.get();

        BigInteger end = proposal.endSnapshot.get();
        BigInteger start = proposal.startSnapshot.get();
        BigInteger snapshot = proposal.voteSnapshot.get();

        if (status.equals(ProposalStatus.ACTIVE.getStatus()) && TimeConstants.getBlockTimestamp().compareTo(end) >= 0) {
            if (_for.add(_against).compareTo(quorum) < 0) {
                status = ProposalStatus.NO_QUORUM.getStatus();
            } else if (ICX.subtract(majority).multiply(_for).compareTo(majority.multiply(_against)) > 0) {
                status = ProposalStatus.SUCCEEDED.getStatus();
            } else {
                status = ProposalStatus.DEFEATED.getStatus();
            }
        }

        return Map.ofEntries(
                Map.entry("id", _vote_index),
                Map.entry("name", proposal.name.get()),
                Map.entry("proposer", proposal.proposer.get()),
                Map.entry("description", proposal.description.get()),
                Map.entry("majority", majority),
                Map.entry("vote snapshot", snapshot),
                Map.entry("start day", start),
                Map.entry("end day", end),
                Map.entry("quorum", quorum),
                Map.entry("for", _for),
                Map.entry("against", _against),
                Map.entry("for_voter_count", proposal.forVotersCount.get()),
                Map.entry("against_voter_count", proposal.againstVotersCount.get()),
                Map.entry("forum", proposal.forumLink.get()),
                Map.entry("status", status)
        );
    }

    @External(readonly = true)
    public Map<String, ?> getVotesOfUser(int vote_index, Address user) {
        ProposalDB proposal = ProposalDB.getByVoteIndex(vote_index);
        if (proposal == null) {
            throw GovernanceException.proposalNotFound(vote_index);
        }
        return Map.of(
                "for", proposal.forVotesOfUser.getOrDefault(user, BigInteger.ZERO),
                "against", proposal.againstVotesOfUser.getOrDefault(user, BigInteger.ZERO)
        );
    }

    @External(readonly = true)
    public BigInteger myVotingWeight(Address _address, BigInteger _timestamp) {
        return ommToken.stakedBalanceOfAt(_address, _timestamp);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        BigInteger voteFee = this.voteDefinitionFee.get();

        if (!Context.getCaller().equals(getAddress(Contracts.OMM_TOKEN.getKey()))) {
            throw GovernanceException.unknown("invalid token sent");
        }
        if (_value.compareTo(voteFee) < 0) {
            throw GovernanceException.insufficientFee();
        }
        String data = new String(_data);
        JsonObject json = Json.parse(data).asObject();

        String method = json.get("method").asString();
        JsonObject params = json.get("params").asObject();

        if (!method.equals("defineVote") || params == null) {
            throw GovernanceException.unknown("No valid method called :: " + data);
        }
        String name = params.getString("name", null);
        String forum = params.getString("forum", "null");
        String description = params.getString("description", null);
        BigInteger voteStart = convertToNumber(params.get("vote_start"));
        BigInteger snapshot = convertToNumber(params.get("snapshot"));

        defineVote(name, description, voteStart, snapshot, _from, forum);

        ommToken.transfer(getAddress(Contracts.DAO_FUND.getKey()), voteFee, null);
        BigInteger remainingOMMToken = _value.subtract(voteFee);
        if (remainingOMMToken.compareTo(BigInteger.ZERO) > 0) {
            ommToken.transfer(_from, remainingOMMToken, null);
        }
    }

}
