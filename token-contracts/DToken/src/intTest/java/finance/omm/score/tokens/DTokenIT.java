package finance.omm.score.tokens;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;

import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.tokens.config.dTokenConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DTokenIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeAll
    static void setup() throws  Exception{
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new dTokenConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();

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
    void borrow(){

        depositToReserve();

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));

        // test client borrows 10 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr,amountToBorrowIUSDC);

        BigInteger userBorrowIndex = ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress());
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.principalTotalSupply());
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.ONE.multiply(ICX), userBorrowIndex);
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.totalSupply());

        // test client borrows 5 ICX
        BigInteger amountToBorrowICX = BigInteger.valueOf(5).multiply(ICX);
        Address icxAddr = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(icxAddr,amountToBorrowICX);

        assertEquals(amountToBorrowICX,ommClient.dICX.principalTotalSupply());
        assertEquals(amountToBorrowICX,ommClient.dICX.balanceOf(testClient.getAddress()));
        assertEquals(BigInteger.ONE.multiply(ICX), ommClient.dICX.getUserBorrowCumulativeIndex(testClient.getAddress()));
        assertEquals(amountToBorrowICX,ommClient.dICX.principalBalanceOf(testClient.getAddress()));
        assertEquals(amountToBorrowICX,ommClient.dICX.totalSupply());

        // omm client borrow 2 ICX
        BigInteger amountBorrowed = BigInteger.TWO.multiply(ICX);
        ommClient.lendingPool.borrow(icxAddr,amountBorrowed);

        assertEquals(amountToBorrowICX.add(amountBorrowed),ommClient.dICX.principalTotalSupply());
        assertEquals(amountBorrowed,ommClient.dICX.balanceOf(ommClient.getAddress()));
//        assertEquals(BigInteger.ONE.multiply(ICX), ommClient.dICX.getUserBorrowCumulativeIndex(ommClient.getAddress()));
        assertEquals(amountBorrowed,ommClient.dICX.principalBalanceOf(ommClient.getAddress()));
        assertEquals(amountToBorrowICX.add(amountBorrowed),ommClient.dICX.totalSupply());

        // test client borrows 12 iusdc
        amountToBorrowIUSDC = amountToBorrowIUSDC.add(BigInteger.TWO.multiply(BigInteger.valueOf(100_000)));
        testClient.lendingPool.borrow(IUSDCAddr,amountToBorrowIUSDC);

        BigInteger totalSupply = amountToBorrowIUSDC.add(BigInteger.TEN.multiply(BigInteger.valueOf(100_000)));

        assertEquals(totalSupply,ommClient.dIUSDC.principalTotalSupply());
        assertEquals(totalSupply,ommClient.dIUSDC.balanceOf(testClient.getAddress()));
////        assertEquals(BigInteger.ONE.multiply(ICX), ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress()));
        assertEquals(totalSupply,ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(totalSupply,ommClient.dIUSDC.totalSupply());

    }

    private void mintToken(){
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(),amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(),amount);
        System.out.println(ommClient.iUSDC.balanceOf(ommClient.getAddress()));
    }

    /*
    ommClient and testClient deposit collateral
     */
    private void depositToReserve(){
        mintToken();
        BigInteger amountToDeposit= BigInteger.valueOf(1000).multiply(ICX);
        byte[] data = createByteArray("deposit",amountToDeposit);

        ((LendingPoolScoreClient)ommClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX),BigInteger.valueOf(1000).multiply(ICX));

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToDeposit,data);

        ((LendingPoolScoreClient)testClient.lendingPool).
                deposit(BigInteger.valueOf(100).multiply(ICX),BigInteger.valueOf(100).multiply(ICX));
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
