package finance.omm.score.tokens.exception;

import finance.omm.utils.exceptions.OMMException;

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

    public static BoostedOMMException notOwner() {
        return new BoostedOMMException(Code.NotOwner, "require owner access");
    }

    public static BoostedOMMException notGovernanceContract() {
        return new BoostedOMMException(Code.NotGovernanceContract, "require Governance contract access");
    }

    public static BoostedOMMException reentrancy(String message) {
        return new BoostedOMMException(Code.RE_ENTRANCY, message);
    }

    //OMMException.BOMMException => 20~
    public enum Code implements Coded {
        Unknown(0), NotOwner(1), NotGovernanceContract(2), RE_ENTRANCY(3);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
