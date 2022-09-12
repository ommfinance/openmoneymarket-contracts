package finance.omm.score.karma.exception;

import finance.omm.utils.exceptions.OMMException.OMMWalletException;

public class KarmaDAOWalletException extends OMMWalletException {

    public KarmaDAOWalletException(Code c) {
        super(c, c.name());
    }

    public KarmaDAOWalletException(Code c, String message) {
        super(c, message);
    }

    public static OMMWalletException notAdmin() {
        return new OMMWalletException(Code.NotAdmin, "require admin access");
    }


    public enum Code implements Coded {
        Unknown(0), NotAdmin(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}
    }
}
