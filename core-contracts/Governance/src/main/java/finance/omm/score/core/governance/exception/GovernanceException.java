package finance.omm.score.core.governance.exception;

import finance.omm.utils.exceptions.OMMException;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;

public class GovernanceException extends OMMException.Governance {

    public GovernanceException(Code c) {
        super(c, c.name());
    }

    public GovernanceException(Code c, String message) {
        super(c, message);
    }

    public static GovernanceException unknown(String message) {
        return new GovernanceException(Code.Unknown, message);
    }

    public static GovernanceException notOwner() {
        return new GovernanceException(Code.NotOwner, "require owner access");
    }

    public static GovernanceException unauthorized(String msg) {
        return new GovernanceException(Code.UnAuthorized, msg);
    }

    public static GovernanceException invalidVoteParams(String msg) {
        return new GovernanceException(Code.InvalidVotingDate, msg);
    }

    public static GovernanceException insufficientStakingBalance(BigInteger value) {
        return new GovernanceException(Code.InsufficientStakingBalance,
                "User needs at least " + MathUtils.percentageInHundred(value)
                        + "% of total omm supply staked to define a vote.");
    }

    public static GovernanceException proposalNotFound(int voteIndex) {
        return new GovernanceException(Code.ProposalNotFound, "Proposal not found with index :: " + voteIndex);

    }

    public static GovernanceException proposalNotActive(int voteIndex) {
        return new GovernanceException(Code.ProposalNotActive, "Proposal is not active index :: " + voteIndex);

    }

    public static GovernanceException insufficientFee() {
        return new GovernanceException(Code.ProposalNotActive, "Insufficient fee to create proposal");
    }


    //OMMException.RewardDistribution => 30 ~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1), UnAuthorized(2), InvalidVotingDate(3),
        InsufficientStakingBalance(4), ProposalNotFound(5), ProposalNotActive(6), InsufficientFee(7);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
