package finance.omm.score.core.oracle.exception;

import finance.omm.utils.exceptions.OMMException;

public class PriceOracleException extends OMMException.PriceOracleException {

    public PriceOracleException(Code c) {
        super(c, c.name());
    }

    public PriceOracleException(Code c, String message) {
        super(c, message);
    }

    public static PriceOracleException notOwner() {
        return new PriceOracleException(Code.NotOwner, "require owner access");
    }

    public enum Code implements Coded {
        Unknown(0), NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
