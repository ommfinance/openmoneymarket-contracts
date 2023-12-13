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
    protected final EnumerableDictDB<Address, BigInteger> collectedFee =
            new EnumerableDictDB<>("collected_fee", Address.class,BigInteger.class);
    protected final VarDB<BigInteger> validatorRewards = Context.newVarDB("validator_fee_collected",BigInteger.class);
    protected final DictDB<Address, BigInteger> accumulatedFee = Context.newDictDB("accumulated_fee", BigInteger.class);
    protected final EnumerableDictDB<Address, BigInteger> feeDistributionWeight = new
            EnumerableDictDB<>("fee_distribution_weight", Address.class, BigInteger.class);

    public AbstractFeeDistribution(Address addressProvider) {
        super(addressProvider, false);
    }


    protected void distributeFee(BigInteger amount) {
        BigInteger remaining = amount;
        Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address daoFundAddr = getAddress(Contracts.DAO_FUND.getKey());
        for (Address receiver : feeDistributionWeight.keySet()) {
            BigInteger percentageToDistribute = feeDistributionWeight.get(receiver);
            BigInteger amountToDistribute = (percentageToDistribute.multiply(amount)).divide(ICX);
            remaining = remaining.subtract(amountToDistribute);

            if (receiver.equals(lendingPoolCoreAddr)) {
                distributeFeeToValidator(lendingPoolCoreAddr,amountToDistribute);
                validatorRewards.set(getValidatorCollectedFee().add(amountToDistribute));
            } else if (receiver.equals(daoFundAddr)) {
                BigInteger feeCollected = collectedFee.getOrDefault(receiver, BigInteger.ZERO);
                collectedFee.put(receiver, feeCollected.add(amountToDistribute));
                call(Contracts.sICX, "transfer", receiver, amountToDistribute);

            } else {
                BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(receiver, BigInteger.ZERO);
                accumulatedFee.set(receiver, feeAccumulatedAfterClaim.add(amountToDistribute));
            }
        }
        if (remaining.signum() > 0){
            call(Contracts.sICX, "transfer", daoFundAddr, remaining);
        }
        FeeDistributed(amount);
    }


    private void distributeFeeToValidator(Address lendingPoolCoreAddr,BigInteger amount) {

        Map<String, BigInteger> ommValidators = call(Map.class, Contracts.STAKING,
                "getActualUserDelegationPercentage", lendingPoolCoreAddr);
        BigInteger amountToDistribute = amount;
        BigInteger denominator = BigInteger.valueOf(100).multiply(ICX);
        for (String validator : ommValidators.keySet()) {
            Address validatorAddr = Address.fromString(validator);

            BigInteger currentPercentage = ommValidators.get(validator);
            BigInteger feePortion = (currentPercentage.multiply(amountToDistribute)).divide(denominator);

            amountToDistribute = amountToDistribute.subtract(feePortion);
            denominator = denominator.subtract(currentPercentage);

            BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(validatorAddr, BigInteger.ZERO);
            accumulatedFee.set(validatorAddr, feeAccumulatedAfterClaim.add(feePortion));

        }

    }

    @EventLog(indexed = 3)
    public void FeeClaimed(Address caller, Address receiver, BigInteger amount) {
    }

    @EventLog(indexed = 1)
    public void FeeDistributed(BigInteger _value) {
    }

    protected void onlyOwner() {
        Address sender = Context.getCaller();
        Address owner = Context.getOwner();
        if (!sender.equals(owner)) {
            throw FeeDistributionException.notOwner();
        }
    }

}
