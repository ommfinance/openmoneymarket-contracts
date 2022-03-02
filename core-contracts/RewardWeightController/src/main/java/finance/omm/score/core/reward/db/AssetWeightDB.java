package finance.omm.score.core.reward.db;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.exception.RewardException;
import finance.omm.score.core.reward.model.Asset;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.Map;
import score.BranchDB;
import score.Context;
import score.DictDB;
import scorex.util.HashMap;

public class AssetWeightDB {

    private final static String TAG = "Entity Weight DB";
    public static final String ID_PREFIX = "ASSET-";
    private final DictDB<String, Asset> assets;

    private final BranchDB<String, BranchDB<Integer, DictDB<String, BigInteger>>> wCheckpoint;
    private final BranchDB<String, DictDB<Integer, BigInteger>> totalCheckpoint;
    private final BranchDB<String, DictDB<Integer, BigInteger>> tCheckpoint;
    private final DictDB<String, Integer> checkpointCounter;
    private final DictDB<String, Integer> nonce;


    public AssetWeightDB(String key) {
        this.checkpointCounter = Context.newDictDB(key + "CheckpointCounter", Integer.class);
        this.totalCheckpoint = Context.newBranchDB(key + "Total", BigInteger.class);
        this.wCheckpoint = Context.newBranchDB(key + "WeightCheckpoint", BigInteger.class);
        this.tCheckpoint = Context.newBranchDB(key + "TimestampCheckpoint", BigInteger.class);

        this.assets = Context.newDictDB(key + "Asset", Asset.class);
        this.nonce = Context.newDictDB(key + "Nonce", Integer.class);
    }


    public String addAsset(String typeId, String name) {
        Integer nonce = this.nonce.getOrDefault(typeId, 1);
        String id = getId(typeId, nonce);
        Asset asset = new Asset(id, typeId);
        asset.name = name;
        this.assets.set(id, asset);
        this.nonce.set(typeId, nonce + 1);
        return id;
    }


    public void setWeights(String typeId, WeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(typeId, 0);
        BigInteger latestCheckpoint = this.tCheckpoint.at(typeId).getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);
        if (compareValue > 0) {
            throw RewardException.unknown("latest " + latestCheckpoint + " checkpoint exists than " + timestamp);
        }

        BigInteger total = this.totalCheckpoint.at(typeId).getOrDefault(checkpointCounter, BigInteger.ZERO);
        if (compareValue == 0) {
            setWeights(typeId, total, weights, checkpointCounter);
        } else {
            DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(typeId).at(checkpointCounter);
            Integer counter = checkpointCounter + 1;
            DictDB<String, BigInteger> newCheckpoint = this.wCheckpoint.at(typeId).at(counter);
            Integer nonce = this.nonce.getOrDefault(typeId, 0);
            for (int i = 1; i < nonce; i++) {
                String key = getId(typeId, i);
                BigInteger value = dictDB.get(key);
                newCheckpoint.set(key, value);
            }
            setWeights(typeId, total, weights, counter);
            this.tCheckpoint.at(typeId).set(counter, timestamp);
            this.checkpointCounter.set(typeId, counter);
        }
    }

    private void setWeights(String typeId, BigInteger total, WeightStruct[] weights, Integer counter) {
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(typeId).at(counter);
        for (WeightStruct tw : weights) {
            if (!isValidId(tw.id)) {
                throw RewardException.unknown(msg("Invalid asset id :: " + tw.id));
            }
            BigInteger prevWeight = dictDB.getOrDefault(tw.id, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.id, tw.weight);
        }
        if (!total.equals(ICX)) {
            throw RewardException.invalidTotalPercentage();
        }
        this.totalCheckpoint.at(typeId).set(counter, total);
    }


    private int searchCheckpoint(String typeId, int checkpoint, BigInteger timestamp) {
        int lower = 0, upper = checkpoint;
        while (upper > lower) {
            int mid = (upper + lower + 1) / 2;
            BigInteger midTimestamp = this.tCheckpoint.at(typeId).get(mid);
            int value = midTimestamp.compareTo(timestamp);
            if (value < 0) {
                lower = mid;
            } else if (value > 0) {
                upper = mid - 1;
            } else {
                return mid;
            }
        }
        return lower;
    }

    public Map<String, BigInteger> getWeight(String assetId) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return getWeight(assetId, timestamp);
    }

    public Map<String, BigInteger> getWeight(String assetId, BigInteger timestamp) {
        Asset asset = this.assets.get(assetId);
        return getWeight(asset, timestamp);
    }

    public Map<String, BigInteger> getWeight(Asset asset, BigInteger timestamp) {
        String typeId = asset.typeId;
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(typeId, 0);
        int index = searchCheckpoint(typeId, checkpointCounter, timestamp);
        return Map.of(
                "index", BigInteger.valueOf(index),
                "value", this.wCheckpoint.at(typeId)
                        .at(index)
                        .getOrDefault(asset.id,
                                BigInteger.ZERO),
                "timestamp", this.tCheckpoint.at(typeId)
                        .get(index)
        );
    }

    public BigInteger getTotal(String typeId) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return getTotal(typeId, timestamp);
    }

    public BigInteger getTotal(String typeId, BigInteger timestamp) {
        int index = searchCheckpoint(typeId, this.checkpointCounter.get(typeId), timestamp);
        return this.totalCheckpoint.at(typeId).getOrDefault(index, BigInteger.ZERO);
    }

    public boolean isValidId(String id) {
        return this.assets.get(id) != null;
    }

    public String getId(String typeId, Integer id) {
        return ID_PREFIX + typeId + "_" + id;
    }

    public static String msg(String message) {
        return TAG + " :: " + message;
    }

    public Asset getAsset(String assetId) {
        return this.assets.get(assetId);
    }

    public BigInteger getTimestamp(String typeId, int index) {
        return this.tCheckpoint.at(typeId).get(index);
    }

    public Integer getCheckpointCount(String typeId) {
        return checkpointCounter.getOrDefault(typeId, 0);
    }

    public Map<String, BigInteger> getWeightByTimestamp(String typeId, BigInteger timestamp) {
        DictDB<String, BigInteger> dictDB = getCheckpoint(typeId, timestamp);
        Map<String, BigInteger> result = new HashMap<>();
        for (int i = 1; i < this.nonce.get(typeId); i++) {
            String id = getId(typeId, i);
            BigInteger value = dictDB.getOrDefault(id, BigInteger.ZERO);
            result.put(id, value);
        }
        return result;
    }

    private DictDB<String, BigInteger> getCheckpoint(String typeId, BigInteger timestamp) {
        int index = searchCheckpoint(typeId, this.checkpointCounter.get(typeId), timestamp);
        return this.wCheckpoint.at(typeId).at(index);
    }

    public Map<String, BigInteger> getAggregatedWeight(BigInteger weight, String typeId, BigInteger timestamp) {
        DictDB<String, BigInteger> dictDB = getCheckpoint(typeId, timestamp);
        BigInteger total = BigInteger.ZERO;
        Map<String, BigInteger> result = new HashMap<>();
        for (int i = 1; i < this.nonce.get(typeId); i++) {
            String id = getId(typeId, i);
            String name = assets.get(id).name;
            BigInteger value = dictDB.getOrDefault(id, BigInteger.ZERO);
            result.put(name, value);
            total = total.add(value);
        }
        result.put("total", total);
        return result;
    }

}
