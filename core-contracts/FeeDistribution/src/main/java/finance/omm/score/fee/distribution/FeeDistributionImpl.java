package finance.omm.score.fee.distribution;

import finance.omm.libs.address.Contracts;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;

import static finance.omm.utils.checks.ArrayChecks.containsDuplicate;
import static finance.omm.utils.math.MathUtils.ICX;

public class FeeDistributionImpl extends AbstractFeeDistribution {

    public FeeDistributionImpl(Address addressProvider) {
        super(addressProvider);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External(readonly = true)
    public BigInteger getFeeDistributionOf(Address address){
        return feeDistributionWeight.getOrDefault(address,BigInteger.ZERO);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getFeeDistributionWeight() {
        Map<String, BigInteger> weight = new HashMap<>();
        for (Address key : feeDistributionWeight.keySet()) {
            weight.put(key.toString(), feeDistributionWeight.get(key));
        }
        return weight;
    }

    @External(readonly = true)
    public BigInteger getCollectedFee(Address address){
        return collectedFee.getOrDefault(address,BigInteger.ZERO);
    }

    @External(readonly = true)
    public Map<String,BigInteger> getAllCollectedFees(){
        Map<String,BigInteger> collectedFeeMap = new HashMap<>();
        for (Address key: collectedFee.keySet()) {
            collectedFeeMap.put(key.toString(),collectedFee.get(key));
        }
        return collectedFeeMap;
    }
    @External(readonly = true)
    public BigInteger getValidatorCollectedFee(){
        return validatorRewards.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getAccumulatedFee(Address address){
        return accumulatedFee.getOrDefault(address,BigInteger.ZERO);
    }

    @External
    public void setFeeDistribution(Address[] addresses, BigInteger[] weights){
        // Note: when setting addresses validator address should precede daoFund address for complete distribution
        onlyOwner();
        int addressSize = addresses.length;
        if (!(addressSize == weights.length)){
            throw FeeDistributionException.unknown(TAG + " :: Invalid pair length of arrays");
        }
        if (containsDuplicate(addresses)){
            throw FeeDistributionException.unknown(TAG + " :: Array has duplicate addresses");
        }
        feeDistributionWeight.clear();
        BigInteger totalWeight = BigInteger.ZERO;
        for (int i = 0; i < addressSize; i++) {
            feeDistributionWeight.put(addresses[i],weights[i]);

            totalWeight = totalWeight.add(weights[i]);
        }

        if (!totalWeight.equals(ICX)){
            throw FeeDistributionException.unknown(TAG + " :: Sum of percentages not equal to 100 " + totalWeight);
        }
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data){
        Address caller = Context.getCaller();
        Address sICX = getAddress(Contracts.sICX.getKey());
        if (!caller.equals(sICX)){
            throw FeeDistributionException.unauthorized();
        }
        BigInteger currentFeeInSystem = this.feeToDistribute.getOrDefault(BigInteger.ZERO);
        this.feeToDistribute.set(currentFeeInSystem.add(_value));
        FeeDistributed(_value);
    }


    @External
    public void claimRewards(@Optional Address receiverAddress){

        Address caller = Context.getCaller();
        if (receiverAddress == null) {
            receiverAddress = caller;
        }

        BigInteger fee = this.feeToDistribute.getOrDefault(BigInteger.ZERO);
        if(fee.signum() > 0){
            distributeFee(fee);
        }

        BigInteger amountToClaim = accumulatedFee.getOrDefault(caller,BigInteger.ZERO);

        if (amountToClaim.compareTo(BigInteger.ZERO)<=0){
            throw FeeDistributionException.unknown(TAG + " :: Caller has no reward to claim");
        }

        accumulatedFee.set(caller,null);

        BigInteger feeCollected = collectedFee.getOrDefault(receiverAddress,BigInteger.ZERO);
        collectedFee.put(receiverAddress,feeCollected.add(amountToClaim));

        call(Contracts.sICX,"transfer",receiverAddress,amountToClaim);

        FeeClaimed(caller,receiverAddress,amountToClaim);
    }
}
