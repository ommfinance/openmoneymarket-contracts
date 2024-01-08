package finance.omm.score.staking;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import finance.omm.core.score.interfaces.Staking;
import finance.omm.core.score.interfaces.SystemInterface;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.score.staking.db.DelegationListDBSdo;
import finance.omm.score.staking.db.LinkedListDB;
import finance.omm.score.staking.db.NodeDB;
import finance.omm.score.staking.utils.Constant;
import finance.omm.score.staking.utils.UnstakeDetails;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static finance.omm.score.staking.db.LinkedListDB.DEFAULT_NODE_ID;
import static finance.omm.score.staking.utils.Checks.checkStatus;
import static finance.omm.score.staking.utils.Checks.onlyOwner;
import static finance.omm.score.staking.utils.Checks.stakingOn;
import static finance.omm.score.staking.utils.Constant.*;
import static finance.omm.score.staking.utils.Constant.TAG;

public class StakingImpl implements Staking {

    private final VarDB<BigInteger> rate = Context.newVarDB(RATE, BigInteger.class);
    private final VarDB<BigInteger> blockHeightWeek = Context.newVarDB(BLOCK_HEIGHT_WEEK, BigInteger.class);
    private final VarDB<Address> sicxAddress = Context.newVarDB(SICX_ADDRESS, Address.class);
    private final VarDB<BigInteger> totalStake = Context.newVarDB(TOTAL_STAKE, BigInteger.class);
    private final VarDB<BigInteger> totalLifetimeReward = Context.newVarDB(TOTAL_LIFETIME_REWARD, BigInteger.class);
    private final VarDB<BigInteger> productivity = Context.newVarDB(PRODUCTIVITY, BigInteger.class);
    private final VarDB<BigInteger> totalUnstakeAmount = Context.newVarDB(TOTAL_UNSTAKE_AMOUNT, BigInteger.class);
    private final ArrayDB<Address> topPreps = Context.newArrayDB(TOP_PREPS, Address.class);
    private final ArrayDB<Address> validPreps = Context.newArrayDB(VALID_PREPS, Address.class);
    private final VarDB<BigInteger> icxToClaim = Context.newVarDB(ICX_TO_CLAIM, BigInteger.class);
    private final DictDB<Address, BigInteger> icxPayable = Context.newDictDB(ICX_PAYABLE, BigInteger.class);
    private final VarDB<BigInteger> unstakeBatchLimit = Context.newVarDB(UNSTAKE_BATCH_LIMIT, BigInteger.class);
    public static final VarDB<Boolean> stakingOn = Context.newVarDB(STAKING_ON, Boolean.class);
    private final LinkedListDB unstakeRequestList = new LinkedListDB(UNSTAKE_DICT);
    private final DictDB<Address, DelegationListDBSdo> userDelegationInPercentage =
            Context.newDictDB(USER_DELEGATION_PERCENTAGE, DelegationListDBSdo.class);
    private final VarDB<DelegationListDBSdo> prepDelegationInIcx = Context.newVarDB(PREP_DELEGATION_ICX,
            DelegationListDBSdo.class);
    private final VarDB<String> currentVersion = Context.newVarDB(VERSION, String.class);
    public final static String STAKING_VERSION = "v1.0.2";
    private final VarDB<Address> statusManager = Context.newVarDB(STATUS_MANAGER, Address.class);
    private final VarDB<BigInteger> feePercentage = Context.newVarDB(FEE_PERCENTAGE, BigInteger.class);
    private final VarDB<Address> feeDistributionAddress = Context.newVarDB(FEE_ADDRESS, Address.class);
    private final VarDB<Address> ommLendingPoolCore = Context.newVarDB(OMM_LENDING_POOL_CORE, Address.class);
    private final VarDB<Address> ommDelegation = Context.newVarDB(OMM_DELEGATION, Address.class);
    private final VarDB<BigInteger> commissionRate = Context.newVarDB(COMMISSION_RATE,BigInteger.class);



    public StakingImpl( @Optional BigInteger _feePercentage, @Optional BigInteger _productivity, 
                        @Optional Address lendingPoolCore, @Optional Address feeDistribution, @Optional Address delegation,
                        @Optional BigInteger _commissionRate) {

        if (blockHeightWeek.get() == null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> termDetails = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getIISSInfo");
            BigInteger nextPrepTerm = (BigInteger) termDetails.get("nextPRepTerm");
            blockHeightWeek.set(nextPrepTerm);
            rate.set(ONE_EXA);
            productivity.set(new BigInteger("90").multiply(ONE_EXA));
            setTopPreps();
            unstakeBatchLimit.set(DEFAULT_UNSTAKE_BATCH_LIMIT);
            stakingOn.set(false);
        }

        if (currentVersion.getOrDefault("").equals(STAKING_VERSION)) {
            Context.revert("Can't Update same version of code");
        }
        currentVersion.set(STAKING_VERSION);
        if (STAKING_VERSION.equals("v1.0.2")) {
            productivity.set(_productivity);
            feePercentage.set(_feePercentage);
            feeDistributionAddress.set(feeDistribution);
            ommLendingPoolCore.set(lendingPoolCore);
            ommDelegation.set(delegation);
            commissionRate.set(_commissionRate);

        }

    }

    @External
    public void updatePreps(){

        int totalPreps = this.topPreps.size();
        for (int i = 0; i < totalPreps; i++) {
            this.topPreps.removeLast();
        }
        List<Address> validPreps = getValidPreps();
        int size = validPreps.size();

        for (int i = 0; i < size; i++) {
            this.validPreps.removeLast();
        }
        setTopPreps();
    }

    // Event logs

    @EventLog(indexed = 2)
    public void FundTransfer(Address destination, BigInteger amount, String note) {
    }

