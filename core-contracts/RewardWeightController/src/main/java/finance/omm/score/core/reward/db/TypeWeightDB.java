package finance.omm.score.core.reward.db;

import static finance.omm.utils.math.MathUtils.HUNDRED_PERCENT;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.score.core.reward.exception.RewardWeightException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import scorex.util.HashMap;

public class TypeWeightDB implements Searchable {

    private final static String TAG = "Type Weight DB";
    private final TypeDB types;
    //checkpoint -> name of type -> weight value
    private final BranchDB<Integer, DictDB<String, BigInteger>> wCheckpoint;
    //checkpoint -> sum of all weights at checkpoint
    private final DictDB<Integer, BigInteger> totalAtCheckpoint;
    //checkpoint -> timestamp
    private final DictDB<Integer, BigInteger> timeCheckpoint;
    private final VarDB<Integer> checkpointCounter;


    public TypeWeightDB(String id) {
        this.types = new TypeDB(id + "type-key-name");
        this.checkpointCounter = Context.newVarDB(id + "CheckpointCounter", Integer.class);
        this.totalAtCheckpoint = Context.newDictDB(id + "Total", BigInteger.class);
        this.wCheckpoint = Context.newBranchDB(id + "WeightCheckpoint", BigInteger.class);
        this.timeCheckpoint = Context.newDictDB(id + "TimestampCheckpoint", BigInteger.class);
    }

    public boolean isTypeExists(String type) {
        return types.get(type) != null;
    }


    public void add(String type, Boolean isPlatformRecipient) {
        if (isTypeExists(type)) {
            throw RewardWeightException.unknown("duplicate type (" + type + ")");
        }
        this.types.put(type, isPlatformRecipient);
    }

    public void setWeights(TypeWeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(0);
        BigInteger latestCheckpoint = this.timeCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);

        if (compareValue > 0) {
            throw RewardWeightException.unknown("latest " + latestCheckpoint + " checkpoint exists than " + timestamp);
        }

        if (getBlockTimestampInSecond().compareTo(timestamp) > 0) {
            throw RewardWeightException.unknown("can't set weight value for old timestamp " + timestamp);
        }

        BigInteger total = this.totalAtCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);

        if (compareValue == 0) {
            setWeights(weights, total, checkpointCounter);
        } else {
            DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(checkpointCounter);
            Integer counter = checkpointCounter + 1;
            DictDB<String, BigInteger> newCheckPoints = this.wCheckpoint.at(counter);
            for (String key : this.types.keySet()) {
                BigInteger value = dictDB.get(key);
                newCheckPoints.set(key, value);
            }

            setWeights(weights, total, counter);
            this.timeCheckpoint.set(counter, timestamp);
            this.checkpointCounter.set(counter);
        }

    }

    private void setWeights(TypeWeightStruct[] weights, BigInteger total, Integer counter) {
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(counter);
        for (TypeWeightStruct tw : weights) {
            if (!isTypeExists(tw.key)) {
                throw RewardWeightException.unknown(msg("Invalid type key :: " + tw.key));
            }
            BigInteger prevWeight = dictDB.getOrDefault(tw.key, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.key, tw.weight);
        }
        if (!total.equals(HUNDRED_PERCENT)) {
            throw RewardWeightException.invalidTotalPercentage();
        }
        this.totalAtCheckpoint.set(counter, total);
    }

    private int searchCheckpoint(BigInteger timestamp) {
        Integer checkpointCount = checkpointCounter.getOrDefault(1);
        return searchCheckpoint(timestamp, checkpointCount, this.timeCheckpoint);
    }


    public BigInteger getTotal(BigInteger timestamp) {
        int index = searchCheckpoint(timestamp);
        return this.totalAtCheckpoint.get(index);
    }

    public static String msg(String message) {
        return TAG + " :: " + message;
    }

    public List<String> getTypes() {
        return types.keySet();
    }

    public BigInteger getTimestamp(int index) {
        return this.timeCheckpoint.get(index);
    }

    public Map<String, BigInteger> weightOfAllTypes(BigInteger timestamp) {
        Map<String, BigInteger> response = new HashMap<>();
        for (String key : types.keySet()) {
            Map<String, BigInteger> map = searchTypeWeight(key, timestamp);
            response.put(key, map.get("value"));
        }
        return response;
    }

    //TODO remove me
    public Map<String, BigInteger> searchTypeWeight(String type) {
        BigInteger timestamp = getBlockTimestampInSecond();
        return searchTypeWeight(type, timestamp);
    }

    public Map<String, BigInteger> searchTypeWeight(String type, BigInteger timestamp) {
        int index = searchCheckpoint(timestamp);
        return Map.of("index", BigInteger.valueOf(index), "value", this.wCheckpoint.at(index)
                        .getOrDefault(type,
                                BigInteger.ZERO),
                "timestamp", this.timeCheckpoint.getOrDefault(index, BigInteger.ZERO));
    }


    public Integer getCheckpointCount() {
        return checkpointCounter.getOrDefault(0);
    }

    public Map<String, BigInteger> getWeightByTimestamp(BigInteger timestamp) {
        int index = searchCheckpoint(timestamp);
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(index);
        Map<String, BigInteger> result = new HashMap<>();
        for (String key : this.types.keySet()) {
            result.put(key, dictDB.getOrDefault(key, BigInteger.ZERO));
        }
        return result;
    }

    public boolean isPlatformRecipient(String typeId) {
        return this.types.getOrDefault(typeId, Boolean.FALSE);
    }
}

