package finance.omm.score.core.lendingpoolDataProvider.exception;

import finance.omm.utils.exceptions.OMMException;

public class LendingPoolDataProviderException extends OMMException.LendingPoolDataProvider {

    public LendingPoolDataProviderException(Code c, String message) {
        super(c, message);
    }

    public LendingPoolDataProviderException(Code c) {
        super(c, c.name());
    }

    public static LendingPoolDataProviderException notOwner() {
        return new LendingPoolDataProviderException(Code.NotOwner, "require owner access" );
    }



    public enum Code implements Coded {
        NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
