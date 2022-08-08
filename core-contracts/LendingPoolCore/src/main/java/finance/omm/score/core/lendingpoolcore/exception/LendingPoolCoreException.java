package finance.omm.score.core.lendingpoolcore.exception;

import finance.omm.utils.exceptions.OMMException;

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

    public static LendingPoolCoreException notOwner() {
        return new LendingPoolCoreException(Code.NotOwner, "require owner access" );
    }

    public static LendingPoolCoreException reserveNotActive(String msg) {
        return new LendingPoolCoreException(Code.ReserveNotActive, msg);
    }




    public enum Code implements Coded {
        Unknown(0),NotOwner(1),ReserveNotActive(2);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
