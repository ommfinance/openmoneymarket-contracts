package finance.omm.score.token.exception;

import finance.omm.utils.exceptions.OMMException;

public class OMMTokenException extends OMMException.OMMToken {

    public OMMTokenException(Code c) {
        super(c, c.name());
    }

    public OMMTokenException(Code c, String message) {
        super(c, message);
    }

    public static OMMTokenException unknown(String message) {
        return new OMMTokenException(Code.Unknown, message);
    }

    public static OMMTokenException notOwner() {
        return new OMMTokenException(Code.NotOwner, "require owner access");
    }

    public static OMMTokenException unauthorized(String msg) {
        return new OMMTokenException(Code.UnAuthorized, msg);
    }

    public static OMMTokenException insufficientBalance(String msg) {
        return new OMMTokenException(Code.Insufficient, msg);
    }

    public static OMMTokenException notSupported(String msg) {
        return new OMMTokenException(Code.NotSupported, msg);
    }

    public static OMMTokenException notPermitted(String msg) {
        return new OMMTokenException(Code.NotPermitted, msg);
    }


    //OMMException.OMMToken => 50 ~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1), UnAuthorized(2), Insufficient(3), NotSupported(4), NotPermitted(5);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
