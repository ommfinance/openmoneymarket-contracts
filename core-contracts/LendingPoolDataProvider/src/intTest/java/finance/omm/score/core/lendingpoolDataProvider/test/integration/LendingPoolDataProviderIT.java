package finance.omm.score.core.lendingpoolDataProvider.test.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.lendingpoolDataProvider.test.integration.config.dataProviderConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.annotation.Optional;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LendingPoolDataProviderIT implements ScoreIntegrationTest {

    private static OMMClient ommClient;

    private static OMMClient testClient;

    private static Map<String, Address> addressMap;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        System.out.println("addressMap " + addressMap);
        Config config = new dataProviderConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        mintToken();
    }

    @Test
    void testName(){
        assertEquals("Omm Lending Pool Data Provider",ommClient.lendingPoolDataProvider.name());
    }


    @Test
    void reserveAccountData_deposit(){
        BigInteger deposited = BigInteger.valueOf(100);
        transfer(testClient,deposited);

        // testClient deposits 100 ICX and 100 IUSDC
        depositICX(testClient,deposited);
        depositIUSDC(testClient,deposited);

        Map<String, BigInteger> data = ommClient.lendingPoolDataProvider.getReserveAccountData();

        Map<String, BigInteger> result_ICX = reserveDataCalculation("ICX",deposited.multiply(ICX),deposited.multiply(ICX),BigInteger.ZERO);

        Map<String, BigInteger> result_USDC = reserveDataCalculation("USDC",deposited.multiply(BigInteger.valueOf(1000_000)),
                deposited.multiply(BigInteger.valueOf(1000_000)),BigInteger.ZERO);

        BigInteger totalLiquidityBalanceUSD = result_ICX.get("totalLiquidityBalanceUSD").add(result_USDC.get("totalLiquidityBalanceUSD"));
        BigInteger availableLiquidityBalanceUSD = result_ICX.get("totalLiquidityBalanceUSD").add(result_USDC.get("totalLiquidityBalanceUSD"));
        BigInteger totalBorrowsBalanceUSD = result_ICX.get("totalBorrowsBalanceUSD").add(result_USDC.get("totalBorrowsBalanceUSD"));
        BigInteger totalCollateralBalanceUSD = result_ICX.get("totalCollateralBalanceUSD").add(result_USDC.get("totalCollateralBalanceUSD"));

        assertEquals(totalLiquidityBalanceUSD,data.get("totalLiquidityBalanceUSD"));
        assertEquals(availableLiquidityBalanceUSD,data.get("availableLiquidityBalanceUSD"));
        assertEquals(totalBorrowsBalanceUSD,data.get("totalBorrowsBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD,data.get("totalCollateralBalanceUSD"));
    }

//    @Test
//    void reserveAccountData_borrow(){
//        reserveAccountData_deposit();
//
//        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());
//        BigInteger amount = BigInteger.valueOf(10);
////        borrow(testClient,iusdc_reserve,amount);
//
//    }
//
    @Test
    public void borrow(){ // not working
        reserveSetup();
        ((LendingPoolScoreClient)testClient.lendingPool).
                deposit(BigInteger.valueOf(1000).multiply(ICX),BigInteger.valueOf(1000).multiply(ICX));
//        client.lendingPool.borrow(reserve,amount);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address icx_reserve = addressMap.get(Contracts.sICX.getKey());
        testClient.lendingPool.borrow(iusdc_reserve,BigInteger.valueOf(100));
    }


    @Test
    public void getUserReserveData_deposit_icx() throws InterruptedException {
        BigInteger deposited = BigInteger.valueOf(200);

        depositICX(testClient,deposited);

        Address icx_reserve = addressMap.get(Contracts.sICX.getKey());

        Map<String, BigInteger> reserve = ommClient.lendingPoolDataProvider.getUserReserveData(icx_reserve, testClient.getAddress());

        Map<String, BigInteger> result = userReserveDataCalculation("ICX", testClient);

        assertEquals(reserve.get("currentOTokenBalance"),result.get("currentOTokenBalance"));
        assertEquals(reserve.get("currentOTokenBalanceUSD"),result.get("currentOTokenBalanceUSD"));
        assertEquals(reserve.get("principalOTokenBalance"),result.get("principalOTokenBalance"));
        assertEquals(reserve.get("principalOTokenBalanceUSD"),result.get("principalOTokenBalanceUSD"));
        assertEquals(reserve.get("currentBorrowBalance"),result.get("currentBorrowBalance"));
        assertEquals(reserve.get("currentBorrowBalanceUSD"),result.get("currentBorrowBalanceUSD"));
        assertEquals(reserve.get("principalBorrowBalance"),result.get("principalBorrowBalance"));
        assertEquals(reserve.get("principalBorrowBalanceUSD"),result.get("principalBorrowBalanceUSD"));
        assertEquals(reserve.get("userLiquidityCumulativeIndex"),result.get("userLiquidityCumulativeIndex"));
        assertEquals(reserve.get("borrowRate"),result.get("borrowRate"));
        assertEquals(reserve.get("userBorrowCumulativeIndex"),result.get("userBorrowCumulativeIndex"));
        assertEquals(reserve.get("exchangeRate"),result.get("exchangeRate"));
        assertEquals(reserve.get("decimals"),result.get("decimals"));
        System.out.println(reserve.get("lastUpdateTimestamp"));
        // origination fee, lastUpdateTimestamp

        Thread.sleep(100000);
        depositICX(testClient,deposited);

        Map<String, BigInteger> reserve1 = ommClient.lendingPoolDataProvider.getUserReserveData(icx_reserve, testClient.getAddress());
        System.out.println("dd "+reserve1.get("lastUpdateTimestamp"));

    }

    @Test
    public void test(){
        Address icx_reserve = addressMap.get(Contracts.sICX.getKey());
        Map<String, Object> tt = ommClient.lendingPoolDataProvider.test(icx_reserve);

        System.out.println(tt.get("daa"));
    }

    @Test
    public void getUserReserveData_deposit_iusdc(){
        BigInteger deposited = BigInteger.valueOf(100);
        transfer(testClient,deposited);

        depositIUSDC(testClient,deposited);

        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        Map<String, BigInteger> reserve = ommClient.lendingPoolDataProvider.getUserReserveData(iusdc_reserve, testClient.getAddress());

        Map<String, BigInteger> result = userReserveDataCalculation("USDC", testClient);

        assertEquals(reserve.get("currentOTokenBalance"),result.get("currentOTokenBalance"));
        assertEquals(reserve.get("currentOTokenBalanceUSD"),result.get("currentOTokenBalanceUSD"));
        assertEquals(reserve.get("principalOTokenBalance"),result.get("principalOTokenBalance"));
        assertEquals(reserve.get("principalOTokenBalanceUSD"),result.get("principalOTokenBalanceUSD"));
        assertEquals(reserve.get("currentBorrowBalance"),result.get("currentBorrowBalance"));
        assertEquals(reserve.get("currentBorrowBalanceUSD"),result.get("currentBorrowBalanceUSD"));
        assertEquals(reserve.get("principalBorrowBalance"),result.get("principalBorrowBalance"));
        assertEquals(reserve.get("principalBorrowBalanceUSD"),result.get("principalBorrowBalanceUSD"));
        assertEquals(reserve.get("userLiquidityCumulativeIndex"),result.get("userLiquidityCumulativeIndex"));
        assertEquals(reserve.get("borrowRate"),result.get("borrowRate"));
        assertEquals(reserve.get("userBorrowCumulativeIndex"),result.get("userBorrowCumulativeIndex"));
        assertEquals(reserve.get("exchangeRate"),result.get("exchangeRate"));
        assertEquals(reserve.get("decimals"),result.get("decimals"));


    }

    @Test
    public void calculateCollateralNeededUSD(){
        // need to borrow
    }

//    @Test
//    public void getReserveData(){ // multiple transactions
//
//        depositICX(testClient,BigInteger.valueOf(10));
//        Address icx_reserve = addressMap.get(Contracts.sICX.getKey());
//        Map<String, Object> reserve = ommClient.lendingPoolDataProvider.getReserveData(icx_reserve);
//
//        Map<String, Object> result =reserveCalculation("ICX");
//
//        assertEquals(reserve.get("exchangePrice"),result.get("exchangePrice"));
//        assertEquals(reserve.get("sICXRate"),result.get("sICXRate"));
//        assertEquals(reserve.get("totalLiquidityUSD"),result.get("totalLiquidityUSD"));
//        assertEquals(reserve.get("availableLiquidityUSD"),result.get("availableLiquidityUSD"));
//        assertEquals(reserve.get("totalBorrowsUSD"),result.get("totalBorrowsUSD"));
//        assertEquals(reserve.get("lendingPercentage"),result.get("lendingPercentage"));
//        assertEquals(reserve.get("borrowingPercentage"),result.get("borrowingPercentage"));
//        assertEquals(reserve.get("rewardPercentage"),result.get("rewardPercentage"));
//
//    }

//    private Map<String, BigInteger> reserveCalculation(String symbol, BigInteger totalLiquidity, BigInteger availableLiquidity,
//                                                       BigInteger totalBorrows){
//        BigInteger price = ICX;
//        BigInteger todayRate = BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.TEN);
//        BigInteger reserveDecimals = BigInteger.valueOf(6);
//        BigInteger lendingPercentage;
//        BigInteger borrowingPercentage;
//        BigInteger rewardPercentage = borrowingPercentage.add(lendingPercentage);
//
//        if (symbol.equals("ICX")){
//            price = exaMultiply(todayRate,price);
//            reserveDecimals = BigInteger.valueOf(18);
//        }
//
//        BigInteger totalLiquidityUSD = exaMultiply(convertToExa(totalLiquidity, reserveDecimals), price);
//        BigInteger availableLiquidityUSD = exaMultiply(convertToExa(availableLiquidity,reserveDecimals),price);
//        BigInteger totalBorrowsUSD = exaMultiply(convertToExa(totalBorrows,reserveDecimals),price);
//
//        return Map.of("exchangePrice",price,
//                "sICXRate",todayRate,
//                "totalLiquidityUSD",totalLiquidityUSD,
//                "availableLiquidityUSD",availableLiquidityUSD,
//                "totalBorrowsUSD",totalBorrowsUSD,
//                "lendingPercentage",lendingPercentage,
//                "borrowingPercentage",borrowingPercentage,
//                "rewardPercentage",rewardPercentage);
//    }


    private Map<String, BigInteger> userReserveDataCalculation(String symbol, OMMClient client){
        BigInteger price = ICX;
        BigInteger reserveDecimals = BigInteger.valueOf(18);
        BigInteger currentOTokenBalance = BigInteger.ZERO;
        BigInteger currentBorrowBalance = BigInteger.ZERO;
        BigInteger principalOTokenBalance = BigInteger.ZERO;
        BigInteger principalBorrowBalance = BigInteger.ZERO;
        BigInteger userLiquidityCumulativeIndex = BigInteger.ZERO;
        BigInteger userBorrowCumulativeIndex = BigInteger.ZERO;
        BigInteger liquidityRate = BigInteger.ZERO;
        BigInteger borrowRate = BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100));;


        if (symbol.equals("ICX")){
            price = exaMultiply(price,
                    BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.TEN));
            currentOTokenBalance = ommClient.oICX.balanceOf(client.getAddress());
            principalOTokenBalance = ommClient.oICX.principalBalanceOf(client.getAddress());
            currentBorrowBalance = ommClient.dICX.balanceOf(client.getAddress());
            principalBorrowBalance = ommClient.dICX.principalBalanceOf(client.getAddress());
            userLiquidityCumulativeIndex = ommClient.oICX.getUserLiquidityCumulativeIndex(client.getAddress());
            userBorrowCumulativeIndex = ommClient.dICX.getUserBorrowCumulativeIndex(client.getAddress());
        }

        if (symbol.equals("USDC")){
           reserveDecimals = BigInteger.valueOf(6);
            currentOTokenBalance = ommClient.oIUSDC.balanceOf(client.getAddress());
            principalOTokenBalance = ommClient.oIUSDC.principalBalanceOf(client.getAddress());
            currentBorrowBalance = ommClient.dIUSDC.balanceOf(client.getAddress());
            principalBorrowBalance = ommClient.dIUSDC.principalBalanceOf(client.getAddress());
            userBorrowCumulativeIndex = ommClient.dIUSDC.getUserBorrowCumulativeIndex(client.getAddress());
            userLiquidityCumulativeIndex = ommClient.oIUSDC.getUserLiquidityCumulativeIndex(client.getAddress());

        }


        BigInteger currentOTokenBalanceUSD = exaMultiply(convertToExa(currentOTokenBalance, reserveDecimals), price);
        BigInteger principalOTokenBalanceUSD = exaMultiply(convertToExa(principalOTokenBalance, reserveDecimals), price);
        BigInteger currentBorrowBalanceUSD = exaMultiply(convertToExa(currentBorrowBalance, reserveDecimals), price);
        BigInteger principalBorrowBalanceUSD = exaMultiply(convertToExa(principalBorrowBalance, reserveDecimals), price);

        Map<String, BigInteger> response = new HashMap<>();

        response.put("currentOTokenBalance", currentOTokenBalance);
        response.put("currentOTokenBalanceUSD", currentOTokenBalanceUSD);
        response.put("principalOTokenBalance", principalOTokenBalance);
        response.put("principalOTokenBalanceUSD", principalOTokenBalanceUSD);
        response.put("currentBorrowBalance", currentBorrowBalance);
        response.put("currentBorrowBalanceUSD", currentBorrowBalanceUSD);
        response.put("principalBorrowBalance", principalBorrowBalance);
        response.put("principalBorrowBalanceUSD", principalBorrowBalanceUSD);
        response.put("userLiquidityCumulativeIndex", userLiquidityCumulativeIndex);
        response.put("borrowRate", borrowRate);
        response.put("liquidityRate", liquidityRate);
