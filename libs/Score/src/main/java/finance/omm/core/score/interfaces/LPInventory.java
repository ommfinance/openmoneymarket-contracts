package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.annotation.EventLog;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface LPInventory {

    String name();

    void setAdmin(Address admin);

    Address getAdmin();

    Address getCandidate();

    void claimAdminRole();

    void stake(BigInteger poolId, BigInteger value);

    void transfer(Address to, BigInteger value, BigInteger poolId, @Optional byte[] data);

    Map<String, BigInteger> balanceOf(Address owner, BigInteger poolId);

    void unstake(BigInteger poolId, BigInteger value);

    void onIRC31Received(Address operator, Address from, BigInteger id, BigInteger value, byte[] data);

    void tokenFallback(Address from, BigInteger value, byte[] data);

    @EventLog(indexed = 2)
    void TokenReceived(Address reserve, BigInteger value);

    @EventLog(indexed = 3)
    void LPTokenReceived(Address from, BigInteger poolId, BigInteger value);

    @EventLog
    void AdminCandidatePushed(Address oldAmin, Address newAdmin);

    @EventLog
    void AdminRoleClaimed(Address oldAdmin, Address newAdmin);
}
