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
    private static OMMClient bob;
    private static OMMClient clint;

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
        bob = omm.newClient(BigInteger.TEN.pow(24));
        clint = omm.newClient(BigInteger.TEN.pow(24));

        // approve owner as issuer to iusdc
        ownerClient.iUSDC.addIssuer(ownerClient.getAddress());
        ownerClient.iUSDC.approve(ownerClient.getAddress(), BigInteger.TEN.multiply(ICX));
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

    private byte[] createByteData(String method) {
        JsonObject internalParameters = new JsonObject();
        JsonObject jsonData = new JsonObject()
                .add("method", method)
                .add("params",internalParameters);
        return jsonData.toString().getBytes();
    }

    private byte[] createLiquidationByteData(Address collateral, Address reserve, String user) {
        JsonObject internalParameters = new JsonObject()
                .add("_collateral", String.valueOf(collateral))
                .add("_reserve", String.valueOf(reserve))
                .add("_user", user);

        JsonObject jsonData = new JsonObject()
                .add("method", "liquidationCall")
                .add("params",internalParameters);
        return jsonData.toString().getBytes();
    }

    private void verifyLiquidityIndexesIncreased(Map<String, Object> before, Map<String, Object> after) {
        BigInteger liquidityCumulativeIndexBefore = toBigInt((String) before.get("liquidityCumulativeIndex"));
        BigInteger liquidityCumulativeIndexAfter = toBigInt((String) after.get("liquidityCumulativeIndex"));
        assertTrue(liquidityCumulativeIndexAfter.compareTo(liquidityCumulativeIndexBefore) > 0);
    }


    private void verifyBorrowIndexesIncreased(Map<String, Object> before, Map<String, Object> after) {
        BigInteger borrowCumulativeIndexBefore = toBigInt((String) before.get("borrowCumulativeIndex"));
        BigInteger borrowCumulativeIndexAfter = toBigInt((String) after.get("borrowCumulativeIndex"));
        assertTrue(borrowCumulativeIndexAfter.compareTo(borrowCumulativeIndexBefore) > 0);
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
        BigInteger THOUSAND = BigInteger.valueOf(1000).multiply(ICX);
        BigInteger POW12 = BigInteger.TEN.pow(12);
        BigInteger POW6 = BigInteger.TEN.pow(6);
        Address collateral = addressMap.get(Contracts.sICX.getKey());
        Address reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address lendingPool = addressMap.get(Contracts.LENDING_POOL.getKey());

        protected void depositICXBob() {
            ((LendingPoolScoreClient)bob.lendingPool).
                    deposit(THOUSAND,THOUSAND);
        }

        protected void getSicxFromIcx() {
            Address sicx = addressMap.get(Contracts.sICX.getKey());
            ((LendingPoolScoreClient)ownerClient.lendingPool).
                    deposit(THOUSAND,THOUSAND);
            ownerClient.lendingPool.redeem(sicx, THOUSAND, false);
        }

        void mintIUSDC(score.Address address) {
            ownerClient.iUSDC.mintTo(address, ICX);
        }

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
            Address sICX = addressMap.get(Contracts.sICX.getKey());
            alice.lendingPool.redeem(sICX, TWENTY, false);
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

            verifyLiquidityIndexesIncreased(sicxReserveDataBefore, sicxReserveDataAfter);
            verifyBorrowIndexesIncreased(sicxReserveDataBefore, sicxReserveDataAfter);

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
            alice.sICX.transfer(lendingPool, val, createByteData("repay"));
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);

            BigInteger liquidityRateBefore = toBigInt((String) sicxReserveDataBefore.get("liquidityRate"));
            BigInteger liquidityRateAfter = toBigInt((String) sicxReserveDataAfter.get("liquidityRate"));
            assertTrue(liquidityRateBefore.compareTo(liquidityRateAfter) > 0);

            BigInteger borrowRateBefore = toBigInt((String) sicxReserveDataBefore.get("borrowRate"));
            BigInteger borrowRateAfter = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertTrue(borrowRateBefore.compareTo(borrowRateAfter) > 0);

            verifyLiquidityIndexesIncreased(sicxReserveDataBefore, sicxReserveDataAfter);
            verifyBorrowIndexesIncreased(sicxReserveDataBefore, sicxReserveDataAfter);

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
            alice.sICX.transfer(lendingPool, val, createByteData("repay"));
            Map<String, Object> sicxReserveDataAfter = getReserveData(sicx);

            // indexes should increase
            BigInteger borrowRateBefore = toBigInt((String) sicxReserveDataBefore.get("borrowRate"));
            BigInteger borrowRateAfter = toBigInt((String) sicxReserveDataAfter.get("borrowRate"));
            assertTrue(borrowRateBefore.compareTo(borrowRateAfter) > 0);

            verifyLiquidityIndexesIncreased(sicxReserveDataBefore, sicxReserveDataAfter);

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
            Address sicx = addressMap.get(Contracts.sICX.getKey());
            alice.lendingPool.redeem(sicx, ALL, false);
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

        @Test
        @DisplayName("Liquidation 1: Single Collateral, Single Borrow, Covers double Liquidation")
        @Order(8)
        void liquidation1() {

            // set price of ICX to 1$
            ownerClient.dummyPriceOracle.set_reference_data("ICX", ICX);
            ownerClient.lendingPool.setLiquidationStatus(true);

            // deposit 1000 ICX
            depositICXBob();

            // owner deposits 1000 USDC
            mintIUSDC(ownerClient.getAddress());
            ownerClient.iUSDC.transfer(lendingPool,
                    BigInteger.valueOf(1000_000_000), createByteData("deposit"));

            // owner get sicx from icx
            getSicxFromIcx();

            // borrow 500 USDC
            bob.lendingPool.borrow(reserve, BigInteger.valueOf(500_000_000));

            // price of ICX falls to 0.7$
            ownerClient.dummyPriceOracle.set_reference_data("ICX", ICX.multiply(BigInteger.valueOf(7)).divide(BigInteger.TEN));

            // before liquidation data
            Map<String, Object> reserveDataBefore = getReserveData(reserve);
            Map<String, Object> collateralDataBefore = getReserveData(collateral);
            Map<String, Object> lqdnDataBefore = ownerClient.lendingPoolDataProvider.getUserLiquidationData(bob.getAddress());
            BigInteger reserveBalanceBefore = ownerClient.dIUSDC.balanceOf(bob.getAddress());
            BigInteger collateralBalanceBefore = ownerClient.oICX.balanceOf(bob.getAddress());
            BigInteger liquidatorSicxBalanceBefore = ownerClient.sICX.balanceOf(ownerClient.getAddress());
            BigInteger liquidatorIUSDCBalanceBefore = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());
            Address feeProvider = addressMap.get(Contracts.FEE_PROVIDER.getKey());
            BigInteger feeProvidersICXBalanceBefore = ownerClient.sICX.balanceOf(feeProvider);
            BigInteger feeProviderUSDCBalanceBefore = ownerClient.iUSDC.balanceOf(feeProvider);

            // liquidation
            BigInteger amtToLiquidate  = toBigInt((String) lqdnDataBefore.get("badDebt")).divide(POW12);
            ownerClient.iUSDC.transfer(lendingPool, amtToLiquidate,
                    createLiquidationByteData(collateral, reserve, bob.getAddress().toString()));

            // data after liquidation
            Map<String, Object> reserveDataAfter = getReserveData(reserve);
            Map<String, Object> collateralDataAfter = getReserveData(collateral);
            Map<String, Object> lqdnDataAfter = ownerClient.lendingPoolDataProvider.getUserLiquidationData(bob.getAddress());
            BigInteger reserveBalanceAfter = ownerClient.dIUSDC.balanceOf(bob.getAddress());
            BigInteger collateralBalanceAfter = ownerClient.oICX.balanceOf(bob.getAddress());
            BigInteger liquidatorSicxBalanceAfter = ownerClient.sICX.balanceOf(ownerClient.getAddress());
            BigInteger liquidatorIUSDCBalanceAfter = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());
            BigInteger feeProvidersICXBalanceAfter = ownerClient.sICX.balanceOf(feeProvider);
            BigInteger feeProviderUSDCBalanceAfter = ownerClient.iUSDC.balanceOf(feeProvider);

            // assert conditions
            BigInteger feeProviderDiff = feeProvidersICXBalanceAfter.subtract(feeProvidersICXBalanceBefore); // sicx
            BigInteger lqdnBonus = liquidatorSicxBalanceAfter.subtract(liquidatorSicxBalanceBefore);
            assertEquals(collateralBalanceBefore,
                    collateralBalanceAfter.add(feeProviderDiff).add(lqdnBonus));

            assertEquals(reserveBalanceAfter, reserveBalanceBefore.subtract(amtToLiquidate));

            assertEquals(lqdnBonus.divide(ICX).longValue(),
                    amtToLiquidate.divide(POW6).longValue()*11/7, 1);
            assertEquals(feeProviderUSDCBalanceAfter, feeProviderUSDCBalanceBefore);
            assertEquals(liquidatorIUSDCBalanceAfter, liquidatorIUSDCBalanceBefore.subtract(amtToLiquidate));

            // reserve data
            float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
            assertEquals(
                    toBigInt((String) reserveDataAfter.get("totalBorrows")).floatValue(),
                    toBigInt((String) reserveDataBefore.get("totalBorrows")).subtract(amtToLiquidate).floatValue(),
                    delta
            );

            assertEquals(
                    toBigInt((String) reserveDataAfter.get("availableLiquidity")),
                    toBigInt((String) reserveDataBefore.get("availableLiquidity")).add(amtToLiquidate)
            );

            assertEquals(
                    toBigInt((String) reserveDataAfter.get("totalLiquidity")),
                    toBigInt((String) reserveDataBefore.get("totalLiquidity"))
            );

            assertTrue(
                    toBigInt((String) reserveDataBefore.get("liquidityRate")).compareTo(
                            toBigInt((String) reserveDataAfter.get("liquidityRate"))) > 0
            );

            assertTrue(
                    toBigInt((String) reserveDataBefore.get("borrowRate")).compareTo(
                            toBigInt((String) reserveDataAfter.get("borrowRate"))) > 0
            );

            verifyLiquidityIndexesIncreased(reserveDataBefore, reserveDataAfter);
            verifyBorrowIndexesIncreased(reserveDataBefore, reserveDataAfter);

            // collateral data

            BigInteger beforeTotalLiquidity = toBigInt((String) collateralDataBefore.get("totalLiquidity"));
            BigInteger afterTotalLiquidity = toBigInt((String) collateralDataAfter.get("totalLiquidity"));
            assertEquals(
                    beforeTotalLiquidity.divide(ICX).longValue(),
                    afterTotalLiquidity.divide(ICX).longValue()+
                            feeProviderDiff.longValue()/(1e18)+
                    amtToLiquidate.divide(POW6).longValue() * 11 / 7,
                    2
            );

            BigInteger beforeAvailableLiquidity = toBigInt((String) collateralDataBefore.get("availableLiquidity"));
            BigInteger afterAvailableLiquidity = toBigInt((String) collateralDataAfter.get("availableLiquidity"));
            assertEquals(
                    beforeAvailableLiquidity.divide(ICX).longValue(),
                    afterAvailableLiquidity.divide(ICX).longValue()+
                            feeProviderDiff.longValue()/(1e18)+
                            amtToLiquidate.divide(POW6).longValue() * 11 / 7,
                    2
            );

            // indexes for collateral should remain same, though collateral goes from borrower's data
            // because icx borrows is cleared, and indexes are not updated for this case.

            BigInteger liquidityCumulativeIndexBefore = toBigInt((String) collateralDataBefore.get("liquidityCumulativeIndex"));
            BigInteger liquidityCumulativeIndexAfter = toBigInt((String) collateralDataAfter.get("liquidityCumulativeIndex"));
            assertEquals(liquidityCumulativeIndexBefore, liquidityCumulativeIndexAfter);

            BigInteger borrowCumulativeIndexBefore = toBigInt((String) collateralDataBefore.get("borrowCumulativeIndex"));
            BigInteger borrowCumulativeIndexAfter = toBigInt((String) collateralDataAfter.get("borrowCumulativeIndex"));
            assertEquals(borrowCumulativeIndexBefore, borrowCumulativeIndexAfter);

            assertEquals(
                    toBigInt((String) collateralDataBefore.get("totalBorrows")),
                    toBigInt((String) collateralDataAfter.get("totalBorrows"))
            );

            assertEquals(
                    toBigInt((String) collateralDataBefore.get("liquidityRate")),
                    toBigInt((String) collateralDataAfter.get("liquidityRate"))
            );

            assertEquals(
                    toBigInt((String) collateralDataBefore.get("borrowRate")),
                    toBigInt((String) collateralDataAfter.get("borrowRate"))
            );

            /*
             * CHECK IF BOB IS STILL UNDER LIQUIDATION
             * 1000 ICX -> $ 1000
             * 500 USD -> $500
             * ICX price decreases
             * 1000 ICX -> $ 700
             * 500/700 = 0.71 > 0.65, so under liquidation
             * pay back $ 150.25, 10% bonus
             * 236.107 ICX back to liquidator
             * ICX remaining: 1000-236.107 = 763.893 = $534.7251
             * Borrow pending: 500 - 150.25 = $ 349.75
             * hf : 349.75/534.7251 = 0.6549 > 0.65
             * so, user still under liquidation
             */

            Map<String, Object> bobAccountData = ownerClient.lendingPoolDataProvider.getUserAccountData(bob.getAddress());
            BigInteger healthFactor = toBigInt( (String) bobAccountData.get("healthFactor"));

            assertEquals(
                    healthFactor.longValue()/1e18,
                    0.99,
                    0.2
            );

            assertEquals(
                    (String) bobAccountData.get("healthFactorBelowThreshold"),
                    "0x1"
            );

            BigInteger newBadDebt = toBigInt((String) lqdnDataAfter.get("badDebt")); // 82.6375

            assertTrue(newBadDebt.compareTo(BigInteger.ZERO) > 0);

            // over Bad Debt Amount
            // check if extra is refunded back to user

            BigInteger newAmtToLiquidate = BigInteger.valueOf(100).multiply(POW6);
            liquidatorIUSDCBalanceBefore = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());

            // liquidation
            ownerClient.iUSDC.transfer(lendingPool, newAmtToLiquidate,
                    createLiquidationByteData(collateral, reserve, bob.getAddress().toString()));

            liquidatorIUSDCBalanceAfter = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());

            // only bad debt amount goes for liquidation
            // 100-82.6375 comes back to user
            float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
            assertEquals(liquidatorIUSDCBalanceAfter.floatValue(),
                    liquidatorIUSDCBalanceBefore.subtract(newBadDebt.divide(POW12)).floatValue(),delta);
            lqdnDataAfter = ownerClient.lendingPoolDataProvider.getUserLiquidationData(bob.getAddress());

            /*
             * Collateral Left: 633.32 ICX, $ 443.32
             * Borrows Left: $ 267.1125
             * hf: 267.1125 / 443.32 = 0.60 > 0.65
             * no longer under liquidation
             */
            assertEquals(toBigInt((String) lqdnDataAfter.get("badDebt")), BigInteger.ZERO);

            bobAccountData = ownerClient.lendingPoolDataProvider.getUserAccountData(bob.getAddress());
            assertTrue(toBigInt((String) bobAccountData.get("healthFactor")).compareTo(ICX) > 0);
            assertEquals((String) bobAccountData.get("healthFactorBelowThreshold"),"0x0");
        }

        @Order(9)
        void borrowOnAllReserves() {
            // after this, till borrow is cleared, indexes should update for both reserve and collateral on liquidation
            ownerClient.lendingPool.borrow(sicx, HUNDRED);
            ownerClient.lendingPool.borrow(reserve, BigInteger.valueOf(100).multiply(POW6));
        }

        @Test
        @Order(10)
        @DisplayName("Liquidation 2: Multi collateral multi borrow")
        void liquidation2() {
            // transaction another client :> clint

            // set price of ICX to 1$
            ownerClient.dummyPriceOracle.set_reference_data("ICX", ICX);
            ownerClient.lendingPool.setLiquidationStatus(true);

            // clint deposits $ 800
            mintIUSDC(clint.getAddress());
            clint.iUSDC.transfer(lendingPool,
                    BigInteger.valueOf(800_000_000), createByteData("deposit"));

            // clint deposits 200 ICX
            BigInteger TWO_HUNDRED = BigInteger.valueOf(200).multiply(ICX);
            ((LendingPoolScoreClient)clint.lendingPool).
                    deposit(TWO_HUNDRED,TWO_HUNDRED);

            // clint borrows $ 100
            clint.lendingPool.borrow(reserve, BigInteger.valueOf(100_000_000));
            clint.lendingPool.borrow(collateral, BigInteger.valueOf(395).multiply(ICX));

            // set ICX price to $ 1.7
            ownerClient.dummyPriceOracle.set_reference_data("ICX",
                    ICX.multiply(BigInteger.valueOf(17)).divide(BigInteger.valueOf(10)));

            // User has sICX already, uncomment if needed
            // getSicxFromIcx();

            // owner liquidates clint
            Map<String, Object> lqdnDataBefore = ownerClient.lendingPoolDataProvider.
                    getUserLiquidationData(clint.getAddress());
            Map<String, Object> reserveDataBefore = getReserveData(reserve);
            Map<String, Object> collateralDataBefore = getReserveData(collateral);
            BigInteger reserveBalanceBefore = ownerClient.dICX.balanceOf(clint.getAddress());
            BigInteger collateralBalanceBefore = ownerClient.oIUSDC.balanceOf(clint.getAddress());
            BigInteger liquidatorIUSDCBalanceBefore = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());
            Address feeProvider = addressMap.get(Contracts.FEE_PROVIDER.getKey());
            BigInteger feeProviderUSDCBalanceBefore = ownerClient.iUSDC.balanceOf(feeProvider);

            BigInteger amtToLiquidate = toBigInt((String) lqdnDataBefore.get("badDebt"));

            // usdc as collateral, sicx as reserve for this case
            // usdc borrows should not change before and after liquidation for clint
            // amtToLiquidate * 1.1 to owner
            // indexes of both reserve should update

            Address iusdc = addressMap.get(Contracts.IUSDC.getKey());
            ownerClient.sICX.transfer(lendingPool, amtToLiquidate,
                    createLiquidationByteData(iusdc, sicx, clint.getAddress().toString()));

            Map<String, Object> lqdnDataAfter = ownerClient.lendingPoolDataProvider.
                    getUserLiquidationData(clint.getAddress());
            Map<String, Object> reserveDataAfter = getReserveData(reserve);
            Map<String, Object> collateralDataAfter = getReserveData(collateral);
            BigInteger reserveBalanceAfter = ownerClient.dICX.balanceOf(clint.getAddress());
            BigInteger collateralBalanceAfter = ownerClient.oIUSDC.balanceOf(clint.getAddress());
            BigInteger liquidatorIUSDCBalanceAfter = ownerClient.iUSDC.balanceOf(ownerClient.getAddress());
            BigInteger feeProviderUSDCBalanceAfter = ownerClient.iUSDC.balanceOf(feeProvider);

            // user balance checks
            BigInteger feeProviderDiff = feeProviderUSDCBalanceAfter.subtract(feeProviderUSDCBalanceBefore);
            BigInteger lqdnBonus = liquidatorIUSDCBalanceAfter.subtract(liquidatorIUSDCBalanceBefore);

            assertEquals((collateralBalanceBefore.subtract(collateralBalanceAfter)).longValue()/1e6,
                    (feeProviderDiff.add(
                            amtToLiquidate.multiply(BigInteger.valueOf(11)).divide(BigInteger.valueOf(10))).divide(POW12)).longValue()/1e6,
                    1
            );
            assertTrue(reserveBalanceBefore.compareTo(reserveBalanceAfter) > 0);
            assertEquals((reserveBalanceBefore.subtract(reserveBalanceAfter)).longValue(),
                    (amtToLiquidate.multiply(BigInteger.valueOf(10)).divide(BigInteger.valueOf(17))).longValue(),
                    1e18
            );
            assertEquals(lqdnBonus.longValue()/1e6,
                    amtToLiquidate.divide(POW12).longValue()*1.1/1e6,
                    0.1);

            assertTrue(feeProviderUSDCBalanceAfter.compareTo(feeProviderUSDCBalanceBefore) > 0);
            verifyBorrowIndexesIncreased(reserveDataBefore, reserveDataAfter);
            verifyLiquidityIndexesIncreased(reserveDataBefore, reserveDataAfter);
            verifyBorrowIndexesIncreased(collateralDataBefore, collateralDataAfter);
            verifyLiquidityIndexesIncreased(collateralDataBefore, collateralDataAfter);

            /*
             * Deposit 800$
             * Deposit 200 ICX -> 200$
             * Borrow 100$
             * Borrow 395 ICX -> 395 $
             * Update ICX price:
             *      395 ICX -> 671.5 $
             *      200 ICX -> 340 $
             * ltv: (671.5+100)/(340+800) = 0.67 > 0.65, so liquidation
             * Liquidation successful
             * New Deposit: 340 + 800 - 222.074
             * New Borrow: 276.2 * 1.7 + 100
             * ltv: 0.62 < 0.65
             */

            assertEquals(toBigInt((String) lqdnDataAfter.get("badDebt")), BigInteger.ZERO);

            Map<String, Object> clintAccountData = ownerClient.lendingPoolDataProvider.getUserAccountData(clint.getAddress());
            // 0.62 > 0.5, so no more borrow available
            assertEquals(toBigInt((String) clintAccountData.get("availableBorrowsUSD")), BigInteger.ZERO);

            assertEquals(
                    toBigInt((String) clintAccountData.get("totalLiquidityBalanceUSD")).divide(ICX).longValue(),
                    340 + 800 - 222.074,
                    1);

            assertEquals(toBigInt((String) clintAccountData.get("totalBorrowBalanceUSD")).divide(ICX).longValue(),
                    276.2 * 1.7 + 100,
                    1);

            assertTrue(toBigInt((String) clintAccountData.get("healthFactor")).compareTo(ICX) > 0);
            assertEquals((String) clintAccountData.get("healthFactorBelowThreshold"),"0x0");


            // iusdc collateral taken away, iusdc scarce in system
            // borrow rate >
            // liquidity rate >
            assertTrue(
                    toBigInt((String) reserveDataAfter.get("liquidityRate")).compareTo(
                            toBigInt((String) reserveDataBefore.get("liquidityRate"))) > 0
            );

            assertTrue(
                    toBigInt((String) reserveDataAfter.get("borrowRate")).compareTo(
                            toBigInt((String) reserveDataBefore.get("borrowRate"))) > 0
            );

            assertEquals(
                    (amtToLiquidate.divide(ICX).multiply(BigInteger.valueOf(11)).divide(BigInteger.TEN)).longValue(),
                    (toBigInt((String) reserveDataBefore.get("availableLiquidity")).subtract(
                    toBigInt((String) reserveDataAfter.get("availableLiquidity")))).longValue()/1e6,
                    2
            );

            // icx loan repaid, icx surplus in system
            // borrow rate <
            // liquidity rate <
            assertTrue(
                    toBigInt((String) collateralDataBefore.get("liquidityRate")).compareTo(
                            toBigInt((String) collateralDataAfter.get("liquidityRate"))) > 0
            );

            assertTrue(
                    toBigInt((String) collateralDataBefore.get("borrowRate")).compareTo(
                            toBigInt((String) collateralDataAfter.get("borrowRate"))) > 0
            );
        }
    }
}
