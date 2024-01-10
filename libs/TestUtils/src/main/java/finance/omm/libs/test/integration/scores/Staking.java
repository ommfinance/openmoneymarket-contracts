package finance.omm.libs.test.integration.scores;

import finance.omm.libs.structs.PrepDelegations;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;


public interface Staking {
    String name();

    BigInteger getFeePercentage();

    BigInteger getPrepProductivity();

    BigInteger getTodayRate();

    @External
    void delegate(PrepDelegations[] _user_delegations);

    @Payable
    void stakeICX(@Optional Address _to, @Optional byte[] _data);

    @External
    void toggleStakingOn();

    @External
    void setSicxAddress(Address _address);

    @External
    void setFeeDistributionAddress(Address _address);

    @External(readonly = true)
    Map<String, BigInteger> getPrepDelegations();

    void setOmmLendingPoolCore(Address _address);

    Map<String, BigInteger> getActualUserDelegationPercentage(Address user);

    @External(readonly = true)
    BigInteger getTotalStake();

    List<Address> getTopPreps();

    List<Address> getPrepList();

    Map<String, BigInteger> getAddressDelegations(Address _address);

    boolean getStakingOn();

    @External(readonly = true)
    List<List<Object>> getUnstakeInfo();

    @External(readonly = true)
    List<Map<String, Object>> getUserUnstakeInfo(Address _address);

    @External(readonly = true)
    BigInteger getUnstakingAmount();

    @External(readonly = true)
    BigInteger claimableICX(Address _address);

    @External
    void claimUnstakedICX(@Optional Address _to);

    @External(readonly = true)
    BigInteger totalClaimableIcx();

    @External(readonly = true)
    Map<String, BigInteger> getbOMMDelegations();

    void updatePreps();
}
