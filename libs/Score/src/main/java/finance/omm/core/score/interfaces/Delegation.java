package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.PrepICXDelegations;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

public interface Delegation {

    String name();

    void initializeVoteToContributors();

    void setVoteThreshold(BigInteger _vote);

    BigInteger getVoteThreshold();

    void addContributor(Address _prep);

    void removeContributor(Address _prep);

    List<Address> getContributors();

    void addAllContributors(Address[] _preps);

    void clearPrevious(Address _user);

    boolean userDefaultDelegation(Address _user);

    List<Address> getPrepList();

    void updateDelegations(@Optional PrepDelegations[] _delegations, @Optional Address _user);

    BigInteger prepVotes(Address _prep);

    BigInteger getWorkingBalance(Address _user);

    BigInteger getWorkingTotalSupply();

    Map<String,BigInteger> userPrepVotes(Address _user);

    PrepDelegations[] getUserDelegationDetails(Address _user);

    List<PrepICXDelegations> getUserICXDelegation(Address _user);

    PrepDelegations[] computeDelegationPercentages();
}

