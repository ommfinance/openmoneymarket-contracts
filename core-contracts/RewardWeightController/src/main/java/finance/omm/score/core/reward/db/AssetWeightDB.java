package finance.omm.score.core.reward.db;

import static finance.omm.utils.math.MathUtils.HUNDRED_PERCENT;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.exception.RewardWeightException;
import finance.omm.score.core.reward.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import scorex.util.HashMap;

public class AssetWeightDB implements Searchable {

    private final static String TAG = "Asset Weight DB";

    //type => checkPointCounter => AssetAddress => weight value;
    private final BranchDB<String, BranchDB<Integer, DictDB<Address, BigInteger>>> weightCheckpoint;
    //type => checkPointCounter => sum of weight for checkpoint;
    private final BranchDB<String, DictDB<Integer, BigInteger>> totalCheckpoint;
    //type => checkPointCounter => timestamp;
    private final BranchDB<String, DictDB<Integer, BigInteger>> timeCheckpoint;
    //type => checkPointCounter;
    private final DictDB<String, Integer> checkpointCounter;

    private final DictDB<Address, Asset> assets;

    // type => array of assets
    private final BranchDB<String, ArrayDB<Address>> assetMap;


    public AssetWeightDB(String key) {
        this.checkpointCounter = Context.newDictDB(key + "CheckpointCounter", Integer.class);
        this.totalCheckpoint = Context.newBranchDB(key + "Total", BigInteger.class);
        this.weightCheckpoint = Context.newBranchDB(key + "WeightCheckpoint", BigInteger.class);
        this.timeCheckpoint = Context.newBranchDB(key + "TimestampCheckpoint", BigInteger.class);

        this.assets = Context.newDictDB(key + "Assets", Asset.class);
        this.assetMap = Context.newBranchDB(key + "TypeAssetMap", Address.class);
    }


    public void addAsset(String type, Address address, String name) {
        if (this.assets.get(address) != null) {
            throw RewardWeightException.invalidAsset(address + " already exists");
        }

        Asset asset = new Asset(address, type);
        asset.name = name;
        assets.set(address, asset);
        assetMap.at(type).add(address);
    }


    public void setWeights(String type, WeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(type, 0);
        BigInteger latestCheckpoint = this.timeCheckpoint.at(type).getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);
        if (compareValue > 0) {
            throw RewardWeightException.unknown("latest " + latestCheckpoint + " checkpoint exists than " + timestamp);
        }

        if (TimeConstants.getBlockTimestamp().compareTo(timestamp) > 0) {
            throw RewardWeightException.unknown("can't set weight value for old timestamp " + timestamp);
        }

        BigInteger total = this.totalCheckpoint.at(type).getOrDefault(checkpointCounter, BigInteger.ZERO);
        if (compareValue == 0) {
            setWeights(type, total, weights, checkpointCounter);
        } else {
            DictDB<Address, BigInteger> dictDB = this.weightCheckpoint.at(type).at(checkpointCounter);
            Integer counter = checkpointCounter + 1;
            DictDB<Address, BigInteger> newCheckpoint = this.weightCheckpoint.at(type).at(counter);

            ArrayDB<Address> addresses = this.assetMap.at(type);

            for (int i = 0; i < addresses.size(); i++) {
                Address address = addresses.get(i);
                BigInteger value = dictDB.getOrDefault(address, BigInteger.ZERO);
                newCheckpoint.set(address, value);
            }
            setWeights(type, total, weights, counter);
            this.timeCheckpoint.at(type).set(counter, timestamp);
            this.checkpointCounter.set(type, counter);
        }
    }

    private void setWeights(String type, BigInteger total, WeightStruct[] weights, Integer counter) {
        DictDB<Address, BigInteger> dictDB = this.weightCheckpoint.at(type).at(counter);
        for (WeightStruct ws : weights) {
            Address address = ws.address;
            Asset asset = this.assets.get(address);
            if (asset == null) {
                throw RewardWeightException.unknown(msg("Invalid asset :: " + ws.address));
            }
            BigInteger prevWeight = dictDB.getOrDefault(address, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(ws.weight);
            dictDB.set(address, ws.weight);
        }

        if (MathUtils.isGreaterThan(total, HUNDRED_PERCENT)) {
            throw RewardWeightException.invalidTotalPercentage();
        }
        this.totalCheckpoint.at(type).set(counter, total);
    }

    private int searchCheckpoint(String type, BigInteger timestamp) {
        Integer checkpointCount = checkpointCounter.getOrDefault(type, 1);
        DictDB<Integer, BigInteger> timeCheckpoints = this.timeCheckpoint.at(type);
        return searchCheckpoint(timestamp, checkpointCount, timeCheckpoints);
    }


    public Map<String, BigInteger> searchAssetWeight(Asset asset, BigInteger timestamp) {
        String typeId = asset.type;
        int index = searchCheckpoint(typeId, timestamp);
        return Map.of(
                "index", BigInteger.valueOf(index),
                "value", this.weightCheckpoint.at(typeId)
                        .at(index)
                        .getOrDefault(asset.address,
                                BigInteger.ZERO),
                "timestamp", this.timeCheckpoint.at(typeId)
                        .getOrDefault(index, BigInteger.ZERO)
        );
    }

    public BigInteger getTotal(String typeId) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return getTotal(typeId, timestamp);
    }

    public BigInteger getTotal(String typeId, BigInteger timestamp) {
        int index = searchCheckpoint(typeId, timestamp);
        return this.totalCheckpoint.at(typeId).getOrDefault(index, BigInteger.ZERO);
    }

    public static String msg(String message) {
        return TAG + " :: " + message;
    }

    public Asset getAsset(Address assetAddress) {
        return this.assets.get(assetAddress);
    }

    public BigInteger getTimestamp(String typeId, int index) {
        return this.timeCheckpoint.at(typeId).get(index);
    }

    public Integer getCheckpointCount(String typeId) {
        return checkpointCounter.getOrDefault(typeId, 0);
    }

    public Map<String, BigInteger> getWeightByTimestamp(String type, BigInteger timestamp) {
        DictDB<Address, BigInteger> dictDB = getCheckpoint(type, timestamp);
        Map<String, BigInteger> result = new HashMap<>();

        ArrayDB<Address> addresses = this.assetMap.at(type);

        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            BigInteger value = dictDB.getOrDefault(address, BigInteger.ZERO);
            result.put(address.toString(), value);
        }

        return result;
    }

    private DictDB<Address, BigInteger> getCheckpoint(String type, BigInteger timestamp) {
        int index = searchCheckpoint(type, timestamp);
        return this.weightCheckpoint.at(type).at(index);
    }

    public Map<String, BigInteger> getAggregatedWeight(String type, BigInteger typeWeight, BigInteger timestamp) {
        DictDB<Address, BigInteger> dictDB = getCheckpoint(type, timestamp);
        BigInteger total = BigInteger.ZERO;
        Map<String, BigInteger> result = new HashMap<>();

        ArrayDB<Address> addresses = this.assetMap.at(type);

        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            Asset asset = this.assets.get(address);
            if (asset != null) {
                String name = asset.name;
                BigInteger value = exaMultiply(dictDB.getOrDefault(address, BigInteger.ZERO), typeWeight);
                result.put(name, value);
                total = total.add(value);
            }
        }
        result.put("total", total);
        return result;
    }

    public ArrayDB<Address> getAssets(String type) {
        return this.assetMap.at(type);
    }

}
