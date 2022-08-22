package finance.omm.score.core.finance.omm.score.core.lendingpoolDataProvider.test.unit;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;

import static finance.omm.score.core.lendingpoolDataProvider.AbstractLendingPoolDataProvider.TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LendingPoolDataProviderUnitTest extends AbstractLendingDataProviderTest {

    @Test
    void testName(){
        assertEquals("Omm " + TAG, score.call("name") );
    }

    @Test
    void setterGetters(){

        score.invoke(owner,"setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        String  symbol = (String) score.call("getSymbol",MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        assertEquals("USDC", symbol);

        Executable call = () -> score.invoke(notOwner,"setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        expectErrorMessage(call,"require owner access");
    }

    @Test
    void getReserveAccountData() {
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress(), "OMM");


        doReturn(BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN))
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger availableLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        // icx reserve
        BigInteger totalLiquidity_icx = BigInteger.valueOf(1000).multiply(ICX);
        BigInteger availableLiquidity_icx = BigInteger.valueOf(200).multiply(ICX);
        BigInteger borrows_icx = BigInteger.valueOf(200).multiply(ICX);
        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18),
                "totalLiquidity",totalLiquidity_icx,
                "availableLiquidity",availableLiquidity_icx,
                "totalBorrows",borrows_icx,
                "usageAsCollateralEnabled",true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve.get(0));
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");

