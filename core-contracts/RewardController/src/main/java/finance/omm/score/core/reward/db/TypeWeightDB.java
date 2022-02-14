package finance.omm.score.core.reward.db;

import finance.omm.score.core.reward.struct.WeightStruct;
import finance.omm.score.core.reward.utils.TimeConstants;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.reward.utils.MathUtils.ICX;

public class TypeWeightDB {

    private final static String TAG = "Type Weight DB";
    public static final String ID_PREFIX = "TYPE_";
    private final DictDB<String, String> name;
    private final BranchDB<Integer, DictDB<String, BigInteger>> wCheckpoint;
    private final DictDB<Integer, BigInteger> totalCheckpoint;
    private final DictDB<Integer, BigInteger> tCheckpoint;
    private final VarDB<Integer> checkpointCounter;
    private final VarDB<Integer> nonce;

    public TypeWeightDB(String id) {
        this.name = Context.newDictDB(id + "Name", String.class);
        this.nonce = Context.newVarDB(id + "Nonce", Integer.class);

        this.checkpointCounter = Context.newVarDB(id + "CheckpointCounter", Integer.class);
        this.totalCheckpoint = Context.newDictDB(id + "Total", BigInteger.class);
        this.wCheckpoint = Context.newBranchDB(id + "WeightCheckpoint", BigInteger.class);
        this.tCheckpoint = Context.newDictDB(id + "TimestampCheckpoint", BigInteger.class);
    }


    @EventLog(indexed = 2)
    public void AddType(String id, String name) {}

    @EventLog(indexed = 3)
    public void ChangeType(Integer id, String name, BigInteger weight, Long timestamp) {}

    public void add(String name) {
        Integer nonce = this.nonce.getOrDefault(1);
        String id = ID_PREFIX + nonce;
        this.nonce.set(nonce + 1);
        this.name.set(id, name);
        AddType(id, name);
    }

    public boolean isValidId(String typeId) {
        return this.name.get(typeId) != null;
    }

    public void setWeights(WeightStruct[] weights, BigInteger timestamp) {
        Integer checkpointCounter = this.checkpointCounter.getOrDefault(0);
        BigInteger latestCheckpoint = this.tCheckpoint.getOrDefault(checkpointCounter, BigInteger.ZERO);
        int compareValue = latestCheckpoint.compareTo(timestamp);
        Context.require(compareValue <= 0, msg(" latest " + latestCheckpoint + " checkpoint exists than " + timestamp));

        if (compareValue == 0) {
            setWeights(weights, checkpointCounter);
        } else {
            DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(checkpointCounter);
            //TODO find better approach
            Integer counter = checkpointCounter + 1;
            for (int i = 1; i < nonce.get(); i++) {
                String key = ID_PREFIX + i;
                BigInteger value = dictDB.get(key);
                this.wCheckpoint.at(counter).set(key, value);
            }

            setWeights(weights, counter);
            this.tCheckpoint.set(counter, timestamp);
        }

    }

    private void setWeights(WeightStruct[] weights, Integer counter) {
        BigInteger total = this.totalCheckpoint.getOrDefault(counter, BigInteger.ZERO);
        DictDB<String, BigInteger> dictDB = this.wCheckpoint.at(counter);
        for (WeightStruct tw : weights) {
            Context.require(isValidId(tw.id), msg("Invalid type id :: " + tw.id));
            BigInteger prevWeight = dictDB.getOrDefault(tw.id, BigInteger.ZERO);
            total = total.subtract(prevWeight).add(tw.weight);
            dictDB.set(tw.id, tw.weight);
        }
        Context.require(total.equals(ICX), msg("Total distribution is not 100%"));
        this.totalCheckpoint.set(counter, total);
    }


    private int searchCheckpoint(int checkpoint, BigInteger timestamp) {
        int lower = 0, upper = checkpoint - 1;
        while (upper > lower) {
            int mid = lower + (upper - lower) / 2;
            BigInteger midTimestamp = this.tCheckpoint.get(mid);
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

}

