package finance.omm.score.core.lendingpool.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.lendingpool.exception.LendingPoolException;
import finance.omm.score.core.lendingpool.integration.config.lendingPoolConfig;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.score.core.lendingpool.AbstractLendingPool.TAG;
import static finance.omm.utils.math.MathUtils.HALF_ICX;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LendingPoolIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;
    private static OMMClient alice;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new lendingPoolConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
        ommClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));
        ommClient.sICX.setMinter(addressMap.get(Contracts.STAKING.getKey()));
        mintToken();
        reserveSetup();
    }

    @Test
    @Order(1)
    void testName() {
        assertEquals("Omm Lending Pool", ommClient.lendingPool.name());
    }

    @Test
    @Order(2)
    void deposit_icx() {
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        BigInteger depositAmt = BigInteger.valueOf(100).multiply(ICX);
        ((LendingPoolScoreClient) testClient.lendingPool).deposit(depositAmt, depositAmt);
        assertEquals(ommClient.lendingPoolCore.getUserBasicReserveData(icxAddr, testClient.getAddress()).
                get("underlyingBalance"), depositAmt);
    }

    @Test
    @Order(3)
    void deposit_token() {

        Address iusdcAddr = addressMap.get(Contracts.IUSDC.getKey());

        ommClient.iUSDC.transfer(testClient.getAddress(),
                BigInteger.valueOf(10).multiply(BigInteger.valueOf(1000_000)), new byte[]{});

        depositIUSDC(testClient, BigInteger.valueOf(10));

        assertEquals(ommClient.lendingPoolCore.getUserBasicReserveData(iusdcAddr, testClient.getAddress()).
                get("underlyingBalance"), BigInteger.valueOf(10_000_000));
    }

    @Test
    @Order(4)
    void deposit_payble() {
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        ommClient.governance.setReserveActiveStatus(icxAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, deposit unsuccessful"),
                () -> depositICX(ommClient, BigInteger.valueOf(10)), null);

        ommClient.governance.setReserveActiveStatus(icxAddr, true);
        ommClient.governance.setReserveFreezeStatus(icxAddr, true);

        assertUserRevert(LendingPoolException.unknown("Reserve is frozen, deposit unsuccessful"),
                () -> depositICX(ommClient, BigInteger.valueOf(10)), null);

        ommClient.governance.setReserveFreezeStatus(icxAddr, false);
        depositICX(testClient, BigInteger.valueOf(10));
        //previous hundred
        assertEquals((ommClient.lendingPoolCore.getUserBasicReserveData(icxAddr, testClient.getAddress())).
                get("underlyingBalance"), BigInteger.valueOf(110).multiply(ICX));

    }

    @Test
    @Order(5)
    void borrow_success_flow() throws InterruptedException {
        depositICX(testClient, BigInteger.valueOf(1000));
        BigInteger borrowAmt = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000));

        Thread.sleep(2000L);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());
        testClient.lendingPool.borrow(iusdc_reserve, borrowAmt);

        Thread.sleep(2000L);

        Map<String, BigInteger> borrowbalance = ommClient.lendingPoolCore.getUserBorrowBalances(iusdc_reserve,
                testClient.getAddress());
        assertEquals(borrowbalance.get("principalBorrowBalance"), BigInteger.valueOf(100_000_000));
