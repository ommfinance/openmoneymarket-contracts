package finance.omm.score.core.stakedLP.exception;

import finance.omm.utils.exceptions.OMMException;

public class StakedLPException extends OMMException.StakedLPImpl {

    public StakedLPException(Coded code, String message) {
        super(code, message);
    }

    public static StakedLPException unknown(String message) {
        return new StakedLPException(Code.Unknown, message);
    }

    public static StakedLPException notOwner() {
        return new StakedLPException(Code.NotOwner, "require owner access" );
    }

    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
