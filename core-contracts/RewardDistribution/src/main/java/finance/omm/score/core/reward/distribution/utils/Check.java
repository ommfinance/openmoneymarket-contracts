package finance.omm.score.core.reward.distribution.utils;

import score.Context;

public class Check {
    public static final int ERROR_NOT_CONTRACT_OWNER = 1;
    public static final int ERROR_UNSUPPORTED_RECIPIENT = 2;
    public static final int ERROR_ASSET_NOT_FOUND = 3;
    public static final int ERROR_PERCENTAGE_MISMATCH = 4;
    public static final int ERROR_GENERIC = 99;

    public static void require(boolean condition, String message) {
        require(condition, ERROR_GENERIC, message);
    }

    public static void require(boolean condition, Integer errorCode, String message) {
        if (!condition) {
            throwError(errorCode, message);
        }
    }

    public static void throwError(Integer errorCode, String message) {
        Context.revert(errorCode, message);
    }
}

