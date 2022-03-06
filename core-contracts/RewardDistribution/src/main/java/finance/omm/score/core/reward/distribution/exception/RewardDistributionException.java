package finance.omm.score.core.reward.distribution.exception;

import finance.omm.utils.exceptions.OMMException;

public class RewardDistributionException extends OMMException.RewardDistribution {

    public RewardDistributionException(Code c) {
        super(c, c.name());
    }

    public RewardDistributionException(Code c, String message) {
        super(c, message);
    }

    public static RewardDistributionException unknown(String message) {
        return new RewardDistributionException(Code.Unknown, message);
    }

    public static RewardDistributionException notOwner() {
        return new RewardDistributionException(Code.NotOwner, "require owner access");
    }

    public static RewardDistributionException notGovernanceContract() {
        return new RewardDistributionException(Code.NotGovernanceContract, "require Governance contract access");
    }

    public static RewardDistributionException invalidRecipient(String message) {
        return new RewardDistributionException(Code.InvalidRecipient, message);
    }

    public static RewardDistributionException invalidAsset(String message) {
        return new RewardDistributionException(Code.InvalidAsset, message);
    }

    public static RewardDistributionException invalidTotalPercentage(String message) {
        return new RewardDistributionException(Code.InvalidTotalPercentage, message);
    }

    public static RewardDistributionException notStakedLp() {
        return new RewardDistributionException(Code.NotStakedLp, "only staked lp contract can call");
    }

    public static RewardDistributionException notLendingPool() {
        return new RewardDistributionException(Code.NotLendingPool, "only lending pool contract can call");

    }

    //OMMException.RewardDistribution => 10 ~
    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1), InvalidRecipient(2), InvalidAsset(3), InvalidTotalPercentage(4),
        NotGovernanceContract(5), NotStakedLp(6), NotLendingPool(7);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
