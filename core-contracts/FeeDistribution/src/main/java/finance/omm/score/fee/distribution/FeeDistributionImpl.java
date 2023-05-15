package finance.omm.score.fee.distribution;

import finance.omm.libs.address.Contracts;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;

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
        return feeDistribution.getOrDefault(address,BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger getFeeDistributed(Address address){
        return collectedFee.getOrDefault(address,BigInteger.ZERO);
    }

    @External
    public void setFeeDistribution(Address[] addresses, BigInteger[] weights){ // weight -> 10^16
        onlyOwner();
        if (!(addresses.length == weights.length)){
            throw FeeDistributionException.unknown(TAG + " :: Invalid pair length of arrays");
        }
        feeDistribution.clear();
        BigInteger totalWeight = BigInteger.ZERO;
        for (int i = 0; i < addresses.length; i++) {
            feeDistribution.put(addresses[i],weights[i]);

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
        distributeFee(_value);

    }

    @External
    public void claimValidatorsRewards(Address receiverAddress){

        Address caller = Context.getCaller();

        BigInteger amountToClaim = validatorsFee.getOrDefault(caller,BigInteger.ZERO);

        if (amountToClaim.compareTo(BigInteger.ZERO)<=0){
            throw FeeDistributionException.unknown(TAG + " :: Caller has no reward to claim");
        }

        validatorsFee.set(caller,BigInteger.ZERO);

        BigInteger feeCollected = collectedFee.getOrDefault(receiverAddress,BigInteger.ZERO);
        collectedFee.set(receiverAddress,feeCollected.add(amountToClaim));

        call(Contracts.sICX,"transfer",receiverAddress,amountToClaim);

        FeeClaimed(caller,receiverAddress,amountToClaim);
    }
}
