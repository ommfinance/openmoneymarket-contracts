package finance.omm.score.core.reward;

import static finance.omm.utils.checks.Check.onlyOwner;
import static finance.omm.utils.constants.TimeConstants.DAYS_PER_YEAR;
import static finance.omm.utils.constants.TimeConstants.DAY_IN_MICRO_SECONDS;
import static finance.omm.utils.constants.TimeConstants.DAY_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.MONTH_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.SECOND;
import static finance.omm.utils.constants.TimeConstants.YEAR_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.getBlockTimestampInSecond;
import static finance.omm.utils.math.MathUtils.HUNDRED_PERCENT;
import static finance.omm.utils.math.MathUtils.HUNDRED_THOUSAND;
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
import finance.omm.utils.constants.TimeConstants.Timestamp;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.HashMap;

public class RewardWeightControllerImpl extends AddressProvider implements RewardWeightController {

    public static final String TAG = "Reward Weight Controller";

    public static final String TIMESTAMP_AT_START = "timestampAtStart";

    public final TypeWeightDB typeWeightDB = new TypeWeightDB("types");
    public final AssetWeightDB assetWeightDB = new AssetWeightDB("assets");
    public final VarDB<BigInteger> distributionStopDay = Context.newVarDB("distributionStopDay",BigInteger.class);

