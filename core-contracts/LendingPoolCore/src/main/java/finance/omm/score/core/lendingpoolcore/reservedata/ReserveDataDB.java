package finance.omm.score.core.lendingpoolcore.reservedata;

public class ReserveDataDB {

    public ReserveData getItem(Byte prefix) {
        return new ReserveData(prefix);
    }

}


