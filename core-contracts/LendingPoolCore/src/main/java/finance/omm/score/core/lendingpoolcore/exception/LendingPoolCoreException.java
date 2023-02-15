package finance.omm.score.core.lendingpoolcore.exception;

import finance.omm.utils.exceptions.OMMException;
import score.Address;

public class LendingPoolCoreException extends OMMException.LendingPoolCore {

    public LendingPoolCoreException(Code c, String message) {
        super(c, message);
    }

    public LendingPoolCoreException(Code c) {
        super(c, c.name());
    }

    public static LendingPoolCoreException unknown(String message) {
        return new LendingPoolCoreException(Code.Unknown, message);
    }


    public static LendingPoolCoreException reserveNotActive(String msg) {
        return new LendingPoolCoreException(Code.ReserveNotActive, msg);
    }

    public static LendingPoolCoreException unauthorized(String msg) {
        return new LendingPoolCoreException(Code.UnAuthorized, msg);
    }

    public static LendingPoolCoreException invalidReserve(Address _reserve) {
        return new LendingPoolCoreException(Code.InvalidReserve, "Invalid reserve :: " + _reserve.toString());
    }


    public enum Code implements Coded {
        Unknown(0), InvalidReserve(1), ReserveNotActive(2), UnAuthorized(3);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