//        response.put("originationFee", originationFee);
        response.put("userBorrowCumulativeIndex", userBorrowCumulativeIndex);
//        response.put("lastUpdateTimestamp", lastUpdateTimestamp);
        response.put("exchangeRate", price);
        response.put("decimals", reserveDecimals);

        return response;


    }



    public void transfer(OMMClient client, BigInteger amount){
        ommClient.iUSDC.transfer(client.getAddress(),amount.multiply(BigInteger.valueOf(1000_000)),
                new byte[]{});
        System.out.println(client.iUSDC.balanceOf(client.getAddress()));
    }

    private Map<String, BigInteger> reserveDataCalculation(String symbol,BigInteger reserveTotalLiquidity,BigInteger reserveAvailableLiquidity,
                                              BigInteger reserveTotalBorrows){
        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger availableLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger reservePrice = ICX;
        BigInteger reserveDecimals = BigInteger.valueOf(6);

        if (symbol.equals("ICX")){
            reservePrice = exaMultiply(reservePrice,
                    BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.TEN));
        }

        if (symbol.equals("USDC")){
            reserveTotalLiquidity = convertToExa(reserveTotalLiquidity, reserveDecimals);
            reserveAvailableLiquidity = convertToExa(reserveAvailableLiquidity, reserveDecimals);
            reserveTotalBorrows = convertToExa(reserveTotalBorrows, reserveDecimals);
        }

        totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));
        availableLiquidityBalanceUSD = availableLiquidityBalanceUSD.add(exaMultiply(reserveAvailableLiquidity, reservePrice));
        totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply(reserveTotalBorrows, reservePrice));
        totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));


        return Map.of("totalLiquidityBalanceUSD", totalLiquidityBalanceUSD,
                "availableLiquidityBalanceUSD", availableLiquidityBalanceUSD,
                "totalBorrowsBalanceUSD", totalBorrowBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD);
    }

    private void mintToken(){
        BigInteger amount = BigInteger.valueOf(100_000_000_000L).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(),amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(),amount);
    }
    private void depositICX(OMMClient client, BigInteger amount){
        ((LendingPoolScoreClient)client.lendingPool).deposit(amount.multiply(ICX),amount.multiply(ICX));
    }

    private void depositIUSDC(OMMClient client, BigInteger amount){
        byte[] data = createByteArray("deposit",amount,null,null,null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),
                amount.multiply(BigInteger.valueOf(1000_000)),data);
    }

    private void reserveSetup(){
        depositICX(ommClient,BigInteger.valueOf(1));
        depositIUSDC(ommClient,BigInteger.valueOf(10000000));
    }

    private byte[] createByteArray(String methodName, BigInteger value,
                                   @Optional score.Address collateral, @Optional score.Address reserve, @Optional score.Address user) {

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
