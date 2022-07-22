package finance.omm.score.tokens;

import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;

import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.tokens.config.oTokenConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Context;

import java.math.BigInteger;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    private static OMMClient testClient;

    private static Map<String, Address> addressMap;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
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
    void transferICX(){
        Context.transfer(testClient.getAddress(),BigInteger.TEN.negate());
    }
    @Test
    void deposit_ICX(){
        assertEquals(BigInteger.ZERO,ommClient.oICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalTotalSupply());
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.ZERO,ommClient.oICX.totalSupply());

        //omm client deposit 1000 ICX
        _deposit(ommClient,1000);
        //test client deposit 1000 ICX
        _deposit(testClient,1000);

        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.balanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(1000).multiply(ICX),ommClient.oICX.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(2000).multiply(ICX),ommClient.oICX.principalTotalSupply());
        assertEquals(BigInteger.valueOf(2000).multiply(ICX),ommClient.oICX.totalSupply());
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
//        mint_and_deposit(testClient,1000);

        BigInteger balance = BigInteger.valueOf(1000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(balance,ommClient.oUSDC.principalBalanceOf(ommClient.getAddress()));
        assertEquals(balance,ommClient.oUSDC.balanceOf(ommClient.getAddress()));
//        assertEquals(balance,ommClient.oUSDC.principalBalanceOf(testClient.getAddress()));
//        assertEquals(balance,ommClient.oUSDC.balanceOf(testClient.getAddress()));
//        BigInteger expected = BigInteger.valueOf(2000).multiply(BigInteger.valueOf(100_0000));
        assertEquals(balance,ommClient.oUSDC.principalTotalSupply());
        assertEquals(balance,ommClient.oUSDC.totalSupply());
    }

    @Test
    void transfer(){
        //testClient deposits 100 ICX
        _deposit(testClient,100);
        assertEquals(BigInteger.ZERO,ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(100).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));

        //transfers 50 ICX to omm Client
        testClient.oICX.transfer(ommClient.getAddress(),BigInteger.valueOf(50).multiply(ICX),"".getBytes());

        assertEquals(BigInteger.valueOf(50).multiply(ICX),ommClient.oICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(BigInteger.valueOf(50).multiply(ICX),ommClient.oICX.principalBalanceOf(testClient.getAddress()));

    }

    @Test
    public void borrow(){
        _deposit(testClient,1000);

        BigInteger amountToBorrowICX = BigInteger.valueOf(10).multiply(ICX);
        score.Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(icxAddr,amountToBorrowICX);

        System.out.println(ommClient.oICX.balanceOf(testClient.getAddress()));
        System.out.println(ommClient.oICX.principalBalanceOf(testClient.getAddress()));

    }

    @Test
    void redeem(){
        //testClient deposits 100 ICX
        _deposit(ommClient,100);

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        ommClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(50).multiply(ICX),false);

        assertEquals(BigInteger.valueOf(50).multiply(ICX),testClient.oICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(BigInteger.valueOf(50).multiply(ICX),testClient.oICX.balanceOf(testClient.getAddress()));

    }

    private void _deposit(OMMClient client,int amount){

        ((LendingPoolScoreClient)client.lendingPool).
                deposit(BigInteger.valueOf(amount).multiply(ICX),BigInteger.valueOf(amount).multiply(ICX));

    }

    private void mint_and_deposit(OMMClient client, int amount){
        mintToken(client);
        BigInteger amountToDeposit= BigInteger.valueOf(amount).multiply(BigInteger.valueOf(100_0000));
        byte[] data = createByteArray("deposit",amountToDeposit);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToDeposit,data);
    }

    private void mintToken(OMMClient client){
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(client.getAddress());
        ommClient.iUSDC.approve(client.getAddress(),amount);
        ommClient.iUSDC.mintTo(client.getAddress(),amount);
    }

    private byte[] createByteArray(String methodName, BigInteger value) {

        JsonObject internalParameters = new JsonObject()
                .add("amount",String.valueOf(value));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();

    }

}
