package finance.omm.score.core.adreess.manager.exception;

import finance.omm.utils.exceptions.OMMException;

public class AddressManagerException extends OMMException.AddressManager{

    public AddressManagerException(Coded code, String message) {
        super(code, message);
    }

    public static AddressManagerException notOwner() {
        return new AddressManagerException(Code.NotOwner, "require owner access");
    }

    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1), InvalidRecipient(2), InvalidAsset(3), InvalidTotalPercentage(4),
        NotGovernanceContract(5), NotStakedLp(6), NotLendingPool(7), HandleActionDisabled(8), RewardClaimDisabled(9);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
