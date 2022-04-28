package finance.omm.score.core.reward.exception;

import finance.omm.utils.exceptions.OMMException;

public class RewardWeightException extends OMMException.RewardWeightError {

    public RewardWeightException(Code c) {
        super(c, c.name());
    }

    public RewardWeightException(Code c, String message) {
        super(c, message);
    }

    public static RewardWeightException unknown(String message) {
        return new RewardWeightException(Code.Unknown, message);
    }

    public static RewardWeightException notOwner() {
        return new RewardWeightException(Code.NotOwner, "require owner access");
    }

    public static RewardWeightException notAuthorized(String message) {
        return new RewardWeightException(Code.UnAuthorized, message);
    }

    public static RewardWeightException notGovernanceContract() {
        return new RewardWeightException(Code.NotGovernanceContract, "require Governance contract access");
    }

    public static RewardWeightException typeNotExist(String type) {
        return new RewardWeightException(Code.NotValidType, "type is not valid :: " + type);
    }

    public static RewardWeightException invalidTotalPercentage() {
        return new RewardWeightException(Code.NotValidTotalPercentage, "Total percentage is not equals to 100%");
    }

    public static RewardWeightException invalidAsset(String message) {
        return new RewardWeightException(Code.NotValidAsset, message);
    }

    //OMMException.RewardWeightController =>10 ~
    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1), NotGovernanceContract(2), NotValidType(3), NotValidTotalPercentage(4),
        NotValidAsset(5), UnAuthorized(6);

        final int code;

        Code(int code) {
            this.code = code;
        }

        @Override
        public int code() {
            return code;
        }
    }
}
