package finance.omm.utils.exceptions;


import score.UserRevertException;
import score.UserRevertedException;

public class OMMException extends UserRevertException {

    /**
     * OMMException.RESERVED => 80 ~ 99
     */
    enum Type {
        RewardWeightController(0),
        RewardDistribution(10),
        bOMMException(20),
        DelegationException(30),
        AddressManager(35),
        Governance(40),
        OMMToken(50),
        StakeLPException(60),
        LendingPool(65),
        RESERVED(80);

        int offset;

        Type(int offset) {
            this.offset = offset;
        }

        int apply(int code) {
            code = offset + code;
            if (this.equals(RESERVED) || code >= values()[ordinal() + 1].offset) {
                throw new IllegalArgumentException();
            }
            return code;
        }

        int recover(int code) {
            code = code - offset;
            if (this.equals(RESERVED) || code < 0) {
                throw new IllegalArgumentException();
            }
            return code;
        }

        static Type valueOf(int code) throws IllegalArgumentException {
            for (Type t : values()) {
                if (code < t.offset) {
                    if (t.ordinal() == 0) {
                        throw new IllegalArgumentException();
                    } else {
                        return t;
                    }
                }
            }
            throw new IllegalArgumentException();
        }
    }

    private final Type type;
    private final int code;

    OMMException(Code c) {
        this(Type.RewardDistribution, c, c.name());
    }

    OMMException(Code code, String message) {
        this(Type.RewardDistribution, code, message);
    }

    OMMException(Type type, Coded code, String message) {
        this(type, code.code(), message);
    }

    OMMException(Type type, int code, String message) {
        super(message);
        this.type = type;
        this.code = type.apply(code);
    }

    OMMException(UserRevertedException e) {
        super(e.getMessage(), e);
        this.code = e.getCode();
        this.type = Type.valueOf(code);
    }

    public static OMMException of(UserRevertedException e) {
        return new OMMException(e);
    }

    @Override
    public int getCode() {
        return code;
    }

    public int getCodeOfType() {
        return type.recover(code);
    }

    public interface Coded {

        int code();

        default boolean equals(OMMException e) {
            return code() == e.getCodeOfType();
        }
    }

    public enum Code implements Coded {
        Unknown(0);

        final int code;

        Code(int code) {this.code = code;}

        public int code() {return code;}

    }

    public static OMMException unknown(String message) {
        return new OMMException(Code.Unknown, message);
    }

    public static class RewardDistribution extends OMMException {

        public RewardDistribution(int code, String message) {
            super(Type.RewardDistribution, code, message);
        }

        public RewardDistribution(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class AddressManager extends OMMException {

        public AddressManager(int code, String message) {
            super(Type.AddressManager, code, message);
        }

        public AddressManager(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class StakedLPImpl extends OMMException {

        public StakedLPImpl(int code, String message) {
            super(Type.StakeLPException, code, message);
        }

        public StakedLPImpl(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class RewardWeightError extends OMMException {

        public RewardWeightError(int code, String message) {
            super(Type.RewardWeightController, code, message);
        }

        public RewardWeightError(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class BOMMException extends OMMException {

        public BOMMException(int code, String message) {
            super(Type.bOMMException, code, message);
        }

        public BOMMException(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class DelegationException extends OMMException {

        public DelegationException(int code, String message) {
            super(Type.DelegationException, code, message);
        }

        public DelegationException(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class Governance extends OMMException {

        public Governance(int code, String message) {
            super(Type.Governance, code, message);
        }

        public Governance(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class OMMToken extends OMMException {

        public OMMToken(int code, String message) {
            super(Type.OMMToken, code, message);
        }

        public OMMToken(Coded code, String message) {
            this(code.code(), message);
        }
    }

    public static class LendingPool extends OMMException {

        public LendingPool(int code, String message) {
            super(Type.LendingPool, code, message);
        }

        public LendingPool(Coded code, String message) {
            this(code.code(), message);
        }
    }
}