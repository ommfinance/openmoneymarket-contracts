package finance.omm.score.core.reward;

import static finance.omm.utils.constants.TimeConstants.DAY_IN_MICRO_SECONDS;
import static finance.omm.utils.constants.TimeConstants.MONTH_IN_MICRO_SECONDS;
import static finance.omm.utils.constants.TimeConstants.YEAR_IN_MICRO_SECONDS;
import static finance.omm.utils.math.MathUtils.MILLION;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.pow;

import finance.omm.core.score.interfaces.RewardController;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.db.AssetWeightDB;
import finance.omm.score.core.reward.db.TypeWeightDB;
import finance.omm.score.core.reward.exception.RewardException;
import finance.omm.score.core.reward.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;

public class RewardControllerImpl extends AddressProvider implements RewardController {

    public static final String TAG = "Reward Controller";
    public static final BigInteger DAYS_PER_YEAR = BigInteger.valueOf(365L);
    public static final String TIMESTAMP_AT_START = "timestampAtStart";

    public final TypeWeightDB typeWeightDB = new TypeWeightDB("type");
    public final AssetWeightDB assetWeightDB = new AssetWeightDB("asset");

    private final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);

    public RewardControllerImpl(Address addressProvider, BigInteger startTimestamp) {
        super(addressProvider);
        if (this._timestampAtStart.getOrDefault(null) == null) {
            this._timestampAtStart.set(startTimestamp);
        }
    }

    @External(readonly = true)
    public String name() {
        return TAG;
    }

    @External
    public void addType(String key, String name) {
        checkRewardDistribution();
        typeWeightDB.add(key, name);
    }

    @External
    public void setTypeWeight(WeightStruct[] weights, @Optional BigInteger timestamp) {
        checkOwner();
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }

        typeWeightDB.setWeights(weights, timestamp);
        SetTypeWeight(timestamp, "Type weight updated");
    }


    @Override
    @External
    public String addAsset(String typeId, String name) {
        checkRewardDistribution();
        checkTypeId(typeId);
        return assetWeightDB.addAsset(typeId, name);
    }


    @External
    public void setAssetWeight(String typeId, WeightStruct[] weights, @Optional BigInteger timestamp) {
        checkOwner();
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        assetWeightDB.setWeights(typeId, weights, timestamp);
    }


    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day) {

        if (MathUtils.isLesThanEqual(_day, BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else if (MathUtils.isLesThanEqual(_day, BigInteger.valueOf(30L))) {
            return MILLION;
        } else if (MathUtils.isLesThanEqual(_day, DAYS_PER_YEAR)) {
            return BigInteger.valueOf(4L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLesThanEqual(_day, DAYS_PER_YEAR.multiply(BigInteger.TWO))) {
            return BigInteger.valueOf(3L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLesThanEqual(_day, BigInteger.valueOf(3L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(2L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLesThanEqual(_day, BigInteger.valueOf(4L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(34L).multiply(MILLION).divide(BigInteger.TEN);
        } else {
            BigInteger index = _day.divide(DAYS_PER_YEAR).subtract(BigInteger.valueOf(4L));
            return pow(BigInteger.valueOf(103L), (index.intValue()))
                    .multiply(BigInteger.valueOf(3L))
                    .multiply(BigInteger.valueOf(383L).multiply(MILLION))
                    .divide(DAYS_PER_YEAR)
                    .divide(pow(BigInteger.valueOf(100L),
                            (index.intValue() + 1)));
        }
    }

    @External(readonly = true)
    public BigInteger getDay() {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return timestamp.subtract(_timestampAtStart.get()).divide(DAY_IN_MICRO_SECONDS);
    }

    private Map<String, BigInteger> getInflationRateByTimestamp(BigInteger tInMicroSeconds) {
        BigInteger startTimestamp = _timestampAtStart.get();
        BigInteger timeDelta = tInMicroSeconds.subtract(startTimestamp);
        if (timeDelta.compareTo(BigInteger.ZERO) > 0) {
            BigInteger numberOfYears = timeDelta.divide(YEAR_IN_MICRO_SECONDS);
            if (numberOfYears.equals(BigInteger.ZERO) && timeDelta.compareTo(MONTH_IN_MICRO_SECONDS) > 0) {
                startTimestamp = startTimestamp.add(MONTH_IN_MICRO_SECONDS);
            }
            startTimestamp = startTimestamp.add(numberOfYears.multiply(YEAR_IN_MICRO_SECONDS));
        }

        BigInteger delta = startTimestamp.subtract(_timestampAtStart.get());

        BigInteger numberOfDay = delta.divide(DAY_IN_MICRO_SECONDS).add(BigInteger.ONE);
        return Map.of(
                "startTimestamp", startTimestamp,
                "rate", getInflationRate(numberOfDay)
        );
    }

    private BigInteger getInflationRate(BigInteger _day) {
        return tokenDistributionPerDay(_day).divide(DAY_IN_MICRO_SECONDS);
    }

    @External(readonly = true)
    public BigInteger getIntegrateIndex(String assetId, BigInteger totalSupply, BigInteger lastUpdatedTimestamp) {
        if (totalSupply.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        BigInteger actual = timestamp;
        BigInteger integrateIndex = BigInteger.ZERO;
        Asset asset = assetWeightDB.getAsset(assetId);
        BigInteger initialTimestamp = this._timestampAtStart.get();
        //TODO other condition to exit loop
        while (timestamp.compareTo(initialTimestamp) >= 0 && timestamp.compareTo(lastUpdatedTimestamp) > 0) {
            Map<String, BigInteger> result = calculateIntegrateIndex(asset, lastUpdatedTimestamp, timestamp, actual);
            integrateIndex = integrateIndex.add(exaDivide(result.get("integrateIndex"), totalSupply));
            actual = result.get("timestamp");
            timestamp = actual.subtract(BigInteger.ONE);
        }

        return integrateIndex;
    }


    private Map<String, BigInteger> calculateIntegrateIndex(Asset asset, BigInteger start, BigInteger timestamp,
            BigInteger actual) {

        Map<String, BigInteger> assetWeight = assetWeightDB.getWeight(asset, timestamp);
        int assetIndex = assetWeight.get("index").intValue();
        BigInteger aTimestamp = assetWeight.get("timestamp");
        BigInteger aWeight = assetWeight.get("value");

        Map<String, BigInteger> typeWeight = typeWeightDB.getWeight(asset.typeId, timestamp);
        int typeIndex = typeWeight.get("index").intValue();
        BigInteger tTimestamp = typeWeight.get("timestamp");
        BigInteger tWeight = typeWeight.get("value");

        Map<String, BigInteger> inflationRate = getInflationRateByTimestamp(timestamp);

        BigInteger maximum = aTimestamp.max(tTimestamp).max(inflationRate.get("startTimestamp")).max(start);

        if (maximum.equals(actual)) {
            return Map.of("integrateIndex", BigInteger.ZERO, "timestamp", actual);
        }

        BigInteger integrateIndex =
                exaMultiply(exaMultiply(inflationRate.get("rate"), tWeight), aWeight).multiply(
                        actual.subtract(maximum).divide(TimeConstants.SECOND));
        return Map.of("integrateIndex", integrateIndex, "timestamp", maximum);
    }

    private void checkRewardDistribution() {
        if (!Context.getCaller().equals(getAddress(Contracts.REWARDS.getKey()))) {
            throw RewardException.notAuthorized("require reward distribution contract access");
        }
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw RewardException.notOwner();
        }
    }

    private void checkTypeId(String typeId) {
        List<String> a = new ArrayList<>();
        if (!typeWeightDB.isValidId(typeId)) {
            throw RewardException.notValidTypeId(typeId);
        }
    }

    @External(readonly = true)
    public BigInteger getTypeCheckpointCount() {
        return BigInteger.valueOf(this.typeWeightDB.getCheckpointCount());
    }

    @External(readonly = true)
    public BigInteger getAssetCheckpointCount(String typeId) {
        return BigInteger.valueOf(this.assetWeightDB.getCheckpointCount(typeId));
    }

    @External(readonly = true)
    public Map<String, BigInteger> getTypeWeightByTimestamp(BigInteger timestamp) {
        return this.typeWeightDB.getWeightByTimestamp(timestamp);
    }


    @External(readonly = true)
    public Map<String, BigInteger> getAssetWeightByTimestamp(String typeId, BigInteger timestamp) {
        return this.assetWeightDB.getWeightByTimestamp(typeId, timestamp);
    }

    @EventLog(indexed = 2)
    public void SetTypeWeight(BigInteger timestamp, String message) {
    }

}
