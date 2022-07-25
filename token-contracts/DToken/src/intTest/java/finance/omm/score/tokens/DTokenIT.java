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
import score.annotation.Optional;

import java.math.BigInteger;
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
//        assertEquals(amountToBorrowICX,ommClient.dICX.totalSupply());

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

    @Test
    void repay(){
        depositToReserve();

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(100).multiply(BigInteger.valueOf(100_000));
        System.out.println("amount to borrow " + amountToBorrowIUSDC);

        // test client borrows 100 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr,amountToBorrowIUSDC);

        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.principalTotalSupply());
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        assertEquals(amountToBorrowIUSDC,ommClient.dIUSDC.totalSupply());

        System.out.println(ommClient.dIUSDC.principalTotalSupply());
        System.out.println(ommClient.dIUSDC.balanceOf(testClient.getAddress()));

        BigInteger amountToRepay = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));
        byte[] data = createByteArray("repay",amountToRepay,null,null,null);


        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToRepay,data);

        System.out.println("after repaying "+ommClient.dIUSDC.principalTotalSupply());

        BigInteger remainingAmount = amountToBorrowIUSDC.subtract(amountToRepay);
        System.out.println("remaining amount "+ remainingAmount);


        System.out.println(ommClient.dIUSDC.principalTotalSupply());
        System.out.println(ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        System.out.println(ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        System.out.println(ommClient.dIUSDC.totalSupply());
        System.out.println(ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress()));

//        assertEquals(remainingAmount,ommClient.dIUSDC.principalTotalSupply(), String.valueOf(1L));
//        assertEquals(remainingAmount,ommClient.dIUSDC.balanceOf(testClient.getAddress()));
////      assertEquals(BigInteger.ONE.multiply(ICX), userBorrowIndex);
//        assertEquals(remainingAmount,ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
//        assertEquals(remainingAmount,ommClient.dIUSDC.totalSupply());

        amountToRepay = BigInteger.valueOf(91).multiply(BigInteger.valueOf(100_000));
        data = createByteArray("repay",amountToRepay,null,null,null);

        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToRepay,data);

        System.out.println("after repaying "+ommClient.dIUSDC.principalTotalSupply());


        System.out.println(ommClient.dIUSDC.principalTotalSupply());
        System.out.println(ommClient.dIUSDC.balanceOf(testClient.getAddress()));
        System.out.println(ommClient.dIUSDC.principalBalanceOf(testClient.getAddress()));
        System.out.println(ommClient.dIUSDC.totalSupply());
        System.out.println(ommClient.dIUSDC.getUserBorrowCumulativeIndex(testClient.getAddress()));

    }

    @Test
    /*
    testClient deposit 100IUSDC
    the price drops from 0.3 to 0.1
     */
    void liquidation(){
        depositToReserve();

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(100).multiply(BigInteger.valueOf(100_000));

        // test client borrows 100 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr,amountToBorrowIUSDC);

        ommClient.dummyPriceOracle.set_reference_data("ICX",
                BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN));

//        byte[] data = createByteArray("liquidationCall",null,testClient.getAddress(),
//                addressMap.get(Contracts.IUSDC.getKey()), )
//        testClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToRepay,data);

    }

    private void mintToken(){
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(),amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(),amount);
    }

    /*
    ommClient and testClient deposit collateral
     */
    private void depositToReserve(){
        mintToken();
        BigInteger amountToDeposit= BigInteger.valueOf(1000).multiply(ICX);
        BigInteger amountIUSDC = BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000));
        byte[] data = createByteArray("deposit",amountToDeposit,null,null,null);

        ((LendingPoolScoreClient)ommClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX),BigInteger.valueOf(1000).multiply(ICX));

        ommClient.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),amountToDeposit,data);

        ((LendingPoolScoreClient)testClient.lendingPool).
                deposit(BigInteger.valueOf(100).multiply(ICX),BigInteger.valueOf(100).multiply(ICX));

        ommClient.iUSDC.transfer(testClient.getAddress(),amountIUSDC,new byte[]{});
        System.out.println( ommClient.iUSDC.balanceOf(testClient.getAddress()));

    }

    @Test
    void transfer(){
        depositToReserve();

        BigInteger amount = BigInteger.valueOf(-10).multiply(BigInteger.valueOf(1000_000));
        System.out.println("amoint  " + amount);

        byte[] data = new byte[]{};

        ommClient.iUSDC.transfer(testClient.getAddress(),amount,data);
        System.out.println( ommClient.iUSDC.balanceOf(testClient.getAddress()));
    }

//    @Test
//    void testTransfer(){
//        BigInteger amount = BigInteger.valueOf(-10).multiply(BigInteger.valueOf(1000_000));
//        System.out.println("amoint  " + amount);
//
//        byte[] data = new byte[]{};
//        ommClient.dICX.transferDummy(testClient.getAddress(),amount,data);
//    }

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

/*
getUserBorrowCumulativeIndex -> users indexes
principalBalanceOf -> the balance and the accured interest for that user
balanceOf-> the balance and accured interest as compyted value
principalTotalSupply-> total supply of dToken
getPrincipalSupply-> the balance of user and total supply of dToken
totalSupply-> totalSupply of tokens in existence
transfer->  transfer of dToken
getTotalStaked-> total supply for reward distribution : total supply and the decimals

burnOnRepay->
burnOnLiquidation->

//        BigInteger amount = BigInteger.valueOf(10).multiply(BigInteger.valueOf(1000_000));
//        byte[] data = new byte[]{};
//        ommClient.iUSDC.transfer(testClient.getAddress(),amount,data);

//    @Test
//    void transfer(){
//        depositToReserve();
//
//        BigInteger amount = BigInteger.valueOf(10).multiply(BigInteger.valueOf(1000_000));
//
//        byte[] data = new byte[]{};
//
//        ommClient.iUSDC.transfer(testClient.getAddress(),amount,data);
//        System.out.println( ommClient.iUSDC.balanceOf(testClient.getAddress()));
//    }

// princ

 */