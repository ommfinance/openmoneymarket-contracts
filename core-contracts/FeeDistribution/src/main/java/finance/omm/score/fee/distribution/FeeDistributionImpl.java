package finance.omm.score.fee.distribution;

import finance.omm.libs.address.Contracts;
import finance.omm.score.fee.distribution.exception.FeeDistributionException;
import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;

import static finance.omm.utils.math.MathUtils.ICX;

public class FeeDistributionImpl extends AbstractFeeDistribution {

    public FeeDistributionImpl(Address _addressProvider) {
        super(_addressProvider);
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
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
    public void setFeeDistribution(Address[] addresses, BigInteger[] weights){ // weight -> 10^18
        onlyOwner();
        if (!(addresses.length == weights.length)){
            throw FeeDistributionException.unknown(TAG + " Invalid pair length of arrays");
        }
        BigInteger totalWeight = BigInteger.ZERO;
        for (int i = 0; i < addresses.length; i++) {
            feeDistribution.put(addresses[i],weights[i]);

            totalWeight = totalWeight.add(weights[i]);
        }

        if (!totalWeight.equals(ICX.multiply(BigInteger.valueOf(100)))){
            throw FeeDistributionException.unknown(TAG + " sum of percentages not equal to 100 " + totalWeight);
        }
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data){
        distributeFee(_value);

    }

    @External
    public void claimValidatorsRewards(Address receiverAddress){

        Address caller = Context.getCaller();

        BigInteger amountToClaim = validatorsFee.getOrDefault(caller,BigInteger.ZERO);

        call(Contracts.sICX,"transfer",receiverAddress,amountToClaim); // here

        validatorsFee.set(caller,BigInteger.ZERO);

        BigInteger feeCollected = collectedFee.getOrDefault(receiverAddress,BigInteger.ZERO);
        collectedFee.set(receiverAddress,feeCollected.add(amountToClaim));

        FeeClaimed(caller,receiverAddress,amountToClaim);
    }
}
