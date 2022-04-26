package finance.omm.score.core.delegation;

import static finance.omm.utils.constants.AddressConstant.ZERO_ADDRESS;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.core.score.interfaces.Delegation;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.PrepICXDelegations;
import finance.omm.score.core.delegation.exception.DelegationException;
import finance.omm.utils.db.EnumerableSet;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;
import scorex.util.HashMap;


public class DelegationImpl extends AddressProvider implements Delegation {

    public static final String TAG = "Delegation";

    public static final String PREPS = "preps";
    public static final String USER_PREPS = "userPreps";
    public static final String PERCENTAGE_DELEGATIONS = "percentageDelegations";
    public static final String PREP_VOTES = "prepVotes";
    public static final String USER_VOTES = "userVotes";
    public static final String TOTAL_VOTES = "totalVotes";
//    public static final String EQUAL_DISTRIBUTION = "equalDistribution";
    public static final String CONTRIBUTORS = "contributors";
    public static final String VOTE_THRESHOLD = "voteThreshold";

    private final EnumerableSet<Address> _preps = new EnumerableSet<>(PREPS, Address.class);
    private final BranchDB<Address, DictDB<BigInteger, Address>> _userPreps = Context.newBranchDB(
            USER_PREPS, Address.class);
    private final BranchDB<Address, DictDB<BigInteger, BigInteger>> _percentageDelegations = Context.newBranchDB(
            PERCENTAGE_DELEGATIONS, BigInteger.class);
    private final DictDB<Address, BigInteger> _prepVotes = Context.newDictDB(PREP_VOTES, BigInteger.class);
    private final DictDB<Address, BigInteger> _userVotes = Context.newDictDB(USER_VOTES, BigInteger.class);
    public final VarDB<BigInteger> _totalVotes = Context.newVarDB(TOTAL_VOTES, BigInteger.class);
    private final ArrayDB<Address> _contributors = Context.newArrayDB(CONTRIBUTORS, Address.class);
    public final VarDB<BigInteger> _voteThreshold = Context.newVarDB(VOTE_THRESHOLD, BigInteger.class);

    public DelegationImpl(Address addressProvider) {
        super(addressProvider);
        _voteThreshold.set(ICX.divide(BigInteger.valueOf(1000)));
    }

    @External(readonly = true)
    public String name() {
        return "OMM "+TAG;
    }

    @External
    public void setVoteThreshold(BigInteger vote) {
        checkOwner();
        _voteThreshold.set(vote);
    }

