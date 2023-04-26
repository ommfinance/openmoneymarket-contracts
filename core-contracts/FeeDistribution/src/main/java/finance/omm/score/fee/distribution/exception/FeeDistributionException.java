package finance.omm.score.fee.distribution.exception;

import finance.omm.utils.exceptions.OMMException;

public class FeeDistributionException extends OMMException.FeeDistributionException {
    public FeeDistributionException(int code, String message) {
        super(code, message);
    }

    public FeeDistributionException(Coded code, String message) {
        super(code, message);
    }
    public static DelegationException notOwner() {
        return new DelegationException(Code.NotOwner, "require owner access");
    }

    //OMMException.FeeDistribution => 85~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
