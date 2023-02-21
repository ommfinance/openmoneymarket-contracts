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

    public static LendingPoolException notOwner() {
        return new LendingPoolException(Code.NotOwner, "require owner access");
    }

    public static LendingPoolException reserveNotActive(String msg) {
        return new LendingPoolException(Code.ReserveNotActive, msg);
    }

    public static LendingPoolException reserveNotValid(String msg) {
        return new LendingPoolException(Code.ReserveNotValid, msg);
    }

    public static LendingPoolException liquidationDisabled(String msg) {
        return new LendingPoolException(Code.LiquidationDisabled, msg);
    }


    public enum Code implements Coded {
        Unknown(0), NotOwner(1), ReserveNotActive(2), ReserveNotValid(3), LiquidationDisabled(4);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
