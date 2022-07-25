package finance.omm.score.core.lendingpool.exception;

import finance.omm.utils.exceptions.OMMException;

public class LendingPoolException extends OMMException.LendingPool {

    public LendingPoolException(Code c, String message) {
        super(c, message);
    }

    public LendingPoolException(Code c) {
        super(c, c.name());
    }

    public static LendingPoolException unknown(String message) {
        return new LendingPoolException(Code.Unknown, message);
    }

    public enum Code implements Coded {
        Unknown(0);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
