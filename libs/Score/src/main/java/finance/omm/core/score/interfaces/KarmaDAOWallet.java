package finance.omm.core.score.interfaces;

import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.annotation.EventLog;

public interface KarmaDAOWallet {


    @EventLog()
    void FundTransferred(BigInteger poolId, BigInteger value);

    @EventLog(indexed = 2)
    void FundWithdrawn(BigInteger poolId, BigInteger value);

    @EventLog(indexed = 2)
    void LPWithdrawn(BigInteger poolId, BigInteger value);

    @EventLog(indexed = 2)
    void LPTokenRemoved(BigInteger poolId, BigInteger value);

    @EventLog(indexed = 2)
    void TokenTransferred(Address token, Address to, BigInteger value);


    @EventLog
    void FundReceived(Address reserve, BigInteger value);

    @EventLog(indexed = 1)
    void AdminCandidatePushed(Address newAdmin);

    @EventLog(indexed = 1)
    void AdminStatusClaimed(Address newAdmin);

    @EventLog(indexed = 2)
    void LPTokenReceived(BigInteger poolId, BigInteger value, Address from);

    String name();

    Address getAdmin();

    Address getCandidate();

    Map<String, BigInteger> balanceOf(BigInteger poolId);

    Map<String, BigInteger> lpBalanceOf(BigInteger poolId);

    boolean bondContract(BigInteger poolId);

    void addBondContract(BigInteger poolId, Address bondContract, Address treasuryContract);

    void pushManagement(BigInteger poolId, Address newOwner);

    void pullManagement(BigInteger poolId);

    void transferOMMToTreasury(BigInteger poolId, BigInteger value);

    void withdrawOMMFromTreasury(BigInteger poolId, BigInteger value);

    void withdrawLPFromTreasury(BigInteger poolId, BigInteger value);

    void toggleBondContract(BigInteger poolId);

    Map<String, Address> getPoolDetails(BigInteger poolId);

    void setAdmin(Address newAdmin);

    void claimAdminStatus();

    void removeLP(BigInteger poolId, BigInteger value);

    void transferToken(Address token, Address to, BigInteger value);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data);
}
