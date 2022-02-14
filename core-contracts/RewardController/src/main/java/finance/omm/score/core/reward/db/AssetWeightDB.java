package finance.omm.score.core.reward.db;

import finance.omm.score.core.reward.model.Asset;
import finance.omm.score.core.reward.struct.WeightStruct;
import finance.omm.score.core.reward.utils.TimeConstants;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.reward.utils.MathUtils.ICX;

public class AssetWeightDB {

    private final static String TAG = "Entity Weight DB";
    public static final String ID_PREFIX = "ASSET_";
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

    @EventLog(indexed = 2)
    public void AssetAdded(String id, String name) {}

    public void addAsset(String typeId, String name, Address address, BigInteger lpID) {
        Integer nonce = this.nonce.getOrDefault(typeId, 1);
        String id = getId(typeId, nonce);
        Asset asset = new Asset(typeId, id);
        asset.address = address;
        asset.lpID = lpID;
        asset.name = name;
        this.assets.set(id, asset);
        this.nonce.set(typeId, nonce + 1);
        AssetAdded(id, name);
    }


    public void setWeights(String typeId, WeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(typeId, 0);
        BigInteger latestCheckpoint = this.tCheckpoint.at(typeId).getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);
        Context.require(compareValue <= 0, msg(" latest " + latestCheckpoint + " checkpoint exists than " + timestamp));
        if (compareValue == 0) {
            setWeights(typeId, weights, checkpointCounter);
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
            setWeights(typeId, weights, counter);
            this.tCheckpoint.at(typeId).set(counter, timestamp);

        }
    }

    private void setWeights(String typeId, WeightStruct[] weights, Integer counter) {
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(typeId).at(counter);
        BigInteger total = this.totalCheckpoint.at(typeId).getOrDefault(counter, BigInteger.ZERO);
        for (WeightStruct tw : weights) {
            Context.require(isValidId(tw.id), msg("Invalid asset id :: " + tw.id));
            BigInteger prevWeight = dictDB.getOrDefault(tw.id, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.id, tw.weight);
        }
        Context.require(total.equals(ICX), msg("Total distribution is not equals to 100%"));
        this.totalCheckpoint.at(typeId).set(counter, total);
    }


    private int searchCheckpoint(String typeId, int checkpoint, BigInteger timestamp) {
        int lower = 0, upper = checkpoint - 1;
        while (upper > lower) {
            int mid = lower + (upper - lower) / 2;
            BigInteger midTimestamp = this.tCheckpoint.at(typeId).get(mid);
            int value = midTimestamp.compareTo(timestamp);
            if (value == 0) {
                return mid;
            } else if (value < 0) {
                lower = mid;
            } else {
                upper = mid - 1;
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
        int index = searchCheckpoint(asset.typeId, this.checkpointCounter.get(typeId), timestamp);
        return Map.of("index", BigInteger.valueOf(index), "value", this.wCheckpoint.at(typeId)
                                                                                   .at(index)
                                                                                   .getOrDefault(asset.id,
                                                                                           BigInteger.ZERO),
                "timestamp", this.tCheckpoint.at(typeId)
                                             .get(index));
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
        return ID_PREFIX + typeId + id;
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
}
