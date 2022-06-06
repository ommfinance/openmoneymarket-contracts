package finance.omm.score.core.stakedLP;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.libs.structs.UserDetails;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import score.Address;
import score.Context;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;

public class StakedLPImpl extends AbstractStakedLP {

    public StakedLPImpl(Address addressProvider) {

        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setMinimumStake(BigInteger _value) {
        onlyOwnerOrElseThrow(StakedLPException.notOwner());
        if (_value.compareTo(ZERO) < 0) {
            throw StakedLPException.unknown("Minimum stake value must be positive, " + _value);
        }
        minimumStake.set(_value);
    }

    @External(readonly = true)
    public BigInteger getMinimumStake() {

        return minimumStake.get();
    }

    @External(readonly = true)
    public TotalStaked getTotalStaked(int _id) {

        return null;
    }

    @External(readonly = true)
    public Map<Address, BigInteger> balanceOf(Address _owner, int _id) {

        return null;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getBalanceByPool() {

        return null;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getPoolBalanceByUser(Address _owner) {

        ArrayList result = new ArrayList();

        return null;
    }

    @External
    public void addPool(int _poolID, Address asset) {
        onlyContractOrElseThrow(Contracts.GOVERNANCE,StakedLPException.unknown("Not Governance contract"));
        this.addressMap.set(_poolID,asset);

        boolean found = false;
        int size = supportedPools.size();

        for(int x=0; x<size; x++ ){
            if(supportedPools.get(x) == _poolID){
                found = true;
                break;
            }
        }
        if(!found){
            this.supportedPools.add(_poolID);
        }
    }

    @External(readonly = true)
    public Address getPoolById(int _id) {

        return this.addressMap.get(_id);
    }

    @External
    public void removePool(int _poolID) {
        onlyContractOrElseThrow(Contracts.GOVERNANCE,StakedLPException.unknown("Not Governance contract"));
        Address pool = this.addressMap.get(_poolID);
        if(pool == null){
            throw StakedLPException.unknown(TAG+ ": "+ _poolID + " is not in address map");
        }
//        this.addressMap.get(_poolID);

        int top = this.supportedPools.pop();
        boolean _is_removed = top == _poolID;
        if(!_is_removed) {
            for (int i = 0; i < supportedPools.size(); i++) {
                if(this.supportedPools.get(i) == _poolID){
//                    this.supportedPools.r
                }

            }
        }
        if(!_is_removed) {
            throw StakedLPException.unknown(TAG+ ": "+ _poolID + " is not in supported pool list");
        }

    }

    @External(readonly = true)
    public Map<String, Address> getSupportedPools() {
        // cast poolId to string
        Map<String, Address> pools = null;
        int size = this.supportedPools.size();

        for(int x=0; x<size; x++ ){
            int pool = supportedPools.get(x);
            pools.put(String.valueOf(pool),addressMap.get(pool));
        }
        return pools;
    }


    public void _stake(Address _user, int _id, BigInteger _value) {
        boolean found = false;
        int size = supportedPools.size();

        for(int x=0; x<size; x++ ){
            if(supportedPools.get(x) == _id){
                found = true;
                break;
            }
        }
        if(!found){
            throw StakedLPException.unknown("pool with id:" + _id +" is not supported");
        }
        if(_value.compareTo(ZERO)<0){
            throw StakedLPException.unknown("Cannot stake less than zero, value to stake" + _value);
        }
        if(_value.compareTo(minimumStake.get())<=0){
            throw StakedLPException.unknown("Amount to stake:" + _value+ " is smaller the minimum stake:" + minimumStake.get());
        }
        BigInteger previousUserStaked = this.poolStakeDetails.at(Context.getCaller()).get(_id);
        BigInteger previousTotalStaked = this.totalStaked.get(Context.getCaller());

        UserDetails userDetails = new UserDetails();
        userDetails._user = _user;
        userDetails._userBalance = previousUserStaked;
        userDetails._totalSupply = previousTotalStaked;
        userDetails._decimals = getAverageDecimals(_id);
        call(Contracts.REWARDS, "handleLPAction", addressMap.get(_id), userDetails);
    }

    @External
    public void unstake(int _id, BigInteger _value) {
        boolean found = false;
        int size = supportedPools.size();

        for(int x=0; x<size; x++ ){
            if(supportedPools.get(x) == _id){
                found = true;
                break;
            }
        }
        if(!found){
            throw StakedLPException.unknown("pool with id:" + _id +" is not supported");
        }
        if(_value.compareTo(ZERO)<0){
            throw StakedLPException.unknown("Cannot unstake less than zero\nvalue to stake" + _value);
        }

        Address _user = Context.getCaller();
        BigInteger previousUserStaked = this.poolStakeDetails.at(Context.getCaller()).get(_id);
        BigInteger previousTotalStaked = this.totalStaked.get(Context.getCaller());

        if(previousUserStaked.compareTo(_value)<0){
            throw StakedLPException.unknown("Cannot unstake,user dont have enough staked balance\namount to unstake" + _value + "staked balance of user: " +_user+ " is " +previousUserStaked);
        }

        UserDetails userDetails = new UserDetails();
        userDetails._user = _user;
        userDetails._userBalance = previousUserStaked;
        userDetails._totalSupply = previousTotalStaked;
        userDetails._decimals = getAverageDecimals(_id);

        call(Contracts.DEX, "transfer", _user,_value,_id,"transferBackToUser");
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte _data) {
        onlyContractOrElseThrow(Contracts.DEX,StakedLPException.unknown("Not Governance contract"));

        String data = new String(String.valueOf(_data));
        JsonObject json = Json.parse(data).asObject();

        String method = json.get("method").asString();
        JsonObject params = json.get("params").asObject();

        if(method.equals("stake")){
            this._stake(_from, _id.intValue(), _value);
        }else {
            throw StakedLPException.unknown("No valid method called :: " + data);
        }
    }

    @External(readonly = true)
    public SupplyDetails getLPStakedSupply(int _id, Address _user) {
        Map<Address, BigInteger> balance = balanceOf(_user, _id);

        SupplyDetails supply = new SupplyDetails();
        supply.decimals = _getAverageDecimals(_id);
        supply.principalUserBalance = balance.get("userStakedBalance");
        supply.principalTotalSupply = balance.get("totalStakedBalance");
        return supply;
    }


    public BigInteger _getAverageDecimals(int _id) {
        Map<String, BigInteger> pool_stats = call(Map.class, Contracts.DEX, "getPoolStats", BigInteger.valueOf(_id));
        BigInteger quote_decimals = pool_stats.get("quote_decimals");
        BigInteger base_decimals = pool_stats.get("base_decimals");
        BigInteger average_decimals = quote_decimals.add(base_decimals).divide(BigInteger.TWO);
        return average_decimals;
    }
}
