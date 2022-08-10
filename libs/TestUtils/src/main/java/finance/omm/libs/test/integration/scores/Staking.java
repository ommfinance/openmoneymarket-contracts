package finance.omm.libs.test.integration.scores;

import finance.omm.libs.structs.PrepDelegations;
import java.math.BigInteger;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;


public interface Staking {

    BigInteger getTodayRate();

    @External
    void delegate(PrepDelegations[] _user_delegations);

    @Payable
    void stakeICX(@Optional Address _to, @Optional byte[] _data);

    @External
    void toggleStakingOn();

    @External
    void setSicxAddress(Address _address);

}
