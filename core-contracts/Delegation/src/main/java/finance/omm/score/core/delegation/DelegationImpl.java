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
import finance.omm.score.core.delegation.db.PREPEnumerableSet;
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
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;
import scorex.util.HashMap;


public class DelegationImpl extends AddressProvider implements Delegation {

    public static final String TAG = "Delegation";

    public static final String USER_PREPS = "userPreps";
    public static final String PERCENTAGE_DELEGATIONS = "percentageDelegations";
    public static final String CONTRIBUTORS = "contributors";
    public static final String VOTE_THRESHOLD = "voteThreshold";

    public static final String WORKING_BALANCE = "workingBalance";
    public static final String WORKING_TOTAL_SUPPLY = "workingTotalSupply";

    public static final String LOCKED_PREPS = "lockedPreps";
    public static final String LOCKED_PREP_VOTES = "lockedPrepVotes";
    public static final String CALLED_BEFORE = "calledBefore";
    public static final int PREP_COUNT = 100;

    private final EnumerableSet<Address> _preps = new PREPEnumerableSet(LOCKED_PREPS, Address.class);
    private final BranchDB<Address, DictDB<Integer, Address>> _userPreps = Context.newBranchDB(
            USER_PREPS, Address.class);
    private final BranchDB<Address, DictDB<Integer, BigInteger>> _percentageDelegations = Context.newBranchDB(
            PERCENTAGE_DELEGATIONS, BigInteger.class);
    private final DictDB<Address, BigInteger> _prepVotes = Context.newDictDB(LOCKED_PREP_VOTES, BigInteger.class);
    private final ArrayDB<Address> _contributors = Context.newArrayDB(CONTRIBUTORS, Address.class);
    public final VarDB<BigInteger> _voteThreshold = Context.newVarDB(VOTE_THRESHOLD, BigInteger.class);

    public final VarDB<BigInteger> workingTotalSupply = Context.newVarDB(WORKING_TOTAL_SUPPLY, BigInteger.class);
    public final DictDB<Address, BigInteger> workingBalance = Context.newDictDB(WORKING_BALANCE, BigInteger.class);

    public final VarDB<Boolean> calledBefore = Context.newVarDB(CALLED_BEFORE, Boolean.class);

    public DelegationImpl(Address addressProvider) {
        super(addressProvider, false);
        if (workingTotalSupply.get() == null) {
            workingTotalSupply.set(BigInteger.ZERO);
        }
        if (calledBefore.get() == null) {
            calledBefore.set(false);
        }
    }

