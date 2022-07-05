package finance.omm.score.tokens.exception;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;

public class BoostedOMMException extends OMMException.BOMMException {

    public BoostedOMMException(Code c) {
        super(c, c.name());
    }

    public BoostedOMMException(Code c, String message) {
        super(c, message);
    }

    public static BoostedOMMException unknown(String message) {
        return new BoostedOMMException(Code.Unknown, message);
    }

    public static BoostedOMMException unauthorized(String msg) {
        return new BoostedOMMException(Code.Unauthorized, msg);
    }

    public static BoostedOMMException reentrancy(String message) {
        return new BoostedOMMException(Code.RE_ENTRANCY, message);
    }

    public static BoostedOMMException invalidMinimumLockingAmount(BigInteger value) {
        return new BoostedOMMException(Code.MINIMUM_LOCKING_AMOUNT,
                "required minimum " + value.divide(ICX) + " OMM for locking");
    }

    //OMMException.BOMMException => 20~
    public enum Code implements Coded {
        Unknown(0), Unauthorized(1), MINIMUM_LOCKING_AMOUNT(2), RE_ENTRANCY(3);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