//        assertTrue(borrowbalance.get("compoundedBorrowBalance").compareTo(borrowbalance.
//                get("principalBorrowBalance"))>0);

    }

    @Test
    @Order(6)
    void repay_tokenfallback() {
        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX);

        transferIusdc();

        BigInteger amount = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000));

        Address iusdcAddr = addressMap.get(Contracts.IUSDC.getKey());

        // test client already borrows 100 IUSDC

        assertEquals(amount, ommClient.dIUSDC.balanceOf(testClient.getAddress()));

        BigInteger repay = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000));
        byte[] data = createByteArray("repay", repay, null, null, null);

        ommClient.governance.setReserveActiveStatus(iusdcAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, repay unsuccessful"),
                () -> testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), repay, data), null);

        ommClient.governance.setReserveActiveStatus(iusdcAddr, true);

        // repaying 40 IUSDC
        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),
                BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000_000)), data);

        BigInteger loanOriginationFee = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000).divide(BigInteger.valueOf(1000)));
        assertEquals(BigInteger.valueOf(60).multiply(BigInteger.valueOf(1000_000)).add(loanOriginationFee),
                ommClient.dIUSDC.balanceOf(testClient.getAddress()));

        assertEquals(BigInteger.valueOf(1060).multiply(BigInteger.valueOf(1000_000)),
                ommClient.iUSDC.balanceOf(testClient.getAddress()));

        // repaying more
        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), repay, data);

        assertEquals(BigInteger.valueOf(9999).multiply(BigInteger.valueOf(1000_00)),
                ommClient.iUSDC.balanceOf(testClient.getAddress()));

        assertUserRevert(LendingPoolException.unknown("The user does not have any borrow pending"),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), repay, data), null);

        assertUserRevert(LendingPoolException.unknown("The user does not have any borrow pending"),
                () -> testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), repay, data), null);

        assertEquals(BigInteger.ZERO, ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.dIUSDC.totalSupply());
    }


    @Test
    @Order(7)
    void liquidation_tokenfallback() throws InterruptedException {

        //configuration after borrow case works
        Address collateral = addressMap.get(Contracts.sICX.getKey());
        Address reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address user = alice.getAddress();

        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX);

        //100 deposit icx 40 borrow usdc
        depositICX(alice, BigInteger.valueOf(1000));

        depositIUSDC(ommClient, BigInteger.valueOf(100));

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(400).multiply(BigInteger.valueOf(1000_000));

        // test client borrows 40 IUSDC
        alice.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);

        Thread.sleep(20000L);
        //0.5 icx
        ommClient.dummyPriceOracle.set_reference_data("ICX", HALF_ICX);

        byte[] data = createByteArray("liquidationCall", null, collateral, reserve, user);
        BigInteger value = BigInteger.valueOf(50).multiply(BigInteger.valueOf(1000_000));

        ommClient.lendingPool.setLiquidationStatus(true);

        Address iusdcAddr = addressMap.get(Contracts.IUSDC.getKey());
        ommClient.governance.setReserveActiveStatus(iusdcAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Borrow reserve is not active,liquidation unsuccessful"),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), value, data), null);

        ommClient.governance.setReserveActiveStatus(iusdcAddr, true);
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        ommClient.governance.setReserveActiveStatus(icxAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Collateral reserve is not active,liquidation unsuccessful"),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), value, data), null);


        ommClient.governance.setReserveActiveStatus(iusdcAddr, true);
        ommClient.governance.setReserveActiveStatus(icxAddr, true);

        BigInteger prevBalance = ommClient.oICX.balanceOf(alice.getAddress());

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), value, data);

        BigInteger balanceAfterLiq = ommClient.oICX.principalBalanceOf(alice.getAddress());
        BigInteger feeProvider = ommClient.sICX.balanceOf(addressMap.get("feeProvider"));
        BigInteger repayEq = BigInteger.valueOf(100).multiply(ICX);
        BigInteger tenPercent = BigInteger.TEN.multiply(ICX);

        assertEquals(prevBalance.subtract(feeProvider.add(repayEq).add(tenPercent)), balanceAfterLiq);
        //10+10% of 10 oICX + fee provider

    }

    @Test
    @Order(8)
    void borrow_more_than_available() {
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        assertUserRevert(LendingPoolException.unknown(TAG + "Amount requested " + borrowAmt +
                        " is more then the "),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);

    }

    @Test
    @Order(8)
    void borrow_not_active_reserve() {
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        ((LendingPoolScoreClient) testClient.lendingPool).deposit(BigInteger.valueOf(200), BigInteger.valueOf(200));


        ommClient.governance.setReserveActiveStatus(iusdc_reserve, false);
        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, borrow unsuccessful"),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);

        ommClient.governance.setReserveActiveStatus(iusdc_reserve, true);
    }

    @Test
    @Order(8)
    void borrow_freeze_reserve() {
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        ((LendingPoolScoreClient) testClient.lendingPool).deposit(BigInteger.valueOf(200), BigInteger.valueOf(200));

        ommClient.governance.setReserveFreezeStatus(iusdc_reserve, true);
        assertUserRevert(LendingPoolException.unknown("Reserve is frozen, borrow unsuccessful"),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);
        ommClient.governance.setReserveFreezeStatus(iusdc_reserve, false);

    }

    @Test
    @Order(8)
    void reserve_borrow_not_enabled() {
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        ((LendingPoolScoreClient) testClient.lendingPool).deposit(BigInteger.valueOf(200), BigInteger.valueOf(200));

        ommClient.governance.updateBorrowingEnabled(iusdc_reserve, false);
        assertUserRevert(LendingPoolException.unknown("Borrow error:borrowing not enabled in the reserve"),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);

        ommClient.governance.updateBorrowingEnabled(iusdc_reserve, true);
    }

    @Test
    @Order(9)
    void testTokenFallbackExceptions() {

        Address collateral = addressMap.get(Contracts.sICX.getKey());
        Address reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address user = testClient.getAddress();

        byte[] data = createByteArray("abcd", null, collateral, reserve, user);
        BigInteger value = BigInteger.TEN.multiply(ICX);
        byte[] finalData = data;
        assertUserRevert(LendingPoolException.unknown(TAG + " No valid method called, data: " + data.toString()),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), value, finalData), null);

        data = createByteArray("liquidationCall", null, null, reserve, user);
        byte[] finalData1 = data;
        assertReverted(new RevertedException(1, TAG + " Invalid data: Collateral: " + collateral +
                " Reserve: " + reserve + " User: " + user), () -> ommClient.iUSDC.transfer(
                addressMap.get(Contracts.LENDING_POOL.getKey()), value, finalData1));

        data = createByteArray("liquidationCall", null, collateral, null, user);
        byte[] finalData2 = data;
        assertReverted(new RevertedException(1, TAG + " Invalid data: Collateral: " + collateral +
                " Reserve: " + reserve + " User: " + user), () -> ommClient.iUSDC.transfer(
                addressMap.get(Contracts.LENDING_POOL.getKey()), value, finalData2));

        data = createByteArray("liquidationCall", null, collateral, reserve, null);
        byte[] finalData3 = data;
        assertReverted(new RevertedException(1, TAG + " Invalid data: Collateral: " + collateral +
                " Reserve: " + reserve + " User: " + user), () -> ommClient.iUSDC.transfer(
                addressMap.get(Contracts.LENDING_POOL.getKey()), value, finalData3));
    }

    @Test
    @Order(11)
    void redeem_reserve_inactive() {
        depositICX(ommClient, BigInteger.valueOf(1000));

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        ommClient.governance.setReserveActiveStatus(icxAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, redeem unsuccessful"),
                () -> ommClient.lendingPool.redeem(addressMap.get(Contracts.oICX.getKey()), BigInteger.valueOf(50).
                        multiply(ICX), false), null);

        ommClient.governance.setReserveActiveStatus(icxAddr, true);

    }

    @Test
    @Order(10)
    void redeem_more_than_liquidity() {
        depositICX(ommClient, BigInteger.valueOf(1000));
        // 1000 ICX deposited in reserve setup
        //total liquidity available = 2000
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        BigInteger amount = BigInteger.valueOf(1000).multiply(ICX);
        BigInteger availableLiquidity = BigInteger.valueOf(2000).multiply(ICX);

        ommClient.lendingPool.redeem(icxAddr, BigInteger.valueOf(2000).multiply(ICX), false);

        assertReverted(new RevertedException(1, "Amount " + amount + " is more than available liquidity " +
                availableLiquidity), () -> ommClient.lendingPool.redeem(icxAddr, amount, false));
    }

    @Test
    @Order(12)
    void redeem_waitForUnstaking() {

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        ommClient.lendingPool.redeem(icxAddr, BigInteger.valueOf(50).multiply(ICX), true);

        Address iusdcaddr = addressMap.get(Contracts.IUSDC.getKey());

        assertUserRevert(LendingPoolException.unknown("Redeem with wait for unstaking failed: Invalid token"),
                () -> ommClient.lendingPool.redeem(iusdcaddr, BigInteger.valueOf(50), true),
                null);
    }

    @Test
    @Order(13)
    void redeem_success_test() {
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        BigInteger amount = BigInteger.valueOf(500).multiply(ICX);

        ommClient.lendingPool.redeem(icxAddr, amount, false);

        assertEquals(ommClient.oICX.balanceOf(ommClient.getAddress()), BigInteger.valueOf(450).multiply(ICX));


        testClient.lendingPool.redeem(icxAddr, amount, false);
        float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
        assertEquals(ommClient.oICX.balanceOf(testClient.getAddress()).floatValue(),
                amount.add(BigInteger.valueOf(110).multiply(ICX)).floatValue(), delta);

    }

    @Test
    @Order(14)
    void redeem_partial_test() throws InterruptedException {

        depositICX(testClient, BigInteger.valueOf(1000));

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        Thread.sleep(2000L);

        BigInteger amount = BigInteger.valueOf(60).multiply(ICX);

        ommClient.lendingPool.redeem(icxAddr, amount, false);

        Thread.sleep(2000L);

        BigInteger beforeBalance = ommClient.sICX.balanceOf(testClient.getAddress());
        testClient.lendingPool.redeem(icxAddr, amount, true);

        depositICX(testClient, BigInteger.valueOf(10));

        Thread.sleep(2000L);

        assertEquals(amount.add(BigInteger.valueOf(2610).multiply(ICX)),
                ommClient.sICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(390).multiply(ICX), ommClient.oICX.balanceOf(ommClient.getAddress()));

        // balance should be equal as sICX is unstaking
        assertEquals(beforeBalance, ommClient.sICX.balanceOf(testClient.getAddress()));
    }


    @Test
    @Order(15)
    void stake() {
        assertReverted(new RevertedException(1, "Staking of OMM token no longer supported."),
                () -> ommClient.lendingPool.stake(BigInteger.valueOf(500).multiply(ICX)));

    }

    @Test
    @Order(15)
    void unstake() {
        BigInteger value = BigInteger.ONE.negate();
        assertReverted(new RevertedException(1, "Cannot unstake less than or equals to zero value to stake " + value),
                () -> ommClient.lendingPool.unstake(value.multiply(ICX)));

        BigInteger finalValue = BigInteger.valueOf(100);
        assertReverted(new RevertedException(1, "Cannot unstake,user dont have enough staked balance amount to unstake "
                        + value + " staked balance of user: " + ommClient.getAddress() + " is " + BigInteger.ZERO),
                () -> ommClient.lendingPool.unstake(finalValue.multiply(ICX)));
    }

    @Test
    @Order(15)
    void checkDepositWallets() {
        depositICX(testClient, BigInteger.valueOf(1000));
        List<Address> depositList = ommClient.lendingPool.getDepositWallets(0);

        assertEquals(3, depositList.size());
        assertEquals(ommClient.getAddress(), depositList.get(0));
        assertEquals(testClient.getAddress(), depositList.get(1));
        assertEquals(alice.getAddress(), depositList.get(2));
    }

    @Test
    @Order(15)
    void checkBorrowWallets() {
        depositICX(testClient, BigInteger.valueOf(1000));

        List<Address> borrowList = ommClient.lendingPool.getBorrowWallets(0);
        assertEquals(2, borrowList.size());

        Address iusdc_reserve = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(iusdc_reserve, BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000)));
        ommClient.lendingPool.borrow(iusdc_reserve, BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000)));


        borrowList = ommClient.lendingPool.getBorrowWallets(0);
        assertEquals(3, borrowList.size());
        assertEquals(testClient.getAddress(), borrowList.get(0));
        assertEquals(alice.getAddress(), borrowList.get(1));
        assertEquals(ommClient.getAddress(), borrowList.get(2));
    }


    @Test
    @Order(16)
    void deposit_tokenfallback() {
        byte[] data = createByteArray("deposit", null, null,
                null, null);

        Address iusdcAddr = addressMap.get(Contracts.IUSDC.getKey());
        ommClient.governance.setReserveActiveStatus(iusdcAddr, false);

        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, deposit unsuccessful"),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), BigInteger.valueOf(1000).multiply(ICX),
                        data), null);

        ommClient.governance.setReserveActiveStatus(iusdcAddr, true);
        ommClient.governance.setReserveFreezeStatus(iusdcAddr, true);

        assertUserRevert(LendingPoolException.unknown("Reserve is frozen, deposit unsuccessful"),
                () -> ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), BigInteger.valueOf(1000).multiply(ICX),
                        data), null);

        ommClient.governance.setReserveFreezeStatus(iusdcAddr, false);

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), BigInteger.valueOf(1000).multiply(BigInteger.valueOf(1000_000)), data);

        float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
        assertEquals(ommClient.oIUSDC.balanceOf(ommClient.getAddress()).floatValue(),
                BigInteger.valueOf(2000).multiply(BigInteger.valueOf(1000_000)).floatValue(), delta);
    }


    @Test
    @Order(19)
    void claimRewards() throws InterruptedException {
        depositICX(ommClient, BigInteger.valueOf(1000));

        Thread.sleep(1000L);
        ommClient.reward.distribute();

        Address daofund = addressMap.get(Contracts.DAO_FUND.getKey());

        BigInteger systemTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger time = systemTime.add(BigInteger.valueOf(1));
        assertTrue(ommClient.ommToken.balanceOf(ommClient.getAddress()).equals(BigInteger.ZERO));
        ommClient.lendingPool.claimRewards();
        assertTrue(ommClient.ommToken.balanceOf(ommClient.getAddress()).compareTo(BigInteger.ZERO) > 0);
    }

    private void depositICX(OMMClient client, BigInteger amount) {
        ((LendingPoolScoreClient) client.lendingPool).deposit(amount.multiply(ICX), amount.multiply(ICX));
    }

    private void transferIusdc() {

        byte[] data = new byte[]{};
        ommClient.iUSDC.transfer(testClient.getAddress(), BigInteger.valueOf(1000).multiply(BigInteger.valueOf(1000_000)), data);
    }

    /*
    deposit IUSDC as collateral
     */
    private void depositIUSDC(OMMClient client, BigInteger amount) {
        byte[] data = createByteArray("deposit", amount, null, null, null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),
                amount.multiply(BigInteger.valueOf(1000_000)), data);
    }

    private void mintToken() {
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(), amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(), amount);
    }

    private byte[] createByteArray(String methodName, BigInteger value,
                                   @Optional Address collateral, @Optional Address reserve, @Optional Address user) {

        JsonObject internalParameters = new JsonObject()
                .add("amount", String.valueOf(value))
                .add("_collateral", String.valueOf(collateral))
                .add("_reserve", String.valueOf(reserve))
                .add("_user", String.valueOf(user));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

    private void reserveSetup() {
        depositICX(ommClient, BigInteger.valueOf(1000));
        depositIUSDC(ommClient, BigInteger.valueOf(1000));
    }

    @Test
    void borrow_more_than_collateral() {
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        assertUserRevert(LendingPoolException.unknown("Borrow error:The user does not have any collateral"),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);
    }

    @Test
    void borrow_insufficient_collateral() {
        ((LendingPoolScoreClient) testClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX), BigInteger.valueOf(1000).multiply(ICX));

        BigInteger borrowAmt = BigInteger.valueOf(1000000000000L);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());


        assertUserRevert(LendingPoolException.unknown("Borrow error: Insufficient collateral to cover new borrow"),
                () -> testClient.lendingPool.borrow(iusdc_reserve, borrowAmt), null);
    }
}
