package finance.omm.score.core.delegation.exception;

import finance.omm.utils.exceptions.OMMException;

public class DelegationException extends OMMException.DelegationException {

    public DelegationException(Code c) {
        super(c, c.name());
    }

    public DelegationException(Code c, String message) {
        super(c, message);
    }

    public static DelegationException unknown(String message) {
        return new DelegationException(Code.Unknown, message);
    }

    public static DelegationException notOwner() {
        return new DelegationException(Code.NotOwner, "require owner access");
    }

    public static DelegationException unauthorized(String message) {
        return new DelegationException(Code.Unauthorized, message);
    }

    //OMMException.DelegationException => 30~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1), Unauthorized(2);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