    @EventLog(indexed = 2)
    public void TokenTransfer(Address recipient, BigInteger amount, String note) {
    }

    @EventLog(indexed = 2)
    public void UnstakingUpdate(BigInteger currentBlock, BigInteger nextUnstakeBlock) {
    }

    @EventLog(indexed = 2)
    public void UnstakeRequest(Address sender, BigInteger amount) {
    }

    @EventLog(indexed = 2)
    public void UnstakeAmountTransfer(Address receiver, BigInteger amount) {
    }

    @EventLog(indexed = 2)
    public void IscoreClaimed(BigInteger block_height, BigInteger rewards) {
    }

    // Read Only methods
    @External(readonly = true)
    public String name() {
        return TAG;
    }

    @External(readonly = true)
    public String version() {
        return currentVersion.getOrDefault("");
    }

    @External
    public void setEmergencyManager(Address _address) {
        onlyOwner();
        statusManager.set(_address);
    }

    @External(readonly = true)
    public Address getEmergencyManager() {
        return statusManager.get();
    }

    @External
    public void setBlockHeightWeek(BigInteger _height) {
        onlyOwner();
        blockHeightWeek.set(_height);
    }

    @External(readonly = true)
    public BigInteger getBlockHeightWeek() {
        return blockHeightWeek.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getTodayRate() {
        return rate.getOrDefault(ONE_EXA);
    }

    @External
    public void toggleStakingOn() {
        onlyOwner();
        stakingOn.set(!stakingOn.getOrDefault(false));
    }

    @External(readonly = true)
    public boolean getStakingOn() {
        return stakingOn.getOrDefault(false);
    }

    @External
    public void setSicxAddress(Address _address) {
        onlyOwner();
        Context.require(_address.isContract(), TAG + ": Address provided is an EOA address. A contract " +
                "address is required.");
        sicxAddress.set(_address);
    }

    @External(readonly = true)
    public Address getSicxAddress() {
        return sicxAddress.get();
    }

    @External
    public void setFeePercentage(BigInteger _percentage) {
        onlyOwner();
        this.feePercentage.set(_percentage);
    }

    @External(readonly = true)
    public BigInteger getFeePercentage() {
        return this.feePercentage.get();
    }

    @External
    public void setFeeDistributionAddress(Address _address) {
        onlyOwner();
        Context.require(_address.isContract(), TAG + ": Address provided is an EOA address. A contract " +
                "address is required.");
        feeDistributionAddress.set(_address);
    }

    @External(readonly = true)
    public Address getFeeDistributionAddress() {
        return feeDistributionAddress.get();
    }

    @External
    public void setOmmLendingPoolCore(Address _address) {
        onlyOwner();
        Context.require(_address.isContract(), TAG + ": Address provided is an EOA address. A contract " +
                "address is required.");
        ommLendingPoolCore.set(_address);
    }

    @External
    public void setOmmDelegation(Address _address) {
        onlyOwner();
        Context.require(_address.isContract(), TAG + ": Address provided is an EOA address. A contract " +
                "address is required.");
        ommDelegation.set(_address);
    }

    @External(readonly = true)
    public Address getOmmLendingPoolCore() {
        return ommLendingPoolCore.get();
    }

    @External(readonly = true)
    public Address getOmmDelegation() {
        return ommDelegation.get();
    }

    @External
    public void setPrepProductivity(BigInteger _productivity) {
        onlyOwner();
        if (_productivity.compareTo(HUNDRED_PERCENTAGE) > 0 || _productivity.compareTo(BigInteger.ZERO) <0){
            Context.revert(TAG +" productivity is not in range");
        }
        productivity.set(_productivity);
    }

    @External(readonly = true)
    public BigInteger getPrepProductivity() {
        return productivity.get();
    }

    @External
    public void setUnstakeBatchLimit(BigInteger _limit) {
        onlyOwner();
        unstakeBatchLimit.set(_limit);
    }

    @External(readonly = true)
    public BigInteger getUnstakeBatchLimit() {
        return unstakeBatchLimit.get();
    }

    @External
    public void setCommissionRate(BigInteger rate){
        onlyOwner();
        commissionRate.set(rate);
    }

    @External(readonly = true)
    public BigInteger getCommissionRate(){
        return commissionRate.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public List<Address> getPrepList() {
        List<Address> topPreps = getTopPreps();
        Map<String, BigInteger> prepDelegations = prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
        for (String prep : prepDelegations.keySet()) {
            if (!topPreps.contains(Address.fromString(prep))) {
                topPreps.add(Address.fromString(prep));
            }
        }
        return topPreps;
    }

    @External(readonly = true)
    public BigInteger getUnstakingAmount() {
        return totalUnstakeAmount.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getTotalStake() {
        return totalStake.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getLifetimeReward() {
        return totalLifetimeReward.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getUndelegatedICX(){
        Map<String, BigInteger> prepDelegationInIcx =
                this.prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
        BigInteger delegatedICX = BigInteger.ZERO;
        List<Address> validPreps = getValidPreps();
        for (Map.Entry<String, BigInteger> prepDelegation : prepDelegationInIcx.entrySet()) {
            Address prep = Address.fromString(prepDelegation.getKey());
            if (validPreps.contains(prep)) {
                delegatedICX = delegatedICX.add(prepDelegation.getValue());
            }
        }
        BigInteger totalStake = getTotalStake();

        return totalStake.subtract(delegatedICX);
    }

    @External(readonly = true)
    public List<Address> getTopPreps() {
        int topPrepsCount = this.topPreps.size();
        List<Address> topPreps = new ArrayList<>(topPrepsCount);
        for (int i = 0; i < topPrepsCount; i++) {
            Address prep = this.topPreps.get(i);
            topPreps.add(prep);
        }
        return topPreps;
    }

    @External(readonly = true)
    public List<Address> getValidPreps() {
        int validPrepsCount = this.validPreps.size();
        List<Address> validPreps = new ArrayList<>(validPrepsCount);
        for (int i = 0; i < validPrepsCount; i++) {
            Address prep = this.validPreps.get(i);
            validPreps.add(prep);
        }
        return validPreps;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getbOMMDelegations() {
        BigInteger unspecifiedICX = getUndelegatedICX();
        Map<String, BigInteger> ommDelegations = getActualUserDelegationPercentage(getOmmLendingPoolCore());
        BigInteger ommPrepSize = BigInteger.valueOf(ommDelegations.size());
        BigInteger remaining = unspecifiedICX;

        List<Address> validPreps = getValidPreps();
        Map<String, BigInteger> allPrepDelegations = new HashMap<>();
        BigInteger topPrepSpecification = BigInteger.ZERO;
        if (ommPrepSize.compareTo(BigInteger.ZERO) > 0) {
            for (Map.Entry<String, BigInteger> prepSet : ommDelegations.entrySet()) {
                Address prep = Address.fromString(prepSet.getKey());
                if (validPreps.contains(prep)) {

                    BigInteger percentageDelegation = prepSet.getValue();
                    BigInteger amountToAdd = unspecifiedICX.multiply(percentageDelegation).divide(HUNDRED_PERCENTAGE);

                    remaining = remaining.subtract(amountToAdd);
                    if (prep.toString().equals(validPreps.get(0).toString())) {
                        topPrepSpecification = amountToAdd;
                    }
                    allPrepDelegations.put(prep.toString(), amountToAdd);
                }
            }
        }
        if (remaining.compareTo(BigInteger.ZERO) > 0) {
            allPrepDelegations.put(validPreps.get(0).toString(), remaining.add(topPrepSpecification));
        }
        return allPrepDelegations;
    }


    @External(readonly = true)
    public Map<String, BigInteger> getPrepDelegations() {
        List<Address> validPreps = getValidPreps();

        Map<String, BigInteger> allPrepDelegations = getbOMMDelegations();
        Map<String, BigInteger> actualPrepDelegations = getActualPrepDelegations();


        for (String prep : actualPrepDelegations.keySet()) {
                    if (validPreps.contains(Address.fromString(prep))) {
                        //  Unsupported JCL method: getOrDefault in java.util.Map
                       BigInteger amount = allPrepDelegations.get(prep);
                       if (amount == null){
                           amount = BigInteger.ZERO;
                       }
                       allPrepDelegations.put(prep, amount.add(actualPrepDelegations.get(prep)));
                    }
        }

        return allPrepDelegations;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getActualPrepDelegations() {
        return prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
    }

    @External(readonly = true)
    public Map<String, BigInteger> getActualUserDelegationPercentage(Address user) {
        return userDelegationInPercentage.getOrDefault(user, DEFAULT_DELEGATION_LIST).toMap();
    }

    @External(readonly = true)
    public BigInteger claimableICX(Address _address) {
        return icxPayable.getOrDefault(_address, BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger totalClaimableIcx() {
        return icxToClaim.getOrDefault(BigInteger.ZERO);
    }

    @Payable
    public void fallback() {
        checkStatus(statusManager);
        stakeICX(Context.getCaller(), null);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        checkStatus(statusManager);
        stakingOn();
        Context.require(Context.getCaller().equals(sicxAddress.get()), TAG + ": The Staking contract only accepts " +
                "sICX tokens.: " + sicxAddress.get());
        Context.require(_value.compareTo(BigInteger.ZERO) >0,TAG+ ": The Staking contract cannot unstake " +
                "value less than or equal to 0");
        String unpackedData = new String(_data);
        JsonObject json = Json.parse(unpackedData).asObject();
        String method = json.get("method").asString();
        if (method.equals("unstake")) {
            JsonValue user = json.get("user");
            if (user != null) {
                unstake(_from, _value, Address.fromString(json.get("user").asString()));
            } else {
                unstake(_from, _value, null);
            }
        } else {
            Context.revert(TAG + ": Invalid Parameters.");
        }
    }


    @SuppressWarnings("unchecked")
    private void checkForIscore() {
        Map<String, Object> iscoreDetails = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "queryIScore",
                Context.getAddress());
        BigInteger iscoreGenerated = (BigInteger) iscoreDetails.get("estimatedICX");
        if (iscoreGenerated.compareTo(BigInteger.ZERO) > 0) {
            Context.call(SYSTEM_SCORE_ADDRESS, "claimIScore");

            List<Address> topPrep = getTopPreps();
            List<Address> validPreps = getValidPreps();
            int size = validPreps.size();
            for (int i = 0; i < size; i++) {
                this.validPreps.removeLast();
            }
            for (Address prep: topPrep) {
                if (checkPrepStatus(prep)){
                    this.validPreps.add(prep);
                }
            }
            IscoreClaimed(BigInteger.valueOf(Context.getBlockHeight()), iscoreGenerated);
        }
    }

    private boolean checkPrepStatus(Address prepAddr){
        Map<String, Object> prepDict = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getPRep", prepAddr);
        BigInteger jailFlags = (BigInteger) prepDict.get("jailFlags");
        BigInteger commissionRate = (BigInteger) prepDict.get("commissionRate");

        return checkCriteria(jailFlags,commissionRate);
    }

    private boolean checkCriteria(BigInteger jailFlags, BigInteger commissionRate){
        boolean notJailed = true;

        if (jailFlags == null){
            notJailed = true;
        }else if (!jailFlags.equals(BigInteger.ZERO)){
            notJailed = false;
        }

        boolean validRate = true;
        if (commissionRate == null){
            validRate = true;
        }  else if (commissionRate.compareTo(getCommissionRate()) > 0) {
            validRate = false;
        }

        return notJailed && validRate;
    }

    @External
    public void claimUnstakedICX(@Optional Address _to) {
        checkStatus(statusManager);
        if (_to == null) {
            _to = Context.getCaller();
        }
        BigInteger payableIcx = claimableICX(_to);
        Context.require(payableIcx.signum() > 0,
                TAG + ": No claimable icx to claim");
        BigInteger icxToClaim = totalClaimableIcx();
        Context.require(payableIcx.compareTo(icxToClaim) <= 0,
                TAG + ": No sufficient icx to claim. Requested: " + payableIcx + " Available: " + icxToClaim);

        BigInteger unclaimedIcx = icxToClaim.subtract(payableIcx);
        this.icxToClaim.set(unclaimedIcx);
        icxPayable.set(_to, null);
        sendIcx(_to, payableIcx, "");
        UnstakeAmountTransfer(_to, payableIcx);
    }

    private void sendIcx(Address to, BigInteger amount, String msg) {
        if (msg == null) {
            msg = "";
        }
        Context.transfer(to, amount);
        FundTransfer(to, amount, msg + amount + " ICX sent to " + to + ".");
    }

    @SuppressWarnings("unchecked")
    private List<Address> setTopPreps() {
        Map<String, Object> prepDict = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getPReps",
                BigInteger.ONE, Constant.TOP_PREP_COUNT);
        List<Map<String, Object>> prepDetails = (List<Map<String, Object>>) prepDict.get("preps");
        List<Address> validPreps = new ArrayList<>();
        BigInteger productivity = this.productivity.get();
        for (Map<String, Object> preps : prepDetails) {
            Address prepAddress = (Address) preps.get("address");
            BigInteger totalBlocks = (BigInteger) preps.get("totalBlocks");
            BigInteger validatedBlocks = (BigInteger) preps.get("validatedBlocks");
            BigInteger power = (BigInteger) preps.get("power");
            BigInteger jailFlags = (BigInteger) preps.get("jailFlags");
            BigInteger commissionRate = (BigInteger) preps.get("commissionRate");
            if (power.equals(BigInteger.ZERO) || totalBlocks.compareTo(BigInteger.ZERO) <= 0) {
                continue;
            }

            BigInteger prepProductivity = validatedBlocks.multiply(HUNDRED_PERCENTAGE).divide(totalBlocks);
            if (prepProductivity.compareTo(productivity) > 0) {
                this.topPreps.add(prepAddress);
                if (checkCriteria(jailFlags,commissionRate)){
                    validPreps.add(prepAddress);
                    this.validPreps.add(prepAddress);
                }
            }
        }
        return validPreps;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAddressDelegations(Address _address) {
        Map<String, BigInteger> delegationIcx = new HashMap<>();
        Map<String, BigInteger> userDelegationInPercentage = this.userDelegationInPercentage.getOrDefault(_address,
                DEFAULT_DELEGATION_LIST).toMap();
        BigInteger balance = (BigInteger) Context.call(sicxAddress.get(), "balanceOf", _address);
        BigInteger totalIcxHold = balance.multiply(getTodayRate()).divide(ONE_EXA);
        if (userDelegationInPercentage.isEmpty()) {
            if (totalIcxHold.compareTo(BigInteger.ZERO) <= 0) {
                return Map.of();
            }
            // return amount distributed to omm preps
            List<Address> validPreps = getValidPreps();
            Map<String, BigInteger> ommDelegations = getActualUserDelegationPercentage(getOmmLendingPoolCore());
            BigInteger ommPrepSize = BigInteger.valueOf(ommDelegations.size());
            BigInteger remaining = totalIcxHold;
            BigInteger topPrepICXSpecification = BigInteger.ZERO;
            if (ommPrepSize.compareTo(BigInteger.ZERO) > 0) {
                for (Map.Entry<String, BigInteger> prepSet : ommDelegations.entrySet()) {
                    Address prep = Address.fromString(prepSet.getKey());
                    if (validPreps.contains(prep)) {
                        BigInteger percentageDelegation = prepSet.getValue();
                        BigInteger amountToAdd = totalIcxHold.multiply(percentageDelegation).divide(HUNDRED_PERCENTAGE);

                        remaining = remaining.subtract(amountToAdd);
                        delegationIcx.put(prep.toString(),amountToAdd);
                        if (prep.toString().equals(validPreps.get(0).toString())) {
                            topPrepICXSpecification = amountToAdd;
                        }

                    }
                }
            }
            if (remaining.compareTo(BigInteger.ZERO)>0){
                delegationIcx.put(validPreps.get(0).toString(),remaining.add(topPrepICXSpecification));
            }
        } else {
            BigInteger totalPercentage = HUNDRED_PERCENTAGE;
            for (Map.Entry<String, BigInteger> userDelegation : userDelegationInPercentage.entrySet()) {
                BigInteger votesInIcx = userDelegation.getValue().multiply(totalIcxHold).divide(totalPercentage);
                delegationIcx.put(userDelegation.getKey(), votesInIcx);
                totalIcxHold = totalIcxHold.subtract(votesInIcx);
                totalPercentage = totalPercentage.subtract(userDelegation.getValue());
            }
        }
        return delegationIcx;
    }

    public boolean checkValidPrep(Address prepAddr){
        Map<String, Object> prepDict = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getPRep", prepAddr);
        if (!prepDict.isEmpty()){
            return true;
        }
        return false;
    }

    private Map<String, BigInteger> verifyUserDelegation(PrepDelegations[] userDelegations) {
        Map<String, BigInteger> prepDelegations = new HashMap<>();
        BigInteger totalPercentage = BigInteger.ZERO;
        if (userDelegations.length == 0) {
            return prepDelegations;
        }
        List<Address> topPreps = getTopPreps();// TODO: should this be valid preps
        for (PrepDelegations userDelegation : userDelegations) {
            Address prepAddress = userDelegation._address;
            if (topPreps.contains(prepAddress) || checkValidPrep(prepAddress)){
                BigInteger votesInPercentage = userDelegation._votes_in_per;
                Context.require(votesInPercentage.compareTo(MINIMUM_DELEGATION_PERCENTAGE) >= 0, TAG + ": You " +
                        "should provide delegation percentage more than 0.001%.");
                Context.require(prepDelegations.get(prepAddress.toString()) == null, TAG + ": You can not delegate same " +
                        "P-Rep twice in a transaction.");
                prepDelegations.put(prepAddress.toString(), votesInPercentage);
                totalPercentage = totalPercentage.add(votesInPercentage);
            }
        }
        Context.require(totalPercentage.equals(HUNDRED_PERCENTAGE), TAG + ": Total delegations should be 100%.");
        return prepDelegations;
    }


    private void stakeAndDelegateInNetwork(BigInteger stakeAmount, Map<String, BigInteger> prepDelegations) {
        List<Address> validPreps = updateTopPreps();
        DelegationListDBSdo prepDelegationsList = DelegationListDBSdo.fromMap(prepDelegations);
        prepDelegationInIcx.set(prepDelegationsList);

        Context.call(SYSTEM_SCORE_ADDRESS, "setStake", stakeAmount);
        updateDelegationInNetwork(prepDelegations, validPreps, stakeAmount);
    }

    @SuppressWarnings("unchecked")
    private List<Address> updateTopPreps() {
        Map<String, Object> termDetails = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getIISSInfo");
        BigInteger nextPrepTerm = (BigInteger) termDetails.get("nextPRepTerm");
        BigInteger destinationBlock = blockHeightWeek.getOrDefault(BigInteger.ZERO).add(BLOCKS_IN_A_WEEK);
        if (nextPrepTerm.compareTo(destinationBlock) > 0) {
            blockHeightWeek.set(nextPrepTerm);
            int totalPreps = this.topPreps.size();
            for (int i = 0; i < totalPreps; i++) {
                this.topPreps.removeLast();
            }
            return setTopPreps();
        } else {
            return getValidPreps();
        }
    }

    @External
    public void delegateForUser(PrepDelegations[] _user_delegations, Address to){
        checkStatus(statusManager);
        stakingOn();
        if (!Context.getCaller().equals(getOmmDelegation())) {
            Context.revert(TAG + ": Only delegation contract can call this function.");
        }
        if (_user_delegations.length > 100){
            Context.revert(TAG + ": Cannot set more than 100 delegations");
        }
        delegation(_user_delegations,to);
    }

    @External
    public void delegate(PrepDelegations[] _user_delegations) {
        checkStatus(statusManager);
        stakingOn();
        Address to = Context.getCaller();
        if (_user_delegations.length > 100){
            Context.revert(TAG + ": Cannot set more than 100 delegations");
        }
        delegation(_user_delegations,to);
    }

    private void delegation(PrepDelegations[] _user_delegations, Address to){
        performChecksForIscoreAndUnstakedBalance();
        Map<String, BigInteger> previousDelegations = userDelegationInPercentage.getOrDefault(to,
                DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> newDelegations = verifyUserDelegation(_user_delegations);
        DelegationListDBSdo userDelegationList = DelegationListDBSdo.fromMap(newDelegations);
        userDelegationInPercentage.set(to, userDelegationList);

        BigInteger balance = (BigInteger) Context.call(sicxAddress.get(), "balanceOf", to);
        BigInteger icxHoldPreviously = balance.multiply(getTodayRate()).divide(ONE_EXA);

        Map<String, BigInteger> prepDelegations = prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();

        if (balance.compareTo(BigInteger.ZERO) > 0) {
            prepDelegations = subtractUserDelegationFromPrepDelegation(prepDelegations, previousDelegations,
                    icxHoldPreviously);
            prepDelegations = addUserDelegationToPrepDelegation(prepDelegations, newDelegations, icxHoldPreviously);
        }
        stakeAndDelegateInNetwork(totalStake.getOrDefault(BigInteger.ZERO), prepDelegations);
    }

    @SuppressWarnings("unchecked")
    private void performChecksForIscoreAndUnstakedBalance() {

        // Calculate ICX available through unstaking
        Map<String, Object> stakeInNetwork = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getStake",
                Context.getAddress());
        BigInteger totalUnstakeInNetwork = BigInteger.ZERO;
        List<Map<String, Object>> unstakeList = (List<Map<String, Object>>) stakeInNetwork.get("unstakes");
        if (!unstakeList.isEmpty()) {
            for (Map<String, Object> unstakeDetails : unstakeList) {
                BigInteger unstakedIcx = (BigInteger) unstakeDetails.get("unstake");
                totalUnstakeInNetwork = totalUnstakeInNetwork.add(unstakedIcx);
            }
        }
        BigInteger totalUnstakeAmount = this.totalUnstakeAmount.getOrDefault(BigInteger.ZERO);
        BigInteger unstakedICX = totalUnstakeAmount.subtract(totalUnstakeInNetwork);
        BigInteger msgValue = Context.getValue();
        // Staking in case of ongoing unstaking cancels icx in unstaking, thus msg value is added in unstaked amount
        BigInteger icxAdded = msgValue.min(totalUnstakeInNetwork);
        unstakedICX = unstakedICX.add(icxAdded);
        BigInteger dailyReward = Context.getBalance(Context.getAddress()).subtract(unstakedICX)
                .subtract(msgValue.subtract(icxAdded))
                .subtract(icxToClaim.getOrDefault(BigInteger.ZERO));

        // If there is I-Score generated then update the rate
        if (dailyReward.compareTo(BigInteger.ZERO) > 0) {
            BigInteger feeAmt = getFeePercentage().multiply(dailyReward).divide(HUNDRED_PERCENTAGE);

            // possible reentry in the future in case of additional features are added to the daofund or feeProvider
            BigInteger sicxToMint = (ONE_EXA.multiply(feeAmt)).divide(getTodayRate());
            Context.call(sicxAddress.get(), "mintTo", getFeeDistributionAddress(), sicxToMint,
                    "staking fee".getBytes());

            totalLifetimeReward.set(getLifetimeReward().add(dailyReward));
            BigInteger totalStake = getTotalStake();
            BigInteger newTotalStake = totalStake.add(dailyReward);
            BigInteger newRate;
            if (newTotalStake.equals(BigInteger.ZERO)) {
                newRate = ONE_EXA;
            } else {
                BigInteger totalSupply = (BigInteger) Context.call(sicxAddress.get(), "totalSupply");
                newRate = newTotalStake.multiply(ONE_EXA).divide(totalSupply);
            }
            rate.set(newRate);
            this.totalStake.set(newTotalStake);

            Map<String, BigInteger> prepDelegations = prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
            BigInteger totalIcxSpecification = BigInteger.ZERO;
            for (Map.Entry<String, BigInteger> prepDelegation : prepDelegations.entrySet()) {
                totalIcxSpecification = totalIcxSpecification.add(prepDelegation.getValue());
            }

            BigInteger additionalRewardForSpecification =
                    totalIcxSpecification.multiply(dailyReward).divide(totalStake);
            Map<String, BigInteger> finalPrepDelegation = new HashMap<>();
            for (Map.Entry<String, BigInteger> prepDelegation : prepDelegations.entrySet()) {
                BigInteger currentAmount = prepDelegation.getValue();
                BigInteger amountToAdd =
                        currentAmount.multiply(additionalRewardForSpecification).divide(totalIcxSpecification);
                finalPrepDelegation.put(prepDelegation.getKey(), currentAmount.add(amountToAdd));
                additionalRewardForSpecification = additionalRewardForSpecification.subtract(amountToAdd);
                totalIcxSpecification = totalIcxSpecification.subtract(currentAmount);
            }
            DelegationListDBSdo prepDelegationsList = DelegationListDBSdo.fromMap(finalPrepDelegation);
            prepDelegationInIcx.set(prepDelegationsList);
        }
        checkForIscore();
        checkForUnstakedBalance(unstakedICX, totalUnstakeAmount);
    }

    private void updateDelegationInNetwork(Map<String, BigInteger> prepDelegations, List<Address> validPreps,
                                           BigInteger totalStake) {
        Map<String, SystemInterface.Delegation> networkDelegationMap = new HashMap<>();

        BigInteger icxPreferredToTopPreps = BigInteger.ZERO;
        for (Map.Entry<String, BigInteger> prepDelegation : prepDelegations.entrySet()) {
            Address prep = Address.fromString(prepDelegation.getKey());
            if (validPreps.contains(prep)) {
                icxPreferredToTopPreps = icxPreferredToTopPreps.add(prepDelegation.getValue());

                SystemInterface.Delegation delegation = new SystemInterface.Delegation();
                delegation.address = prep;
                delegation.value = prepDelegation.getValue();

                networkDelegationMap.put(prep.toString(), delegation);
            }
        }

        BigInteger equallyDistributableIcx = totalStake.subtract(icxPreferredToTopPreps);

        Map<String, BigInteger> ommDelegations = getActualUserDelegationPercentage(getOmmLendingPoolCore());
        BigInteger ommPrepSize = BigInteger.valueOf(ommDelegations.size());
        BigInteger remaining = equallyDistributableIcx;
        if (ommPrepSize.compareTo(BigInteger.ZERO) > 0) {
            for (Map.Entry<String, BigInteger> prepSet : ommDelegations.entrySet()) {
                Address prep = Address.fromString(prepSet.getKey());
                if (validPreps.contains(prep)) {

                    BigInteger percentageDelegation = prepSet.getValue();
                    BigInteger amountToAdd = equallyDistributableIcx.multiply(percentageDelegation).divide(HUNDRED_PERCENTAGE);
                    updateDelegationMap(networkDelegationMap, prep, amountToAdd);
                    remaining = remaining.subtract(amountToAdd);
                }
            }
        }
        if (remaining.compareTo(BigInteger.ZERO) > 0) {

            Address validPrep = validPreps.get(0);
            updateDelegationMap(networkDelegationMap, validPrep, remaining);
        }

        List<SystemInterface.Delegation> finalNetworkDelegations = new ArrayList<>(networkDelegationMap.size());
        for (Map.Entry<String, SystemInterface.Delegation> networkDelegation : networkDelegationMap.entrySet()) {
            SystemInterface.Delegation value = networkDelegation.getValue();
            finalNetworkDelegations.add(value);
        }

        Context.call(SYSTEM_SCORE_ADDRESS, "setDelegation", finalNetworkDelegations);
    }

    private void updateDelegationMap(Map<String, SystemInterface.Delegation> delegationMap,
                                     Address prep, BigInteger amountToAdd) {
        SystemInterface.Delegation delegation = delegationMap.get(prep.toString());
        if (delegation != null) {
            BigInteger previousDelegation = delegation.value;
            delegation.value = previousDelegation.add(amountToAdd);
        } else {
            delegation = new SystemInterface.Delegation();
            delegation.address = prep;
            delegation.value = amountToAdd;
        }
        delegationMap.put(prep.toString(), delegation);
    }

    @External
    @Payable
    public BigInteger stakeICX(@Optional Address _to, @Optional byte[] _data) {
        checkStatus(statusManager);
        stakingOn();
        if (_data == null) {
            _data = new byte[0];
        }
        if (_to == null) {
            _to = Context.getCaller();
        }
        performChecksForIscoreAndUnstakedBalance();
        BigInteger addedIcx = Context.getValue();

        BigInteger sicxToMint = (ONE_EXA.multiply(addedIcx)).divide(getTodayRate());

        Map<String, BigInteger> userCurrentDelegation = userDelegationInPercentage.getOrDefault(_to,
                DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> prepDelegations = prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> finalDelegation;
        if (!userCurrentDelegation.isEmpty()) {
            finalDelegation = addUserDelegationToPrepDelegation(prepDelegations, userCurrentDelegation, addedIcx);
        } else {
            finalDelegation = prepDelegations;
        }
        BigInteger newTotalStake = this.totalStake.getOrDefault(BigInteger.ZERO).add(addedIcx);
        this.totalStake.set(newTotalStake);
        stakeAndDelegateInNetwork(newTotalStake, finalDelegation);

        emitUnstakingEvent();
        Context.call(sicxAddress.get(), "mintTo", _to, sicxToMint, _data);
        TokenTransfer(_to, sicxToMint, sicxToMint + " sICX minted to " + _to);
        return sicxToMint;
    }

    private void emitUnstakingEvent(){
        List<UnstakeDetails> unstakeDetails = unstakeRequestList.iterate();
        if (!unstakeDetails.isEmpty()){
            UnstakeDetails topUnstake = unstakeDetails.get(0);
            BigInteger nextUnstakeBlock =  topUnstake.unstakeBlockHeight;

            BigInteger currentBlockHeight = BigInteger.valueOf(Context.getBlockHeight());
            UnstakingUpdate(currentBlockHeight, nextUnstakeBlock);
        }
    }

    @External
    public void transferUpdateDelegations(Address _from, Address _to, BigInteger _value) {
        checkStatus(statusManager);
        stakingOn();
        if (!Context.getCaller().equals(sicxAddress.get())) {
            Context.revert(TAG + ": Only sicx token contract can call this function.");
        }

        Map<String, BigInteger> senderDelegationsInPercentage = userDelegationInPercentage.getOrDefault(_from,
                DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> receiverDelegationsInPercentage = userDelegationInPercentage.getOrDefault(_to,
                DEFAULT_DELEGATION_LIST).toMap();
        if (senderDelegationsInPercentage.isEmpty() && receiverDelegationsInPercentage.isEmpty()) {
            return;
        }

        BigInteger icxValue = _value.multiply(getTodayRate()).divide(ONE_EXA);
        Map<String, BigInteger> prepDelegationInIcx =
                this.prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();

        if (!receiverDelegationsInPercentage.isEmpty()) {
            prepDelegationInIcx = addUserDelegationToPrepDelegation(prepDelegationInIcx,
                    receiverDelegationsInPercentage, icxValue);
                   }
        if (!senderDelegationsInPercentage.isEmpty()) {
            prepDelegationInIcx = subtractUserDelegationFromPrepDelegation(prepDelegationInIcx,
                    senderDelegationsInPercentage, icxValue);
        }
        stakeAndDelegateInNetwork(totalStake.getOrDefault(BigInteger.ZERO), prepDelegationInIcx);
    }

    private Map<String, BigInteger> addUserDelegationToPrepDelegation(Map<String, BigInteger> prepDelegation,
                                                                      Map<String, BigInteger> userDelegationInPercentage, BigInteger amount) {
        Map<String, BigInteger> sumDelegation = new HashMap<>();
        sumDelegation.putAll(prepDelegation);
        BigInteger totalPercentage = HUNDRED_PERCENTAGE;
        for (Map.Entry<String, BigInteger> delegationInPercentage : userDelegationInPercentage.entrySet()) {
            BigInteger amountToAdd = delegationInPercentage.getValue().multiply(amount).divide(totalPercentage);
            String prepAddress = delegationInPercentage.getKey();
            BigInteger currentAmount = sumDelegation.get(prepAddress);
            currentAmount = currentAmount == null ? BigInteger.ZERO : currentAmount;
            sumDelegation.put(prepAddress, currentAmount.add(amountToAdd));
            totalPercentage = totalPercentage.subtract(delegationInPercentage.getValue());
            amount = amount.subtract(amountToAdd);
        }
        return sumDelegation;
    }

    private Map<String, BigInteger> subtractUserDelegationFromPrepDelegation(Map<String, BigInteger> prepDelegation,
                                                                             Map<String, BigInteger> userDelegationInPercentage, BigInteger amount) {
        Map<String, BigInteger> resultDelegation = new HashMap<>();
        resultDelegation.putAll(prepDelegation);
        BigInteger totalPercentage = HUNDRED_PERCENTAGE;
        for (Map.Entry<String, BigInteger> delegationInPercentage : userDelegationInPercentage.entrySet()) {
            BigInteger amountToReduce = delegationInPercentage.getValue().multiply(amount).divide(totalPercentage);
            String prepAddress = delegationInPercentage.getKey();
            BigInteger currentAmount = resultDelegation.get(prepAddress);
            if (currentAmount != null) {
                currentAmount = currentAmount.subtract(amountToReduce);
                if (currentAmount.compareTo(BigInteger.ZERO) > 0) {
                    resultDelegation.put(prepAddress, currentAmount);
                } else {
                    resultDelegation.remove(prepAddress);
                }
                totalPercentage = totalPercentage.subtract(delegationInPercentage.getValue());
                amount = amount.subtract(amountToReduce);
            }
        }
        return resultDelegation;
    }

    private void checkForUnstakedBalance(BigInteger unstakedICX, BigInteger totalUnstakeAmount) {

        if (unstakedICX.compareTo(BigInteger.ZERO) <= 0) {
            return;
        }

        BigInteger icxToClaim = this.icxToClaim.getOrDefault(BigInteger.ZERO);

        BigInteger currentId = unstakeRequestList.headId.getOrDefault(DEFAULT_NODE_ID);
        if (currentId.equals(DEFAULT_NODE_ID)) {
            return;
        }

        NodeDB node;
        UnstakeDetails unstakeData;
        BigInteger payout;
        int maxLoop = unstakeBatchLimit.getOrDefault(DEFAULT_UNSTAKE_BATCH_LIMIT).intValue();
        for (int i = 0; i < maxLoop; i++) {

            node = unstakeRequestList.getNode(currentId);
            unstakeData = new UnstakeDetails(currentId, node.getValue(), node.getKey(), node.getBlockHeight(),
                    node.getSenderAddress());

            BigInteger unstakeAmount = unstakeData.unstakeAmount;
            if (unstakeAmount.compareTo(unstakedICX) <= 0) {
                payout = unstakeAmount;
                unstakeRequestList.remove(currentId);
            } else {
                payout = unstakedICX;
                unstakeRequestList.updateNode(unstakeData.key, unstakeAmount.subtract(payout),
                        unstakeData.unstakeBlockHeight, unstakeData.receiverAddress, currentId);
            }
            totalUnstakeAmount = totalUnstakeAmount.subtract(payout);
            unstakedICX = unstakedICX.subtract(payout);
            icxToClaim = icxToClaim.add(payout);
            icxPayable.set(unstakeData.receiverAddress, icxPayable.getOrDefault(unstakeData.receiverAddress,
                    BigInteger.ZERO).add(payout));

            currentId = unstakeRequestList.headId.getOrDefault(DEFAULT_NODE_ID);

            if (currentId.equals(DEFAULT_NODE_ID)) {
                break;
            }

            if (unstakedICX.compareTo(BigInteger.ZERO) <= 0) {
                break;
            }
        }

        this.totalUnstakeAmount.set(totalUnstakeAmount);
        this.icxToClaim.set(icxToClaim);
    }

    @SuppressWarnings("unchecked")
    private void unstake(Address to, BigInteger value, Address senderAddress) {
        Context.call(sicxAddress.get(), "burn", value);
        BigInteger amountToUnstake = (value.multiply(getTodayRate())).divide(ONE_EXA);
        totalUnstakeAmount.set(totalUnstakeAmount.getOrDefault(BigInteger.ZERO).add(amountToUnstake));

        Map<String, BigInteger> userDelegationPercentage = userDelegationInPercentage.getOrDefault(to,
                DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> prepDelegations = prepDelegationInIcx.getOrDefault(DEFAULT_DELEGATION_LIST).toMap();
        Map<String, BigInteger> finalDelegation;
        if (!userDelegationPercentage.isEmpty()) {
            finalDelegation = subtractUserDelegationFromPrepDelegation(prepDelegations, userDelegationPercentage,
                    amountToUnstake);
        } else {
            finalDelegation = prepDelegations;
        }

        // Unstake in network. Reverse order of stake.
        BigInteger newTotalStake = getTotalStake().subtract(amountToUnstake);
        Context.require(newTotalStake.signum() >= 0, TAG + ": Total staked amount can't be set negative");
        List<Address> topPreps = updateTopPreps();
        totalStake.set(newTotalStake);
        DelegationListDBSdo prepDelegationsList = DelegationListDBSdo.fromMap(finalDelegation);
        prepDelegationInIcx.set(prepDelegationsList);

        // First set the decreased delegation and stake
        updateDelegationInNetwork(finalDelegation, topPreps, newTotalStake);
        Context.call(SYSTEM_SCORE_ADDRESS, "setStake", newTotalStake);

        // Add unstake details to unstake request list
        Address addressToSend = senderAddress != null ? senderAddress : to;
        Map<String, BigInteger> estimatedUnlockPeriod = (Map<String, BigInteger>) Context.call(SYSTEM_SCORE_ADDRESS,
                "estimateUnstakeLockPeriod");
        BigInteger unlockPeriod = estimatedUnlockPeriod.get("unstakeLockPeriod");
        long currentBlockHeight = Context.getBlockHeight();
        BigInteger unstakeHeight = BigInteger.valueOf(currentBlockHeight).add(unlockPeriod);
        unstakeRequestList.append(to, amountToUnstake, unstakeHeight, addressToSend,
                unstakeRequestList.tailId.getOrDefault(BigInteger.ZERO).add(BigInteger.ONE));
        UnstakeRequest(addressToSend, amountToUnstake);
    }


    @External(readonly = true)
    public List<List<Object>> getUnstakeInfo() {
        List<UnstakeDetails> unstakeDetails = unstakeRequestList.iterate();
        List<List<Object>> unstakeResponse = new ArrayList<>(unstakeDetails.size());
        for (UnstakeDetails unstakeDetail : unstakeDetails) {
            unstakeResponse.add(List.of(unstakeDetail.nodeId, unstakeDetail.unstakeAmount, unstakeDetail.key,
                    unstakeDetail.unstakeBlockHeight, unstakeDetail.receiverAddress));
        }
        return unstakeResponse;
    }

    @External(readonly = true)
    public List<Map<String, Object>> getUserUnstakeInfo(Address _address) {
        List<UnstakeDetails> linkedListIter = unstakeRequestList.iterate();
        List<Map<String, Object>> response = new ArrayList<>(linkedListIter.size());
        for (UnstakeDetails unstakeData : linkedListIter) {
            if (unstakeData.receiverAddress.equals(_address)) {
                response.add(Map.of("amount", unstakeData.unstakeAmount, "from", unstakeData.key, "blockHeight",
                        unstakeData.unstakeBlockHeight, "sender", unstakeData.receiverAddress));
            }
        }
        return response;
    }



}