    private final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);

    public RewardWeightControllerImpl(Address addressProvider, BigInteger startTimestamp) {
        super(addressProvider, false);
        if (this._timestampAtStart.get() == null) {
            TimeConstants.checkIsValidTimestamp(startTimestamp, Timestamp.MICRO_SECONDS);
            this._timestampAtStart.set(startTimestamp);
        }
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setStopDay(BigInteger day) {
        onlyOwner();
        this.distributionStopDay.set(day);
    }

    @External(readonly = true)
    public BigInteger getStopDay(){
        return this.distributionStopDay.get();
    }

    @External
    public void addType(String key, boolean isPlatformRecipient, @Optional Address address) {
        onlyOrElseThrow(Contracts.REWARDS,
                RewardWeightException.notAuthorized("Only Reward distribution contract can call add type method"));
        typeWeightDB.add(key, isPlatformRecipient);

        if (isPlatformRecipient) {
            if (address == null) {
                throw RewardWeightException.unknown("asset address can't be null");
            }

            if (!address.isContract()) {
                throw RewardWeightException.unknown("asset address is not valid contract address");
            }

            assetWeightDB.addAsset(key, address, key);
            WeightStruct weightStruct = new WeightStruct();
            weightStruct.weight = HUNDRED_PERCENT;
            weightStruct.address = address;
            _setAssetWeight(key, new WeightStruct[]{weightStruct}, BigInteger.ZERO);
        }
    }


    @External
    public void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                RewardWeightException.notAuthorized("Only Governance contract can call set type method"));
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        TimeConstants.checkIsValidTimestamp(timestamp, Timestamp.SECONDS);

        typeWeightDB.setWeights(weights, timestamp);
        SetTypeWeight(timestamp, "Type weight updated");
    }

    @External(readonly = true)
    public BigInteger getTypeWeight(String type, @Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        return this.typeWeightDB.searchTypeWeight(type, timestamp).get("value");
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAllTypeWeight(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        return this.typeWeightDB.weightOfAllTypes(timestamp);
    }

    @External
    public void addAsset(String type, Address address, String name) {
        onlyOrElseThrow(Contracts.REWARDS,
                RewardWeightException.notAuthorized("Only Reward distribution contract can call add asset method"));
        checkType(type);
        assetWeightDB.addAsset(type, address, name);
    }

    @External
    public void setAssetWeight(String type, WeightStruct[] weights, @Optional BigInteger timestamp) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                RewardWeightException.notAuthorized("Only Governance contract can call set asset weight method"));
        _setAssetWeight(type, weights, timestamp);
    }

    private void _setAssetWeight(String type, WeightStruct[] weights, BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        TimeConstants.checkIsValidTimestamp(timestamp, Timestamp.SECONDS);
        assetWeightDB.setWeights(type, weights, timestamp);
        SetAssetWeight(type, timestamp, "Asset weight updated");
    }


    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day) {

        BigInteger stopDay = getStopDay();

        if (MathUtils.isGreaterThanEqual(_day, stopDay)) {
            return BigInteger.ZERO;
        } else {
            return _tokenDistributionDay(_day);
        }
    }

    private BigInteger _tokenDistributionDay(BigInteger _day) {
        // normal omm distribution flow
        if (MathUtils.isLessThan(_day, BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(30L))) {
            return MILLION;
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR)) {
            return BigInteger.valueOf(4L).multiply(HUNDRED_THOUSAND);
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR.multiply(BigInteger.TWO))) {
            return BigInteger.valueOf(3L).multiply(HUNDRED_THOUSAND);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(3L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(2L).multiply(HUNDRED_THOUSAND);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(4L).multiply(DAYS_PER_YEAR))) {
            return HUNDRED_THOUSAND;
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
    public Map<String, BigInteger> getInflationRateByTimestamp(BigInteger tInSeconds) {
        BigInteger rateChangedOn;
        BigInteger startTimestampInSeconds = rateChangedOn = getStartTimestamp().divide(SECOND);
        if (tInSeconds.compareTo(rateChangedOn) <= 0) {
            return Map.of(
                    "rateChangedOn", rateChangedOn,
                    "ratePerSecond", getInflationRate(BigInteger.ZERO)
            );
        }

        BigInteger timeDelta = tInSeconds.subtract(rateChangedOn);

        BigInteger numberOfYears = timeDelta.divide(YEAR_IN_SECONDS);

        if (numberOfYears.equals(BigInteger.ZERO) && timeDelta.compareTo(MONTH_IN_SECONDS) > 0) {
            rateChangedOn = rateChangedOn.add(MONTH_IN_SECONDS);
        }
        rateChangedOn = rateChangedOn.add(numberOfYears.multiply(YEAR_IN_SECONDS));

        BigInteger numberOfDay = tInSeconds.subtract(startTimestampInSeconds).divide(DAY_IN_SECONDS);

        return Map.of(
                "rateChangedOn", rateChangedOn,
                "ratePerSecond", getInflationRate(numberOfDay)
        );
    }

    private BigInteger getInflationRate(BigInteger _day) {
        return tokenDistributionPerDay(_day).divide(DAY_IN_SECONDS);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getEmissionRate(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, BigInteger> response = new HashMap<>();
        BigInteger inflationRate = getInflationRateByTimestamp(timestamp).get("ratePerSecond");
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");

            ArrayDB<Address> addresses = this.assetWeightDB.getAssets(type);
            for (int i = 0; i < addresses.size(); i++) {
                Address address = addresses.get(i);
                Asset asset = this.assetWeightDB.getAsset(address);
                Map<String, BigInteger> assetWeightMap = this.assetWeightDB.searchAssetWeight(asset, timestamp);
                BigInteger aWeight = assetWeightMap.get("value");

                BigInteger aggregateWeight = exaMultiply(tWeight, aWeight);
                BigInteger rate = exaMultiply(inflationRate, aggregateWeight);
                response.put(address.toString(), rate);
            }
        }
        return response;
    }

    @External(readonly = true)
    public BigInteger calculateIntegrateIndex(Address assetAddr, BigInteger totalSupply, BigInteger fromInSeconds,
            BigInteger toInSeconds) {
        if (totalSupply.compareTo(BigInteger.ZERO) <= 0) {
            return BigInteger.ZERO;
        }

        if (fromInSeconds.compareTo(toInSeconds) >= 0) {
            return BigInteger.ZERO;
        }

        TimeConstants.checkIsValidTimestamp(fromInSeconds, Timestamp.SECONDS);
        TimeConstants.checkIsValidTimestamp(toInSeconds, Timestamp.SECONDS);

        toInSeconds = toInSeconds.subtract(BigInteger.ONE);

        BigInteger integrateIndex = BigInteger.ZERO;
        Asset asset = assetWeightDB.getAsset(assetAddr);
        if (asset == null) {
            return BigInteger.ZERO;
        }
        BigInteger initialTimestamp = this.getStartTimestamp().divide(SECOND);

        BigInteger prevTimestamp = BigInteger.ZERO;

        while (toInSeconds.compareTo(initialTimestamp) >= 0 && toInSeconds.compareTo(fromInSeconds) > 0
                && !toInSeconds.equals(prevTimestamp)) {
            prevTimestamp = toInSeconds;
            Map<String, BigInteger> result = calculateRewardDistribution(asset, fromInSeconds, toInSeconds);
            integrateIndex = integrateIndex.add(exaDivide(result.get("totalRewards"), totalSupply));
            toInSeconds = result.get("timestamp").subtract(BigInteger.ONE);
        }

        return integrateIndex;
    }


    private Map<String, BigInteger> calculateRewardDistribution(Asset asset, BigInteger fromTimestamp,
            BigInteger toTimestamp) {
        Map<String, BigInteger> assetWeight = assetWeightDB.searchAssetWeight(asset, toTimestamp);
        BigInteger aTimestamp = assetWeight.get("timestamp");
        BigInteger aWeight = assetWeight.get("value");

        Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(asset.type, toTimestamp);
        BigInteger tTimestamp = typeWeight.get("timestamp");
        BigInteger tWeight = typeWeight.get("value");

        Map<String, BigInteger> inflationRate = getInflationRateByTimestamp(toTimestamp);

        BigInteger maximum = aTimestamp.max(tTimestamp).max(inflationRate.get("rateChangedOn")).max(fromTimestamp);

        BigInteger rate = exaMultiply(exaMultiply(inflationRate.get("ratePerSecond"), tWeight), aWeight);
        BigInteger timeDeltaInSeconds = toTimestamp.add(BigInteger.ONE).subtract(maximum);
        BigInteger totalReward = rate.multiply(timeDeltaInSeconds);
        return Map.of("totalRewards", totalReward, "timestamp", maximum);
    }


    private void checkType(String type) {
        if (!typeWeightDB.isTypeExists(type)) {
            throw RewardWeightException.typeNotExist(type);
        }

        if (typeWeightDB.isPlatformRecipient(type)) {
            throw RewardWeightException.unknown("Platform Recipient type can't have child assets");
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
    public BigInteger getAssetTimestampAtCheckpoint(String typeId, int checkpointId) {
        return assetWeightDB.getTimestamp(typeId, checkpointId);
    }

    @External(readonly = true)
    public BigInteger getTypeTimestampAtCheckpoint(int checkpointId) {
        return typeWeightDB.getTimestamp(checkpointId);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getTypeWeightByTimestamp(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        return this.typeWeightDB.getWeightByTimestamp(timestamp);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAssetWeightByTimestamp(String type, @Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }

        var result = this.assetWeightDB.getAggregatedWeight(type, HUNDRED_PERCENT, timestamp);
        result.remove("total");
        return result;
    }

    @External(readonly = true)
    public BigInteger getAssetWeight(Address assetAddr, @Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        Asset asset = this.assetWeightDB.getAsset(assetAddr);
        BigInteger typeWeight = getTypeWeight(asset.type, timestamp);
        Map<String, BigInteger> result = this.assetWeightDB.searchAssetWeight(asset, timestamp);
        return exaMultiply(result.get("value"), typeWeight);
    }

    @External(readonly = true)
    public Map<String, ?> getAllAssetDistributionPercentage(@Optional BigInteger timestamp) {
        if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
            timestamp = getBlockTimestampInSecond();
        }
        BigInteger total = BigInteger.ZERO;
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, Object> response = new HashMap<>();
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(type, tWeight, timestamp);
            total = total.add(assetWeights.get("total"));
            response.put(type, assetWeights);
        }
        response.put("total", total);
        return response;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAssetDailyRewards(@Optional BigInteger _day) {
        BigInteger timestamp = getBlockTimestampInSecond();
        if (_day == null || BigInteger.ZERO.equals(_day)) {
            _day = getDay();
        } else {
            timestamp = this._timestampAtStart.get()
                    .add(_day.multiply(DAY_IN_MICRO_SECONDS))
                    .divide(SECOND);
        }
        BigInteger _distribution = tokenDistributionPerDay(_day);
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, BigInteger> response = new HashMap<>();
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            BigInteger _distributionValue = exaMultiply(_distribution, tWeight);
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(type, _distributionValue,
                    timestamp);
            assetWeights.remove("total");
            response.putAll(assetWeights);
        }
        response.put("day", _day);
        return response;
    }


    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day) {
        BigInteger timestamp = getBlockTimestampInSecond();
        if (_day == null || BigInteger.ZERO.equals(_day)) {
            _day = getDay();
        } else {
            timestamp = this._timestampAtStart.get()
                    .add(_day.multiply(DAY_IN_MICRO_SECONDS))
                    .divide(SECOND);
        }
        BigInteger _distribution = tokenDistributionPerDay(_day);
        List<String> types = this.typeWeightDB.getTypes();
        Map<String, Object> response = new HashMap<>();
        BigInteger totalRewards = BigInteger.ZERO;
        for (String type : types) {
            Map<String, BigInteger> typeWeight = typeWeightDB.searchTypeWeight(type, timestamp);
            BigInteger tWeight = typeWeight.get("value");
            BigInteger _distributionValue = exaMultiply(_distribution, tWeight);
            Map<String, BigInteger> assetWeights = this.assetWeightDB.getAggregatedWeight(type, _distributionValue,
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
            timestamp = getBlockTimestampInSecond();
        }
        Map<String, BigInteger> lpAssetIds = Context.call(Map.class,
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
    public Map<String, ?> precompute(BigInteger day) {
        Map<String, Object> response = new HashMap<>() {{
            put("isValid", true);
        }};
        BigInteger today = getDay();

        if (day.compareTo(today) >= 0) {
            response.put("isValid", false);
            return response;
        }
        BigInteger nextDay = day.add(BigInteger.ONE);

        BigInteger amountToMint = BigInteger.ZERO;
        for (int i = nextDay.intValue(); i <= today.intValue(); i++) {
            amountToMint = amountToMint.add(tokenDistributionPerDay(BigInteger.valueOf(i)));
        }

        response.put("amountToMint", amountToMint);
        response.put("day", today);
        response.put("timestamp", today.multiply(DAY_IN_MICRO_SECONDS).add(this.getStartTimestamp()).divide(SECOND));

        return response;
    }


    @EventLog(indexed = 2)
    public void SetTypeWeight(BigInteger timestamp, String message) {
    }

    @EventLog(indexed = 2)
    public void SetAssetWeight(String type, BigInteger timestamp, String message) {
    }

}
