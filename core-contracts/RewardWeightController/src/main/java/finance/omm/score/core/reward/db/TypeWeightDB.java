package finance.omm.score.core.reward.db;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.score.core.reward.exception.RewardWeightException;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class TypeWeightDB implements Searchable {

    private final static String TAG = "Type Weight DB";
    private final TypeDB types;
    private final BranchDB<Integer, DictDB<String, BigInteger>> wCheckpoint;
    private final DictDB<Integer, BigInteger> totalCheckpoint;
    private final DictDB<Integer, BigInteger> tCheckpoint;
    private final VarDB<Integer> checkpointCounter;


    public TypeWeightDB(String id) {
        this.types = new TypeDB(id + "type-key-name");
        this.checkpointCounter = Context.newVarDB(id + "CheckpointCounter", Integer.class);
        this.totalCheckpoint = Context.newDictDB(id + "Total", BigInteger.class);
        this.wCheckpoint = Context.newBranchDB(id + "WeightCheckpoint", BigInteger.class);
        this.tCheckpoint = Context.newDictDB(id + "TimestampCheckpoint", BigInteger.class);
    }

    public boolean isKeyExists(String key) {
        return types.getOrDefault(key, null) != null;
    }


    public void add(String key, Boolean transferToContract) {
        if (isKeyExists(key)) {
            throw RewardWeightException.unknown("duplicate key (" + key + ")");
        }
        this.types.put(key, transferToContract);
    }

    public boolean isValidId(String typeId) {
        return this.types.get(typeId) != null;
    }


    public void setWeights(TypeWeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(0);
        BigInteger latestCheckpoint = this.tCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);

        if (compareValue > 0) {
            throw RewardWeightException.unknown("latest " + latestCheckpoint + " checkpoint exists than " + timestamp);
        }
        BigInteger total = this.totalCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        if (compareValue == 0) {
            setWeights(weights, total, checkpointCounter);
        } else {
            DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(checkpointCounter);
            Integer counter = checkpointCounter + 1;
            for (String key : this.types.keySet()) {
                BigInteger value = dictDB.get(key);
                this.wCheckpoint.at(counter).set(key, value);
            }

            setWeights(weights, total, counter);
            this.tCheckpoint.set(counter, timestamp);
            this.checkpointCounter.set(counter);
        }

    }

    private void setWeights(TypeWeightStruct[] weights, BigInteger total, Integer counter) {
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(counter);
        for (TypeWeightStruct tw : weights) {
            if (!isValidId(tw.key)) {
                throw RewardWeightException.unknown(msg("Invalid type key :: " + tw.key));
            }
            BigInteger prevWeight = dictDB.getOrDefault(tw.key, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.key, tw.weight);
        }
        if (!total.equals(ICX)) {
            throw RewardWeightException.invalidTotalPercentage();
        }
        this.totalCheckpoint.set(counter, total);
    }

    private int searchCheckpoint(BigInteger timestamp) {
        Integer checkpointCount = checkpointCounter.getOrDefault(1);
        DictDB<Integer, BigInteger> timeCheckpoints = this.tCheckpoint;
        return searchCheckpoint(timestamp, checkpointCount, timeCheckpoints);
    }


    public BigInteger getTotal(BigInteger timestamp) {
        int index = searchCheckpoint(timestamp);
        return this.totalCheckpoint.get(index);
    }

    public static String msg(String message) {
        return TAG + " :: " + message;
    }

    public List<String> getContractTypeIds() {
        List<String> response = new ArrayList<>();
        List<String> list = types.keySet();
        for (String key : list) {
            if (isContractType(key)) {
                response.add(key);
            }
        }
        return response;
    }

    public List<String> getTypes() {
        return types.keySet();
    }

    public Map<String, BigInteger> weightOfAllTypes(BigInteger timestamp) {
        Map<String, BigInteger> response = new HashMap<>();
        for (String key : types.keySet()) {
            Map<String, BigInteger> map = searchTypeWeight(key, timestamp);
            response.put(key, map.get("value"));
        }
        return response;
    }


    public Map<String, BigInteger> searchTypeWeight(String type) {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return searchTypeWeight(type, timestamp);
    }

    public Map<String, BigInteger> searchTypeWeight(String type, BigInteger timestamp) {
        int index = searchCheckpoint(timestamp);
        return Map.of("index", BigInteger.valueOf(index), "value", this.wCheckpoint.at(index)
                        .getOrDefault(type,
                                BigInteger.ZERO),
                "timestamp", this.tCheckpoint.getOrDefault(index, BigInteger.ZERO));
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

    public boolean isContractType(String typeId) {
        Boolean isContract = this.types.get(typeId);
        return Boolean.TRUE.compareTo(isContract) == 0;
    }
}

