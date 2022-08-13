package finance.omm.score.core.lendingpoolcore.reservedata;

public class ReserveDataDB {

    public ReserveData getItem(byte[] prefix) {
        return new ReserveData(prefix);
    }

}


