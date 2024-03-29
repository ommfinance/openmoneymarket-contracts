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

    public static final String COLLECTED_FEE = "collected_fee";
    public static final String VALIDATOR_FEE_COLLECTED = "validator_fee_collected";
    public static final String ACCUMULATED_FEE = "accumulated_fee";
    public static final String FEE_DISTRIBUTION_WEIGHT = "fee_distribution_weight";
    public static final String FEE_TO_DISTRIBUTE = "fee_to_distribute";
    public static final String JAILED_VALIDATOR_SHARE = "jailed_validator_share";
    protected final EnumerableDictDB<Address, BigInteger> collectedFee =
            new EnumerableDictDB<>(COLLECTED_FEE, Address.class, BigInteger.class);
    protected final VarDB<BigInteger> validatorRewards = Context.newVarDB(VALIDATOR_FEE_COLLECTED, BigInteger.class);
    protected final DictDB<Address, BigInteger> accumulatedFee = Context.newDictDB(ACCUMULATED_FEE, BigInteger.class);
    protected final EnumerableDictDB<Address, BigInteger> feeDistributionWeight = new

            EnumerableDictDB<>(FEE_DISTRIBUTION_WEIGHT, Address.class, BigInteger.class);
    protected final VarDB<BigInteger> feeToDistribute = Context.newVarDB(FEE_TO_DISTRIBUTE,BigInteger.class);
    protected final VarDB<BigInteger> validatorShare = Context.newVarDB(JAILED_VALIDATOR_SHARE,BigInteger.class);



    public AbstractFeeDistribution(Address addressProvider) {
        super(addressProvider, false);
    }


    protected void distributeFee(BigInteger amount) {
        BigInteger remaining = amount;
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

                    BigInteger validatorFee = distributeFeeToValidator(lendingPoolCoreAddr,amountToDistribute);

                    BigInteger currentValidatorRewards = amountToDistribute.subtract(validatorFee);
                    validatorRewards.set(getValidatorCollectedFee().add(currentValidatorRewards));

                    if (validatorFee.compareTo(BigInteger.ZERO) > 0){
                        this.validatorShare.set(getValidatorShare().add(validatorFee));
                    }

                } else if (receiver.equals(daoFundAddr)) {

                    BigInteger feeCollected = collectedFee.getOrDefault(receiver, BigInteger.ZERO);
                    BigInteger totalDistributionFee = amountToDistribute.add(getValidatorShare());
                    collectedFee.put(receiver, feeCollected.add(totalDistributionFee));
                    this.validatorShare.set(BigInteger.ZERO);
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
        BigInteger status = (BigInteger) prepDict.get("status");
        if (!status.equals(BigInteger.ZERO)){
            return false;
        }
        BigInteger jailFlags = (BigInteger) prepDict.get("jailFlags");

        return jailFlags == null || jailFlags.signum() == 0;
    }

    private BigInteger getValidatorShare(){
        return this.validatorShare.getOrDefault(BigInteger.ZERO);
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
