package finance.omm.score.core.reward.db;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.exception.RewardException;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.Map;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import scorex.util.HashMap;

public class TypeWeightDB {

    private final static String TAG = "Type Weight DB";
    private final TypeDB names;
    private final BranchDB<Integer, DictDB<String, BigInteger>> wCheckpoint;
    private final DictDB<Integer, BigInteger> totalCheckpoint;
    private final DictDB<Integer, BigInteger> tCheckpoint;
    private final VarDB<Integer> checkpointCounter;


    public TypeWeightDB(String id) {
        this.names = new TypeDB("type-key-name");
        this.checkpointCounter = Context.newVarDB(id + "CheckpointCounter", Integer.class);
        this.totalCheckpoint = Context.newDictDB(id + "Total", BigInteger.class);
        this.wCheckpoint = Context.newBranchDB(id + "WeightCheckpoint", BigInteger.class);
        this.tCheckpoint = Context.newDictDB(id + "TimestampCheckpoint", BigInteger.class);
    }

    public boolean isKeyExists(String key) {
        return names.getOrDefault(key, null) != null;
    }


    public void add(String key, String name) {
        if (isKeyExists(key)) {
            throw RewardException.unknown("duplicate key (" + key + ")");
        }
        this.names.put(key, name);
    }

    public boolean isValidId(String typeId) {
        return this.names.get(typeId) != null;
    }

    public void setWeights(WeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(0);
        BigInteger latestCheckpoint = this.tCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);

        if (compareValue > 0) {
            throw RewardException.unknown("latest " + latestCheckpoint + " checkpoint exists than " + timestamp);
        }
        BigInteger total = this.totalCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        if (compareValue == 0) {
            setWeights(weights, total, checkpointCounter);
        } else {
            DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(checkpointCounter);
            Integer counter = checkpointCounter + 1;
            for (String key : this.names.keySet()) {
                BigInteger value = dictDB.get(key);
                this.wCheckpoint.at(counter).set(key, value);
            }

            setWeights(weights, total, counter);
            this.tCheckpoint.set(counter, timestamp);
            this.checkpointCounter.set(counter);
        }

    }

    private void setWeights(WeightStruct[] weights, BigInteger total, Integer counter) {
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(counter);
        for (WeightStruct tw : weights) {
            if (!isValidId(tw.id)) {
                throw RewardException.unknown(msg("Invalid type id :: " + tw.id));
            }
            BigInteger prevWeight = dictDB.getOrDefault(tw.id, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.id, tw.weight);
        }
        if (!total.equals(ICX)) {
            throw RewardException.invalidTotalPercentage();
        }
        this.totalCheckpoint.set(counter, total);
    }

    private int searchCheckpoint(int checkpoint, BigInteger timestamp) {
        int lower = 0, upper = checkpoint;
        while (upper > lower) {
            int mid = (upper + lower + 1) / 2;
            BigInteger midTimestamp = this.tCheckpoint.get(mid);
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


    public BigInteger getTotal(BigInteger timestamp) {
        int index = searchCheckpoint(this.checkpointCounter.get(), timestamp);
        return this.totalCheckpoint.get(index);
    }

    public static String msg(String message) {
        return TAG + " :: " + message;
    }


    public Map<String, BigInteger> getWeight(String typeId) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return getWeight(typeId, timestamp);
    }

    public Map<String, BigInteger> getWeight(String typeId, BigInteger timestamp) {
        int index = searchCheckpoint(this.checkpointCounter.get(), timestamp);
        return Map.of("index", BigInteger.valueOf(index), "value", this.wCheckpoint.at(index)
                        .getOrDefault(typeId,
                                BigInteger.ZERO),
                "timestamp", this.tCheckpoint.get(index));
    }


    public Integer getCheckpointCount() {
        return checkpointCounter.getOrDefault(0);
    }

    public Map<String, BigInteger> getWeightByTimestamp(BigInteger timestamp) {
        int index = searchCheckpoint(this.checkpointCounter.get(), timestamp);
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(index);
        Map<String, BigInteger> result = new HashMap<>();
        for (String key : this.names.keySet()) {
            result.put(key, dictDB.getOrDefault(key, BigInteger.ZERO));
        }
        return result;
    }
}