    /**
     * This method should be called only once after bOMM upgrade calledBefore flag used to prevent calling it multiple
     * times
     */
    @External
    public void initializeVoteToContributors() {
        if (calledBefore.get()) {
            throw DelegationException.unknown(TAG + " : This method cannot be called again.");
        }

        checkOwner();
        PrepDelegations[] defaultDelegations = computeDelegationPercentages();
        Object[] params = new Object[]{
                defaultDelegations
        };
        call(Contracts.LENDING_POOL_CORE, "updatePrepDelegations", params);
        calledBefore.set(true);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
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
        checkOwner();
        boolean isContributor = isContributor(_prep);
        if (!isContributor) {
            throw DelegationException.unknown(TAG + ": " + _prep + " is not in contributor list");
        }

        Address topPrep = _contributors.pop();
        int size = _contributors.size();
        if (!topPrep.equals(_prep)) {
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
    public void clearPrevious() {
        PrepDelegations[] defaultDelegation = distributeVoteToContributors();
        updateDelegations(defaultDelegation, Context.getCaller());
    }

    @External(readonly = true)
    public boolean userDefaultDelegation(Address _user) {

        PrepDelegations[] userDetails = getUserDelegationDetails(_user);
        List<PrepDelegations> contributors = List.of(distributeVoteToContributors());

        if (userDetails.length != contributors.size()) {
            return false;
        }

        for (PrepDelegations prepDelegation : userDetails) {
            if (!contributors.contains(prepDelegation)) {
                return false;
            }
        }

        return true;
    }

    private void resetUserVotes(Address _user) {
        BigInteger userWorkingBalance = workingBalance.getOrDefault(_user, BigInteger.ZERO);

        DictDB<Integer, Address> userPrep = _userPreps.at(_user);
        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);

        for (int i = 0; i < PREP_COUNT; i++) {
            if (userPrep.get(i) == null ){
                break;
            }
            BigInteger prepVote = exaMultiply(percentageDelegationsOfUser
                    .getOrDefault(i, BigInteger.ZERO), userWorkingBalance);

            if (prepVote.compareTo(BigInteger.ZERO) > 0) {
                Address currentPrep = userPrep.get(i);

                BigInteger currentPrepVote = _prepVotes.getOrDefault(currentPrep, BigInteger.ZERO);
                BigInteger newPrepVote = currentPrepVote.subtract(prepVote);
                _prepVotes.set(currentPrep, newPrepVote);
            }
            userPrep.set(i, null);
            percentageDelegationsOfUser.set(i, null);
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

    private void updateWorkingBalance(Address user, BigInteger bOMMBalance) {
        BigInteger currentWorkingTotal = getWorkingTotalSupply();
        BigInteger currentWorkingBalance = getWorkingBalance(user);

        workingBalance.set(user, bOMMBalance);
        BigInteger newWorkingTotal = currentWorkingTotal.subtract(currentWorkingBalance).add(bOMMBalance);
        workingTotalSupply.set(newWorkingTotal);
    }

    @External
    public void onKick(Address user, BigInteger bOMMUserBalance, @Optional byte[] data) {
        onlyOrElseThrow(Contracts.BOOSTED_OMM,
                DelegationException.unauthorized("Only bOMM contract is allowed to call onKick method"));
        updateUserDelegations(null, user, bOMMUserBalance);
        UserKicked(user, data);
    }

    @External
    public void onBalanceUpdate(Address user) {
        onlyOrElseThrow(Contracts.BOOSTED_OMM,
                DelegationException.unauthorized("Only bOMM contract is allowed to call onBalanceUpdate method"));
        updateUserDelegations(null, user, null);
    }

    @External
    public void updateDelegationAtOnce(PrepDelegations[] _delegations){
        Address user = Context.getCaller();
        updateUserDelegations(_delegations,user,null);

        int delegationsLength = _delegations.length;
        PrepDelegations[] updatedDelegations = new PrepDelegations[delegationsLength];
        for (int i = 0; i < delegationsLength; i++) {

            Address _address = _delegations[i]._address;
            BigInteger _votes_in_per = _delegations[i]._votes_in_per.multiply(BigInteger.valueOf(100));
            updatedDelegations[i] = new PrepDelegations(_address,_votes_in_per);
        }

        call(Contracts.STAKING,"delegateForUser",updatedDelegations,user);
    }

    @External
    public void updateDelegations(@Optional PrepDelegations[] _delegations, @Optional Address _user) {
        Address bOMMAddress = getAddress(Contracts.BOOSTED_OMM.getKey());
        Address currentUser;
        Address caller = currentUser = Context.getCaller();
        if (_user != null && caller.equals(bOMMAddress)) {
            currentUser = _user;
        }
        updateUserDelegations(_delegations, currentUser, null);
    }

    private void updateUserDelegations(PrepDelegations[] _delegations, Address _user, BigInteger bOMMUserBalance) {

        PrepDelegations[] userDelegations;

        if (_delegations == null || _delegations.length == 0) {
            userDelegations = getUserDelegationDetails(_user);
        } else {
            int delegationsLength = _delegations.length;
            if (delegationsLength > PREP_COUNT) {
                throw DelegationException.unknown(TAG +
                        " updating delegation unsuccessful, more than 100 preps provided by user" +
                        " delegations provided " + delegationsLength);
            }
            userDelegations = _delegations;
        }

        if (userDelegations.length == 0) {
            userDelegations = distributeVoteToContributors();
        }

        calculatePREPDelegation(userDelegations, _user, bOMMUserBalance);
    }

    private void calculatePREPDelegation(PrepDelegations[] userDelegations, Address user, BigInteger bOMMUserBalance) {
        BigInteger totalPercentage = BigInteger.ZERO;

        resetUserVotes(user);
        if (bOMMUserBalance == null) {
            bOMMUserBalance = call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user);
        }

        updateWorkingBalance(user, bOMMUserBalance);

        DictDB<Integer, Address> userPreps = _userPreps.at(user);
        DictDB<Integer, BigInteger> percentageDelegations = _percentageDelegations.at(user);

        for (int i = 0; i < userDelegations.length; i++) {
            PrepDelegations userDelegation = userDelegations[i];
            Address address = userDelegation._address;
            BigInteger votes = userDelegation._votes_in_per;

            if (votes.signum() == -1 ){
                throw DelegationException.unknown(TAG + " Negative vote percentage");
            }

            _preps.add(address);

            if (!bOMMUserBalance.equals(BigInteger.ZERO)) {
                BigInteger prepVote = exaMultiply(votes, bOMMUserBalance);
                BigInteger newPrepVote = _prepVotes.getOrDefault(address, BigInteger.ZERO).add(prepVote);
                _prepVotes.set(address, newPrepVote);
            }

            userPreps.set(i, address);
            percentageDelegations.set(i, votes);

            totalPercentage = totalPercentage.add(votes);
        }
        if (!totalPercentage.equals(ICX)) {
            throw DelegationException.unknown(TAG +
                    ": updating delegation unsuccessful,sum of percentages not equal to 100 " +
                    "sum total of percentages " + totalPercentage +
                    " delegation preferences " + userDelegations.length);
        }
        updatePREPDelegations();
    }

    private void updatePREPDelegations() {
        PrepDelegations[] updatedDelegation = computeDelegationPercentages();

        Object[] params = new Object[]{
                updatedDelegation
        };

        call(Contracts.LENDING_POOL_CORE, "updatePrepDelegations", params);
    }

    public PrepDelegations[] distributeVoteToContributors() {

        int contributorSize = _contributors.size();
        PrepDelegations[] userDetails = new PrepDelegations[contributorSize];
        BigInteger totalContributors = BigInteger.valueOf(contributorSize);
        BigInteger prepPercentage = ICX.divide(totalContributors);
        BigInteger totalPercentage = BigInteger.ZERO;

        for (int i = 0; i < contributorSize; i++) {
            Address prep = _contributors.get(i);
            userDetails[i] = new PrepDelegations(prep, prepPercentage);
            totalPercentage = totalPercentage.add(prepPercentage);
        }

        BigInteger dustVotes = ICX.subtract(totalPercentage);
        if (dustVotes.compareTo(BigInteger.ZERO) > 0 && userDetails.length > 0) {
            BigInteger currentVotes = userDetails[0]._votes_in_per;
            userDetails[0]._votes_in_per = dustVotes.add(currentVotes);
        }

        return userDetails;
    }

    @External(readonly = true)
    public BigInteger prepVotes(Address _prep) {
        return _prepVotes.getOrDefault(_prep, BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getWorkingBalance(Address user) {
        return workingBalance.getOrDefault(user, BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getWorkingTotalSupply() {
        return workingTotalSupply.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public Map<String, BigInteger> userPrepVotes(Address _user) {
        BigInteger userWorkingBalance = getWorkingBalance(_user);

        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        Map<String, BigInteger> response = new HashMap<>();

        for (int i = 0; i < PREP_COUNT; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (prep.equals(ZERO_SCORE_ADDRESS)){
                break;
            }
            BigInteger vote = exaMultiply(percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO),
                    userWorkingBalance);
            response.put(prep.toString(), vote);

        }

        return response;
    }

    @External(readonly = true)
    public PrepDelegations[] getUserDelegationDetails(Address _user) {

        List<PrepDelegations> userDetails = new ArrayList<>();
        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        for (int i = 0; i < PREP_COUNT; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (prep.equals(ZERO_SCORE_ADDRESS)){
                break;
            }
            BigInteger votesInPer = percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO);
            PrepDelegations prepDelegation = new PrepDelegations(prep, votesInPer);
            userDetails.add(prepDelegation);

        }

        return getPrepDelegations(userDetails);
    }

    @External(readonly = true)
    public List<PrepICXDelegations> getUserICXDelegation(Address _user) {
        List<PrepICXDelegations> userDetails = new ArrayList<>();

        BigInteger userWorkingBalance = getWorkingBalance(_user);
        BigInteger workingTotal = getWorkingTotalSupply();

        Address lendingPoolCore = getAddress(Contracts.LENDING_POOL_CORE.getKey());

        BigInteger sicxIcxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
        BigInteger coresICXBalance = call(BigInteger.class, Contracts.sICX, "balanceOf", lendingPoolCore);
        BigInteger ommICXPower = exaMultiply(sicxIcxRate, exaDivide(coresICXBalance, workingTotal));

        DictDB<Integer, BigInteger> percentageDelegationsOfUser = _percentageDelegations.at(_user);
        DictDB<Integer, Address> userPreps = _userPreps.at(_user);

        for (int i = 0; i < PREP_COUNT; i++) {
            Address prep = userPreps.getOrDefault(i, ZERO_SCORE_ADDRESS);
            if (prep.equals(ZERO_SCORE_ADDRESS)){
                break;
            }
            BigInteger votesInPer = percentageDelegationsOfUser.getOrDefault(i, BigInteger.ZERO);
            BigInteger voteInICX = exaMultiply(ommICXPower, exaMultiply(votesInPer, userWorkingBalance));
            PrepICXDelegations prepICXDelegation = new PrepICXDelegations(prep, votesInPer, voteInICX);
            userDetails.add(prepICXDelegation);

        }
        return userDetails;
    }

    @External(readonly = true)
    public PrepDelegations[] computeDelegationPercentages() {
        BigInteger totalVotes = getWorkingTotalSupply();

        if (totalVotes.equals(BigInteger.ZERO)) {
            PrepDelegations[] defaultPreference = distributeVoteToContributors();
            BigInteger totalPercentage = BigInteger.ZERO;
            for (int i = 0; i < defaultPreference.length; i++) {
                BigInteger votes = defaultPreference[i]._votes_in_per.multiply(BigInteger.valueOf(100));
                totalPercentage = totalPercentage.add(votes);
                defaultPreference[i]._votes_in_per = votes;
            }
            BigInteger dustVotes = ICX.multiply(BigInteger.valueOf(100L)).subtract(totalPercentage);
            if (dustVotes.compareTo(BigInteger.ZERO) > 0) {
                defaultPreference[0]._votes_in_per = defaultPreference[0]._votes_in_per.add(dustVotes);
            }
            return defaultPreference;
        }

        List<Address> prepList = getPrepList();
        List<PrepDelegations> prepDelegations = new ArrayList<>();
        if (prepList.size() == 0) {
            return new PrepDelegations[0];
        }
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
                    maxVotePrepIndex = prepDelegations.size() - 1;
                }
            }
        }
        BigInteger dustVotes = ICX.multiply(BigInteger.valueOf(100)).subtract(totalPercentage);
        if (dustVotes.compareTo(BigInteger.ZERO) > 0) {
            PrepDelegations e = prepDelegations.get(maxVotePrepIndex);
            e._votes_in_per = e._votes_in_per.add(dustVotes);
            prepDelegations.set(maxVotePrepIndex, e);
        }
        return getPrepDelegations(prepDelegations);
    }

    private PrepDelegations[] getPrepDelegations(List<PrepDelegations> userDetails) {
        int size = userDetails.size();
        PrepDelegations[] prepDelegations = new PrepDelegations[size];

        for (int i = 0; i < size; i++) {
            prepDelegations[i] = userDetails.get(i);
        }

        return prepDelegations;
    }

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


    @EventLog(indexed = 1)
    public void UserKicked(Address user, byte[] _data) {}
}
