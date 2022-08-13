package finance.omm.score.core.lendingpoolcore.userreserve;

public class UserReserveDataDB {

    public UserReserveData getItem(byte[] prefix) {
        return new UserReserveData(prefix);
    }

}


