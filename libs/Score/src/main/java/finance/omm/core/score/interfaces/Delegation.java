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

    void setVoteThreshold(BigInteger vote);

    BigInteger getVoteThreshold();

    void addContributor(Address prep);

    void removeContributor(Address prep);

    List<Address> getContributors();

    void addAllContributors(Address[] preps);

    void clearPrevious(Address user);

    boolean userDefaultDelegation(Address user);

    List<Address> getPrepList();

    void updateDelegations(@Optional PrepDelegations[] delegations, @Optional Address user);

    BigInteger prepVotes(Address prep);

    Map<String,BigInteger> userPrepVotes(Address user);

    PrepDelegations[] getUserDelegationDetails(Address user);

    List<PrepICXDelegations> getUserICXDelegation(Address user);

    PrepDelegations[] computeDelegationPercentages();
}

