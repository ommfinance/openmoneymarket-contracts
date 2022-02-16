package finance.omm.score.core.reward.distribution.utils;

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
        return new RewardDistributionException(Code.NotOwner, "Not an owner");
    }

    public static RewardDistributionException notGovernanceContract() {
        return new RewardDistributionException(Code.NotGovernanceContract, "Caller is not Governance contract");
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

    //OMMException.RewardDistribution => 0 ~ 5
    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1), InvalidRecipient(2), InvalidAsset(3), InvalidTotalPercentage(4),
        NotGovernanceContract(5);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

        static public Code of(int code) {
            for (Code c : values()) {
                if (c.code == code) {
                    return c;
                }
            }
            throw new IllegalArgumentException();
        }
    }
}
