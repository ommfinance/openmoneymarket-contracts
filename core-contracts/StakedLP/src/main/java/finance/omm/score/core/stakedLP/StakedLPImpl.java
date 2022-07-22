package finance.omm.score.core.stakedLP;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

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

    // added new api to call totalStaked
    @External(readonly = true)
    public BigInteger totalStaked(int _id){
        return this.totalStaked.getOrDefault(_id,ZERO);
    }

    @External(readonly = true)
    public Map<String, BigInteger> balanceOf(Address _owner, int _id) {
        BigInteger id = BigInteger.valueOf(_id);
        BigInteger userBalance=call(BigInteger.class,Contracts.DEX,"balanceOf",_owner, id);
        BigInteger stakedBalance = poolStakeDetails.at(_owner).at(_id).getOrDefault(STAKED,ZERO);
        return Map.of(
                "poolID",id,
                "userTotalBalance",userBalance.add(stakedBalance) ,
                "userAvailableBalance",userBalance,
                "userStakedBalance", stakedBalance,
                "totalStakedBalance",totalStaked(_id));
    }

    @External(readonly = true)
    public List<Map<String, BigInteger>> getBalanceByPool() {
        List<Map<String,BigInteger>> result = new ArrayList<>();
        for (int i = 0; i < supportedPools.size(); i++) {
            BigInteger id = BigInteger.valueOf(supportedPools.get(i));
            BigInteger totalBalance = call(BigInteger.class, Contracts.DEX,"balanceOf",
                    Context.getAddress(), id);
            Map<String, BigInteger> stakedBalance = Map.of(
                    "poolID",id,
                    "totalStakedBalance",totalBalance);
            result.add(stakedBalance);
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
    public void addPool(int _id, Address _pool) {
        onlyContractOrElseThrow(Contracts.GOVERNANCE,
                StakedLPException.unauthorized("Sender not score governance error: (sender) " +
                        Context.getCaller()+ " governance " + getAddress(Contracts.GOVERNANCE.getKey())));
        this.addressMap.set(_id,_pool);
        boolean isSupported = inSupportedPools(_id);
        if (!isSupported){
            supportedPools.add(_id);
        }
        // throws when trying to add pool id which is already added.
//        else {
//            throw StakedLPException.unknown(_id +" pool already added");
//        }
    }

    @External(readonly = true)
    public Address getPoolById(int _id) {
        return this.addressMap.get(_id);
    }

    @External
    public void removePool(int _poolID) {
        onlyContractOrElseThrow(
                Contracts.GOVERNANCE,StakedLPException.unauthorized("Sender not score governance error: (sender) " +
                        Context.getCaller()+ " governance " + getAddress(Contracts.GOVERNANCE.getKey())));
        Address pool = this.addressMap.get(_poolID);
        if (pool == null){
            throw StakedLPException.unknown(TAG + ": " + _poolID + " is not in address map");
        }
        this.addressMap.set(_poolID,null);
        Integer top = this.supportedPools.pop();
        boolean isRemoved = top.equals(_poolID);

        if (!isRemoved){
            for (int i = 0; i < this.supportedPools.size(); i++) {
                if (this.supportedPools.get(i).equals(_poolID)) {
                    this.supportedPools.set(i,top);
                    isRemoved = true;
                }
            }
        }

        if (!isRemoved){
            throw StakedLPException.unknown(TAG + ": " + _poolID + "is not in supported pool list");
        }
    }

    @External(readonly = true)
    public Map<String, Address> getSupportedPools() {
        Map<String,Address> supportedPool = new HashMap<>();
        for (int i = 0; i < this.supportedPools.size(); i++) {
            int poolId = this.supportedPools.get(i);
            Address address = this.addressMap.get(poolId);
            supportedPool.put(String.valueOf(poolId),address);
        }
        return supportedPool;
    }


    @External
    public void unstake(int _id, BigInteger _value) {
        boolean isSupported = inSupportedPools(_id);
        if (!isSupported){
            throw StakedLPException.unknown("pool with id: " + _id + "is not supported");
        }

        if(_value.compareTo(ZERO)<=0){
            throw StakedLPException.unknown("Cannot unstake less than zero value to stake" + _value);
        }

        Address _user = Context.getCaller();
        BigInteger previousUserStaked = this.poolStakeDetails.at(_user).at(_id).getOrDefault(STAKED,ONE);
        BigInteger previousTotalStaked = totalStaked(_id);

        if (previousUserStaked.compareTo(_value) < 0){
            throw StakedLPException.unknown("Cannot unstake,user dont have enough staked balance" +
                    "amount to unstake " + _value +
                    "staked balance of user:" + _user  + "is" + previousUserStaked);
        }

        BigInteger afterUserStaked = previousUserStaked.subtract(_value);
        BigInteger afterTotalStaked = previousTotalStaked.subtract(_value);

        poolStakeDetails.at(_user).at(_id).set(STAKED,afterUserStaked);
        totalStaked.set(_id,afterTotalStaked);


        BigInteger decimals = getAverageDecimals(_id);
        Map<String,Object> userDetails = new HashMap<>();
        userDetails.put("_user",_user);
        userDetails.put("_userBalance", previousUserStaked);
        userDetails.put("_totalSupply", previousTotalStaked);
        userDetails.put("_decimals", decimals);

        call(Contracts.REWARDS,"handleLPAction",addressMap.get(_id),userDetails);
        BigInteger id = BigInteger.valueOf(_id);
        call(Contracts.DEX, "transfer", _user, id, _value, "transferBackToUser".getBytes());
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        onlyContractOrElseThrow(Contracts.DEX,StakedLPException.unauthorized(
                "Sender not score dex error: (sender) " +
                        Context.getCaller()+ " dex " + getAddress(Contracts.DEX.getKey())));

        String data = new String(_data);
        JsonObject json = Json.parse(data).asObject();

        String method = json.get("method").asString();

        if(method.equals("stake")){
            this.stake(_from, _id.intValue(), _value);
        }else {
            throw StakedLPException.unknown("No valid method called :: " + data);
        }
    }

    @External(readonly = true)
    public SupplyDetails getLPStakedSupply(int _id, Address _user) {
        Map<String, BigInteger> balance = balanceOf(_user, _id);
        SupplyDetails supplyDetails = new SupplyDetails();
        supplyDetails.decimals = getAverageDecimals(_id);
        supplyDetails.principalTotalSupply = balance.get("totalStakedBalance");
        supplyDetails.principalUserBalance = balance.get("userStakedBalance");
        return supplyDetails;
    }
}
