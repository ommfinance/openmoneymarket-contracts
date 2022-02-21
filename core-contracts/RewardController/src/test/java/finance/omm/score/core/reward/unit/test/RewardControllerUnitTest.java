package finance.omm.score.core.reward.unit.test;


import static finance.omm.score.core.reward.db.TypeWeightDB.ID_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.structs.WeightStruct;
import finance.omm.score.core.reward.RewardControllerImpl;
import finance.omm.score.core.reward.db.AssetWeightDB;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class RewardControllerUnitTest extends TestBase {

    private static final ServiceManager sm = getServiceManager();
    private Account owner;
    private Score score;
    private RewardControllerImpl scoreSpy;


    private final Map<String, BigInteger> DISTRIBUTION_PERCENTAGE = new HashMap<>() {{
        put("worker", BigInteger.valueOf(30).multiply(ICX).divide(BigInteger.valueOf(100)));
        put("daoFund", BigInteger.valueOf(40).multiply(ICX).divide(BigInteger.valueOf(100)));
        put("lendingBorrow", BigInteger.valueOf(10).multiply(ICX).divide(BigInteger.valueOf(100)));
        put("liquidityProvider", BigInteger.valueOf(20).multiply(ICX).divide(BigInteger.valueOf(100)));
    }};

    private Map<String, Account> mockAddress = new HashMap<>() {{
        put("mockAddressProvider", Account.newScoreAccount(1));
        put("mockLendingPoolCore", Account.newScoreAccount(2));
        put("mockLendingPool", Account.newScoreAccount(3));
        put("mockOMMToken", Account.newScoreAccount(4));
        put("mockWorkerToken", Account.newScoreAccount(5));
        put("mockLPToken", Account.newScoreAccount(6));
        put("mockDAOFund", Account.newScoreAccount(7));
        put("mockStakedLP", Account.newScoreAccount(8));
        put("mockGovernance", Account.newScoreAccount(9));
        put("mockAsset1", Account.newScoreAccount(10));
        put("mockAsset2", Account.newScoreAccount(11));
    }};
    private final BigInteger startTimestamp = getTimestamp();


    @BeforeEach
    void setup() throws Exception {
        owner = sm.createAccount(100);
        score = sm.deploy(owner, RewardControllerImpl.class, startTimestamp);
        scoreSpy = (RewardControllerImpl) spy(score.getInstance());
        score.setInstance(scoreSpy);

    }

    private void addType(Account account, String name) {
        score.invoke(account, "addType", name);
    }

    @DisplayName("verify token inflation rate")
    @Test
    public void testTokenDistributionPerDay() {
        BigInteger tokenIn5Years = (BigInteger) this.score.call("tokenDistributionPerDay", BigInteger.valueOf(5 * 365));
        BigInteger tokenIn6Years = (BigInteger) this.score.call("tokenDistributionPerDay", BigInteger.valueOf(6 * 365));
        BigDecimal inflation_56 = new BigDecimal(tokenIn6Years.subtract(tokenIn5Years)).divide(
                new BigDecimal(tokenIn5Years), 2, RoundingMode.HALF_UP);
        assertEquals(0.03, inflation_56.doubleValue());
        BigInteger tokenIn7Years = (BigInteger) this.score.call("tokenDistributionPerDay", BigInteger.valueOf(7 * 365));
        BigDecimal inflation_67 = new BigDecimal(tokenIn7Years.subtract(tokenIn6Years)).divide(
                new BigDecimal(tokenIn6Years), 2, RoundingMode.HALF_UP);
        assertEquals(0.03, inflation_67.doubleValue());
    }

    @DisplayName("add type")
    @Test
    public void testAddType() {
        addType(owner, "Type 1");
        verify(scoreSpy).AddType(ID_PREFIX + 1, "Type 1");

        addType(owner, "Type 2");
        verify(scoreSpy).AddType(ID_PREFIX + 2, "Type 2");
    }

    @DisplayName("invalid total type weight")
    @Test
    public void testSetInvalidTypeWeight() {
        addType(owner, "Type 2");
        WeightStruct[] weights = new WeightStruct[1];

        WeightStruct struct = new WeightStruct();
        struct.weight = BigInteger.TEN.multiply(ICX).divide(BigInteger.valueOf(100));
        struct.id = ID_PREFIX + 1;
        weights[0] = struct;

        Object[] params = new Object[]{weights, BigInteger.ZERO};
        Executable call = () -> score.invoke(owner, "setTypeWeight", params);
        expectErrorMessage(call, "Total percentage is not equals to 100%");
    }

    @DisplayName("add type weight")
    @Test
    public void testSetTypeWeight() {
        initTypeWeight(BigInteger.ZERO, 10L, 20L, 70L);

        Map<Integer, BigInteger> snapshots = new HashMap<>();
        snapshots.put(1, getTimestamp());

        verify(scoreSpy).SetTypeWeight(getTimestamp(), "Type weight updated");

        BigInteger checkpoints = (BigInteger) score.call("getTypeCheckpointCount");
        assertEquals(BigInteger.ONE, checkpoints);

        setTypeWeight(snapshots.get(1), new HashMap<>() {{
            put(1, 40L);
            put(3, 40L);
        }});

        checkpoints = (BigInteger) score.call("getTypeCheckpointCount");
        assertEquals(BigInteger.ONE, checkpoints);

        Executable call = () -> setTypeWeight(snapshots.get(1), new HashMap<>() {{
            put(1, 40L);
            put(2, 0L);
        }});

        expectErrorMessage(call, "Total percentage is not equals to 100%");


    }

    @DisplayName("type weight snapshot")
    @Test
    public void testTypeWeightSnapshot() {
        initTypeWeight(BigInteger.ZERO, 10L, 20L, 30L, 40L);

        Map<Integer, BigInteger> snapshots = new HashMap<>();
        Map<Integer, Map<Integer, Long>> values = new HashMap<>();
        snapshots.put(1, getTimestamp());
        values.put(1, new HashMap<>() {{
            put(1, 10L);
            put(2, 20L);
            put(3, 30L);
            put(4, 40L);
        }});
        Random r = new Random();
        for (int i = 2; i <= 10; i++) {
            sm.getBlock().increase(r.nextInt(1000) + 1);
            long a = r.nextInt(25) + 1;
            long b = r.nextInt(25) + 1;
            long c = r.nextInt(25) + 1;
            long d = 100 - a - b - c;
            Map<Integer, Long> map = new HashMap<>() {{
                put(1, a);
                put(2, b);
                put(3, c);
                put(4, d);
            }};
            setTypeWeight(BigInteger.ZERO, map);
            snapshots.put(i, getTimestamp());
            values.put(i, map);
        }

        for (int i = 10; i > 1; i--) {
            BigInteger timestamp = snapshots.get(i);
            Map<String, BigInteger> nextTime = (Map<String, BigInteger>) score.call("getTypeWeightByTimestamp",
                    timestamp.add(BigInteger.ONE));
            Map<String, BigInteger> exactTime = (Map<String, BigInteger>) score.call("getTypeWeightByTimestamp",
                    timestamp);
            Map<String, BigInteger> prevTime = (Map<String, BigInteger>) score.call("getTypeWeightByTimestamp",
                    timestamp.subtract(BigInteger.ONE));

            Map<Integer, Long> value = values.get(i);
            for (Map.Entry<Integer, Long> entry : value.entrySet()) {
                String id = ID_PREFIX + entry.getKey();
                BigInteger next = nextTime.get(id);
                BigInteger exact = exactTime.get(id);
                BigInteger expected = BigInteger.valueOf(entry.getValue())
                        .multiply(ICX)
                        .divide(BigInteger.valueOf(100));
                assertEquals(expected, next, "next data not match at " + id);
                assertEquals(expected, exact, "exact data not match at " + id);
            }

            value = values.get(i - 1);
            for (Map.Entry<Integer, Long> entry : value.entrySet()) {
                String id = ID_PREFIX + entry.getKey();
                BigInteger prev = prevTime.get(id);
                BigInteger expected = BigInteger.valueOf(entry.getValue())
                        .multiply(ICX)
                        .divide(BigInteger.valueOf(100));

                assertEquals(expected, prev, "previous data not match at " + id);
            }
        }

    }

    @DisplayName("test add asset")
    @Test
    public void testAddAsset() {
        initTypeWeight(BigInteger.ZERO, 10L, 20L, 30L, 40L);
        Address asset = Account.newScoreAccount(11).getAddress();
        String typeId = ID_PREFIX + 1;
        addAsset(1, typeId, asset);
        verify(scoreSpy).AssetAdded(AssetWeightDB.ID_PREFIX + typeId + "_" + 1, typeId, "Asset 1", asset,
                BigInteger.ZERO);
    }

    @DisplayName("test asset weight")
    @Test
    public void testSetAssetWeight() {
        initTypeWeight(BigInteger.ZERO, 25L, 75L);
        Map<String, BigInteger> snapshots = new HashMap<>();

        Address asset = Account.newScoreAccount(101).getAddress();
        String typeId = ID_PREFIX + 1;
        addAsset(1, typeId, asset);

        Address asset_2 = Account.newScoreAccount(102).getAddress();
        String typeId_2 = ID_PREFIX + 2;
        addAsset(2, typeId_2, asset_2);

        initAssetWeight(BigInteger.ZERO, typeId, 10L, 20L, 30L, 40L);
        snapshots.put(typeId, getTimestamp());

        BigInteger checkpoints = (BigInteger) score.call("getAssetCheckpointCount",
                typeId);
        assertEquals(BigInteger.ONE, checkpoints);

        BigInteger futureTime = getTimestamp().add(TimeConstants.SECOND.multiply(BigInteger.valueOf(100)));
        initAssetWeight(futureTime, typeId_2, 50L, 50L);
        snapshots.put(typeId_2, futureTime);

        checkpoints = (BigInteger) score.call("getAssetCheckpointCount",
                typeId_2);
        assertEquals(BigInteger.ONE, checkpoints);

        setAssetWeight(BigInteger.ZERO, typeId, new HashMap<>() {{
            put(1, 40L);
            put(4, 10L);
        }});
        checkpoints = (BigInteger) score.call("getAssetCheckpointCount",
                typeId);
        assertEquals(BigInteger.TWO, checkpoints);

        //shouldn't able to set new asset weight if future weight is already exists
        BigInteger current = getTimestamp().add(TimeConstants.SECOND.multiply(BigInteger.TWO));
        Executable call = () -> setAssetWeight(BigInteger.ZERO, typeId_2, new HashMap<>() {{
            put(1, 40L);
            put(2, 60L);
        }});
        expectErrorMessage(call,
                "latest " + snapshots.get(typeId_2) + " checkpoint exists than " + current);


    }

    @DisplayName("asset weight snapshot")
    @Test
    public void testAssetWeightSnapshot() {
        initTypeWeight(BigInteger.ZERO, 25L, 75L);

        Address asset = Account.newScoreAccount(101).getAddress();
        String typeId = ID_PREFIX + 1;
        addAsset(1, typeId, asset);

        Address asset_2 = Account.newScoreAccount(102).getAddress();
        String typeId_2 = ID_PREFIX + 2;
        addAsset(2, typeId_2, asset_2);

        initAssetWeight(BigInteger.ZERO, typeId, 10L, 20L, 30L, 40L);
        Map<Integer, BigInteger> snapshots = new HashMap<>();
        Map<Integer, Map<Integer, Long>> values = new HashMap<>();
        snapshots.put(1, getTimestamp());
        values.put(1, new HashMap<>() {{
            put(1, 10L);
            put(2, 20L);
            put(3, 30L);
            put(4, 40L);
        }});

        initAssetWeight(BigInteger.ZERO, typeId_2, 25L, 25L, 25L, 25L);
        snapshots.put(2, getTimestamp());
        values.put(2, new HashMap<>() {{
            put(1, 25L);
            put(2, 25L);
            put(3, 25L);
            put(4, 25L);
        }});
        Random r = new Random();

        for (int i = 3; i <= 20; i++) {
            sm.getBlock().increase(r.nextInt(1000) + 1);
            long a = r.nextInt(25) + 1;
            long b = r.nextInt(25) + 1;
            long c = r.nextInt(25) + 1;
            long d = 100 - a - b - c;
            Map<Integer, Long> map = new HashMap<>() {{
                put(1, a);
                put(2, b);
                put(3, c);
                put(4, d);
            }};
            String type_id = i % 2 == 0 ? typeId_2 : typeId;
            setAssetWeight(BigInteger.ZERO, type_id, map);
            snapshots.put(i, getTimestamp());
            values.put(i, map);
        }

        for (int i = 20; i > 2; i--) {
            BigInteger timestamp = snapshots.get(i);
            String type_id = i % 2 == 0 ? typeId_2 : typeId;
            Map<String, BigInteger> nextTime = (Map<String, BigInteger>) score.call("getAssetWeightByTimestamp",
                    type_id,
                    timestamp.add(BigInteger.ONE));
            Map<String, BigInteger> exactTime = (Map<String, BigInteger>) score.call("getAssetWeightByTimestamp",
                    type_id,
                    timestamp);
            Map<String, BigInteger> prevTime = (Map<String, BigInteger>) score.call("getAssetWeightByTimestamp",
                    type_id,
                    timestamp.subtract(BigInteger.ONE));

            Map<Integer, Long> value = values.get(i);
            for (Map.Entry<Integer, Long> entry : value.entrySet()) {
                String id = AssetWeightDB.ID_PREFIX + type_id + "_" + entry.getKey();
                BigInteger next = nextTime.get(id);
                BigInteger exact = exactTime.get(id);
                BigInteger expected = BigInteger.valueOf(entry.getValue())
                        .multiply(ICX)
                        .divide(BigInteger.valueOf(100));
                assertEquals(expected, next, "next data not match at " + id);
                assertEquals(expected, exact, "exact data not match at " + id);
            }

            value = values.get(i - 2);
            for (Map.Entry<Integer, Long> entry : value.entrySet()) {
                String id = AssetWeightDB.ID_PREFIX + type_id + "_" + entry.getKey();
                BigInteger prev = prevTime.get(id);
                BigInteger expected = BigInteger.valueOf(entry.getValue())
                        .multiply(ICX)
                        .divide(BigInteger.valueOf(100));

                assertEquals(expected, prev, "previous data not match at " + id);
            }
        }

    }

    @DisplayName("Integrate index test")
    @Test
    public void testIntegrateIndex() {
        sm.getBlock().increase(30 * 86400 / 2 - 3008);
        initTypeWeight(BigInteger.ZERO, 25L, 75L); //3 calls

        String typeId = ID_PREFIX + 1;

        initAssetWeight(BigInteger.ZERO, typeId, 10L, 20L, 30L, 40L); //5 calls
        BigInteger currentTimestamp = getTimestamp();
        sm.getBlock().increase(1000);
        Object[] params = new Object[]{
                AssetWeightDB.ID_PREFIX + typeId + "_" + 1,
                BigInteger.valueOf(100).multiply(ICX),
                currentTimestamp
        };

        BigInteger index = (BigInteger) score.call("getIntegrateIndex", params);

        BigInteger time_delta = getTimestamp().subtract(currentTimestamp).divide(TimeConstants.SECOND);

        /*
          type percentage 25%
          asset percentage 10%
          daily token distribution 1_000_000
         */
        float emissionPerSecond = 0.10f * 0.25f * 1_000_000 / TimeConstants.DAY_IN_MICRO_SECONDS.floatValue();
        float expectedIndex = emissionPerSecond * time_delta.floatValue() / 100;

        assertEquals(expectedIndex, index.floatValue() / ICX.floatValue());

        sm.getBlock().increase(599);
        setTypeWeight(BigInteger.ZERO, new HashMap<>() {{
            put(1, 40L);
            put(2, 60L);
        }}); //1 calls
        /*
          no change in rate till now
          type percentage 25%
          asset percentage 10%
          daily token distribution 1_000_000
         */
        time_delta = getTimestamp().subtract(currentTimestamp).divide(TimeConstants.SECOND);
        expectedIndex = emissionPerSecond * time_delta.floatValue() / 100;
        index = (BigInteger) score.call("getIntegrateIndex", params);
        currentTimestamp = getTimestamp();
        assertEquals(expectedIndex, index.floatValue() / ICX.floatValue(), 0.00001);

        sm.getBlock().increase(599);
        setAssetWeight(BigInteger.ZERO, typeId, new HashMap<>() {{
            put(1, 50L);
            put(3, 10L);
            put(4, 20L);
        }}); //1 calls
        /*
          no change in rate till now
          type percentage 40%
          asset percentage 10%
          daily token distribution 1_000_000
         */

        index = (BigInteger) score.call("getIntegrateIndex", params);
        time_delta = getTimestamp().subtract(currentTimestamp).divide(TimeConstants.SECOND);
        emissionPerSecond = 0.10f * 0.40f * 1_000_000 / TimeConstants.DAY_IN_MICRO_SECONDS.floatValue();
        expectedIndex = expectedIndex + emissionPerSecond * time_delta.floatValue()
                / 100; //integrateIndex -> {BigInteger@3661} "555092592592592592637" //integrateIndex -> {BigInteger@3694} "925636574074074074548"
        currentTimestamp = getTimestamp();

        assertEquals(expectedIndex, index.floatValue() / ICX.floatValue(), 0.00001);


        /*
        200 block ahead of month from start timestamp
         */
        sm.getBlock().increase(1000);
        /*
          type percentage 40%
          asset percentage 50%
          daily token distribution 1_000_000
         */

        time_delta = getTimestamp()
                .subtract(currentTimestamp).divide(TimeConstants.SECOND).subtract(BigInteger.valueOf(402));
        System.out.println("time_delta = "
                + time_delta);
        emissionPerSecond = 0.40f * 0.50f * 1_000_000 / TimeConstants.DAY_IN_MICRO_SECONDS.floatValue();
        expectedIndex = expectedIndex + emissionPerSecond * time_delta.floatValue() / 100;

        index = (BigInteger) score.call("getIntegrateIndex", params);

        /*
          type percentage 40%
          asset percentage 50%
          daily token distribution 400_000 after 1 month
         */
        time_delta = BigInteger.valueOf(402);
        emissionPerSecond = 0.40f * 0.50f * 400_000 / TimeConstants.DAY_IN_MICRO_SECONDS.floatValue();
        expectedIndex = expectedIndex + emissionPerSecond * time_delta.floatValue() / 100;
        assertEquals(expectedIndex, index.floatValue() / ICX.floatValue(), 0.00001);
    }

    private void addAsset(Integer id, String typeId, Address asset) {
        Object[] params = new Object[]{
                typeId, "Asset " + id, asset, BigInteger.ZERO
        };
        score.invoke(owner, "addAsset", params);
    }

    private void setAssetWeight(BigInteger timestamp, String typeId, Map<Integer, Long> map) {
        WeightStruct[] weights = map.entrySet().stream().map(e -> {
            WeightStruct struct = new WeightStruct();
            struct.weight = BigInteger.valueOf(e.getValue()).multiply(ICX).divide(BigInteger.valueOf(100));
            struct.id = AssetWeightDB.ID_PREFIX + typeId + "_" + e.getKey();
            return struct;
        }).toArray(WeightStruct[]::new);

        Object[] params = new Object[]{typeId, weights, timestamp};

        score.invoke(owner, "setAssetWeight", params);
    }

    private void initAssetWeight(BigInteger timestamp, String typeId, Long... values) {
        WeightStruct[] weights = new WeightStruct[values.length];
        IntStream.range(0, values.length)
                .forEach(idx -> {
                    Address asset = Account.newScoreAccount(100 + idx).getAddress();
                    addAsset((idx + 1), typeId, asset);
                    WeightStruct struct = new WeightStruct();
                    struct.weight = BigInteger.valueOf(values[idx]).multiply(ICX).divide(BigInteger.valueOf(100));
                    struct.id = AssetWeightDB.ID_PREFIX + typeId + "_" + (idx + 1);
                    weights[idx] = struct;
                });

        Object[] params = new Object[]{typeId, weights, timestamp};

        score.invoke(owner, "setAssetWeight", params);
    }


    private void setTypeWeight(BigInteger timestamp, Map<Integer, Long> map) {
        WeightStruct[] weights = map.entrySet().stream().map(e -> {
            WeightStruct struct = new WeightStruct();
            struct.weight = BigInteger.valueOf(e.getValue()).multiply(ICX).divide(BigInteger.valueOf(100));
            struct.id = ID_PREFIX + e.getKey();
            return struct;
        }).toArray(WeightStruct[]::new);

        Object[] params = new Object[]{weights, timestamp};

        score.invoke(owner, "setTypeWeight", params);
    }

    private void initTypeWeight(BigInteger timestamp, Long... values) {
        WeightStruct[] weights = new WeightStruct[values.length];
        IntStream.range(0, values.length)
                .forEach(idx -> {
                    addType(owner, "Type " + (idx + 1));
                    WeightStruct struct = new WeightStruct();
                    struct.weight = BigInteger.valueOf(values[idx]).multiply(ICX).divide(BigInteger.valueOf(100));
                    struct.id = ID_PREFIX + (idx + 1);
                    weights[idx] = struct;
                });

        Object[] params = new Object[]{weights, timestamp};

        score.invoke(owner, "setTypeWeight", params);
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }


    private BigInteger getTimestamp() {
        return BigInteger.valueOf(sm.getBlock().getTimestamp());
    }

}
