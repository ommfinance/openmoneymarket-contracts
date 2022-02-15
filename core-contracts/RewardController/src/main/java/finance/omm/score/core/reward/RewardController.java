package finance.omm.score.core.reward;

import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.db.AssetWeightDB;
import finance.omm.score.core.reward.db.TypeWeightDB;
import finance.omm.score.core.reward.model.Asset;
import finance.omm.score.core.reward.utils.TimeConstants;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.libs.math.MathUtils.*;
import static finance.omm.score.core.reward.utils.Errors.*;
import static finance.omm.score.core.reward.utils.TimeConstants.*;

public class RewardController {
    public static final String TAG = "Reward Controller";
    public static final BigInteger DAYS_PER_YEAR = BigInteger.valueOf(365L);
    public static final String TIMESTAMP_AT_START = "timestampAtStart";
    public static final String INFLATION_RATE = "inflationRate";

    private final TypeWeightDB typeWeightDB = new TypeWeightDB("type");
    private final AssetWeightDB assetWeightDB = new AssetWeightDB("asset");

    private final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);
    private final DictDB<BigInteger, BigInteger> inflationRate = Context.newDictDB(INFLATION_RATE, BigInteger.class);


    public RewardController(BigInteger startTimestamp) {
        if (this._timestampAtStart.getOrDefault(null) == null) {
            this._timestampAtStart.set(startTimestamp);
        }
    }

    @External(readonly = true)
    public String name() {
        return TAG;
    }

    @External
    public void addType(String name) {
        checkOwner();
        typeWeightDB.add(name);
    }


    @External
    public void setTypeWeight(WeightStruct[] weights, @Optional BigInteger timestamp) {
        checkOwner();
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }

        typeWeightDB.setWeights(weights, timestamp);
    }

    @External
    public void addAsset(String typeId, String name, @Optional Address address, @Optional BigInteger poolID) {
        checkOwner();
        checkTypeId(typeId);

        assetWeightDB.addAsset(typeId, name, address, poolID);
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
        if (isLesThanEqual(_day, BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else if (isLesThanEqual(_day, BigInteger.valueOf(30L))) {
            return MILLION;
        } else if (isLesThanEqual(_day, DAYS_PER_YEAR)) {
            return BigInteger.valueOf(4L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (isLesThanEqual(_day, DAYS_PER_YEAR.multiply(BigInteger.TWO))) {
            return BigInteger.valueOf(3L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (isLesThanEqual(_day, BigInteger.valueOf(3L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(2L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (isLesThanEqual(_day, BigInteger.valueOf(4L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(34L).multiply(MILLION).divide(BigInteger.TEN);
        } else {
            BigInteger index = _day.divide(DAYS_PER_YEAR.subtract(BigInteger.valueOf(4L)));
            return BigInteger.valueOf(103L)
                             .multiply(pow10(index.intValue()))
                             .multiply(BigInteger.valueOf(3L))
                             .multiply(BigInteger.valueOf(383L).multiply(MILLION))
                             .divide(DAYS_PER_YEAR)
                             .divide(BigInteger.valueOf(100L).multiply(pow10(index.intValue() - 1)));
        }
    }

    @External(readonly = true)
    public BigInteger getDay() {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return timestamp.subtract(_timestampAtStart.get()).divide(DAY_IN_SECONDS);
    }

    private BigInteger getEpoch(BigInteger tInSeconds) {
        BigInteger startTimestamp = _timestampAtStart.get();
        BigInteger timeDelta = tInSeconds.subtract(startTimestamp);
        if (timeDelta.compareTo(BigInteger.ZERO) <= 0) {
            return startTimestamp;
        }
        BigInteger numberOfYears = timeDelta.divide(YEAR_IN_SECONDS);
        if (numberOfYears.equals(BigInteger.ZERO) && timeDelta.compareTo(MONTH_IN_SECONDS) > 0) {
            return startTimestamp.add(MONTH_IN_SECONDS);
        }
        return startTimestamp.add(numberOfYears.multiply(YEAR_IN_SECONDS));
    }

    private BigInteger getInflationRateByTimestamp(BigInteger timestampInSeconds) {
        BigInteger delta = timestampInSeconds.subtract(_timestampAtStart.get());

        BigInteger numberOfDay = delta.divide(DAY_IN_SECONDS);
        return getInflationRate(numberOfDay);
    }

    private BigInteger getInflationRate(BigInteger _day) {
        return tokenDistributionPerDay(_day).divide(DAY_IN_SECONDS);
    }

    @External(readonly = true)
    public BigInteger getIntegrateIndex(String assetId, BigInteger totalSupply, BigInteger lastUpdatedTimestamp) {
        if (totalSupply.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        BigInteger integrateIndex = BigInteger.ZERO;
        Asset asset = assetWeightDB.getAsset(assetId);
        BigInteger initialTimestamp = this._timestampAtStart.get();
        //TODO other condition to exit loop
        while (timestamp.compareTo(initialTimestamp) >= 0 || timestamp.compareTo(lastUpdatedTimestamp) > 0) {
            Map<String, BigInteger> result = calculateIntegrateIndex(asset, lastUpdatedTimestamp, timestamp);
            integrateIndex = integrateIndex.add(exaDivide(result.get("integrateIndex"), totalSupply));
            timestamp = result.get("timestamp").subtract(BigInteger.ONE);
        }

        return integrateIndex;
    }

    private Map<String, BigInteger> calculateIntegrateIndex(Asset asset, BigInteger start, BigInteger timestamp) {

        Map<String, BigInteger> assetWeight = assetWeightDB.getWeight(asset, timestamp);
        int assetIndex = assetWeight.get("index").intValue();
        BigInteger aTimestamp = assetWeight.get("timestamp");
        BigInteger aWeight = assetWeight.get("value");

        Map<String, BigInteger> typeWeight = typeWeightDB.getWeight(asset.typeId, timestamp);
        int typeIndex = typeWeight.get("index").intValue();
        BigInteger tTimestamp = typeWeight.get("timestamp");
        BigInteger tWeight = typeWeight.get("value");

        BigInteger epochTime = getEpoch(timestamp);
        BigInteger rate = getInflationRateByTimestamp(epochTime);

        BigInteger maximum = aTimestamp.max(tTimestamp).max(epochTime).max(start);
        BigInteger integrateIndex =
                exaMultiply(exaMultiply(rate, tWeight), aWeight).multiply(timestamp.subtract(maximum));
        return Map.of("integrateIndex", integrateIndex, "timestamp", maximum);
    }


    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            Context.revert(ERROR_NOT_CONTRACT_OWNER, "Not Contract Owner");
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            Context.revert(ERROR_NOT_GENERIC, message);
        }
    }

    private void checkTypeId(String typeId) {
        if (!typeWeightDB.isValidId(typeId)) {
            throwError(ERROR_INVALID_TYPE_ID, "Invalid typeId");
        }
    }

    private void throwError(Integer errorCode, String message) {
        Context.revert(errorCode, message);
    }
}
