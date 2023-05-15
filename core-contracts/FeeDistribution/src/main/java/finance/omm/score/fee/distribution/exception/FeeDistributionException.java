package finance.omm.score.fee.distribution.exception;

import finance.omm.utils.exceptions.OMMException;

public class FeeDistributionException extends OMMException.FeeDistributionException {
    public FeeDistributionException(int code, String message) {
        super(code, message);
    }

    public FeeDistributionException(Coded code, String message) {
        super(code, message);
    }
    public static FeeDistributionException notOwner() {
        return new FeeDistributionException(Code.NotOwner, "require owner access");
    }
    public static FeeDistributionException unauthorized() {
        return new FeeDistributionException(Code.Unauthorized, "Token Fallback: Only sicx contract is allowed to call");
    }

    //OMMException.FeeDistribution => 85~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1), Unauthorized(2);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
