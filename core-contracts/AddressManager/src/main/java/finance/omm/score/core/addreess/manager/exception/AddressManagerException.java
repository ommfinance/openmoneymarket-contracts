package finance.omm.score.core.addreess.manager.exception;

import finance.omm.utils.exceptions.OMMException;

public class AddressManagerException extends OMMException.AddressManager {

    public AddressManagerException(Coded code, String message) {
        super(code, message);
    }

    public static AddressManagerException notOwner() {
        return new AddressManagerException(Code.NotOwner, "require owner access");
    }

//    public static AddressManagerException unknown(String msg) {
//        return new AddressManagerException(Code.Unknown, msg);
//    }

    public enum Code implements OMMException.Coded {
        Unknown(0), NotOwner(1);

        final int code;

        Code(int code) {this.code = code;}

        @Override
        public int code() {return code;}

    }
}
