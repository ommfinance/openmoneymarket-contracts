package finance.omm.score.core.governance.db;

import finance.omm.score.core.governance.enums.ProposalStatus;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;

public class ProposalDB {

    private static final String PREFIX = "ProposalDB_";

    public final DictDB<String, Integer> id;
    public final VarDB<Integer> proposalsCount;
    public final VarDB<Address> proposer;
    public final VarDB<BigInteger> quorum;
    public final VarDB<BigInteger> majority;
    public final VarDB<BigInteger> voteSnapshot;
    public final VarDB<BigInteger> startSnapshot;
    public final VarDB<BigInteger> endSnapshot;
    public final VarDB<String> actions;
    public final VarDB<String> name;
    public final VarDB<String> description;
    public final VarDB<Boolean> active;
    public final DictDB<Address, BigInteger> forVotesOfUser;
    public final DictDB<Address, BigInteger> againstVotesOfUser;
    public final VarDB<BigInteger> totalForVotes;
    public final VarDB<BigInteger> forVotersCount;
    public final VarDB<BigInteger> againstVotersCount;
    public final VarDB<BigInteger> totalAgainstVotes;
    public final VarDB<BigInteger> totalVotingWeight;
    public final VarDB<String> status;
    public final VarDB<BigInteger> fee;
    public final VarDB<Boolean> feeRefunded;
    public final VarDB<String> forumLink;

    private ProposalDB(int varKey) {
        String key = PREFIX + varKey;
        id = Context.newDictDB(PREFIX + "_id", Integer.class);
        proposalsCount = Context.newVarDB(PREFIX + "_proposals_count", Integer.class);
        proposer = Context.newVarDB(key + "_proposer", Address.class);
        quorum = Context.newVarDB(key + "_quorum", BigInteger.class);
        majority = Context.newVarDB(key + "_majority", BigInteger.class);
        voteSnapshot = Context.newVarDB(key + "_vote_snapshot", BigInteger.class);
        startSnapshot = Context.newVarDB(key + "_start_snapshot", BigInteger.class);
        endSnapshot = Context.newVarDB(key + "_end_snapshot", BigInteger.class);
        actions = Context.newVarDB(key + "_actions", String.class);
        name = Context.newVarDB(key + "_name", String.class);
        description = Context.newVarDB(key + "_description", String.class);
        active = Context.newVarDB(key + "_active", Boolean.class);
        forVotesOfUser = Context.newDictDB(key + "_for_votes_of_user", BigInteger.class);
        againstVotesOfUser = Context.newDictDB(key + "_against_votes_of_user", BigInteger.class);
        totalForVotes = Context.newVarDB(key + "_total_for_votes", BigInteger.class);
        forVotersCount = Context.newVarDB(key + "_for_voters_count", BigInteger.class);
        againstVotersCount = Context.newVarDB(key + "_against_voters_count", BigInteger.class);
        totalAgainstVotes = Context.newVarDB(key + "_total_against_votes", BigInteger.class);
        totalVotingWeight = Context.newVarDB(key + "_total_voting_weight", BigInteger.class);
        status = Context.newVarDB(key + "_status", String.class);
        fee = Context.newVarDB(key + "_fee", BigInteger.class);
        feeRefunded = Context.newVarDB(key + "_fee_refunded", Boolean.class);
        forumLink = Context.newVarDB(key + "_forum_link", String.class);
    }


    public static Integer getProposalId(String name) {
        ProposalDB db = new ProposalDB(0);
        return db.id.getOrDefault(name, 0);
    }

    public static Integer getProposalCount() {
        ProposalDB db = new ProposalDB(0);
        return db.proposalsCount.getOrDefault(0);
    }


    public static class ProposalBuilder {

        public final Address proposer;
        public final String name;
        public BigInteger quorum;
        public BigInteger majority;
        public String description;
        public BigInteger snapshot;
        public BigInteger startVote;
        public BigInteger endVote;
        public BigInteger totalVotingWeight;
        public BigInteger fee;
        public String forum;


        public ProposalBuilder(Address proposer, String name) {
            this.proposer = proposer;
            this.name = name;
        }

        public ProposalBuilder setQuorum(BigInteger quorum) {
            this.quorum = quorum;
            return this;
        }

        public ProposalBuilder setMajority(BigInteger majority) {
            this.majority = majority;
            return this;
        }


        public ProposalBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public ProposalBuilder setSnapshot(BigInteger snapshot) {
            this.snapshot = snapshot;
            return this;
        }

        public ProposalBuilder setStartVote(BigInteger startVote) {
            this.startVote = startVote;
            return this;
        }

        public ProposalBuilder setEndVote(BigInteger endVote) {
            this.endVote = endVote;
            return this;
        }

        public ProposalBuilder setTotalVotingWeight(BigInteger totalVotingWeight) {
            this.totalVotingWeight = totalVotingWeight;
            return this;
        }

        public ProposalBuilder setFee(BigInteger fee) {
            this.fee = fee;
            return this;
        }

        public ProposalBuilder setForum(String forum) {
            this.forum = forum;
            return this;
        }

        public ProposalDB build() {
            Integer voteIndex = ProposalDB.getProposalCount() + 1;
            ProposalDB proposal = new ProposalDB(voteIndex);

            proposal.proposalsCount.set(voteIndex);
            proposal.id.set(name, voteIndex);

            proposal.proposer.set(proposer);
            proposal.quorum.set(quorum);
            proposal.majority.set(majority);
            proposal.voteSnapshot.set(snapshot);
            proposal.startSnapshot.set(startVote);
            proposal.endSnapshot.set(endVote);
            proposal.name.set(name);
            proposal.description.set(description);
            proposal.status.set(ProposalStatus.ACTIVE.getStatus());
            proposal.active.set(true);
            proposal.fee.set(fee);
            proposal.feeRefunded.set(false);
            proposal.forumLink.set(forum);

            proposal.forVotersCount.set(BigInteger.ZERO);
            proposal.againstVotersCount.set(BigInteger.ZERO);
            proposal.totalVotingWeight.set(totalVotingWeight);
            validateProposalDB(proposal);
            return proposal;
        }

        private void validateProposalDB(ProposalDB proposal) {
//TODO
        }
    }

    public static ProposalDB getByVoteIndex(int voteIndex) {
        if (voteIndex < 1 || voteIndex > ProposalDB.getProposalCount()) {
            return null;
        }
        return new ProposalDB(voteIndex);
    }


}
