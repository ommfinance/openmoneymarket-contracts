package finance.omm.score.tokens;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.tokens.config.oTokenConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;
import score.RevertedException;
import score.UserRevertedException;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    private static OMMClient testClient;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new oTokenConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        ommClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));

    }

    @Order(1)
    @Test
    void testName() {
        assertEquals("SICX Interest Token", ommClient.oICX.name());
    }

    @Order(1)
    @Test
    void deposit_ICX() {
        assertEquals(BigInteger.ZERO, ommClient.oICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oICX.principalTotalSupply());
        assertEquals(BigInteger.ZERO, ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oICX.totalSupply());

        assertEquals(BigInteger.ZERO, ommClient.oICX.getUserLiquidityCumulativeIndex(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oICX.getUserLiquidityCumulativeIndex(testClient.getAddress()));

        //omm client deposit 1000 ICX
        _deposit(ommClient, 1000);

        assertEquals(BigInteger.ONE.multiply(ICX), ommClient.oICX.getUserLiquidityCumulativeIndex(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oICX.getUserLiquidityCumulativeIndex(testClient.getAddress()));

        //test client deposit 1000 ICX
        _deposit(testClient, 1000);


        assertEquals(BigInteger.valueOf(1000).multiply(ICX), ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX), ommClient.oICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX), ommClient.oICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX), ommClient.oICX.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(2000).multiply(ICX), ommClient.oICX.principalTotalSupply());
        assertEquals(BigInteger.valueOf(2000).multiply(ICX), ommClient.oICX.totalSupply());
    }

    @Order(3)
    @Test
    void checkLiquidityIndex() throws InterruptedException {
        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX);

        // ommClient deposit 1000 IUSDC
        _depositIUSDC(ommClient, 1000);
        BigInteger previous = ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress());

        // testClient deposit 5000 ICX
        _deposit(testClient, 5000);
        System.out.println(ommClient.oICX.getUserLiquidityCumulativeIndex(testClient.getAddress()));

        Thread.sleep(1000L);

        score.Address icxAddr = addressMap.get(Contracts.IUSDC.getKey());
        testClient.lendingPool.borrow(icxAddr, BigInteger.valueOf(500));
        System.out.println(ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        Thread.sleep(1000L);

        testClient.lendingPool.borrow(icxAddr, BigInteger.valueOf(30000));
        System.out.println(ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        Thread.sleep(1000L);

        testClient.lendingPool.borrow(icxAddr, BigInteger.valueOf(50000));
        System.out.println(ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        Thread.sleep(1000L);

        _depositIUSDC(ommClient, 10);
        System.out.println(ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress()));
        assertTrue(ommClient.oIUSDC.getUserLiquidityCumulativeIndex(ommClient.getAddress()).compareTo(previous) > 0);
    }

    @Test
    @Order(8)
    void liquidation() {

        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX);
        ommClient.lendingPool.setLiquidationStatus(true);

        mint_and_deposit(ommClient, 10000);

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(2600).multiply(BigInteger.valueOf(1000_000));

        // test client borrows 2000 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);

        //0.5 icx
        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX.divide(BigInteger.valueOf(2)));

        //transfer fail
        UserRevertedException failed = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(), BigInteger.valueOf(4000).multiply(ICX), "".getBytes()));

        float delta = (ICX.divide(BigInteger.valueOf(1000))).floatValue();
        assertEquals(BigInteger.valueOf(5900).multiply(ICX).floatValue(),
                ommClient.oICX.principalBalanceOf(testClient.getAddress()).floatValue(),delta);
        assertEquals(BigInteger.valueOf(5900).multiply(ICX).floatValue(),
                ommClient.oICX.balanceOf(testClient.getAddress()).floatValue(),delta);


        BigInteger prevBalance = ommClient.oICX.balanceOf(testClient.getAddress());

        //next user repay 10USDC
        byte[] data = createByteArray("liquidationCall", null,
                addressMap.get(Contracts.sICX.getKey()), addressMap.get(Contracts.IUSDC.getKey()), testClient.getAddress());
        BigInteger amountToRepay = BigInteger.valueOf(5).multiply(BigInteger.valueOf(1000_000));

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), amountToRepay, data);

        BigInteger balanceAfterLiq = ommClient.oICX.principalBalanceOf(testClient.getAddress());
        BigInteger feeProvider = ommClient.sICX.balanceOf(addressMap.get("feeProvider"));
        BigInteger repayEq = BigInteger.TEN.multiply(ICX);
        BigInteger tenPercent = BigInteger.ONE.multiply(ICX);

        assertEquals(prevBalance.subtract(feeProvider.add(repayEq).add(tenPercent)).floatValue(),
                balanceAfterLiq.floatValue(),delta);
        //10+10% of 10 oICX + fee provider

    }

    @Order(2)
    @Test
    void deposit_iUSDC() {
        assertEquals(BigInteger.ZERO, ommClient.oIUSDC.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oIUSDC.principalTotalSupply());
        assertEquals(BigInteger.ZERO, ommClient.oIUSDC.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.oIUSDC.totalSupply());

        //omm client deposit 1000 iUSDC
        mint_and_deposit(ommClient, 1000);

        //test client deposit 1000 iUSDC
        mint_and_deposit(testClient, 1000);

        BigInteger balance = BigInteger.valueOf(1000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(balance, ommClient.oIUSDC.principalBalanceOf(ommClient.getAddress()));
        assertEquals(balance, ommClient.oIUSDC.balanceOf(ommClient.getAddress()));
        assertEquals(balance, ommClient.oIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(balance, ommClient.oIUSDC.balanceOf(testClient.getAddress()));
        BigInteger expected = BigInteger.valueOf(2000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(expected, ommClient.oIUSDC.principalTotalSupply());
        assertEquals(expected, ommClient.oIUSDC.totalSupply());
    }

    @Order(4)
    @Test
    void transfer() {

        assertEquals(BigInteger.valueOf(1000L).multiply(ICX), ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(6000).multiply(ICX), ommClient.oICX.principalBalanceOf(testClient.getAddress()));

        UserRevertedException lessThanZero = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(), BigInteger.ONE.multiply(ICX).negate(), "".getBytes()));
        UserRevertedException highThanBalance = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(), BigInteger.valueOf(6001).multiply(ICX), "".getBytes()));
        //transfers 50 oICX to omm Client
        testClient.oICX.transfer(ommClient.getAddress(), BigInteger.valueOf(50).multiply(ICX), "".getBytes());

        assertEquals(BigInteger.valueOf(1050).multiply(ICX), ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(5950).multiply(ICX), ommClient.oICX.principalBalanceOf(testClient.getAddress()));

    }

    @Order(5)
    @Test
    public void borrow() throws InterruptedException {
        BigInteger amountToBorrowICX = BigInteger.valueOf(85).multiply(ICX);
        score.Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(icxAddr, amountToBorrowICX);

        Thread.sleep(10000L);

        assertEquals(true, (ommClient.oICX.balanceOf(testClient.getAddress()).compareTo(ommClient.
                oICX.principalBalanceOf(testClient.getAddress()))) > 0);

    }

    @Order(6)
    @Test
    void redeem() {
        assertEquals(BigInteger.valueOf(5950).multiply(ICX), testClient.oICX.principalBalanceOf(testClient.getAddress()));

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        //when tries to transfer more than balance
        RevertedException moreThanBalance = assertThrows(RevertedException.class, () ->
                testClient.lendingPool.redeem(icxAddr, BigInteger.valueOf(5951).multiply(ICX), false));

        testClient.lendingPool.redeem(icxAddr, BigInteger.valueOf(50).multiply(ICX), false);

        BigInteger principalBalanceOf =  testClient.oICX.principalBalanceOf(testClient.getAddress());
        assertTrue(principalBalanceOf.compareTo(BigInteger.valueOf(5900))>0);
        assertTrue((testClient.oICX.balanceOf(testClient.getAddress()).compareTo(BigInteger.valueOf(5900)))>0);

    }

    @Order(7)
    @Test
    void handleAction() {

        assertTrue(ommClient.oICX.isHandleActionEnabled());

        RevertedException handleAction = assertThrows(RevertedException.class, () ->
                ommClient.oICX.enableHandleAction());

        handleAction = assertThrows(RevertedException.class, () -> ommClient.oICX.disableHandleAction());
    }

    private void _deposit(OMMClient client, int amount) {

        ((LendingPoolScoreClient) client.lendingPool).
                deposit(BigInteger.valueOf(amount).multiply(ICX), BigInteger.valueOf(amount).multiply(ICX));

    }

    private void mint_and_deposit(OMMClient client, int amount) {
        mintToken(client);
        _depositIUSDC(client,amount);
    }

    private void _depositIUSDC(OMMClient client, int amount){
        BigInteger amountToDeposit = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(1000_000));
        byte[] data = createByteArray("deposit", amountToDeposit, null, null, null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), amountToDeposit, data);
    }

    private void mintToken(OMMClient client) {
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        try {
            ommClient.iUSDC.addIssuer(client.getAddress());
        } catch (Exception e) {
        }
        ommClient.iUSDC.approve(client.getAddress(), amount);
        client.iUSDC.mintTo(client.getAddress(), amount);
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

}