    @External(readonly = true)
    public BigInteger getVoteThreshold() {
        return _voteThreshold.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void addContributor(Address prep) {
        checkOwner();
        List<Address> contributors = getContributors();
        boolean isContributor = contains(prep, contributors);
        if (!isContributor) {
            _contributors.add(prep);
        }
    }

    @External
    public void removeContributor(Address prep) {
        List<Address> contributors = getContributors();
        boolean isContributor = contains(prep, contributors);
        Context.require(isContributor,TAG + ": " + prep + " is not in contributor list");

        Address topPrep = _contributors.pop();
        if (! topPrep.equals(prep)) {
            for (int i = 0; i < _contributors.size(); i++) {
                if (_contributors.get(i).equals(prep)) {
                    _contributors.set(i, topPrep);
                }
            }
        }
    }

    @External(readonly = true)
    public List<Address> getContributors() {
        List<Address> contributorList = new ArrayList<>();
        for (int i = 0; i < _contributors.size(); i++) {
            contributorList.add(_contributors.get(i));
        }
        return contributorList;
    }

    @External
    public void addAllContributors(Address[] preps) {
        for (Address prep : preps) {
            addContributor(prep);
        }
    }

    @External
    public void clearPrevious(Address user) {
        Context.require(user.equals(Context.getCaller()), TAG+
                " :You are not authorized to clear others delegation preference");
        PrepDelegations[] defaultDelegation = distributeVoteToContributors();
        updateDelegations(defaultDelegation, user);
    }

    @External(readonly = true)
    public boolean userDefaultDelegation(Address user) {
        return getUserDelegationDetails(user).equals(distributeVoteToContributors());
    }

    private void resetUser(Address user) {
        Address boostedOMM = getAddress(Contracts.BOOSTED_OMM.getKey());
        if (Context.getCaller().equals(user) || Context.getCaller().equals(boostedOMM)) {
            BigInteger prepVotes = BigInteger.ZERO;
            BigInteger userVote = _userVotes.getOrDefault(user, BigInteger.ZERO);
            for (int i = 0; i < 5; i++) {
                BigInteger index = BigInteger.valueOf(i);
                BigInteger prepVote = exaMultiply(_percentageDelegations.at(user).
                        getOrDefault(BigInteger.ONE, BigInteger.ZERO), userVote);

                if (prepVote.compareTo(BigInteger.ZERO) > 0) {

                    DictDB<BigInteger, Address> userPrep = _userPreps.at(user);
                    Address currentPrep = userPrep.getOrDefault(index, ZERO_ADDRESS);

                    BigInteger currentPrepVote = _prepVotes.getOrDefault(currentPrep, BigInteger.ZERO);
                    BigInteger newPrepVote = currentPrepVote.subtract(prepVote);
                    _prepVotes.set(currentPrep, newPrepVote);
                }
                _userPreps.at(user).set(index, ZERO_ADDRESS);
                _percentageDelegations.at(user).set(index, BigInteger.ZERO);
                prepVotes = prepVotes.add(prepVote);
            }

            BigInteger beforeTotalVotes = _totalVotes.get();
            _totalVotes.set(beforeTotalVotes.subtract(prepVotes));
            _userVotes.set(user, BigInteger.ZERO);
        }
    }

    private void validatePrep(Address address) {
        Map<String, ?> prepDetails = (Map<String, ?>) Context.call(ZERO_ADDRESS, "getPRep", address);
        boolean isActive = false;

        if (prepDetails.get("status").equals(BigInteger.ZERO)) {
            isActive = true;
        }
        Context.require(isActive, TAG + ": Invalid prep: "+address);
    }

    @External(readonly = true)
    public List<Address> getPrepList() {
        List<Address> prepList = new ArrayList<>();
        for (int i = 0; i < _preps.length(); i++) {
            prepList.add(_preps.at(i));
        }
        return prepList;
    }

    @External
    public void updateDelegations(@Optional PrepDelegations[] delegations, @Optional Address user) {
        Address boostedOMM = getAddress(Contracts.BOOSTED_OMM.getKey());
        Address currentUser;
        if (!(user == null) && (Context.getCaller().equals(boostedOMM))) {
            currentUser = user;
        } else {
            currentUser = Context.getCaller();
        }
        PrepDelegations[] userDelegations;

        if (delegations == null) {
            userDelegations = getUserDelegationDetails(currentUser);
        } else {
            Context.require(delegations.length <=5, TAG +
                    " updating delegation unsuccessful, more than 5 preps provided by user"+
                    " delegations provided " + delegations);
            userDelegations = delegations;
        }

        if (userDelegations == null) {
            userDelegations = distributeVoteToContributors();
        }

        handleCalculation(userDelegations, user);
    }

    private void handleCalculation(PrepDelegations[] userDelegations, Address user) {
        BigInteger totalPercentage = BigInteger.ZERO;
        BigInteger prepVotes = BigInteger.ZERO;

        Address ommToken = getAddress(Contracts.OMM_TOKEN.getKey());
        Map<String, BigInteger> userDetailsBalance = (Map<String, BigInteger>) Context.call(ommToken, "details_balanceOf", user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");
        resetUser(user);

        for (int i = 0; i < userDelegations.length; i++) {
            PrepDelegations userDelegation = userDelegations[i];
            Address address = userDelegation._address;
            BigInteger votes = userDelegation._votes_in_per;

            if (! _preps.contains(address)) {
                validatePrep(address);
                _preps.add(address);
            }

            BigInteger prepVote = exaMultiply(votes, userStakedToken);
            BigInteger newPrepVote = _prepVotes.getOrDefault(address, BigInteger.ZERO).add(prepVote);
            _prepVotes.set(address, newPrepVote);

            BigInteger index = BigInteger.valueOf(i);
            _userPreps.at(user).set(index, address);
            _percentageDelegations.at(user).set(index, votes);

            prepVotes = prepVotes.add(prepVote);
            totalPercentage = totalPercentage.add(votes);
        }
        Context.require(totalPercentage.equals(ICX), TAG+
                ": updating delegation unsuccessful,sum of percentages not equal to 100"+
                "sum total of percentages " + totalPercentage +
                "delegation preferences "+ userDelegations);
        PrepDelegations[] updatedDelegation = computeDelegationPercentages();

        BigInteger totalVotes = _totalVotes.getOrDefault(BigInteger.ZERO);
        _totalVotes.set(totalVotes.add(prepVotes));

        scoreCall(Contracts.LENDING_POOL_CORE,"updatePrepDelegations", (Object[]) updatedDelegation);
    }

    private PrepDelegations[] distributeVoteToContributors() {

        int contributorSize = _contributors.size();
        PrepDelegations[] userDetails = new PrepDelegations[contributorSize];
        BigInteger totalContributors =  BigInteger.valueOf(contributorSize);
        BigInteger prepPercentage = ICX.divide(totalContributors);
        BigInteger totalPercentage = BigInteger.ZERO;

        for (int i = 0; i < _contributors.size(); i++) {
            Map<String, Object> response = new HashMap<>();
            response.put("_address", _contributors.get(i));
            response.put("_votes_in_per", prepPercentage);

            PrepDelegations e = PrepDelegations.fromMap(response);
            userDetails[i] = e;
            totalPercentage = totalPercentage.add(prepPercentage);
        }

        BigInteger dustVotes = ICX.subtract(totalPercentage);
        if (dustVotes.compareTo(BigInteger.ZERO) > 0) {
            PrepDelegations firstPrep = userDetails[0];
            BigInteger currentVotes = firstPrep._votes_in_per;
            firstPrep._votes_in_per = dustVotes.add(currentVotes);
            userDetails[0] = firstPrep;
        }

        return userDetails;
    }

    @External(readonly = true)
    public BigInteger prepVotes(Address prep) {
        return _prepVotes.getOrDefault(prep, BigInteger.ZERO);
    }

    @External(readonly = true)
    public Map<String,BigInteger> userPrepVotes(Address user) {
        Map<String, BigInteger> response = new HashMap<>();
        Address ommToken = getAddress(Contracts.OMM_TOKEN.getKey());
        Map<String, BigInteger> userDetailsBalance = (Map<String, BigInteger>) Context.call(ommToken, "details_balanceOf", user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");

        for (int i = 0; i < 5; i++) {
            BigInteger index = BigInteger.valueOf(i);
            Address prep = _userPreps.at(user).getOrDefault(index, ZERO_ADDRESS);
            if (!prep.equals(ZERO_ADDRESS)) {
                BigInteger vote = exaMultiply(_percentageDelegations.at(user).getOrDefault(index, BigInteger.ZERO),
                        userStakedToken);
                response.put(prep.toString(), vote);
            }
        }
        return response;
    }

    @External(readonly = true)
    public PrepDelegations[] getUserDelegationDetails(Address user) {
        PrepDelegations[] userDetails = new PrepDelegations[5];
        for (int i = 0; i < 5; i++) {
            BigInteger index = BigInteger.valueOf(i);
            Address prep = _userPreps.at(user).getOrDefault(index, ZERO_ADDRESS);
            if (!prep.equals(ZERO_ADDRESS)) {
                Map<String, Object> response = new HashMap<>();
                response.put("_address", prep);
                response.put("_votes_in_per",
                        _percentageDelegations.at(user).getOrDefault(index, BigInteger.ZERO));

                PrepDelegations prepDelegation = PrepDelegations.fromMap(response);
                userDetails[i] = prepDelegation;
            }
        }
        return userDetails;
    }

    @External(readonly = true)
    public List<PrepICXDelegations> getUserICXDelegation(Address user) {
        List<PrepICXDelegations> userDetails = new ArrayList<>();

        Address ommToken = getAddress(Contracts.OMM_TOKEN.getKey());
        Map<String, BigInteger> userDetailsBalance = (Map<String, BigInteger>) Context.call(ommToken, "details_balanceOf", user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");
        Map<String, BigInteger> totalStaked =(Map<String, BigInteger>) Context.call(ommToken, "getTotalStaked");
        BigInteger totalStakedToken = totalStaked.get("totalStaked");

        Address staking = Address.fromString(Contracts.STAKING.getKey());
        Address sICX = Address.fromString(Contracts.sICX.getKey());
        Address lendingPoolCore = Address.fromString(Contracts.LENDING_POOL_CORE.getKey());

        BigInteger sicxIcxRate = Context.call(BigInteger.class, staking, "getTodayRate");
        BigInteger coresICXBalance = Context.call(BigInteger.class, sICX, "balanceOf", lendingPoolCore);
        BigInteger ommICXPower = exaMultiply(sicxIcxRate, exaDivide(coresICXBalance, totalStakedToken));

        for (int i = 0; i < 5; i++) {
            BigInteger index = BigInteger.valueOf(i);
            Address prep = _userPreps.at(user).getOrDefault(index, ZERO_ADDRESS);
            if (!prep.equals(ZERO_ADDRESS)) {
                BigInteger votes_in_per = _percentageDelegations.at(user).getOrDefault(index, BigInteger.ZERO);
                Map<String, Object> response = new HashMap<>();
                response.put("_address", prep);
                response.put("_votes_in_per", votes_in_per);

                BigInteger voteInICX = exaMultiply(ommICXPower, exaMultiply(votes_in_per, userStakedToken));
                response.put("_votes_in_icx",voteInICX);

                PrepICXDelegations prepICXDelegation = PrepICXDelegations.fromMap(response);
                userDetails.add(prepICXDelegation);
            }
        }
        return userDetails;
    }

    @External(readonly = true)
    public PrepDelegations[] computeDelegationPercentages() {
        BigInteger totalVotes = _totalVotes.getOrDefault(BigInteger.ZERO);
        if (totalVotes.equals(BigInteger.ZERO)) {
            PrepDelegations[] defaultPreference = distributeVoteToContributors();
            for (PrepDelegations prepDelegations : defaultPreference) {
                BigInteger votes = prepDelegations._votes_in_per;
                prepDelegations._votes_in_per = votes.multiply(BigInteger.valueOf(100));
            }
            return defaultPreference;
        }

        List<Address> prepList = getPrepList();
        int prepListSize = prepList.size();
        PrepDelegations[] prepDelegations = new PrepDelegations[prepListSize];
        if (prepListSize > 0) {
            BigInteger totalPercentage = BigInteger.ZERO;
            int maxVotePrepIndex = 0;
            BigInteger maxVotes = BigInteger.ZERO;
            BigInteger votingThreshold = _voteThreshold.getOrDefault(BigInteger.ZERO);
            for (int i = 0; i < prepListSize; i++) {
                Address prep = prepList.get(i);
                BigInteger votes =  exaDivide(_prepVotes.getOrDefault(prep, BigInteger.ZERO), totalVotes)
                        .multiply(BigInteger.valueOf(100));
                if (votes.compareTo(votingThreshold) > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("_address", prep);
                    response.put("_votes_in_per", votes);

                    PrepDelegations prepDelegation = PrepDelegations.fromMap(response);
                    prepDelegations[i] = prepDelegation;

                    totalPercentage = totalPercentage.add(votes);

                    if (votes.compareTo(maxVotes) > 0) {
                        maxVotes = votes;
                        maxVotePrepIndex = prepDelegations.length;
                    }
                }
            }
            BigInteger dustVotes = ICX.multiply(BigInteger.valueOf(100)).subtract(totalPercentage);
            if (dustVotes.compareTo(BigInteger.ZERO) > 0) {
                PrepDelegations maxPrepDelegation = prepDelegations[maxVotePrepIndex];
                maxPrepDelegation._votes_in_per = maxPrepDelegation._votes_in_per.add(dustVotes);
                prepDelegations[maxVotePrepIndex] = maxPrepDelegation;
            }
            return prepDelegations;
        }
        return  null;
    }

    public boolean contains(Address target, List<Address> addresses) {
        for(Address address : addresses) {
            if (address.equals(target)){
                return true;
            }
        }
        return false;
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw DelegationException.notOwner();
        }
    }

    public void scoreCall(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }
}
