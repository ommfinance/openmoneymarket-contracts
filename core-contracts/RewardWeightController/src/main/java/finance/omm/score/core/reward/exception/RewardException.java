package finance.omm.score.core.reward.exception;

import finance.omm.utils.exceptions.OMMException;

public class RewardException extends OMMException.RewardController {

    public RewardException(Code c) {
        super(c, c.name());
    }

    public RewardException(Code c, String message) {
        super(c, message);
    }

    public static RewardException unknown(String message) {
        return new RewardException(Code.Unknown, message);
    }

    public static RewardException notOwner() {
        return new RewardException(Code.NotOwner, "require owner access");
    }

    public static RewardException notAuthorized(String message) {
        return new RewardException(Code.UnAuthorized, message);
    }

    public static RewardException notGovernanceContract() {
        return new RewardException(Code.NotGovernanceContract, "require Governance contract access");
    }

    public static RewardException notValidTypeId(String typeId) {
        return new RewardException(Code.NotGovernanceContract, "type id is not valid :: " + typeId);
    }

    public static RewardException invalidTotalPercentage() {
        return new RewardException(Code.NotValidTotalPercentage, "Total percentage is not equals to 100%");
    }

    public static RewardException invalidAsset(String message) {
        return new RewardException(Code.NotValidAsset, message);
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
