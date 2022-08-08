package finance.omm.score.core.lendingpoolcore.userreserve;

public class UserReserveDataDB {

    public UserReserveData getItem(Byte prefix) {
        return new UserReserveData(prefix);
    }

}