//        iusdc reserve
        BigInteger totalLiquidity_iusdc = BigInteger.valueOf(1000).multiply(ICX);
        BigInteger availableLiquidity_iusdc = BigInteger.valueOf(200).multiply(ICX);
        BigInteger borrows_iusdc = BigInteger.valueOf(200).multiply(ICX);
        doReturn(Map.of(
                "decimals", BigInteger.valueOf(6),
                "totalLiquidity", totalLiquidity_iusdc,
                "availableLiquidity", availableLiquidity_iusdc,
                "totalBorrows", borrows_iusdc,
                "usageAsCollateralEnabled", true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve.get(1));
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "USDC", "USD");


        // omm reserve
        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18),
                "totalLiquidity",ICX,
                "availableLiquidity",ICX,
                "totalBorrows",BigInteger.ZERO,
                "usageAsCollateralEnabled",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve.get(2));
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "OMM", "USD");


        Map<String, BigInteger> IUSDC = reserveDataCalculation("USDC", totalLiquidity_iusdc, availableLiquidity_iusdc, borrows_iusdc, true);
        Map<String,BigInteger> icx = reserveDataCalculation("ICX",totalLiquidity_icx,availableLiquidity_icx,borrows_icx,true);
        Map<String,BigInteger> omm = reserveDataCalculation("OMM",ICX,ICX,BigInteger.ZERO,false);


        totalLiquidityBalanceUSD = IUSDC.get("totalLiquidityBalanceUSD").add(icx.get("totalLiquidityBalanceUSD")).add(omm.get("totalLiquidityBalanceUSD"));
        availableLiquidityBalanceUSD = IUSDC.get("availableLiquidityBalanceUSD").add(icx.get("availableLiquidityBalanceUSD")).add(omm.get("availableLiquidityBalanceUSD"));
        totalBorrowBalanceUSD = IUSDC.get("totalBorrowsBalanceUSD").add(icx.get("totalBorrowsBalanceUSD")).add(omm.get("totalBorrowsBalanceUSD"));
        totalCollateralBalanceUSD = IUSDC.get("totalCollateralBalanceUSD").add(icx.get("totalCollateralBalanceUSD")).add(omm.get("totalCollateralBalanceUSD"));


        Map<String, BigInteger> result = (Map<String, BigInteger>) score.call("getReserveAccountData");

        assertEquals(totalLiquidityBalanceUSD,result.get("totalLiquidityBalanceUSD"));
        assertEquals(availableLiquidityBalanceUSD,result.get("availableLiquidityBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD,result.get("totalBorrowsBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD,result.get("totalCollateralBalanceUSD"));
    }

    @Test
    void getUserAccountData_underLyingBalance_compoundedBorrowBalance_zero(){
        Account user = sm.createAccount(100);

        doReturn(BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN))
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");


        // when user has not deposited
        doReturn(Map.of(
                "underlyingBalance", BigInteger.ZERO,
                "compoundedBorrowBalance",BigInteger.ZERO
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user.getAddress());

        Map<String, Object>  result = (Map<String, Object> )score.call("getUserAccountData", user.getAddress());

        assertEquals(BigInteger.ZERO,result.get("totalLiquidityBalanceUSD"));
        assertEquals(BigInteger.ZERO,result.get("totalCollateralBalanceUSD"));
        assertEquals(BigInteger.ZERO,result.get("totalBorrowBalanceUSD"));
        assertEquals(BigInteger.ZERO,result.get("totalFeesUSD"));
        assertEquals(BigInteger.ZERO,result.get("availableBorrowsUSD"));
        assertEquals(BigInteger.ZERO,result.get("currentLtv"));
        assertEquals(BigInteger.ZERO,result.get("currentLiquidationThreshold"));
        assertEquals(BigInteger.ONE.negate(),result.get("healthFactor"));
        assertEquals(BigInteger.ZERO,result.get("borrowingPower"));
        assertEquals(false,result.get("healthFactorBelowThreshold"));
    }

    @Test
    void getUserAccountData_deposited_multiple_reserve(){
        Account user = sm.createAccount(100);

        doReturn(BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN))
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        // when user has deposited ICX
        BigInteger depositedBalance_icx = BigInteger.valueOf(10).multiply(ICX);
        BigInteger compoundedBorrowBalance_icx = BigInteger.ZERO;
        BigInteger originationFee_icx = BigInteger.ZERO;
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_icx,
                "compoundedBorrowBalance",BigInteger.ZERO,
                "originationFee",originationFee_icx
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user.getAddress());

        BigInteger baseLTVasCollateral = BigInteger.valueOf(500000000000000000L);
        BigInteger liquidationThreshold = BigInteger.valueOf(650000000000000000L);
        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18),
                "usageAsCollateralEnabled",true,
                "baseLTVasCollateral",baseLTVasCollateral,
                "liquidationThreshold",liquidationThreshold

        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(0));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "ICX", "USD");


        // when user deposits iusdc
        BigInteger depositedBalance_iusdc = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger compoundedBorrowBalance_iusdc = BigInteger.ZERO;
        BigInteger originationFee_iusdc = BigInteger.ZERO;
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_iusdc,
                "compoundedBorrowBalance",BigInteger.ZERO,
                "originationFee",originationFee_iusdc
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(1), user.getAddress());

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(6),
                "usageAsCollateralEnabled",true,
                "baseLTVasCollateral",baseLTVasCollateral,
                "liquidationThreshold",liquidationThreshold

        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(1));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "USDC", "USD");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        Map<String, Object>  result = (Map<String, Object> )score.call("getUserAccountData", user.getAddress());
        System.out.println("thus "+result.get("totalLiquidityBalanceUSD"));

        Map<String, Object> icx = userAccountDataCalculation("ICX",depositedBalance_icx,compoundedBorrowBalance_icx,
                originationFee_icx,true,baseLTVasCollateral,liquidationThreshold);

        Map<String, Object> iusdc = userAccountDataCalculation("USDC",depositedBalance_iusdc,compoundedBorrowBalance_iusdc,
                originationFee_iusdc,true,baseLTVasCollateral,liquidationThreshold);

        BigInteger totalLiquidityBalanceUSD = ((BigInteger) icx.get("totalLiquidityBalanceUSD")).
                add((BigInteger) iusdc.get("totalLiquidityBalanceUSD"));
        BigInteger totalCollateralBalanceUSD = ((BigInteger) icx.get("totalCollateralBalanceUSD")).add((BigInteger) iusdc.get("totalCollateralBalanceUSD"));
        BigInteger totalBorrowBalanceUSD = ((BigInteger) icx.get("totalBorrowBalanceUSD")).add((BigInteger) iusdc.get("totalBorrowBalanceUSD"));
        BigInteger totalFeesUSD = ((BigInteger) icx.get("totalFeesUSD")).add((BigInteger) iusdc.get("totalFeesUSD"));
        BigInteger currentLtv = ((BigInteger) icx.get("currentLtv")).add((BigInteger) iusdc.get("currentLtv"));
        BigInteger currentLiquidationThreshold = ((BigInteger) icx.get("currentLiquidationThreshold")).add((BigInteger) iusdc.get("currentLiquidationThreshold"));

        BigInteger availableBorrowICX = ((BigInteger) icx.get("availableBorrowsUSD"));
        BigInteger availableBorrowIUSDC = ((BigInteger) iusdc.get("availableBorrowsUSD"));

        BigInteger availableBorrowsUSD = availableBorrow(availableBorrowICX,availableBorrowIUSDC);


        BigInteger healthFactor = healthFactor(totalBorrowBalanceUSD,totalCollateralBalanceUSD,totalFeesUSD,liquidationThreshold);

        boolean healthFactorBelowThreshold = healthFactor.compareTo(ICX) < 0 &&
                (!healthFactor.equals(BigInteger.valueOf(-1)));

        BigInteger borrowingPower=  borrowingPower(totalCollateralBalanceUSD,totalBorrowBalanceUSD,totalFeesUSD,currentLiquidationThreshold);


        assertEquals(totalLiquidityBalanceUSD,result.get("totalLiquidityBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD,result.get("totalCollateralBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD,result.get("totalBorrowBalanceUSD"));
        assertEquals(totalFeesUSD,result.get("totalFeesUSD"));
        assertEquals(availableBorrowsUSD,result.get("availableBorrowsUSD"));
        assertEquals(currentLtv.divide(BigInteger.TWO),result.get("currentLtv"));
        assertEquals(currentLiquidationThreshold.divide(BigInteger.TWO),result.get("currentLiquidationThreshold"));
        assertEquals(healthFactor,result.get("healthFactor"));
        assertEquals(borrowingPower,result.get("borrowingPower"));
        assertEquals(healthFactorBelowThreshold,result.get("healthFactorBelowThreshold"));


    }

    @Test
    public void getUserAccountData_borrowed(){
        Account user = sm.createAccount(100);

        doReturn(BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN))
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        // when user has deposited ICX
        BigInteger depositedBalance_icx = BigInteger.valueOf(100).multiply(ICX);
        BigInteger compoundedBorrowBalance_icx = BigInteger.ZERO;
        BigInteger originationFee_icx = BigInteger.ZERO;
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_icx,
                "compoundedBorrowBalance",compoundedBorrowBalance_icx,
                "originationFee",originationFee_icx
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user.getAddress());

        BigInteger baseLTVasCollateral = BigInteger.valueOf(500000000000000000L);
        BigInteger liquidationThreshold = BigInteger.valueOf(650000000000000000L);
        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18),
                "usageAsCollateralEnabled",true,
                "baseLTVasCollateral",baseLTVasCollateral,
                "liquidationThreshold",liquidationThreshold

        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(0));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "ICX", "USD");


        // when user borrows iusdc and also deposited
        BigInteger depositedBalance_iusdc = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger compoundedBorrowBalance_iusdc = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger originationFee_iusdc =BigInteger.valueOf(100_000);
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_iusdc,
                "compoundedBorrowBalance",compoundedBorrowBalance_iusdc,
                "originationFee",originationFee_iusdc
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(1), user.getAddress());

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(6),
                "usageAsCollateralEnabled",true,
                "baseLTVasCollateral",baseLTVasCollateral,
                "liquidationThreshold",liquidationThreshold

        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(1));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "USDC", "USD");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        Map<String, Object>  result = (Map<String, Object> )score.call("getUserAccountData", user.getAddress());


        Map<String, Object> icx = userAccountDataCalculation("ICX",depositedBalance_icx,compoundedBorrowBalance_icx,
                originationFee_icx,true,baseLTVasCollateral,liquidationThreshold);

        Map<String, Object> iusdc = userAccountDataCalculation("USDC",depositedBalance_iusdc,compoundedBorrowBalance_iusdc,
                originationFee_iusdc,true,baseLTVasCollateral,liquidationThreshold);


        BigInteger totalLiquidityBalanceUSD = ((BigInteger) icx.get("totalLiquidityBalanceUSD")).add((BigInteger) iusdc.get("totalLiquidityBalanceUSD"));
        BigInteger totalCollateralBalanceUSD = ((BigInteger) icx.get("totalCollateralBalanceUSD")).add((BigInteger) iusdc.get("totalCollateralBalanceUSD"));
        BigInteger totalBorrowBalanceUSD = ((BigInteger) icx.get("totalBorrowBalanceUSD")).add((BigInteger) iusdc.get("totalBorrowBalanceUSD"));
        BigInteger totalFeesUSD = ((BigInteger) icx.get("totalFeesUSD")).add((BigInteger) iusdc.get("totalFeesUSD"));
        BigInteger currentLtv = ((BigInteger) icx.get("currentLtv")).add((BigInteger) iusdc.get("currentLtv"));
        BigInteger currentLiquidationThreshold = ((BigInteger) icx.get("currentLiquidationThreshold")).add((BigInteger) iusdc.get("currentLiquidationThreshold"));

        BigInteger availableBorrowICX = ((BigInteger) icx.get("availableBorrowsUSD"));
        BigInteger availableBorrowIUSDC = ((BigInteger) iusdc.get("availableBorrowsUSD"));

        BigInteger availableBorrowsUSD = availableBorrow(availableBorrowICX,availableBorrowIUSDC);


        BigInteger healthFactor = healthFactor(totalBorrowBalanceUSD,totalCollateralBalanceUSD,totalFeesUSD,liquidationThreshold);

        boolean healthFactorBelowThreshold = healthFactor.compareTo(ICX) < 0 &&
                (!healthFactor.equals(BigInteger.valueOf(-1)));

        BigInteger borrowingPower=  borrowingPower(totalCollateralBalanceUSD,totalBorrowBalanceUSD,totalFeesUSD,currentLiquidationThreshold);



        assertEquals(totalLiquidityBalanceUSD,result.get("totalLiquidityBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD,result.get("totalCollateralBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD,result.get("totalBorrowBalanceUSD"));
        assertEquals(totalFeesUSD,result.get("totalFeesUSD"));
        assertEquals(availableBorrowsUSD,result.get("availableBorrowsUSD"));
        assertEquals(currentLtv.divide(BigInteger.TWO),result.get("currentLtv"));
        assertEquals(currentLiquidationThreshold.divide(BigInteger.TWO),result.get("currentLiquidationThreshold"));
        assertEquals(healthFactor,result.get("healthFactor"));
        assertEquals(borrowingPower,result.get("borrowingPower"));
        assertEquals(healthFactorBelowThreshold,result.get("healthFactorBelowThreshold"));

    }


    @Test
    public void userReserveData_icx_reserve(){

    }





}
