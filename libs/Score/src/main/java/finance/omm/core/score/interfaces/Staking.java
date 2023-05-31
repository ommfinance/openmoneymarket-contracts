package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.PrepDelegations;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ScoreInterface(suffix = "Client")
public interface Staking {

    @External(readonly = true)
    String name();

    @External
    void setEmergencyManager(Address _address);

    @External(readonly = true)
    Address getEmergencyManager();

    @External
    void setBlockHeightWeek(BigInteger _height);

    @External(readonly = true)
    BigInteger getBlockHeightWeek();

    @External(readonly = true)
    BigInteger getTodayRate();

    @External
    void toggleStakingOn();

    @External(readonly = true)
    boolean getStakingOn();

    @External(readonly = true)
    Address getSicxAddress();

    @External
    void setUnstakeBatchLimit(BigInteger _limit);

    @External(readonly = true)
    BigInteger getUnstakeBatchLimit();

    @External(readonly = true)
    List<Address> getPrepList();

    @External(readonly = true)
    BigInteger getUnstakingAmount();

    @External(readonly = true)
    BigInteger getTotalStake();

    @External(readonly = true)
    BigInteger getLifetimeReward();

    @External(readonly = true)
    List<Address> getTopPreps();

    @External(readonly = true)
    Map<String, BigInteger> getPrepDelegations();

    @External(readonly = true)
    Map<String, BigInteger> getActualPrepDelegations();

    @External(readonly = true)
    Map<String, BigInteger> getActualUserDelegationPercentage(Address user);

    @External
    void setSicxAddress(Address _address);

    @External(readonly = true)
    BigInteger claimableICX(Address _address);

    @External(readonly = true)
    BigInteger totalClaimableIcx();

    @External
    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    @External
    void claimUnstakedICX(@Optional Address _to);

    @External(readonly = true)
    Map<String, BigInteger> getAddressDelegations(Address _address);

    @External
    void delegate(PrepDelegations[] _user_delegations);

    @External
    @Payable
    BigInteger stakeICX(@Optional Address _to, @Optional byte[] _data);

    @External
    void transferUpdateDelegations(Address _from, Address _to, BigInteger _value);

    @External(readonly = true)
    List<List<Object>> getUnstakeInfo();

    @External(readonly = true)
    List<Map<String, Object>> getUserUnstakeInfo(Address _address);

    @External
    void setOmmLendingPoolCore(Address _address);

    @External
    public void setPrepProductivity(BigInteger _productivity);

    @External
    public void setFeePercentage(BigInteger _feePercentage);

    @External
    public void setFeeDistributionAddress(Address _address);

    @External(readonly = true)
    public BigInteger getPrepProductivity();

    @External(readonly = true)
    public BigInteger getFeePercentage();

    @External(readonly = true)
    Map<String, BigInteger> getbOMMDelegations();
}
