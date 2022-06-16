package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import score.Address;
import score.annotation.EventLog;

public interface BoostedOMMCallback {

    void onKick(Address user, BigInteger bOMMUserBalance, byte[] data);

    void onBalanceUpdate(Address user);

    @EventLog
    void UserKicked(Address user, byte[] data);
}
