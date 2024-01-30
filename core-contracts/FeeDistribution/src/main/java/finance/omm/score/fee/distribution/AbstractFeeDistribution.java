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

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;
import static finance.omm.utils.math.MathUtils.ICX;

public abstract class AbstractFeeDistribution extends AddressProvider implements FeeDistribution {

    public static final String TAG = "Fee Distribution";
    protected final EnumerableDictDB<Address, BigInteger> collectedFee =
            new EnumerableDictDB<>("collected_fee", Address.class,BigInteger.class);
    protected final VarDB<BigInteger> validatorRewards = Context.newVarDB("validator_fee_collected",BigInteger.class);
    protected final DictDB<Address, BigInteger> accumulatedFee = Context.newDictDB("accumulated_fee", BigInteger.class);
    protected final EnumerableDictDB<Address, BigInteger> feeDistributionWeight = new
            EnumerableDictDB<>("fee_distribution_weight", Address.class, BigInteger.class);
    protected final VarDB<BigInteger> feeToDistribute = Context.newVarDB("fee_to_distribute",BigInteger.class);

    public AbstractFeeDistribution(Address addressProvider) {
        super(addressProvider, false);
    }


    protected void distributeFee(BigInteger amount) {
        BigInteger remaining = amount;
        BigInteger remainingDaoFund = BigInteger.ZERO;
        BigInteger denominator = ICX;
        BigInteger amountToDistribute;

        Address lendingPoolCoreAddr = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address daoFundAddr = getAddress(Contracts.DAO_FUND.getKey());

        for (Address receiver : feeDistributionWeight.keySet()) {
            BigInteger percentageToDistribute = feeDistributionWeight.get(receiver);
            if (percentageToDistribute.signum() > 0) {
                amountToDistribute = (percentageToDistribute.multiply(remaining)).divide(denominator);
            } else {
                amountToDistribute = BigInteger.ZERO;
            }

            if (amountToDistribute.signum() > 0) {
                if (receiver.equals(lendingPoolCoreAddr)) {

                    remainingDaoFund = distributeFeeToValidator(lendingPoolCoreAddr,amountToDistribute);
                    validatorRewards.set(getValidatorCollectedFee().add(amountToDistribute));

                } else if (receiver.equals(daoFundAddr)) {

                    BigInteger feeCollected = collectedFee.getOrDefault(receiver, BigInteger.ZERO);
                    BigInteger totalDistributionFee = amountToDistribute.add(remainingDaoFund);
                    collectedFee.put(receiver, feeCollected.add(totalDistributionFee));

                    call(Contracts.sICX, "transfer", receiver, totalDistributionFee);

                } else {

                    BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(receiver, BigInteger.ZERO);
                    accumulatedFee.set(receiver, feeAccumulatedAfterClaim.add(amountToDistribute));
                }
                remaining = remaining.subtract(amountToDistribute);
            }
            denominator = denominator.subtract(percentageToDistribute);
        }
        if (remaining.signum() > 0){
            BigInteger feeCollected = collectedFee.getOrDefault(daoFundAddr, BigInteger.ZERO);
            collectedFee.put(daoFundAddr, feeCollected.add(remaining));

            call(Contracts.sICX, "transfer", daoFundAddr, remaining);
        }
        
        this.feeToDistribute.set(BigInteger.ZERO);
        FeeDisbursed(amount);

        
    }


    private BigInteger distributeFeeToValidator(Address lendingPoolCoreAddr,BigInteger amount) {

        Map<String, BigInteger> ommValidators = call(Map.class, Contracts.STAKING,
                "getActualUserDelegationPercentage", lendingPoolCoreAddr);
        BigInteger amountToDistribute = amount;
        BigInteger denominator = BigInteger.valueOf(100).multiply(ICX);

        BigInteger remaining = BigInteger.ZERO;

        for (String validator : ommValidators.keySet()) {
            if (denominator.signum() == 0){
                break;
            }
            Address validatorAddr = Address.fromString(validator);

            BigInteger currentPercentage = ommValidators.get(validator);
            BigInteger feePortion = (currentPercentage.multiply(amountToDistribute)).divide(denominator);

            amountToDistribute = amountToDistribute.subtract(feePortion);
            denominator = denominator.subtract(currentPercentage);
            if (checkPrepStatus(validatorAddr)){
                BigInteger feeAccumulatedAfterClaim = accumulatedFee.getOrDefault(validatorAddr, BigInteger.ZERO);
                accumulatedFee.set(validatorAddr, feeAccumulatedAfterClaim.add(feePortion));
            }else {
                remaining = remaining.add(feePortion);
            }

        }

        return remaining;
    }

    private boolean checkPrepStatus(Address prepAddr){
        Map<String, Object> prepDict = call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", prepAddr);
        BigInteger jailFlags = (BigInteger) prepDict.get("jailFlags");

        return jailFlags == null || jailFlags.signum() == 0;
    }

    @EventLog(indexed = 3)
    public void FeeClaimed(Address caller, Address receiver, BigInteger amount) {
    }

    @EventLog(indexed = 1)
    public void FeeDistributed(BigInteger _value) {
    }

    @EventLog
    public void FeeDisbursed(BigInteger _value) {
    }

    protected void onlyOwner() {
        Address sender = Context.getCaller();
        Address owner = Context.getOwner();
        if (!sender.equals(owner)) {
            throw FeeDistributionException.notOwner();
        }
    }

    public <K> K call(Class<K> kClass, Address address, String method, Object... params) {
        return Context.call(kClass, address, method, params);
    }

}
