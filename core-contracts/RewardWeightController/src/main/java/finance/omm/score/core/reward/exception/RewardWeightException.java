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

    public static RewardWeightException notValidType(String typeId) {
        return new RewardWeightException(Code.NotGovernanceContract, "type id is not valid :: " + typeId);
    }

    public static RewardWeightException invalidTotalPercentage() {
        return new RewardWeightException(Code.NotValidTotalPercentage, "Total percentage is not equals to 100%");
    }

    public static RewardWeightException invalidAsset(String message) {
        return new RewardWeightException(Code.NotValidAsset, message);
    }

    //OMMException.RewardController => 0 ~ 5
    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1), NotGovernanceContract(2), NotValidTypeID(3), NotValidTotalPercentage(4),
        NotValidAsset(5), UnAuthorized(6);

        final int code;

        Code(int code) {
            this.code = code;
        }

        @Override
        public int code() {
            return code;
        }

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
