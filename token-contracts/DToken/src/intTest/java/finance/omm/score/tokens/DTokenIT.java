package finance.omm.score.tokens;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.tokens.config.dTokenConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;
import score.RevertedException;
import score.UserRevertException;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.score.tokens.DTokenImpl.TAG;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeEach
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new dTokenConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        mintToken();
        reserveSetup();
    }

    @Test
    void testName() {
        assertEquals("SICX Debt Token", ommClient.dICX.name());
    }

    @Test
    /*
    testClient and ommClient borrows iusdc and icx
    totalSupply and users balanceOf is asserted
     */
    void borrow() throws InterruptedException {

        depositICX(testClient, BigInteger.valueOf(100));
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());

        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));
        BigInteger ommBorrowICX = BigInteger.valueOf(5).multiply(ICX);

        // omm client borrows 5 ICX
        ommClient.lendingPool.borrow(icxAddr, ommBorrowICX);

        BigInteger userBorrowIndexICX = ommClient.dICX.getUserBorrowCumulativeIndex(ommClient.getAddress());

        assertEquals(ommBorrowICX, ommClient.dICX.principalTotalSupply());
        assertEquals(ommBorrowICX, ommClient.dICX.balanceOf(ommClient.getAddress()));
        assertEquals(ommBorrowICX, ommClient.dICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(ommBorrowICX, ommClient.dICX.totalSupply());
        assertEquals(ICX, userBorrowIndexICX);


        // test client borrows 10 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);
        BigInteger testBorrowIndexIUSDC = ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress());

        assertEquals(amountToBorrowIUSDC, ommClient.dIUSDC.principalTotalSupply());
        assertEquals(amountToBorrowIUSDC, ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(ICX, testBorrowIndexIUSDC);
        assertEquals(amountToBorrowIUSDC, ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(amountToBorrowIUSDC, ommClient.dIUSDC.totalSupply());

        // test client borrows 5 ICX
        BigInteger testBorrowICX = BigInteger.valueOf(5).multiply(ICX);

        testClient.lendingPool.borrow(icxAddr, testBorrowICX);

        assertEquals(testBorrowICX.add(ommBorrowICX), ommClient.dICX.principalTotalSupply());
        assertEquals(testBorrowICX, ommClient.dICX.balanceOf(testClient.getAddress()));
        assertEquals(testBorrowICX, ommClient.dICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(testBorrowICX.add(ommBorrowICX), ommClient.dICX.totalSupply());


        BigInteger testIndexICX = ommClient.dICX.getUserBorrowCumulativeIndex(testClient.getAddress());
        assertTrue(testIndexICX.longValue() > userBorrowIndexICX.longValue());

        // omm client borrow 2 ICX
        BigInteger ommBorrowICX_again = BigInteger.TWO.multiply(ICX);
        ommClient.lendingPool.borrow(icxAddr, ommBorrowICX_again);

        BigInteger totalSupplyICX = ommBorrowICX.add(testBorrowICX).add(ommBorrowICX_again);
        Thread.sleep(5000);

        assertTrue(totalSupplyICX.longValue() < ommClient.dICX.principalTotalSupply().longValue());
        assertTrue(ommBorrowICX.add(ommBorrowICX_again).longValue() <
                ommClient.dICX.balanceOf(ommClient.getAddress()).longValue());
        assertTrue(ommBorrowICX.add(ommBorrowICX_again).longValue() <
                ommClient.dICX.principalBalanceOf(ommClient.getAddress()).longValue());
        assertTrue(totalSupplyICX.longValue() < ommClient.dICX.totalSupply().longValue()); // can be greater than principal

        BigInteger ommIndex = ommClient.dICX.getUserBorrowCumulativeIndex(ommClient.getAddress());

        assertTrue(ommIndex.longValue() > testIndexICX.longValue());
//
        // test client borrows 12 iusdc
        amountToBorrowIUSDC = BigInteger.valueOf(12).multiply(BigInteger.valueOf(100_000));
        testClient.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);

        BigInteger totalSupply = amountToBorrowIUSDC.add(BigInteger.TEN.multiply(BigInteger.valueOf(100_000)));

        BigInteger userBorrowIndex = ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress());

        Thread.sleep(10000);

        assertTrue(testBorrowIndexIUSDC.longValue() < userBorrowIndex.longValue());
        assertEquals(totalSupply, ommClient.dIUSDC.principalTotalSupply());
        assertEquals(totalSupply, ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(totalSupply, ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(totalSupply, ommClient.dIUSDC.totalSupply());

    }

    @Test
    void repay() {

        BigInteger amount = BigInteger.valueOf(100).multiply(BigInteger.valueOf(100_000));
        depositICX(testClient, BigInteger.valueOf(100));
        ommClient.iUSDC.transfer(testClient.getAddress(), amount, new byte[]{});

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());

        // test client borrows 100 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr, amount);

        assertEquals(amount, ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.ONE.multiply(ICX),
                ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress()));

        BigInteger repay = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));
        byte[] data = createByteArray("repay", repay, null, null, null);

        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), repay, data);

        BigInteger remainingAmount = ommClient.dIUSDC.balanceOf(testClient.getAddress());

        data = createByteArray("repay", remainingAmount, null, null, null);

        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), remainingAmount, data);

        assertEquals(BigInteger.ZERO, ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.ZERO, ommClient.dIUSDC.totalSupply());


    }

    @Test
    /*
    testClient deposits 100 ICX and borrows 100 IUSDC
    the price drops of ICX from 0.3 to 0.1
    ommClient calls liquidation
     */
    void liquidation() {
        depositICX(testClient, BigInteger.valueOf(100));

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(100).multiply(BigInteger.valueOf(100_000));

        // test client borrows 100 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);

        assertEquals(amountToBorrowIUSDC, ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));

        ommClient.dummyPriceOracle.set_reference_data("ICX",
                BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN));

        BigInteger balanceOfOmmClient = ommClient.iUSDC.balanceOf(ommClient.getAddress());
        byte[] data = createByteArray("liquidationCall", null,
                addressMap.get(Contracts.sICX.getKey()),
                addressMap.get(Contracts.IUSDC.getKey()), testClient.getAddress());
        BigInteger amountToRepay = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()), amountToRepay, data);

        BigInteger balanceofOmmClientAfter = balanceOfOmmClient.subtract(amountToRepay);
        BigInteger balanceofTestClientAfter = amountToBorrowIUSDC.subtract(amountToRepay);

        assertEquals(balanceofTestClientAfter, ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));

        assertEquals(balanceofOmmClientAfter, ommClient.iUSDC.balanceOf(ommClient.getAddress()));


    }

    @Test
    void transfer() {
        BigInteger amount = BigInteger.valueOf(10).multiply(ICX);

        assertUserRevert(new UserRevertException(TAG + "Transfer not allowed in debt token"),
                () -> ommClient.dICX.transfer(testClient.getAddress(), amount, new byte[]{}), null);

    }

    @Test
    void handleAction() {

        assertTrue(ommClient.dICX.isHandleActionEnabled());

        RevertedException handleAction = assertThrows(RevertedException.class, () ->
                ommClient.dICX.enableHandleAction());

        handleAction = assertThrows(RevertedException.class, () -> ommClient.dICX.disableHandleAction());
    }

    private void mintToken() {
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(), amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(), amount);
    }

    /*
   deposit ICX as collateral
     */
    private void depositICX(OMMClient client, BigInteger amount) {
        ((LendingPoolScoreClient) client.lendingPool).deposit(amount.multiply(ICX), amount.multiply(ICX));
    }

    /*
    deposit IUSDC as collateral
     */
    private void depositIUSDC(OMMClient client, BigInteger amount) {
        byte[] data = createByteArray("deposit", amount, null, null, null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),
                amount.multiply(BigInteger.valueOf(100_000)), data);
    }

    private void reserveSetup() {
        depositICX(ommClient, BigInteger.valueOf(1000));
        depositIUSDC(ommClient, BigInteger.valueOf(10000000));
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
