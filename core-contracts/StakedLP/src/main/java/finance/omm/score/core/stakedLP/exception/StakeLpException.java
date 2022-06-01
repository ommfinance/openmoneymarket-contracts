package finance.omm.score.core.stakedLP.exception;

import finance.omm.utils.exceptions.OMMException;

public class StakeLpException extends OMMException.StakedLPImpl {

    public StakeLpException(Coded code, String message) {
        super(code, message);
    }

    public static StakeLpException notOwner(){
        return new StakeLpException(Code.NotOwner, "require owner access" );
    }

    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
