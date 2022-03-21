package finance.omm.score.core.reward;

import static finance.omm.utils.constants.TimeConstants.DAY_IN_MICRO_SECONDS;
import static finance.omm.utils.constants.TimeConstants.DAY_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.MONTH_IN_MICRO_SECONDS;
import static finance.omm.utils.constants.TimeConstants.YEAR_IN_MICRO_SECONDS;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.MILLION;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.pow;

import finance.omm.core.score.interfaces.RewardWeightController;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.db.AssetWeightDB;
import finance.omm.score.core.reward.db.TypeWeightDB;
import finance.omm.score.core.reward.exception.RewardWeightException;
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
import scorex.util.HashMap;

public class RewardWeightControllerImpl extends AddressProvider implements RewardWeightController {

    public static final String TAG = "Reward Weight Controller";
    public static final BigInteger DAYS_PER_YEAR = BigInteger.valueOf(365L);
    public static final String TIMESTAMP_AT_START = "timestampAtStart";

    public final TypeWeightDB typeWeightDB = new TypeWeightDB("types");
    public final AssetWeightDB assetWeightDB = new AssetWeightDB("assets");

    private final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);

    public RewardWeightControllerImpl(Address addressProvider, BigInteger startTimestamp) {
        super(addressProvider, false);
        if (this._timestampAtStart.getOrDefault(null) == null) {
            this._timestampAtStart.set(startTimestamp);
        }
    }

    @External(readonly = true)
    public String name() {
        return TAG;
    }

    @External
    public void addType(String key, boolean transferToContract, @Optional Address address) {
        checkRewardDistribution();
        typeWeightDB.add(key, transferToContract);

        if (transferToContract) {
            if (address == null) {
                throw RewardWeightException.unknown("asset address can't be null");
            }
            assetWeightDB.addAsset(key, address, key);
            WeightStruct weightStruct = new WeightStruct();
            weightStruct.weight = ICX;
            weightStruct.address = address;
            _setAssetWeight(key, new WeightStruct[]{weightStruct}, BigInteger.ZERO);
        }
    }


    @External
    public void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp) {
        checkOwner();
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }

        typeWeightDB.setWeights(weights, timestamp);
        SetTypeWeight(timestamp, "Type weight updated");
    }

    @External(readonly = true)
    public BigInteger getTypeWeight(String type, @Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        return this.typeWeightDB.searchTypeWeight(type, timestamp).get("value");
    }

    @External(readonly = true)
    public Map<String, BigInteger> getALlTypeWeight(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        return this.typeWeightDB.weightOfAllTypes(timestamp);
    }


    @Override
    @External
    public void addAsset(String type, Address address, String name) {
        checkRewardDistribution();
        checkType(type);
        assetWeightDB.addAsset(type, address, name);
    }

    @External
    public void setAssetWeight(String type, WeightStruct[] weights, @Optional BigInteger timestamp) {
        checkOwner();
        _setAssetWeight(type, weights, timestamp);
    }

    private void _setAssetWeight(String type, WeightStruct[] weights, BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        assetWeightDB.setWeights(type, weights, timestamp);
        SetAssetWeight(type, timestamp, "Asset weight updated");
    }


    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day) {

        if (MathUtils.isLessThan(_day, BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(30L))) {
            return MILLION;
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR)) {
            return BigInteger.valueOf(4L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR.multiply(BigInteger.TWO))) {
            return BigInteger.valueOf(3L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(3L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(2L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(4L).multiply(DAYS_PER_YEAR))) {
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

    /*
    _timestampAtStart=0;
    rate changes in [0,100,1000];

    if tInMicroSeconds=1100 then;
    rateChangeOn=1000;


    if tInMicroSeconds=1000 then;
    rateChangeOn=100;
     */
    private Map<String, BigInteger> getInflationRateByTimestamp(BigInteger tInMicroSeconds) {
        BigInteger rateChangedOn = _timestampAtStart.get();

        if (tInMicroSeconds.compareTo(rateChangedOn) <= 0) {
            return Map.of(
                    "rateChangedOn", rateChangedOn,
                    "rate", getInflationRate(BigInteger.ZERO)
            );
        }
        /*
         *convert tInMicroSeconds=1000 to 999ms
         */
        BigInteger timeDelta = tInMicroSeconds.subtract(BigInteger.ONE).subtract(rateChangedOn);

        BigInteger numberOfYears = timeDelta.divide(YEAR_IN_MICRO_SECONDS);

        if (numberOfYears.equals(BigInteger.ZERO) && timeDelta.compareTo(MONTH_IN_MICRO_SECONDS) > 0) {
            rateChangedOn = rateChangedOn.add(MONTH_IN_MICRO_SECONDS);
        }
        rateChangedOn = rateChangedOn.add(numberOfYears.multiply(YEAR_IN_MICRO_SECONDS));

        BigInteger delta = rateChangedOn.subtract(_timestampAtStart.get());

        BigInteger numberOfDay = delta.divide(DAY_IN_MICRO_SECONDS);
        return Map.of(
                "rateChangedOn", rateChangedOn,
                "rate", getInflationRate(numberOfDay)
        );
    }

    private BigInteger getInflationRate(BigInteger _day) {
        return tokenDistributionPerDay(_day).divide(DAY_IN_SECONDS);
    }


    @External(readonly = true)
    public BigInteger getIntegrateIndex(Address assetAddr, BigInteger totalSupply, BigInteger lastUpdatedTimestamp) {
        if (totalSupply.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger timestamp = TimeConstants.getBlockTimestamp();

        BigInteger integrateIndex = BigInteger.ZERO;
        Asset asset = assetWeightDB.getAsset(assetAddr);
        if (asset == null) {
            return BigInteger.ZERO;
        }
        BigInteger initialTimestamp = this._timestampAtStart.get();
        BigInteger prevTimestamp = BigInteger.ZERO;
        //TODO other condition to exit loop
        while (timestamp.compareTo(initialTimestamp) >= 0 && timestamp.compareTo(lastUpdatedTimestamp) > 0
                && !timestamp.equals(prevTimestamp)) {
            prevTimestamp = timestamp;
            Map<String, BigInteger> result = calculateRewardDistribution(asset, lastUpdatedTimestamp, timestamp);
            integrateIndex = integrateIndex.add(exaDivide(result.get("totalRewards"), totalSupply));
            timestamp = result.get("timestamp");
        }

        return integrateIndex;
    }


    private Map<String, BigInteger> calculateRewardDistribution(Asset asset, BigInteger start, BigInteger timestamp) {
        Map<String, BigInteger> assetWeight = assetWeightDB.searchAssetWeight(asset, timestamp);
        BigInteger aTimestamp = assetWeight.get("timestamp");
        BigInteger aWeight = assetWeight.get("value");

        Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(asset.type, timestamp);
        BigInteger tTimestamp = typeWeight.get("timestamp");
        BigInteger tWeight = typeWeight.get("value");

        Map<String, BigInteger> inflationRate = getInflationRateByTimestamp(timestamp);

        BigInteger maximum = aTimestamp.max(tTimestamp).max(inflationRate.get("rateChangedOn")).max(start);

        if (maximum.equals(timestamp)) {
            return Map.of("totalRewards", BigInteger.ZERO, "timestamp", timestamp);
        }
        BigInteger rate = exaMultiply(exaMultiply(inflationRate.get("rate"), tWeight), aWeight);
        BigInteger timeDeltaInSeconds = timestamp.subtract(maximum).divide(TimeConstants.SECOND);
        BigInteger totalReward = rate.multiply(timeDeltaInSeconds);
        return Map.of("totalRewards", totalReward, "timestamp", maximum);
    }

    private void checkRewardDistribution() {
        if (!Context.getCaller().equals(getAddress(Contracts.REWARDS.getKey()))) {
            throw RewardWeightException.notAuthorized("require reward distribution contract access");
        }
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw RewardWeightException.notOwner();
        }
    }

    private void checkType(String type) {
        if (!typeWeightDB.isValidId(type)) {
            throw RewardWeightException.notValidType(type);
        }

        if (typeWeightDB.isContractType(type)) {
            throw RewardWeightException.unknown("Contract type can't have child assets");
        }
    }

    @External(readonly = true)
    public BigInteger getTypeCheckpointCount() {
        return BigInteger.valueOf(this.typeWeightDB.getCheckpointCount());
    }

    @External(readonly = true)
    public BigInteger getAssetCheckpointCount(String type) {
        return BigInteger.valueOf(this.assetWeightDB.getCheckpointCount(type));
    }

    @External(readonly = true)
    public Map<String, BigInteger> getTypeWeightByTimestamp(BigInteger timestamp) {
        return this.typeWeightDB.getWeightByTimestamp(timestamp);
    }


    @External(readonly = true)
    public Map<String, BigInteger> getAssetWeightByTimestamp(String type, BigInteger timestamp) {
        BigInteger typeWeight = getTypeWeight(type, timestamp);
        return this.assetWeightDB.getWeightByTimestamp(type, typeWeight, timestamp);
    }

    @External(readonly = true)
    public BigInteger getAssetWeight(Address assetAddr, @Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        Asset asset = this.assetWeightDB.getAsset(assetAddr);
        BigInteger typeWeight = getTypeWeight(asset.type, timestamp);
        Map<String, BigInteger> result = this.assetWeightDB.searchAssetWeight(asset, timestamp);
        return exaMultiply(result.get("value"), typeWeight);
    }

    @External(readonly = true)
    public Map<String, ?> getAllAssetDistributionPercentage(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        BigInteger total = BigInteger.ZERO;
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, Object> response = new HashMap<>();
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(tWeight, type, timestamp);
            total = total.add(assetWeights.get("total"));
            response.put(type, assetWeights);
        }
        response.put("total", total);
        return response;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAssetDailyRewards(@Optional BigInteger _day) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        if (_day == null || BigInteger.ZERO.equals(_day)) {
            _day = getDay();
        } else {
            timestamp = this._timestampAtStart.get().add(_day.multiply(DAY_IN_MICRO_SECONDS));
        }
        BigInteger _distribution = tokenDistributionPerDay(_day);
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, BigInteger> response = new HashMap<>();
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            BigInteger _distributionValue = exaMultiply(_distribution, tWeight);
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(_distributionValue, type,
                    timestamp);
            assetWeights.remove("total");
            response.putAll(assetWeights);
        }
        response.put("day", _day);
        return response;
    }


    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        if (_day == null || BigInteger.ZERO.equals(_day)) {
            _day = getDay();
        } else {
            timestamp = this._timestampAtStart.get().add(_day.multiply(DAY_IN_MICRO_SECONDS));
        }
        BigInteger _distribution = tokenDistributionPerDay(_day);
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, Object> response = new HashMap<>();
        BigInteger totalRewards = BigInteger.ZERO;
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            BigInteger _distributionValue = exaMultiply(_distribution, tWeight);
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(_distributionValue, type,
                    timestamp);
            response.put(type, assetWeights);
            totalRewards = totalRewards.add(_distributionValue);
        }
        response.put("day", _day);
        response.put("total", totalRewards);
        return response;
    }


    @External(readonly = true)
    public Map<String, ?> getDistPercentageOfLP(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = TimeConstants.getBlockTimestamp();
        }
        Map<String, BigInteger> lpAssetIds = (Map<String, BigInteger>) Context.call(
                getAddress(Contracts.REWARDS.toString()), "getLiquidityProviders");
        Map<String, BigInteger> response = new HashMap<>();
        BigInteger typeWeight = this.getTypeWeight("liquidity", timestamp);

        for (Map.Entry<String, BigInteger> entry : lpAssetIds.entrySet()) {
            Address assetAddress = Address.fromString(entry.getKey());
            BigInteger lpID = entry.getValue();
            Asset asset = this.assetWeightDB.getAsset(assetAddress);

            if (asset != null) {
                BigInteger value = this.assetWeightDB.searchAssetWeight(asset, timestamp).get("value");
                response.put(lpID.toString(10), exaMultiply(value, typeWeight));
            }
        }
        return Map.of("liquidity", response);
    }

    @External(readonly = true)
    public List<String> getTypes() {
        return this.typeWeightDB.getTypes();
    }

    @External(readonly = true)
    public BigInteger getStartTimestamp() {
        return this._timestampAtStart.get();
    }


    @External(readonly = true)
    public Map<String, ?> distributionDetails(BigInteger day) {
        Map<String, Object> response = new HashMap<>() {{
            put("isValid", true);
        }};
        BigInteger today = getDay().add(BigInteger.ONE);

        if (day.compareTo(today) >= 0) {
            response.put("isValid", false);
            return response;
        }
        BigInteger distribution = BigInteger.ZERO;
        for (int i = day.intValue(); i < today.intValue(); i++) {
            distribution = distribution.add(tokenDistributionPerDay(BigInteger.valueOf(i)));
        }

        response.put("distribution", distribution);
        response.put("day", today);
        return response;
    }

    @EventLog(indexed = 2)
    public void SetTypeWeight(BigInteger timestamp, String message) {
    }

    @EventLog(indexed = 2)
    public void SetAssetWeight(String type, BigInteger timestamp, String asset_weight_updated) {
    }

}
