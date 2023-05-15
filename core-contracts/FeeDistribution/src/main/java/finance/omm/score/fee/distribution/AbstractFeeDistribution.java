package finance.omm.score.fee.distribution;

import finance.omm.core.score.interfaces.FeeDistribution;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import finance.omm.utils.db.EnumerableDictDB;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public abstract class AbstractFeeDistribution extends AddressProvider implements FeeDistribution {

    public static final String TAG = "Fee Distribution";
    protected final DictDB<Address, BigInteger> collectedFee = Context.newDictDB("fee_collected",BigInteger.class);
    protected final DictDB<Address,BigInteger> validatorsFee = Context.newDictDB("validators_fee",BigInteger.class);
    protected final EnumerableDictDB<Address,BigInteger> feeDistribution = new EnumerableDictDB<>("fee_distribution",
            Address.class,BigInteger.class);

    public AbstractFeeDistribution(Address addressProvider){
        super(addressProvider,false);
    }


    protected void distributeFee(BigInteger amount){
        for (Address receiver: feeDistribution.keySet()) {
            BigInteger percentageToDistribute = feeDistribution.get(receiver);
            BigInteger amountToDistribute = (percentageToDistribute.multiply(amount)).divide(ICX);

            Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
            if (receiver.equals(lendingPoolCoreAddr)){

                distributeFeeToValidator(amountToDistribute);
            }
            else {
                call(Contracts.sICX,"transfer",receiver,amountToDistribute);
                BigInteger feeCollected = collectedFee.getOrDefault(receiver,BigInteger.ZERO);
                collectedFee.set(receiver,feeCollected.add(amountToDistribute));
            }
        }
        FeeDistributed(amount);
    }


    private void distributeFeeToValidator(BigInteger amount){

        Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Map<String,BigInteger> ommValidators = call(Map.class,Contracts.STAKING,
                "getActualUserDelegationPercentage",lendingPoolCoreAddr);
        for (String validator : ommValidators.keySet()){
            BigInteger feePortion = (ommValidators.get(validator).divide(BigInteger.valueOf(100)).multiply(amount)).divide(ICX);
            BigInteger feeAccumulatedAfterClaim = validatorsFee.getOrDefault(Address.fromString(validator),BigInteger.ZERO);
            validatorsFee.set(Address.fromString(validator),feeAccumulatedAfterClaim.add(feePortion));

        }

    }

    @EventLog(indexed = 3)
    public void FeeClaimed(Address caller, Address receiver,BigInteger amount){}

    @EventLog(indexed = 1)
    public void FeeDistributed( BigInteger _value) {
    }

    protected void onlyOwner() {
        Address sender = Context.getCaller();
        Address owner = Context.getOwner();
        if (!sender.equals(owner)) {
            throw FeeDistributionException.notOwner();
        }
    }

}
