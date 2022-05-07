package finance.omm.score.core.delegation;

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaDivideFloor;
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
    private final BranchDB<Address, DictDB<Integer, Address>> _userPreps = Context.newBranchDB(
            USER_PREPS, Address.class);
    private final BranchDB<Address, DictDB<Integer, BigInteger>> _percentageDelegations = Context.newBranchDB(
            PERCENTAGE_DELEGATIONS, BigInteger.class);
    private final DictDB<Address, BigInteger> _prepVotes = Context.newDictDB(PREP_VOTES, BigInteger.class);
    private final DictDB<Address, BigInteger> _userVotes = Context.newDictDB(USER_VOTES, BigInteger.class);
    public final VarDB<BigInteger> _totalVotes = Context.newVarDB(TOTAL_VOTES, BigInteger.class);
    private final ArrayDB<Address> _contributors = Context.newArrayDB(CONTRIBUTORS, Address.class);
    public final VarDB<BigInteger> _voteThreshold = Context.newVarDB(VOTE_THRESHOLD, BigInteger.class);

    public DelegationImpl(Address addressProvider) {
        super(addressProvider);
        if (_voteThreshold.get()== null) {
            _voteThreshold.set(ICX.divide(BigInteger.valueOf(1000)));
        }
    }

    @External(readonly = true)
    public String name() {
        return "OMM "+TAG;
    }

    @External(readonly = true)
    public  BigInteger getTotalVotes() {
        return _totalVotes.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void setVoteThreshold(BigInteger _vote) {
        checkOwner();
        _voteThreshold.set(_vote);
    }

    @External(readonly = true)
    public BigInteger getVoteThreshold() {
        return _voteThreshold.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void addContributor(Address _prep) {
        checkOwner();
        addContributorInternal(_prep);
    }

    @External
    public void removeContributor(Address _prep) {
        boolean isContributor = isContributor(_prep);
        if (!isContributor) {
            throw DelegationException.unknown(TAG + ": " + _prep + " is not in contributor list");
        }

        int size = _contributors.size();
        Address topPrep = _contributors.pop();
        if (! topPrep.equals(_prep)) {
            for (int i = 0; i < size; i++) {
                if (_contributors.get(i).equals(_prep)) {
                    _contributors.set(i, topPrep);
                }
            }
        }
    }

    @External(readonly = true)
    public List<Address> getContributors() {
        List<Address> contributorList = new ArrayList<>();
        int size = _contributors.size();
        for (int i = 0; i < size; i++) {
            contributorList.add(_contributors.get(i));
        }
        return contributorList;
    }

    @External
    public void addAllContributors(Address[] _preps) {
        checkOwner();
        for (Address prep : _preps) {
            addContributorInternal(prep);
        }
    }

    @External
    public void clearPrevious(Address _user) {
        if (_user != Context.getCaller()) {
            throw DelegationException.unknown(TAG+
                    " :You are not authorized to clear others delegation preference");
        }
        PrepDelegations[] defaultDelegation = distributeVoteToContributors();
        updateDelegations(defaultDelegation, _user);
    }

    @External(readonly = true)
    public boolean userDefaultDelegation(Address _user) {
        PrepDelegations[] userDetails = getUserDelegationDetails(_user);
        PrepDelegations[] contributors = distributeVoteToContributors();
        return contributors==userDetails;
    }

    private void resetUser(Address _user) {
        Address ommToken = getAddress(Contracts.OMM_TOKEN.getKey());
        if (Context.getCaller().equals(_user) || Context.getCaller().equals(ommToken)) {
            BigInteger prepVotes = BigInteger.ZERO;
            BigInteger userVote = _userVotes.getOrDefault(_user, BigInteger.ZERO);

            DictDB<Integer, Address> userPrep = _userPreps.at(_user);
            DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);

            for (int i = 0; i < 5; i++) {
                BigInteger prepVote = exaMultiply(percentageDelegationsOfUser
                        .getOrDefault(i, BigInteger.ZERO), userVote);

                if (prepVote.compareTo(BigInteger.ZERO) > 0) {
                    Address currentPrep = userPrep.getOrDefault(i, ZERO_SCORE_ADDRESS);

                    BigInteger currentPrepVote = _prepVotes.getOrDefault(currentPrep, BigInteger.ZERO);
                    BigInteger newPrepVote = currentPrepVote.subtract(prepVote);
                    _prepVotes.set(currentPrep, newPrepVote);
                }
                userPrep.set(i, ZERO_SCORE_ADDRESS);
                percentageDelegationsOfUser.set(i, BigInteger.ZERO);
                prepVotes = prepVotes.add(prepVote);
            }

            BigInteger beforeTotalVotes = _totalVotes.getOrDefault(BigInteger.ZERO);
            _totalVotes.set(beforeTotalVotes.subtract(prepVotes));
            _userVotes.set(_user, BigInteger.ZERO);
        }
    }

    private void validatePrep(Address _address) {
        Map<String, ?> prepDetails = call(Map.class, ZERO_SCORE_ADDRESS, "getPRep", _address);
        boolean isActive = prepDetails.get("status").equals(BigInteger.ZERO);
        if(! isActive) {
            throw DelegationException.unknown(TAG + ": Invalid prep: "+_address);
        }
    }

    @External(readonly = true)
    public List<Address> getPrepList() {
        List<Address> prepList = new ArrayList<>();

        int size = _preps.length();
        for (int i = 0; i < size; i++) {
            prepList.add(_preps.at(i));
        }
        return prepList;
    }

    @External
    public void updateDelegations(@Optional PrepDelegations[] _delegations, @Optional Address _user) {
        Address ommToken = getAddress(Contracts.OMM_TOKEN.getKey());
        Address currentUser;
        if (!(_user == null) && (Context.getCaller().equals(ommToken))) {
            currentUser = _user;
        } else {
            currentUser = Context.getCaller();
        }

        PrepDelegations[] userDelegations;
        int delegationsLength = _delegations.length;

        if (_delegations == null || delegationsLength == 0) {
            userDelegations = getUserDelegationDetails(currentUser);
        } else {
            if (delegationsLength > 5) {
                throw DelegationException.unknown(TAG +
                        " updating delegation unsuccessful, more than 5 preps provided by user"+
                        " delegations provided " + delegationsLength);
            }
            userDelegations = _delegations;
        }

        if (userDelegations.length == 0) {
            userDelegations = distributeVoteToContributors();
        }

        handleCalculation(userDelegations, currentUser);
    }

    private void handleCalculation(PrepDelegations[] userDelegations, Address user) {
        BigInteger totalPercentage = BigInteger.ZERO;
        BigInteger prepVotes = BigInteger.ZERO;

        Map<String, BigInteger> userDetailsBalance = call(Map.class,
                Contracts.OMM_TOKEN, "details_balanceOf", user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");
        resetUser(user);

        DictDB<Integer, Address> userPreps = _userPreps.at(user);
        DictDB<Integer, BigInteger> percentageDelegations = _percentageDelegations.at(user);

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

            userPreps.set(i, address);
            percentageDelegations.set(i, votes);

            prepVotes = prepVotes.add(prepVote);
            totalPercentage = totalPercentage.add(votes);
        }
        if (! totalPercentage.equals(ICX)) {
            throw DelegationException.unknown(TAG+
                    ": updating delegation unsuccessful,sum of percentages not equal to 100 "+
                    "sum total of percentages " + totalPercentage +
                    " delegation preferences "+ userDelegations.length);
        }

        _userVotes.set(user, userStakedToken);
        BigInteger totalVotes = _totalVotes.getOrDefault(BigInteger.ZERO);
        _totalVotes.set(totalVotes.add(prepVotes));

        PrepDelegations[] updatedDelegation = computeDelegationPercentages();

        Object[] params = new Object[]{
                updatedDelegation
        };

        call(Contracts.LENDING_POOL_CORE,"updatePrepDelegations", params);
    }

    public PrepDelegations[] distributeVoteToContributors() {

        int contributorSize = _contributors.size();
        PrepDelegations[] userDetails = new PrepDelegations[contributorSize];
        BigInteger totalContributors =  BigInteger.valueOf(contributorSize);
        BigInteger prepPercentage = ICX.divide(totalContributors);
        BigInteger totalPercentage = BigInteger.ZERO;

        for (int i = 0; i < contributorSize; i++) {
            Address prep = _contributors.get(i);
            PrepDelegations prepDelegation = new PrepDelegations(prep, prepPercentage);
            userDetails[i] = prepDelegation;
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
    public BigInteger prepVotes(Address _prep) {
        return _prepVotes.getOrDefault(_prep, BigInteger.ZERO);
    }

    @External(readonly = true)
    public Map<String,BigInteger> userPrepVotes(Address _user) {
        Map<String, BigInteger> userDetailsBalance = call(Map.class,
                Contracts.OMM_TOKEN, "details_balanceOf", _user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");

        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        Map<String, BigInteger> response = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (!prep.equals(ZERO_SCORE_ADDRESS)) {
                BigInteger vote = exaMultiply(percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO),
                        userStakedToken);
                response.put(String.valueOf(prep), vote);
            }
        }

        return response;
    }

    @External(readonly = true)
    public PrepDelegations[] getUserDelegationDetails(Address _user) {

        List<PrepDelegations> userDetails = new ArrayList<>();
        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        for (int i = 0; i < 5; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (!prep.equals(ZERO_SCORE_ADDRESS)) {
                BigInteger votesInPer = percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO);
                PrepDelegations prepDelegation = new PrepDelegations(prep, votesInPer);
                userDetails.add(prepDelegation);
            }
        }

        int size = userDetails.size();
        PrepDelegations[] prepDelegations = new PrepDelegations[size];

        for (int i = 0; i < size; i++) {
            PrepDelegations delegation = userDetails.get(i);
            PrepDelegations prepDelegation = new PrepDelegations(delegation._address, delegation._votes_in_per);
            prepDelegations[i] = prepDelegation;
        }

        return prepDelegations;
    }

    @External(readonly = true)
    public List<PrepICXDelegations> getUserICXDelegation(Address _user) {
        List<PrepICXDelegations> userDetails = new ArrayList<>();

        Map<String, BigInteger> userDetailsBalance = call(Map.class,
                Contracts.OMM_TOKEN, "details_balanceOf", _user);
        BigInteger userStakedToken = userDetailsBalance.get("stakedBalance");
        Map<String, BigInteger> totalStaked = call(Map.class,
                Contracts.OMM_TOKEN, "getTotalStaked");
        BigInteger totalStakedToken = totalStaked.get("totalStaked");

        Address lendingPoolCore = getAddress(Contracts.LENDING_POOL_CORE.getKey());

        BigInteger sicxIcxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
        BigInteger coresICXBalance = call(BigInteger.class, Contracts.sICX, "balanceOf", lendingPoolCore);
        BigInteger ommICXPower = exaMultiply(sicxIcxRate, exaDivide(coresICXBalance, totalStakedToken));

        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        for (int i = 0; i < 5; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (!prep.equals(ZERO_SCORE_ADDRESS)) {
                BigInteger votes_in_per = percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO);
                BigInteger voteInICX = exaMultiply(ommICXPower, exaMultiply(votes_in_per, userStakedToken));
                PrepICXDelegations prepICXDelegation = new PrepICXDelegations(prep, votes_in_per, voteInICX);
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
        List<PrepDelegations> prepDelegations = new ArrayList<>();
        if (prepList.size() > 0) {
            BigInteger totalPercentage = BigInteger.ZERO;
            int maxVotePrepIndex = 0;
            BigInteger maxVotes = BigInteger.ZERO;
            BigInteger votingThreshold = getVoteThreshold();
            for (Address prep : prepList) {
                BigInteger votes = exaDivideFloor(prepVotes(prep), totalVotes).multiply(BigInteger.valueOf(100));
                if (votes.compareTo(votingThreshold) > 0) {
                    prepDelegations.add(new PrepDelegations(prep, votes));
                    totalPercentage = totalPercentage.add(votes);
                    if (votes.compareTo(maxVotes) > 0) {
                        maxVotes = votes;
                        maxVotePrepIndex = prepDelegations.size()-1;
                    }
                }
            }
            BigInteger dustVotes = ICX.multiply(BigInteger.valueOf(100)).subtract(totalPercentage);
            if (dustVotes.compareTo(BigInteger.ZERO) > 0 ) {
                PrepDelegations e = prepDelegations.get(maxVotePrepIndex);
                e._votes_in_per = e._votes_in_per.add(dustVotes);
                prepDelegations.set(maxVotePrepIndex, e);
            }
            int size = prepDelegations.size();
            PrepDelegations[] prepDelegationsArr = new PrepDelegations[size];

            for (int i = 0; i < size; i++) {
                PrepDelegations delegation = prepDelegations.get(i);
                PrepDelegations prepDelegation = new PrepDelegations(delegation._address, delegation._votes_in_per);
                prepDelegationsArr[i] = prepDelegation;
            }
            return prepDelegationsArr;
        }
        return  new PrepDelegations[0];
    }

    public boolean contains(Address target, List<Address> addresses) {
        for(Address address : addresses) {
            if (address.equals(target)){
    private boolean isContributor(Address prep) {
        int size = _contributors.size();
        for (int i = 0; i < size; i++) {
            if (_contributors.get(i).equals(prep)) {
                return true;
            }
        }
        return false;
    }

    private void addContributorInternal(Address prep) {
        boolean isContributor = isContributor(prep);
        if (!isContributor) {
            _contributors.add(prep);
        }
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw DelegationException.notOwner();
        }
    }
    public void call(Contracts contract, String method, Object... params) {
        Context.call(getAddress(contract.getKey()), method, params);
    }

    public <K> K call(Class<K> kClass, Contracts contract, String method, Object... params) {
        return Context.call(kClass, getAddress(contract.getKey()), method, params);
    }

    public <K> K call(Class<K> kClass, Address contract, String method, Object... params) {
        return Context.call(kClass, contract, method, params);
    }
}
