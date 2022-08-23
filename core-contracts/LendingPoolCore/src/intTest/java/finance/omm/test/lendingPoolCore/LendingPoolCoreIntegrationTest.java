package finance.omm.test.lendingPoolCore;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.pow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.Constant;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.test.lendingPoolCore.config.LendingPoolCoreConfig;
import finance.omm.utils.math.MathUtils;
import foundation.icon.jsonrpc.Address;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import score.UserRevertedException;
import scorex.util.ArrayList;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class LendingPoolCoreIntegrationTest implements ScoreIntegrationTest {
    private static OMMClient ownerClient;
    private static OMMClient alice;

    private static Map<String, Address> addressMap;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new LendingPoolCoreConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
    }

    @Test
    public void name() {
        String name = ownerClient.lendingPoolCore.name();
        assertEquals(name, "Omm Lending Pool Core");
    }

    @Test
    @Order(1)
    public void reserveInitializeChecks() {
        Address sicx = addressMap.get(Contracts.sICX.getKey());

        Map<String, Object> reserveData = getReserveData(sicx);

        Address oicx = addressMap.get(Contracts.oICX.getKey());
        Address dicx = addressMap.get(Contracts.dICX.getKey());

        assertEquals(oicx.toString(), reserveData.get("oTokenAddress"));
        assertEquals(dicx.toString(), reserveData.get("dTokenAddress"));

        assertEquals(BigInteger.ZERO, toBigInt((String) reserveData.get("liquidityRate")));
        assertEquals(BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) reserveData.get("borrowRate")));

        assertEquals(ICX, toBigInt((String) reserveData.get("liquidityCumulativeIndex")));
        assertEquals(ICX, toBigInt((String) reserveData.get("borrowCumulativeIndex")));

        assertEquals(new BigInteger("500000000000000000"), toBigInt((String) reserveData.get("baseLTVasCollateral")));
        assertEquals(new BigInteger("650000000000000000"), toBigInt((String) reserveData.get("liquidationThreshold")));
        assertEquals(new BigInteger("100000000000000000"), toBigInt((String) reserveData.get("liquidationBonus")));

        assertEquals("0x1", reserveData.get("borrowingEnabled"));
        assertEquals("0x1", reserveData.get("usageAsCollateralEnabled"));
        assertEquals("0x0", reserveData.get("isFreezed"));
        assertEquals("0x1", reserveData.get("isActive"));
    }

    @Test
    @Order(1)
    public void getReserves() {
        List<Address> list = new ArrayList<>();
        list.add(addressMap.get(Contracts.IUSDC.getKey()));
        list.add(addressMap.get(Contracts.sICX.getKey()));
        assertEquals(list, getReservesInternal());
    }

    @Test
    @Order(1)
    public void getReserveConstants() {
        Address iusdc = addressMap.get(Contracts.IUSDC.getKey());
        Map<String, Object> configData = ownerClient.lendingPoolCore.getReserveConstants(iusdc);
        assertEquals(iusdc.toString(), configData.get("reserve"));
        assertEquals(BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN),
                toBigInt((String) configData.get("optimalUtilizationRate")));
        assertEquals(BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) configData.get("baseBorrowRate")));
        assertEquals(BigInteger.valueOf(6).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) configData.get("slopeRate1")));
        assertEquals(BigInteger.valueOf(2).multiply(MathUtils.ICX),
                toBigInt((String) configData.get("slopeRate2")));
    }

    private List<score.Address> getReservesInternal() {
        return ownerClient.lendingPoolCore.getReserves();
    }

    private BigInteger toBigInt(String inputString) {
        return new BigInteger(inputString.substring(2), 16);
    }

    protected BigInteger getNormalizedIncome(Address reserve) {
        return ownerClient.lendingPoolCore.getNormalizedIncome(reserve);
    }

    protected BigInteger getNormalizedDebt(Address reserve) {
        return ownerClient.lendingPoolCore.getNormalizedDebt(reserve);
    }

    protected Map<String, Object> getReserveData(Address reserve) {
        return ownerClient.lendingPoolCore.getReserveData(reserve);
    }

    private Map<String, BigInteger> getUserBorrowBalances(Address reserve, String user) {
        return ownerClient.lendingPoolCore.getUserBorrowBalances(reserve, Address.fromString(user));
    }

    private boolean assertBetween(BigInteger start, BigInteger end, BigInteger value) {
        return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
    }

    private byte[] createRepayData() {
        JsonObject internalParameters = new JsonObject();
        JsonObject jsonData = new JsonObject()
                .add("method", "repay")
                .add("params",internalParameters);
        return jsonData.toString().getBytes();
    }

    Map<String, Boolean> STATES = new HashMap<>();

    @DisplayName("Configuration Change Revert Tests")
    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    class ConfigurationChangesTest {

        Address sicx = addressMap.get(Contracts.sICX.getKey());

        @Test
        @Order(1)
        void updateBorrowThreshold() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateBorrowThreshold(sicx, BigInteger.TEN));
        }

        @Test
        @Order(1)
        void updateBaseLTVasCollateral() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateBaseLTVasCollateral(sicx, BigInteger.TEN));
        }

        @Test
        @Order(1)
        void updateLiquidationThreshold() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateLiquidationThreshold(sicx, BigInteger.TEN));
        }

        @Test
        @Order(1)
        void updateLiquidationBonus() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateLiquidationBonus(sicx, BigInteger.TEN));
        }

        @Test
        @Order(1)
        void updateBorrowingEnabled() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateBorrowingEnabled(sicx, true));
        }

        @Test
        @Order(1)
        void updateUsageAsCollateralEnabled() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateUsageAsCollateralEnabled(sicx, true));
        }

        @Test
        @Order(1)
        void updateIsFreezed() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateIsFreezed(sicx, true));
        }

        @Test
        @Order(1)
        void updateIsActive() {
            assertThrows(UserRevertedException.class, () ->
                    ownerClient.lendingPoolCore.updateIsActive(sicx, true));
        }
    }

    @DisplayName("General Transactions Test")
    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    class DepositTest {
        BigInteger HUNDRED = BigInteger.valueOf(100).multiply(ICX);
        Address sicx = addressMap.get(Contracts.sICX.getKey());

        protected void depositICX() {
            ((LendingPoolScoreClient)alice.lendingPool).
                    deposit(HUNDRED,HUNDRED);
        }
        @Test
        @Order(1)
        @DisplayName("Deposit: ICX Deposit Alice")
        void icx_deposit() {

            // txn
            depositICX();

            // verification
            Map<String, Object> sicxReserveData = getReserveData(sicx);
            assertEquals(HUNDRED, toBigInt((String) sicxReserveData.get("totalLiquidity")));
            assertEquals(HUNDRED, toBigInt((String) sicxReserveData.get("availableLiquidity")));
            assertEquals(BigInteger.ZERO, toBigInt((String) sicxReserveData.get("totalBorrows")));
            assertEquals(BigInteger.valueOf(90).multiply(ICX), toBigInt((String) sicxReserveData.get("availableBorrows")));


            Map<String, BigInteger> aliceSicxReserveData = ownerClient.lendingPoolCore.getUserReserveData(sicx, alice.getAddress());
            assertEquals(BigInteger.ZERO, aliceSicxReserveData.get("originationFee"));

            // normalized income = 1
            assertEquals(ICX, getNormalizedIncome(sicx));
            // normalized debt = 1
            assertEquals(ICX, getNormalizedDebt(sicx));

            STATES.put("icx_deposit_alice_1", true);
        }

        @Test
        @Order(2)
        @DisplayName("Borrow: ICX Borrow Alice")
        void icx_borrow() throws InterruptedException {
            if (STATES.getOrDefault("icx_borrow_alice_1", false)) {
                return;
            }
            BigInteger FORTY = BigInteger.valueOf(40).multiply(ICX);
            // txn
            Thread.sleep(2000);
            alice.lendingPool.borrow(sicx, FORTY);
            // verification
            Map<String, Object> sicxReserveData = getReserveData(sicx);
            assertEquals(FORTY, toBigInt((String) sicxReserveData.get("totalBorrows")));
            Map<String, BigInteger> aliceSicxReserveData = ownerClient.lendingPoolCore.getUserReserveData(sicx, alice.getAddress());
            assertEquals(exaMultiply(FORTY, Constant.LOAN_ORIGINATION_FEE_PERCENTAGE), aliceSicxReserveData.get("originationFee"));

            // for first borrow
            assertEquals(ICX, getNormalizedIncome(sicx));
            assertEquals(ICX, getNormalizedDebt(sicx));
            STATES.put("icx_borrow_alice_1", true);
        }

        @Test
        @Order(3)
        @DisplayName("Deposit Again: ICX Deposit Alice")
        void icx_deposit2() throws InterruptedException {
            if (STATES.getOrDefault("icx_deposit_alice_2", false)) {
                return;
            }
            // 100 ICX has been deposited, 40 ICX has been borrowed before
            Thread.sleep(3000);
            depositICX();

            Map<String, Object> sicxReserveData = getReserveData(sicx);
            // totalBorrows > 0, indexes update
            BigInteger liquidityCumulativeIndex = toBigInt((String) sicxReserveData.get("liquidityCumulativeIndex"));
            BigInteger borrowCumulativeIndex = toBigInt((String) sicxReserveData.get("borrowCumulativeIndex"));
            assertTrue(liquidityCumulativeIndex.compareTo(ICX) > 0);
            assertTrue(borrowCumulativeIndex.compareTo(ICX) > 0);

            assertEquals(HUNDRED.multiply(BigInteger.TWO), toBigInt((String) sicxReserveData.get("totalLiquidity")));
            assertEquals(BigInteger.valueOf(160).multiply(ICX), toBigInt((String) sicxReserveData.get("availableLiquidity")));

            assertTrue(getNormalizedDebt(sicx).compareTo(ICX) > 0);
            assertTrue(getNormalizedIncome(sicx).compareTo(ICX) > 0);

            STATES.put("icx_deposit_alice_2", true);
        }

        @Test
        @Order(4)
        @DisplayName("Redeem: ICX Redeem Alice")
        void icx_redeem() throws InterruptedException {
            if (STATES.getOrDefault("icx_redeem_alice_1", false)) {
                return;
            }
            Map<String, Object> sicxReserveDataBefore = getReserveData(sicx);
            Thread.sleep(2000);

            BigInteger TWENTY = BigInteger.valueOf(20).multiply(ICX);
            Address oICX = addressMap.get(Contracts.oICX.getKey());
            alice.lendingPool.redeem(oICX, TWENTY, false);
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);

            assertEquals(BigInteger.valueOf(180).multiply(ICX), toBigInt((String) sicxReserveDataAfter.get("totalLiquidity")));
            assertEquals(BigInteger.valueOf(140).multiply(ICX), toBigInt((String) sicxReserveDataAfter.get("availableLiquidity")));
            assertEquals(BigInteger.valueOf(40).multiply(ICX), toBigInt((String) sicxReserveDataAfter.get("totalBorrows")));
            assertEquals(BigInteger.valueOf(140 * 9 / 10).multiply(ICX), toBigInt((String) sicxReserveDataAfter.get("availableBorrows")));

            BigInteger liquidityRateBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityRate"));
            BigInteger liquidityRateAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityRate"));
            assertTrue(liquidityRateAfter.compareTo(liquidityRateBefore) > 0);

            BigInteger borrowRateBefore = toBigInt((String) sicxReserveDataBefore.get("borrowRate"));
            BigInteger borrowRateAfter = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertTrue(borrowRateAfter.compareTo(borrowRateBefore) > 0);

            BigInteger liquidityCumulativeIndexBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityCumulativeIndex"));
            BigInteger liquidityCumulativeIndexAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityCumulativeIndex"));
            assertTrue(liquidityCumulativeIndexAfter.compareTo(liquidityCumulativeIndexBefore) > 0);

            BigInteger borrowCumulativeIndexBefore = toBigInt((String) sicxReserveDataBefore.get("borrowCumulativeIndex"));
            BigInteger borrowCumulativeIndexAfter = toBigInt((String) sicxReserveDataAfter.get("borrowCumulativeIndex"));
            assertTrue(borrowCumulativeIndexAfter.compareTo(borrowCumulativeIndexBefore) > 0);

            STATES.put("icx_redeem_alice_1", true);
        }

        @Test
        @Order(5)
        @DisplayName("Repay: ICX Repay Alice")
        void icx_repay() throws InterruptedException {
            if (STATES.getOrDefault("icx_repay_alice_1", false)) {
                return;
            }
            Map<String, Object> sicxReserveDataBefore = getReserveData(sicx);
            Thread.sleep(2000);

            Address lendingPool = addressMap.get(Contracts.LENDING_POOL.getKey());
            BigInteger val = BigInteger.valueOf(40).multiply(ICX);
            alice.sICX.transfer(lendingPool, val, createRepayData());
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);

            BigInteger liquidityRateBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityRate"));
            BigInteger liquidityRateAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityRate"));
            assertTrue(liquidityRateBefore.compareTo(liquidityRateAfter) > 0);

            BigInteger borrowRateBefore = toBigInt((String) sicxReserveDataBefore.get("borrowRate"));
            BigInteger borrowRateAfter = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertTrue(borrowRateBefore.compareTo(borrowRateAfter) > 0);

            BigInteger liquidityCumulativeIndexBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityCumulativeIndex"));
            BigInteger liquidityCumulativeIndexAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityCumulativeIndex"));
            assertTrue(liquidityCumulativeIndexAfter.compareTo(liquidityCumulativeIndexBefore) > 0);

            BigInteger borrowCumulativeIndexBefore = toBigInt((String) sicxReserveDataBefore.get("borrowCumulativeIndex"));
            BigInteger borrowCumulativeIndexAfter = toBigInt((String) sicxReserveDataAfter.get("borrowCumulativeIndex"));
            assertTrue(borrowCumulativeIndexAfter.compareTo(borrowCumulativeIndexBefore) > 0);

            // total liquidity
            BigInteger totalLiquidityBefore = toBigInt((String) sicxReserveDataBefore.get("totalLiquidity"));
            BigInteger totalLiquidityAfter = toBigInt((String) sicxReserveDataAfter.get("totalLiquidity"));

            // interest added after repay
            assertTrue(totalLiquidityAfter.compareTo(totalLiquidityBefore) > 0);

            // available liquidity
            BigInteger availableLiquidityBefore = toBigInt((String) sicxReserveDataBefore.get("availableLiquidity"));
            BigInteger availableLiquidityAfter = toBigInt((String) sicxReserveDataAfter.get("availableLiquidity"));
            assertTrue(availableLiquidityBefore.add(val).compareTo(availableLiquidityAfter) > 0);

            // total borrows
            BigInteger totalBorrowsAfter = toBigInt((String) sicxReserveDataAfter.get("totalBorrows"));
            // borrow still pending because of accumulated interest
            assertTrue(totalBorrowsAfter.compareTo(BigInteger.ZERO) > 0);

            // 0.1% of  goes to fee provider
            BigInteger amt = exaMultiply(val, ownerClient.feeProvider.getLoanOriginationFeePercentage());
            assertTrue(ownerClient.sICX.balanceOf(addressMap.get(Contracts.FEE_PROVIDER.getKey())).compareTo(amt) > 0);


            Map<String, BigInteger> userBorrowBalance = getUserBorrowBalances(sicx,alice.getAddress().toString());
            // loan of 40 ICX, repaid 40 ICX
            // ~0.1% as origination fee
            // a little accrued interest
            // pending borrow should be a bit > 0.1% of 40ICX
            BigInteger a = val.divide(BigInteger.valueOf(1000));
            assertTrue(userBorrowBalance.get("compoundedBorrowBalance").compareTo(a) > 0);
            BigInteger pow13 = pow(BigInteger.TEN, 13);
            // assertAlmostEquals
            assertEquals(userBorrowBalance.get("compoundedBorrowBalance").longValue(), a.longValue(), pow13.longValue());

            STATES.put("icx_repay_alice_1", true);
        }

        @Test
        @Order(6)
        @DisplayName("Repay Again: ICX Repay Alice")
        void icx_repay_2 () throws InterruptedException {
            if (STATES.getOrDefault("icx_repay_alice_2", false)) {
                return;
            }

            Map<String, Object> sicxReserveDataBefore = getReserveData(sicx);
            Thread.sleep(2000);

            // pending borrow balance is ~0.04
            // repay 20 sICX
            // should refund extra
            Address lendingPool = addressMap.get(Contracts.LENDING_POOL.getKey());
            BigInteger val = BigInteger.valueOf(20).multiply(ICX);
            alice.sICX.transfer(lendingPool, val, createRepayData());
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);

            // indexes should increase
            BigInteger borrowRateBefore = toBigInt((String) sicxReserveDataBefore.get("borrowRate"));
            BigInteger borrowRateAfter = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertTrue(borrowRateBefore.compareTo(borrowRateAfter) > 0);

            BigInteger liquidityCumulativeIndexBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityCumulativeIndex"));
            BigInteger liquidityCumulativeIndexAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityCumulativeIndex"));
            assertTrue(liquidityCumulativeIndexAfter.compareTo(liquidityCumulativeIndexBefore) > 0);

            // after all loan is repaid, liquidity rate = 0
            BigInteger liquidityRate = toBigInt((String) sicxReserveDataAfter.get("liquidityRate"));
            assertEquals(BigInteger.ZERO, liquidityRate);
            // borrow rate = base borrow rate
            BigInteger borrowRate = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertEquals(ICX.multiply(BigInteger.TWO).divide(BigInteger.valueOf(100)), borrowRate);

            BigInteger availableLiquidityBefore = toBigInt((String) sicxReserveDataBefore.get("availableLiquidity"));
            BigInteger availableLiquidityAfter = toBigInt((String) sicxReserveDataAfter.get("availableLiquidity"));
            assertTrue(availableLiquidityAfter.compareTo(availableLiquidityBefore) > 0);

            Map<String, BigInteger> userBorrowBalanceAfter = getUserBorrowBalances(sicx,alice.getAddress().toString());

            assertEquals(BigInteger.ZERO, userBorrowBalanceAfter.get("borrowBalanceIncrease"));
            assertEquals(BigInteger.ZERO, userBorrowBalanceAfter.get("compoundedBorrowBalance"));
            assertEquals(BigInteger.ZERO, userBorrowBalanceAfter.get("principalBorrowBalance"));

            // repay ~0.04 sICX
            // aliceBalance > 19.95
            BigInteger reqd = BigInteger.valueOf(1995).multiply(ICX).divide(BigInteger.valueOf(100));
            assertTrue(ownerClient.sICX.balanceOf(alice.getAddress()).compareTo(reqd) > 0);

            STATES.put("icx_repay_alice_2", true);
        }

        @Test
        @Order(7)
        @DisplayName("Redeem Again: ICX Redeem All")
        void icx_redeem2() throws InterruptedException {
            if (STATES.getOrDefault("icx_redeem_alice_2", false)) {
                return;
            }
            Thread.sleep(2000);

            BigInteger ALL = BigInteger.valueOf(-1);
            Address oICX = addressMap.get(Contracts.oICX.getKey());
            alice.lendingPool.redeem(oICX, ALL, false);
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);
            BigInteger balanceAfter = ownerClient.sICX.balanceOf(alice.getAddress());
            assertTrue(
                    assertBetween(
                            BigInteger.valueOf(199).multiply(ICX),
                            BigInteger.valueOf(200).multiply(ICX),
                            balanceAfter
                    )
            );
            // dust amount should be pending for available, total liquidity
            BigInteger dustValue = BigInteger.valueOf((long) 1e5);
            // available liquidity == total liquidity
            BigInteger availableLiquidityAfter = toBigInt((String) sicxReserveDataAfter.get("availableLiquidity"));
            BigInteger totalLiquidityAfter = toBigInt((String) sicxReserveDataAfter.get("totalLiquidity"));
            assertEquals(availableLiquidityAfter, totalLiquidityAfter);
            assertTrue(dustValue.compareTo(totalLiquidityAfter) > 0);
            STATES.put("icx_redeem_alice_2", true);
        }
    }
}
