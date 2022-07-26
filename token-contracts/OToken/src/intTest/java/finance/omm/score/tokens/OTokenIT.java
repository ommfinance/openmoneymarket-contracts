package finance.omm.score.tokens;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;

import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.tokens.config.oTokenConfig;
import score.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Context;
import score.UserRevertedException;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    private static OMMClient testClient;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        System.out.println("Addressmap"+addressMap);
        Config config = new oTokenConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();

    }

    @Test
    void testName() {
        assertEquals("SICX Interest Token", ommClient.oICX.name());
    }

    @Test
    void deposit_ICX() throws InterruptedException {
        assertEquals(BigInteger.ZERO,ommClient.oICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalTotalSupply());
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oICX.totalSupply());

        System.out.println(ommClient.oICX.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        //omm client deposit 1000 ICX
        _deposit(ommClient,1000);

        System.out.println(ommClient.oICX.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        //test client deposit 1000 ICX
        _deposit(testClient,1000);

        Thread.sleep(10000L);

//        BigInteger amountToBorrowICX = BigInteger.valueOf(85).multiply(ICX);
//        score.Address icxAddr = addressMap.get(Contracts.sICX.getKey());
//        testClient.lendingPool.borrow(icxAddr,amountToBorrowICX);

//        _deposit(ommClient,1000);

        System.out.println(ommClient.oICX.getUserLiquidityCumulativeIndex(testClient.getAddress()));

        System.out.println(ommClient.oICX.getUserLiquidityCumulativeIndex(ommClient.getAddress()));

        score.Address icxAddr = addressMap.get(Contracts.sICX.getKey());

//        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
//        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.balanceOf(ommClient.getAddress()));
//        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));
//        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.balanceOf(testClient.getAddress()));
//        assertEquals(BigInteger.valueOf(2000).multiply(ICX),ommClient.oICX.principalTotalSupply());
//        assertEquals(BigInteger.valueOf(2000).multiply(ICX),ommClient.oICX.totalSupply());
    }

    @Test
    void liquidation(){

        ommClient.dummyPriceOracle.set_reference_data("ICX",ICX);

        //100 deposit icx 40 borrow usdc
        _deposit(testClient,100);

        mint_and_deposit(ommClient,100);

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000_000));

        // test client borrows 40 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr,amountToBorrowIUSDC);

        //0.5 icx
        ommClient.dummyPriceOracle.set_reference_data("ICX",ICX.divide(BigInteger.valueOf(2)));

        //transfer fail
        UserRevertedException failed = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(),BigInteger.valueOf(50).multiply(ICX),"".getBytes()));

        System.out.println(ommClient.oICX.principalBalanceOf(testClient.getAddress()));
        System.out.println(ommClient.oICX.balanceOf(testClient.getAddress()));

        System.out.println(ommClient.oICX.balanceOf(ommClient.getAddress()));

        System.out.println(ommClient.getAddress() + "test" + testClient.getAddress());

        //next user repay 10USDC
        byte[] data = createByteArray("liquidationCall",null,
                addressMap.get(Contracts.sICX.getKey()),addressMap.get(Contracts.IUSDC.getKey()),testClient.getAddress() );
        BigInteger amountToRepay = BigInteger.valueOf(5).multiply(BigInteger.valueOf(1000_000));

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToRepay,data);

        System.out.println(ommClient.oICX.balanceOf(ommClient.getAddress()));


        //20+10% of 20 oICX
        System.out.println(ommClient.oICX.principalBalanceOf(testClient.getAddress()));
        System.out.println(ommClient.oICX.balanceOf(testClient.getAddress()));

    }

    @Test
    void deposit_iUSDC(){
        assertEquals(BigInteger.ZERO,ommClient.oUSDC.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oUSDC.principalTotalSupply());
        assertEquals(BigInteger.ZERO,ommClient.oUSDC.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oUSDC.totalSupply());

        //omm client deposit 1000 iUSDC
        mint_and_deposit(ommClient,1000);

        //test client deposit 1000 iUSDC
        mint_and_deposit(testClient,1000);

        BigInteger balance = BigInteger.valueOf(1000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(balance,ommClient.oUSDC.principalBalanceOf(ommClient.getAddress()));
        assertEquals(balance,ommClient.oUSDC.balanceOf(ommClient.getAddress()));
        assertEquals(balance,ommClient.oUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(balance,ommClient.oUSDC.balanceOf(testClient.getAddress()));
        BigInteger expected = BigInteger.valueOf(2000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(expected,ommClient.oUSDC.principalTotalSupply());
        assertEquals(expected,ommClient.oUSDC.totalSupply());
    }

    @Test
    void transfer(){
        //testClient deposits 100 ICX
        _deposit(testClient,100);
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(100).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));

        UserRevertedException lessThanZero = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(),BigInteger.ONE.multiply(ICX).negate(),"".getBytes()));
        UserRevertedException highThanBalance = assertThrows(UserRevertedException.class, () ->
                testClient.oICX.transfer(ommClient.getAddress(),BigInteger.valueOf(101).multiply(ICX),"".getBytes()));
        //transfers 50 ICX to omm Client
        testClient.oICX.transfer(ommClient.getAddress(),BigInteger.valueOf(50).multiply(ICX),"".getBytes());

        assertEquals(BigInteger.valueOf(50).multiply(ICX),ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(50).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));

    }

    @Test
    public void borrow() throws InterruptedException {
        _deposit(testClient,1000);
        _deposit(ommClient,1000);

        BigInteger amountToBorrowICX = BigInteger.valueOf(85).multiply(ICX);
        score.Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(icxAddr,amountToBorrowICX);

        Thread.sleep(10000L);

        assertEquals(true,(ommClient.oICX.balanceOf(testClient.getAddress()).compareTo(ommClient.
                oICX.principalBalanceOf(testClient.getAddress())))>0);
        System.out.println();

    }

    @Test
    void redeem(){
        //testClient deposits 100 ICX
        _deposit(testClient,100);

        assertEquals(BigInteger.valueOf(100).multiply(ICX),testClient.oICX.balanceOf(testClient.getAddress()));

        Address icxAddr = addressMap.get(Contracts.oICX.getKey());

        //when tries to transfer more than balance
        UserRevertedException moreThanBalance = assertThrows(UserRevertedException.class, () ->
                testClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(110).multiply(ICX),false));

        testClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(50).multiply(ICX),false);

        assertEquals(BigInteger.valueOf(50).multiply(ICX),testClient.oICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(50).multiply(ICX),testClient.oICX.balanceOf(testClient.getAddress()));

    }

    private void _deposit(OMMClient client,int amount){

        ((LendingPoolScoreClient)client.lendingPool).
                deposit(BigInteger.valueOf(amount).multiply(ICX),BigInteger.valueOf(amount).multiply(ICX));

    }

    private void mint_and_deposit(OMMClient client, int amount){
        mintToken(client);
        BigInteger amountToDeposit= BigInteger.valueOf(amount).multiply(BigInteger.valueOf(1000_000));
        byte[] data = createByteArray("deposit",amountToDeposit,null,null,null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToDeposit,data);
    }

    private void mintToken(OMMClient client){
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(client.getAddress());
        ommClient.iUSDC.approve(client.getAddress(),amount);
        client.iUSDC.mintTo(client.getAddress(),amount);
    }

    private byte[] createByteArray(String methodName, BigInteger value,
                                   @Optional Address collateral, @Optional Address reserve, @Optional Address user) {

        JsonObject internalParameters = new JsonObject()
                .add("amount",String.valueOf(value))
                .add("_collateral",String.valueOf(collateral))
                .add("_reserve",String.valueOf(reserve))
                .add("_user",String.valueOf(user));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();

    }

}
