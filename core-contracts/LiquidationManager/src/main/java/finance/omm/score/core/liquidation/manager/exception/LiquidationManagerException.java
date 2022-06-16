package finance.omm.score.core.liquidation.manager.exception;

import finance.omm.utils.exceptions.OMMException;


public class LiquidationManagerException extends OMMException.LiquidityManager {

    public LiquidationManagerException(Code c) {
        super(c, c.name());
    }

    public LiquidationManagerException(Code c, String message) {
        super(c, message);
    }


    public static LiquidationManagerException unauthorized(String msg) {
        return new LiquidationManagerException(Code.UnAuthorized, msg);
    }


    public enum Code implements Coded {
        UnAuthorized(2);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
