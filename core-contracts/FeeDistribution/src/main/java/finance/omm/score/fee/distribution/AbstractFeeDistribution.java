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
    protected final DictDB<Address, BigInteger> collectedFee = Context.newDictDB("fee_collected", BigInteger.class);
    protected final VarDB<BigInteger> validatorRewards = Context.newVarDB("validator_fee_collected",BigInteger.class);
    protected final DictDB<Address, BigInteger> accumulatedFee = Context.newDictDB("accumulated_fee", BigInteger.class);
    protected final EnumerableDictDB<Address, BigInteger> feeDistributionWeight = new
            EnumerableDictDB<>("fee_distribution_weight", Address.class, BigInteger.class);

    public AbstractFeeDistribution(Address addressProvider) {
        super(addressProvider, false);
    }


    protected void distributeFee(BigInteger amount) {
        for (Address receiver : feeDistributionWeight.keySet()) {
            BigInteger percentageToDistribute = feeDistributionWeight.get(receiver);
            BigInteger amountToDistribute = (percentageToDistribute.multiply(amount)).divide(ICX);

            Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
            Address daoFundAddr = getAddress(Contracts.DAO_FUND.getKey());
            if (receiver.equals(lendingPoolCoreAddr)) {
                distributeFeeToValidator(amountToDistribute);
                validatorRewards.set(getValidatorCollectedFee().add(amountToDistribute));
            } else if (receiver.equals(daoFundAddr)) {
                BigInteger feeCollected = collectedFee.getOrDefault(receiver, BigInteger.ZERO);
                collectedFee.set(receiver, feeCollected.add(amountToDistribute));
                call(Contracts.sICX, "transfer", receiver, amountToDistribute);

            } else {
                BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(receiver, BigInteger.ZERO);
                accumulatedFee.set(receiver, feeAccumulatedAfterClaim.add(amountToDistribute));
            }
        }
        FeeDistributed(amount);
    }


    private void distributeFeeToValidator(BigInteger amount) {

        Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Map<String, BigInteger> ommValidators = call(Map.class, Contracts.STAKING,
                "getActualUserDelegationPercentage", lendingPoolCoreAddr);
        for (String validator : ommValidators.keySet()) {
            BigInteger feePortion = (ommValidators.get(validator).multiply(amount)).divide(ICX).divide(BigInteger.valueOf(100));
            BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(Address.fromString(validator), BigInteger.ZERO);
            accumulatedFee.set(Address.fromString(validator), feeAccumulatedAfterClaim.add(feePortion));

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
