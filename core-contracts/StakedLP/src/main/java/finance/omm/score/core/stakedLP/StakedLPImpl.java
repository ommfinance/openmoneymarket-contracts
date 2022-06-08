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
import scorex.util.HashMap;
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
        BigInteger decimals = getAverageDecimals(_id);

        TotalStaked totalStaked = new TotalStaked();
        totalStaked.decimals = decimals;
        totalStaked.totalStaked = this.totalStaked.get(_id);

        return totalStaked;
    }

    @External(readonly = true)
    public Map<String, BigInteger> balanceOf(Address _owner, int _id) {
        BigInteger userBalance=call(BigInteger.class,Contracts.DEX,"balanceOf",_owner, _id);

        Map<String, BigInteger> balance = new HashMap<>();

        balance.put("poolID",BigInteger.valueOf(_id));
        balance.put("userTotalBalance",userBalance.add(this.poolStakeDetails.at(_owner).at(_id).getOrDefault(STAKED,BigInteger.ONE)));
        balance.put("userAvailableBalance",userBalance);
        balance.put("userStakedBalance",userBalance.add(this.poolStakeDetails.at(_owner).at(_id).getOrDefault(STAKED,BigInteger.ONE)));
        balance.put("totalStakedBalance",totalStaked.get(_id));
        return balance;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getBalanceByPool() {
        List<Map<String, BigInteger>> result = new ArrayList<>();


        for (int i = 0; i < supportedPools.size(); i++) {
            int _id = this.supportedPools.get(i);
            BigInteger totalBalance = call(BigInteger.class, Contracts.DEX,"balanceOf",Context.getAddress(), _id);
            Map<String, BigInteger> pool_details = new HashMap<>();

            pool_details.put("poolID",BigInteger.valueOf(_id));
            pool_details.put("totalStakedBalance",totalBalance);

            result.add(pool_details);

        }
        return result;
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getPoolBalanceByUser(Address _owner) {

        List<Map<String, BigInteger>> result = new ArrayList<>();
        for (int i = 0; i < supportedPools.size(); i++) {
            int _id = this.supportedPools.get(i);
            Map<String, BigInteger> userBalance = this.balanceOf(_owner,_id);

            result.add(userBalance);

        }
        return result;
    }

    @External
    public void addPool(int _poolID, Address asset) {
        onlyContractOrElseThrow(Contracts.GOVERNANCE,StakedLPException.unknown("Not Governance contract"));
        this.addressMap.set(_poolID,asset);
        int size = supportedPools.size();

        for(int x=0; x<size; x++ ) {
            if (!(supportedPools.get(x) == _poolID)) {
                this.supportedPools.add(_poolID);
            }
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
        this.addressMap.set(_poolID,null);

        int top = this.supportedPools.pop();
        //change
        boolean _is_removed = top == _poolID;
        if(!_is_removed) {
            for (int i = 0; i < supportedPools.size(); i++) {
                int _id = this.supportedPools.get(i);
                if(_id == _poolID){
                    this.supportedPools.set(_id,top);
                    _is_removed = true;
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
        Map<String, Address> pools = new HashMap<>();
        int size = this.supportedPools.size();

        for(int x=0; x<size; x++ ){
            int pool = supportedPools.get(x);
            pools.put(String.valueOf(pool),addressMap.get(pool));
        }
        return pools;
    }


    @External
    public void unstake(int _id, BigInteger _value) {
        int size = supportedPools.size();

        for(int x=0; x<size; x++ ){
            if(!(supportedPools.get(x) == _id)){
                throw StakedLPException.unknown("pool with id:" + _id +" is not supported");
            }
        }

        if(_value.compareTo(ZERO)<0){
            throw StakedLPException.unknown("Cannot unstake less than zero\nvalue to stake" + _value);
        }

        Address _user = Context.getCaller();
        BigInteger previousUserStaked = this.poolStakeDetails.at(Context.getCaller()).at(_id).getOrDefault(STAKED,BigInteger.ONE);
        BigInteger previousTotalStaked = this.totalStaked.get(_id);

        if(previousUserStaked.compareTo(_value)<0){
            throw StakedLPException.unknown("Cannot unstake,user dont have enough staked balance\namount to unstake" + _value + "staked balance of user: " +_user+ " is " +previousUserStaked);
        }

        BigInteger decimals = getAverageDecimals(_id);
        Map<String,Object> userDetails = new HashMap<>();
        userDetails.put("_user",_user);
        userDetails.put("_userBalance", previousUserStaked);
        userDetails.put("_totalSupply", previousTotalStaked);
        userDetails.put("_decimals", decimals);

        call(Contracts.DEX, "transfer", _user,_value,_id,"transferBackToUser");
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        onlyContractOrElseThrow(Contracts.DEX,StakedLPException.unknown("Not Governance contract"));

        String data = new String(_data);
        JsonObject json = Json.parse(data).asObject();

        String method = json.get("method").asString();
        JsonObject params = json.get("params").asObject();

        if(method.equals("stake")){
            this.stake(_from, _id.intValue(), _value);
        }else {
            throw StakedLPException.unknown("No valid method called :: " + data);
        }
    }

    @External(readonly = true)
    public SupplyDetails getLPStakedSupply(int _id, Address _user) {
        Map<String, BigInteger> balance = balanceOf(_user, _id);

        SupplyDetails supply = new SupplyDetails();
        supply.decimals = getAverageDecimals(_id);
        supply.principalUserBalance = balance.get("userStakedBalance");
        supply.principalTotalSupply = balance.get("totalStakedBalance");
        return supply;
    }

}
