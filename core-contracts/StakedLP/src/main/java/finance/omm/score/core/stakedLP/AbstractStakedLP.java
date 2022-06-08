package finance.omm.score.core.stakedLP;

import finance.omm.core.score.interfaces.StakedLP;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.stakedLP.exception.StakedLPException;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import scorex.util.HashMap;

public abstract class AbstractStakedLP extends AddressProvider implements StakedLP,
        Authorization<StakedLPException> {
    public static final String TAG = "Staked Lp";
    public static final BigInteger ZERO = BigInteger.ZERO;
    public final ArrayDB<Integer> supportedPools = Context.newArrayDB("supportedPools", Integer.class);
    public final BranchDB<Address,BranchDB<Integer,DictDB<Integer,BigInteger>>>  poolStakeDetails = Context.newBranchDB(
            "poolStakeDetails", BigInteger.class);
    public final DictDB<Integer,BigInteger> totalStaked = Context.newDictDB("totalStaked", BigInteger.class);
    public final DictDB<Integer, Address> addressMap = Context.newDictDB("addressMap", Address.class);
    public final VarDB<BigInteger> minimumStake = Context.newVarDB("minimumStake", BigInteger.class);

    public static final Integer STAKED = 1;


    public AbstractStakedLP(Address addressProvider, boolean _update) {
        super(addressProvider, _update);

        if (minimumStake.get() == null) {
            minimumStake.set(ZERO);
        }
    }

    protected BigInteger getAverageDecimals(Integer _id){
        Map<String, BigInteger> pool_stats = call(Map.class, Contracts.DEX, "getPoolStats", BigInteger.valueOf(_id));
        BigInteger quote_decimals = pool_stats.get("quote_decimals");
        BigInteger base_decimals = pool_stats.get("base_decimals");
        BigInteger average_decimals = quote_decimals.add(base_decimals).divide(BigInteger.TWO);
        return average_decimals;
    }

    protected void stake(Address _user, Integer _id, BigInteger _value) {
        //
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
        BigInteger previousUserStaked = this.poolStakeDetails.at(Context.getCaller()).at(_id).getOrDefault(STAKED,BigInteger.ONE);
        BigInteger previousTotalStaked = this.totalStaked.get(_id);

        BigInteger decimals = getAverageDecimals(_id);
        Map<String,Object> userDetails = new HashMap<>();
        userDetails.put("_user",_user);
        userDetails.put("_userBalance", previousUserStaked);
        userDetails.put("_totalSupply", previousTotalStaked);
        userDetails.put("_decimals", decimals);

//        UserDetails userDetails = new UserDetails();
//        userDetails._user = _user;
//        userDetails._userBalance = previousUserStaked;
//        userDetails._totalSupply = previousTotalStaked;
//        userDetails._decimals = getAverageDecimals(_id);

        call(Contracts.REWARDS, "handleLPAction", addressMap.get(_id), userDetails);

    }



}
