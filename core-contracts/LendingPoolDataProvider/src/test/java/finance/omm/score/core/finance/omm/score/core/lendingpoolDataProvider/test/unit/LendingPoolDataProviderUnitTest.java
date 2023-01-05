package finance.omm.score.core.finance.omm.score.core.lendingpoolDataProvider.test.unit;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static finance.omm.score.core.lendingpoolDataProvider.AbstractLendingPoolDataProvider.TAG;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class LendingPoolDataProviderUnitTest extends AbstractLendingDataProviderTest {

    @Test
    void testName() {
        assertEquals("Omm " + TAG, score.call("name"));
    }

    @Test
    void setterGetters() {

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        String symbol = (String) score.call("getSymbol", MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        assertEquals("USDC", symbol);

        Executable call = () -> score.invoke(notOwner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        expectErrorMessage(call, "Not an owner");
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
                "decimals", BigInteger.valueOf(18),
                "totalLiquidity", totalLiquidity_icx,
                "availableLiquidity", availableLiquidity_icx,
                "totalBorrows", borrows_icx,
                "usageAsCollateralEnabled", true
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
                "decimals", BigInteger.valueOf(18),
                "totalLiquidity", ICX,
                "availableLiquidity", ICX,
                "totalBorrows", BigInteger.ZERO,
                "usageAsCollateralEnabled", false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve.get(2));
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "OMM", "USD");

        Map<String, BigInteger> IUSDC = reserveDataCalculation("USDC", totalLiquidity_iusdc,
                availableLiquidity_iusdc, borrows_iusdc, true);
        Map<String, BigInteger> icx = reserveDataCalculation("ICX", totalLiquidity_icx,
                availableLiquidity_icx, borrows_icx, true);
        Map<String, BigInteger> omm = reserveDataCalculation("OMM", ICX, ICX, BigInteger.ZERO, false);

        totalLiquidityBalanceUSD = IUSDC.get("totalLiquidityBalanceUSD").add(icx.get("totalLiquidityBalanceUSD"))
                .add(omm.get("totalLiquidityBalanceUSD"));
        availableLiquidityBalanceUSD = IUSDC.get("availableLiquidityBalanceUSD")
                .add(icx.get("availableLiquidityBalanceUSD")).add(omm.get("availableLiquidityBalanceUSD"));
        totalBorrowBalanceUSD = IUSDC.get("totalBorrowsBalanceUSD").add(icx.get("totalBorrowsBalanceUSD"))
                .add(omm.get("totalBorrowsBalanceUSD"));
        totalCollateralBalanceUSD = IUSDC.get("totalCollateralBalanceUSD").add(icx.get("totalCollateralBalanceUSD"))
                .add(omm.get("totalCollateralBalanceUSD"));

        Map<String, BigInteger> result = (Map<String, BigInteger>) score.call("getReserveAccountData");

        assertEquals(totalLiquidityBalanceUSD, result.get("totalLiquidityBalanceUSD"));
        assertEquals(availableLiquidityBalanceUSD, result.get("availableLiquidityBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD, result.get("totalBorrowsBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD, result.get("totalCollateralBalanceUSD"));
    }

    @Test
    void getUserAccountData_underLyingBalance_compoundedBorrowBalance_zero() {
        Account user = sm.createAccount(100);

        doReturn(BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN))
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        // when user has not deposited
        doReturn(Map.of(
                "underlyingBalance", BigInteger.ZERO,
                "compoundedBorrowBalance", BigInteger.ZERO
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveDataProxy", reserve.get(0), user.getAddress());

        Map<String, Object> result = (Map<String, Object>) score.call("getUserAccountData", user.getAddress());

        assertEquals(BigInteger.ZERO, result.get("totalLiquidityBalanceUSD"));
        assertEquals(BigInteger.ZERO, result.get("totalCollateralBalanceUSD"));
        assertEquals(BigInteger.ZERO, result.get("totalBorrowBalanceUSD"));
        assertEquals(BigInteger.ZERO, result.get("totalFeesUSD"));
        assertEquals(BigInteger.ZERO, result.get("availableBorrowsUSD"));
        assertEquals(BigInteger.ZERO, result.get("currentLtv"));
        assertEquals(BigInteger.ZERO, result.get("currentLiquidationThreshold"));
        assertEquals(BigInteger.ONE.negate(), result.get("healthFactor"));
        assertEquals(BigInteger.ZERO, result.get("borrowingPower"));
        assertEquals(false, result.get("healthFactorBelowThreshold"));
    }

    @Test
    void getUserAccountData_deposited_multiple_reserve() {
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

        BigInteger baseLTVasCollateral = BigInteger.valueOf(500000000000000000L);
        BigInteger liquidationThreshold = BigInteger.valueOf(650000000000000000L);
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_icx,
                "compoundedBorrowBalance", BigInteger.ZERO,
                "originationFee", originationFee_icx,
                "decimals", BigInteger.valueOf(18),
                "usageAsCollateralEnabled", true,
                "baseLTVasCollateral", baseLTVasCollateral,
                "liquidationThreshold", liquidationThreshold
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveDataProxy", reserve.get(0), user.getAddress());


//        doReturn(Map.of(
//                "decimals", BigInteger.valueOf(18),
//                "usageAsCollateralEnabled", true,
//                "baseLTVasCollateral", baseLTVasCollateral,
//                "liquidationThreshold", liquidationThreshold
//
//        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
//                "getReserveConfiguration", reserve.get(0));
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
                "compoundedBorrowBalance", BigInteger.ZERO,
                "originationFee", originationFee_iusdc,
                "decimals", BigInteger.valueOf(6),
                "usageAsCollateralEnabled", true,
                "baseLTVasCollateral", baseLTVasCollateral,
                "liquidationThreshold", liquidationThreshold
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveDataProxy", reserve.get(1), user.getAddress());

//        doReturn(Map.of(
//                "decimals", BigInteger.valueOf(6),
//                "usageAsCollateralEnabled", true,
//                "baseLTVasCollateral", baseLTVasCollateral,
//                "liquidationThreshold", liquidationThreshold
//
//        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
//                "getReserveConfiguration", reserve.get(1));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "USDC", "USD");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserAccountData", user.getAddress());

        Map<String, Object> icx = userAccountDataCalculation("ICX", depositedBalance_icx, compoundedBorrowBalance_icx,
                originationFee_icx, true, baseLTVasCollateral, liquidationThreshold);

        Map<String, Object> iusdc = userAccountDataCalculation("USDC", depositedBalance_iusdc,
                compoundedBorrowBalance_iusdc,
                originationFee_iusdc, true, baseLTVasCollateral, liquidationThreshold);

        BigInteger totalLiquidityBalanceUSD = ((BigInteger) icx.get("totalLiquidityBalanceUSD")).
                add((BigInteger) iusdc.get("totalLiquidityBalanceUSD"));
        BigInteger totalCollateralBalanceUSD = ((BigInteger) icx.get("totalCollateralBalanceUSD"))
                .add((BigInteger) iusdc.get("totalCollateralBalanceUSD"));
        BigInteger totalBorrowBalanceUSD = ((BigInteger) icx.get("totalBorrowBalanceUSD"))
                .add((BigInteger) iusdc.get("totalBorrowBalanceUSD"));
        BigInteger totalFeesUSD = ((BigInteger) icx.get("totalFeesUSD")).add((BigInteger) iusdc.get("totalFeesUSD"));
        BigInteger currentLtv = ((BigInteger) icx.get("currentLtv")).add((BigInteger) iusdc.get("currentLtv"));
        BigInteger currentLiquidationThreshold = ((BigInteger) icx.get("currentLiquidationThreshold"))
                .add((BigInteger) iusdc.get("currentLiquidationThreshold"));

        BigInteger availableBorrowICX = ((BigInteger) icx.get("availableBorrowsUSD"));
        BigInteger availableBorrowIUSDC = ((BigInteger) iusdc.get("availableBorrowsUSD"));

        BigInteger availableBorrowsUSD = availableBorrow(availableBorrowICX, availableBorrowIUSDC);

        BigInteger healthFactor = healthFactor(totalBorrowBalanceUSD, totalCollateralBalanceUSD,
                totalFeesUSD, liquidationThreshold);

        boolean healthFactorBelowThreshold = healthFactor.compareTo(ICX) < 0 &&
                (!healthFactor.equals(BigInteger.valueOf(-1)));

        BigInteger borrowingPower = borrowingPower(totalCollateralBalanceUSD, totalBorrowBalanceUSD,
                totalFeesUSD, currentLiquidationThreshold);

        assertEquals(totalLiquidityBalanceUSD, result.get("totalLiquidityBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD, result.get("totalCollateralBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD, result.get("totalBorrowBalanceUSD"));
        assertEquals(totalFeesUSD, result.get("totalFeesUSD"));
        assertEquals(availableBorrowsUSD, result.get("availableBorrowsUSD"));
        assertEquals(currentLtv.divide(BigInteger.TWO), result.get("currentLtv"));
        assertEquals(currentLiquidationThreshold.divide(BigInteger.TWO), result.get("currentLiquidationThreshold"));
        assertEquals(healthFactor, result.get("healthFactor"));
        assertEquals(borrowingPower, result.get("borrowingPower"));
        assertEquals(healthFactorBelowThreshold, result.get("healthFactorBelowThreshold"));


    }

    @Test
    public void getUserAccountData_borrowed() {
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

        BigInteger baseLTVasCollateral = BigInteger.valueOf(500000000000000000L);
        BigInteger liquidationThreshold = BigInteger.valueOf(650000000000000000L);
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_icx,
                "compoundedBorrowBalance", compoundedBorrowBalance_icx,
                "originationFee", originationFee_icx,
                "decimals",BigInteger.valueOf(18),
                "usageAsCollateralEnabled", true,
                "baseLTVasCollateral", baseLTVasCollateral,
                "liquidationThreshold", liquidationThreshold
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveDataProxy", reserve.get(0), user.getAddress());

//        BigInteger baseLTVasCollateral = BigInteger.valueOf(500000000000000000L);
//        BigInteger liquidationThreshold = BigInteger.valueOf(650000000000000000L);
//        doReturn(Map.of(
//                "decimals", BigInteger.valueOf(18),
//                "usageAsCollateralEnabled", true,
//                "baseLTVasCollateral", baseLTVasCollateral,
//                "liquidationThreshold", liquidationThreshold
//
//        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
//                "getReserveConfiguration", reserve.get(0));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "ICX", "USD");

        // when user borrows iusdc and also deposited
        BigInteger depositedBalance_iusdc = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger compoundedBorrowBalance_iusdc = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger originationFee_iusdc = BigInteger.valueOf(100_000);
        doReturn(Map.of(
                "underlyingBalance", depositedBalance_iusdc,
                "compoundedBorrowBalance", compoundedBorrowBalance_iusdc,
                "originationFee", originationFee_iusdc,
                "decimals", BigInteger.valueOf(6),
                "usageAsCollateralEnabled", true,
                "baseLTVasCollateral", baseLTVasCollateral,
                "liquidationThreshold", liquidationThreshold
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveDataProxy", reserve.get(1), user.getAddress());

//        doReturn(Map.of(
//                "decimals", BigInteger.valueOf(6),
//                "usageAsCollateralEnabled", true,
//                "baseLTVasCollateral", baseLTVasCollateral,
//                "liquidationThreshold", liquidationThreshold
//
//        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
//                "getReserveConfiguration", reserve.get(1));
        doReturn(ICX)
                .when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                        "USDC", "USD");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserAccountData", user.getAddress());

        Map<String, Object> icx = userAccountDataCalculation("ICX", depositedBalance_icx, compoundedBorrowBalance_icx,
                originationFee_icx, true, baseLTVasCollateral, liquidationThreshold);

        Map<String, Object> iusdc = userAccountDataCalculation("USDC", depositedBalance_iusdc,
                compoundedBorrowBalance_iusdc,
                originationFee_iusdc, true, baseLTVasCollateral, liquidationThreshold);

        BigInteger totalLiquidityBalanceUSD = ((BigInteger) icx.get("totalLiquidityBalanceUSD"))
                .add((BigInteger) iusdc.get("totalLiquidityBalanceUSD"));
        BigInteger totalCollateralBalanceUSD = ((BigInteger) icx.get("totalCollateralBalanceUSD"))
                .add((BigInteger) iusdc.get("totalCollateralBalanceUSD"));
        BigInteger totalBorrowBalanceUSD = ((BigInteger) icx.get("totalBorrowBalanceUSD"))
                .add((BigInteger) iusdc.get("totalBorrowBalanceUSD"));
        BigInteger totalFeesUSD = ((BigInteger) icx.get("totalFeesUSD")).add((BigInteger) iusdc.get("totalFeesUSD"));
        BigInteger currentLtv = ((BigInteger) icx.get("currentLtv")).add((BigInteger) iusdc.get("currentLtv"));
        BigInteger currentLiquidationThreshold = ((BigInteger) icx.get("currentLiquidationThreshold"))
                .add((BigInteger) iusdc.get("currentLiquidationThreshold"));

        BigInteger availableBorrowICX = ((BigInteger) icx.get("availableBorrowsUSD"));
        BigInteger availableBorrowIUSDC = ((BigInteger) iusdc.get("availableBorrowsUSD"));

        BigInteger availableBorrowsUSD = availableBorrow(availableBorrowICX, availableBorrowIUSDC);

        BigInteger healthFactor = healthFactor(totalBorrowBalanceUSD, totalCollateralBalanceUSD,
                totalFeesUSD, liquidationThreshold);

        boolean healthFactorBelowThreshold = healthFactor.compareTo(ICX) < 0 &&
                (!healthFactor.equals(BigInteger.valueOf(-1)));

        BigInteger borrowingPower = borrowingPower(totalCollateralBalanceUSD, totalBorrowBalanceUSD,
                totalFeesUSD, currentLiquidationThreshold);

        assertEquals(totalLiquidityBalanceUSD, result.get("totalLiquidityBalanceUSD"));
        assertEquals(totalCollateralBalanceUSD, result.get("totalCollateralBalanceUSD"));
        assertEquals(totalBorrowBalanceUSD, result.get("totalBorrowBalanceUSD"));
        assertEquals(totalFeesUSD, result.get("totalFeesUSD"));
        assertEquals(availableBorrowsUSD, result.get("availableBorrowsUSD"));
        assertEquals(currentLtv.divide(BigInteger.TWO), result.get("currentLtv"));
        assertEquals(currentLiquidationThreshold.divide(BigInteger.TWO), result.get("currentLiquidationThreshold"));
        assertEquals(healthFactor, result.get("healthFactor"));
        assertEquals(borrowingPower, result.get("borrowingPower"));
        assertEquals(healthFactorBelowThreshold, result.get("healthFactorBelowThreshold"));

    }


    @Test
    /*
    user has deposited 100 ICX an borrowed 20ICX
     */
    public void userReserveData_icx_reserve() {
        Address icx_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Account user = sm.createAccount();

        Address oTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        Address dTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.dICX).getAddress();
        BigInteger borrowRate = BigInteger.valueOf(10).divide(BigInteger.valueOf(100));
        BigInteger decimals = BigInteger.valueOf(18);
        BigInteger liquidityRate = BigInteger.valueOf(7).divide(BigInteger.valueOf(1000));
        BigInteger originationFee = BigInteger.valueOf(1).divide(BigInteger.valueOf(100));
        BigInteger lastUpdateTimestamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger currentOTokenBalance = BigInteger.valueOf(101).multiply(ICX);
        BigInteger principalOTokenBalance = BigInteger.valueOf(100).multiply(ICX);
        BigInteger userLiquidityCumulativeIndex = BigInteger.valueOf((long) 1.032f);
        BigInteger principalBorrowBalance = BigInteger.valueOf(20).multiply(ICX);
        BigInteger currentBorrowBalance = BigInteger.valueOf(21).multiply(ICX);
        BigInteger userBorrowCumulativeIndex = BigInteger.valueOf((long) 1.0356f);
        BigInteger price = BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN); // 0.1

        doReturn(Map.of(
                "oTokenAddress", oTokenAddr,
                "dTokenAddress", dTokenAddr,
                "borrowRate", borrowRate,
                "decimals", decimals,
                "liquidityRate", liquidityRate)
        ).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", icx_reserve);

        doReturn(Map.of(
                "originationFee", originationFee,
                "lastUpdateTimestamp", lastUpdateTimestamp
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserReserveData", icx_reserve, user.getAddress());

        doReturn(currentOTokenBalance).when(scoreSpy).
                call(BigInteger.class, oTokenAddr, "balanceOf", user.getAddress());
        doReturn(principalOTokenBalance).when(scoreSpy).
                call(BigInteger.class, oTokenAddr, "principalBalanceOf", user.getAddress());
        doReturn(userLiquidityCumulativeIndex).when(scoreSpy)
                .call(BigInteger.class, oTokenAddr, "getUserLiquidityCumulativeIndex", user.getAddress());
        doReturn(principalBorrowBalance).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "principalBalanceOf", user.getAddress());
        doReturn(currentBorrowBalance).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "balanceOf", user.getAddress());
        doReturn(userBorrowCumulativeIndex).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "getUserBorrowCumulativeIndex", user.getAddress());

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");

        doReturn(price)
                .when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        BigInteger currentOTokenBalanceUSD = exaMultiply(convertToExa(currentOTokenBalance, decimals), price);
        BigInteger principalOTokenBalanceUSD = exaMultiply(convertToExa(principalOTokenBalance, decimals), price);
        BigInteger currentBorrowBalanceUSD = exaMultiply(convertToExa(currentBorrowBalance, decimals), price);
        BigInteger principalBorrowBalanceUSD = exaMultiply(convertToExa(principalBorrowBalance, decimals), price);

        Map<String, BigInteger> result = (Map<String, BigInteger>) score.call("getUserReserveData",
                icx_reserve, user.getAddress());

        assertEquals(currentOTokenBalance, result.get("currentOTokenBalance"));
        assertEquals(currentOTokenBalanceUSD, result.get("currentOTokenBalanceUSD"));
        assertEquals(principalOTokenBalance, result.get("principalOTokenBalance"));
        assertEquals(principalOTokenBalanceUSD, result.get("principalOTokenBalanceUSD"));
        assertEquals(currentBorrowBalance, result.get("currentBorrowBalance"));
        assertEquals(currentBorrowBalanceUSD, result.get("currentBorrowBalanceUSD"));
        assertEquals(principalBorrowBalance, result.get("principalBorrowBalance"));
        assertEquals(principalBorrowBalanceUSD, result.get("principalBorrowBalanceUSD"));
        assertEquals(userLiquidityCumulativeIndex, result.get("userLiquidityCumulativeIndex"));
        assertEquals(borrowRate, result.get("borrowRate"));
        assertEquals(liquidityRate, result.get("liquidityRate"));
        assertEquals(originationFee, result.get("originationFee"));
        assertEquals(userBorrowCumulativeIndex, result.get("userBorrowCumulativeIndex"));
        assertEquals(lastUpdateTimestamp, result.get("lastUpdateTimestamp"));
        assertEquals(price, result.get("exchangeRate"));
        assertEquals(decimals, result.get("decimals"));
        assertEquals(price, result.get("sICXRate"));
    }

    @Test
    /*
    user has deposited 100 IUSDC an borrowed 23 IUSDC
     */
    public void userReserveData_iusdc_reserve() {
        Address iusdc_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress();
        Account user = sm.createAccount();

        Address oTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.oIUSDC).getAddress();
        Address dTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.dIUSDC).getAddress();
        BigInteger borrowRate = BigInteger.valueOf(10).divide(BigInteger.valueOf(100));
        BigInteger decimals = BigInteger.valueOf(6);
        BigInteger liquidityRate = BigInteger.valueOf(2).divide(BigInteger.valueOf(1000));
        BigInteger originationFee = BigInteger.valueOf(1).divide(BigInteger.valueOf(100));
        BigInteger lastUpdateTimestamp = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        BigInteger currentOTokenBalance = BigInteger.valueOf(101).multiply(BigInteger.valueOf(1000_000));
        BigInteger principalOTokenBalance = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000));
        BigInteger userLiquidityCumulativeIndex = BigInteger.valueOf((long) 1.032f);
        BigInteger principalBorrowBalance = BigInteger.valueOf(23).multiply(BigInteger.valueOf(1000_000));
        BigInteger currentBorrowBalance = BigInteger.valueOf(24).multiply(BigInteger.valueOf(1000_000));
        BigInteger userBorrowCumulativeIndex = BigInteger.valueOf((long) 1.0356f);

        doReturn(Map.of(
                "oTokenAddress", oTokenAddr,
                "dTokenAddress", dTokenAddr,
                "borrowRate", borrowRate,
                "decimals", decimals,
                "liquidityRate", liquidityRate)
        ).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", iusdc_reserve);

        doReturn(Map.of(
                "originationFee", originationFee,
                "lastUpdateTimestamp", lastUpdateTimestamp
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserReserveData", iusdc_reserve, user.getAddress());

        doReturn(currentOTokenBalance).when(scoreSpy).
                call(BigInteger.class, oTokenAddr, "balanceOf", user.getAddress());
        doReturn(principalOTokenBalance).when(scoreSpy).
                call(BigInteger.class, oTokenAddr, "principalBalanceOf", user.getAddress());
        doReturn(userLiquidityCumulativeIndex).when(scoreSpy)
                .call(BigInteger.class, oTokenAddr, "getUserLiquidityCumulativeIndex", user.getAddress());
        doReturn(principalBorrowBalance).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "principalBalanceOf", user.getAddress());
        doReturn(currentBorrowBalance).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "balanceOf", user.getAddress());
        doReturn(userBorrowCumulativeIndex).when(scoreSpy).
                call(BigInteger.class, dTokenAddr, "getUserBorrowCumulativeIndex", user.getAddress());

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "USDC", "USD");

        BigInteger currentOTokenBalanceUSD = exaMultiply(convertToExa(currentOTokenBalance, decimals), ICX);
        BigInteger principalOTokenBalanceUSD = exaMultiply(convertToExa(principalOTokenBalance, decimals), ICX);
        BigInteger currentBorrowBalanceUSD = exaMultiply(convertToExa(currentBorrowBalance, decimals), ICX);
        BigInteger principalBorrowBalanceUSD = exaMultiply(convertToExa(principalBorrowBalance, decimals), ICX);

        Map<String, BigInteger> result = (Map<String, BigInteger>) score.call("getUserReserveData",
                iusdc_reserve, user.getAddress());

        assertEquals(currentOTokenBalance, result.get("currentOTokenBalance"));
        assertEquals(currentOTokenBalanceUSD, result.get("currentOTokenBalanceUSD"));
        assertEquals(principalOTokenBalance, result.get("principalOTokenBalance"));
        assertEquals(principalOTokenBalanceUSD, result.get("principalOTokenBalanceUSD"));
        assertEquals(currentBorrowBalance, result.get("currentBorrowBalance"));
        assertEquals(currentBorrowBalanceUSD, result.get("currentBorrowBalanceUSD"));
        assertEquals(principalBorrowBalance, result.get("principalBorrowBalance"));
        assertEquals(principalBorrowBalanceUSD, result.get("principalBorrowBalanceUSD"));
        assertEquals(userLiquidityCumulativeIndex, result.get("userLiquidityCumulativeIndex"));
        assertEquals(borrowRate, result.get("borrowRate"));
        assertEquals(liquidityRate, result.get("liquidityRate"));
        assertEquals(originationFee, result.get("originationFee"));
        assertEquals(userBorrowCumulativeIndex, result.get("userBorrowCumulativeIndex"));
        assertEquals(lastUpdateTimestamp, result.get("lastUpdateTimestamp"));
        assertEquals(ICX, result.get("exchangeRate"));
        assertEquals(decimals, result.get("decimals"));
        assertNull(result.get("sICXRate"));
    }

    @Test
    public void balanceDecreaseAllowed_colleteral_not_enabled() {
        Address icx_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Account user = sm.createAccount();

        doReturn(Map.of(
                "liquidationThreshold", BigInteger.valueOf(650000000000000000L),
                "usageAsCollateralEnabled", false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", icx_reserve);

        assertTrue((Boolean) score.call("balanceDecreaseAllowed", icx_reserve, user.getAddress(),
                BigInteger.valueOf(100)));
    }

    @Test
    /*
    user deposited 50, borrowed 20 and tried to redeem 20
     */
    public void balanceDecreaseAllowed_iusdc_returns_false() {
        Address iusdc_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress();
        Account user = sm.createAccount();

        doReturn(Map.of(
                "liquidationThreshold", BigInteger.valueOf(650000000000000000L),
                "usageAsCollateralEnabled", true,
                "decimals", BigInteger.valueOf(6)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", iusdc_reserve);

        doReturn(Map.of(
                "totalCollateralBalanceUSD", BigInteger.valueOf(50).multiply(ICX),
                "totalBorrowBalanceUSD", BigInteger.valueOf(20).multiply(ICX),
                "totalFeesUSD", BigInteger.valueOf(1).multiply(ICX),
                "currentLiquidationThreshold", BigInteger.valueOf(500000000000000000L)
        )).when(scoreSpy).getUserAccountData(user.getAddress());

        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class,
                Contracts.PRICE_ORACLE, "get_reference_data", "USDC", "USD");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        assertFalse((boolean) score.call("balanceDecreaseAllowed", iusdc_reserve, user.getAddress(),
                BigInteger.valueOf(20).multiply(ICX)));
    }

    @Test
    public void calculateCollateralNeededUSD() {
        Address iusdc_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress();
        BigInteger amount = BigInteger.valueOf(50).multiply(ICX);
        BigInteger fee = BigInteger.ONE.multiply(BigInteger.valueOf(1000000L));
        BigInteger userCurrentBorrow = BigInteger.valueOf(20).multiply(ICX);
        BigInteger userCurrentFee = BigInteger.ONE.multiply(BigInteger.valueOf(10000L));
        BigInteger userCurrentLTV = BigInteger.valueOf(450000000000000000L);
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "USDC", "USD");
        doReturn(Map.of(
                "decimals", BigInteger.valueOf(6)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", iusdc_reserve);

        BigInteger expectedCollateralNeededUSD = collateralNeededUSDCalculation("USDC", amount,
                userCurrentBorrow, userCurrentFee, userCurrentLTV);
        BigInteger actualCollateralNeededUSD = (BigInteger) score.call("calculateCollateralNeededUSD",
                iusdc_reserve, amount, fee, userCurrentBorrow,
                userCurrentFee, userCurrentLTV);

        assertEquals(expectedCollateralNeededUSD, actualCollateralNeededUSD);
    }


    @Test
    public void userALlReserveData() {
        Account user = sm.createAccount(100);

        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        doReturn(Map.of()).when(scoreSpy).getUserReserveData(reserve.get(0), user.getAddress());
        doReturn(Map.of()).when(scoreSpy).getUserReserveData(reserve.get(1), user.getAddress());
        score.call("getUserAllReserveData", user.getAddress());
    }

    @Test
    /*
    user has not taken any borrows initially
     */
    public void userLiquidationData_no_borrows() {
        Address user = sm.createAccount().getAddress();
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

//        doReturn(Map.of("healthFactorBelowThreshold", false)).when(scoreSpy).getUserAccountData(user);

        Map<String, Object> uBasicReserveData = new java.util.HashMap<>();
        uBasicReserveData.put("compoundedBorrowBalance", BigInteger.ZERO);
        uBasicReserveData.put("underlyingBalance", BigInteger.valueOf(100).multiply(ICX));
        uBasicReserveData.put("originationFee", ICX.divide(BigInteger.valueOf(1000)));

        doReturn(uBasicReserveData).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user);


        Map<String, Object> config = new java.util.HashMap<>();
        config.put("decimals", BigInteger.valueOf(18));
        config.put("usageAsCollateralEnabled", true);
        config.put("baseLTVasCollateral", BigInteger.valueOf(50).multiply(ICX).divide(BigInteger.valueOf(100)));
        config.put("liquidationThreshold", BigInteger.valueOf(65).multiply(ICX).divide(BigInteger.valueOf(100)));
        doReturn(config).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve.get(0));

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");

        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");
        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserLiquidationData", user);

        Map<String, Map<String, BigInteger>> result_borrow = (Map<String, Map<String, BigInteger>>) result.get(
                "borrows");
        Map<String, Map<String, BigInteger>> result_collateral = (Map<String, Map<String, BigInteger>>) result.get(
                "collaterals");

        Map<String, Object> expected = liquidationDataCalculation("ICX", BigInteger.ZERO,
                BigInteger.valueOf(100).multiply(ICX), BigInteger.ZERO);
        Map<String, Map<String, BigInteger>> expected_collateral = (Map<String, Map<String, BigInteger>>) expected.get(
                "collaterals");

        assertEquals(result.get("badDebt"), BigInteger.ZERO);
        assertEquals(result_borrow.get("ICX"), null);
        assertEquals(result_collateral.get("ICX"), expected_collateral.get("ICX"));
        assertEquals(result_collateral.get("ICX").get("underlyingBalance"), BigInteger.valueOf(100).multiply(ICX));
        assertEquals(result_collateral.get("ICX").get("underlyingBalanceUSD"),
                expected_collateral.get("ICX").get("underlyingBalanceUSD"));

    }

    @Test
    /*
    user has borrow of 30ICX initially with 100 ICX as collateral and no badDebt
     */
    public void userLiquidationData_health_factor_above_threshold() {
        Address user = sm.createAccount().getAddress();
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        doReturn(Map.of("healthFactorBelowThreshold", false)).when(scoreSpy).getUserAccountData(user);

        Map<String, Object> uBasicReserveData = new java.util.HashMap<>();
        uBasicReserveData.put("compoundedBorrowBalance", BigInteger.valueOf(30).multiply(ICX));
        uBasicReserveData.put("underlyingBalance", BigInteger.valueOf(100).multiply(ICX));
        uBasicReserveData.put("originationFee", ICX.divide(BigInteger.valueOf(1000)));

        doReturn(uBasicReserveData).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user);


        Map<String, Object> config = new java.util.HashMap<>();
        config.put("decimals", BigInteger.valueOf(18));
        config.put("usageAsCollateralEnabled", true);
        config.put("baseLTVasCollateral", BigInteger.valueOf(50).multiply(ICX).divide(BigInteger.valueOf(100)));
        config.put("liquidationThreshold", BigInteger.valueOf(65).multiply(ICX).divide(BigInteger.valueOf(100)));
        doReturn(config).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve.get(0));

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");

        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");
        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserLiquidationData", user);
        Map<String, Map<String, BigInteger>> resultBorrows = (Map<String, Map<String, BigInteger>>) result.get("borrows");
        Map<String, Map<String, BigInteger>> resultCollateral = (Map<String, Map<String, BigInteger>>) result.get("collaterals");


        Map<String, Object> expected = liquidationDataCalculation("ICX", BigInteger.valueOf(30).multiply(ICX),
                BigInteger.valueOf(100).multiply(ICX), BigInteger.ZERO);
        Map<String, Map<String, BigInteger>> expectedBorrows = (Map<String, Map<String, BigInteger>>) expected.get("borrows");
        Map<String, Map<String, BigInteger>> expectedCollateral = (Map<String, Map<String, BigInteger>>) expected.get("collaterals");

        assertEquals(result.get("badDebt"), expected.get("badDebt"));
        assertEquals(resultBorrows.get("ICX"), expectedBorrows.get("ICX"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalanceUSD"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalanceUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidateUSD"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidateUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidate"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidate"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalance"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalance"));
        assertEquals(resultCollateral.get("ICX"), expectedCollateral.get("ICX"));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalance"), BigInteger.valueOf(100).multiply(ICX));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalanceUSD"),
                expectedCollateral.get("ICX").get("underlyingBalanceUSD"));

    }

    @Test
    /*
     user has borrow of 30ICX initially with 100 ICX as collateral and badDebt of 13ICX
     */
    public void userLiquidationData_bad_debt() {
        Address user = sm.createAccount().getAddress();
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        BigInteger totalBorrowBalanceUSD = BigInteger.valueOf(70).multiply(ICX);
        BigInteger totalFeesUSD = ICX.divide(BigInteger.valueOf(10000));
        BigInteger totalCollateralBalanceUSD = BigInteger.valueOf(100).multiply(ICX);
        BigInteger currentLtv = BigInteger.valueOf(500000000000000000L);

        BigInteger badDebt = BigInteger.valueOf(13).multiply(ICX);
        doReturn(badDebt).when(scoreSpy).
                call(BigInteger.class, Contracts.LIQUIDATION_MANAGER, "calculateBadDebt",
                        totalBorrowBalanceUSD, totalFeesUSD, totalCollateralBalanceUSD, currentLtv
                );

        Map<String, Object> uBasicReserveData = new java.util.HashMap<>();
        uBasicReserveData.put("compoundedBorrowBalance", totalBorrowBalanceUSD.multiply(BigInteger.TEN));
        uBasicReserveData.put("underlyingBalance", totalCollateralBalanceUSD.multiply(BigInteger.TEN));
        uBasicReserveData.put("originationFee", totalFeesUSD.multiply(BigInteger.TEN));

        doReturn(uBasicReserveData).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user);

        Map<String, Object> config = new java.util.HashMap<>();
        config.put("decimals", BigInteger.valueOf(18));
        config.put("usageAsCollateralEnabled", true);
        config.put("baseLTVasCollateral", BigInteger.valueOf(50).multiply(ICX).divide(BigInteger.valueOf(100)));
        config.put("liquidationThreshold", BigInteger.valueOf(65).multiply(ICX).divide(BigInteger.valueOf(100)));
        doReturn(config).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve.get(0));

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");

        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");
        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserLiquidationData", user);
        Map<String, Map<String, BigInteger>> resultBorrows = (Map<String, Map<String, BigInteger>>) result.get(
                "borrows");
        Map<String, Map<String, BigInteger>> resultCollateral = (Map<String, Map<String, BigInteger>>) result.get(
                "collaterals");

        Map<String, Object> expected = liquidationDataCalculation("ICX", BigInteger.valueOf(700).multiply(ICX),
                BigInteger.valueOf(1000).multiply(ICX), badDebt);
        Map<String, Map<String, BigInteger>> expectedBorrows = (Map<String, Map<String, BigInteger>>) expected.get(
                "borrows");
        Map<String, Map<String, BigInteger>> expectedCollateral = (Map<String, Map<String, BigInteger>>) expected.get(
                "collaterals");

        assertEquals(result.get("badDebt"), expected.get("badDebt"));
        assertEquals(resultBorrows.get("ICX"), expectedBorrows.get("ICX"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalanceUSD"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalanceUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidateUSD"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidateUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidate"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidate"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalance"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalance"));
        assertEquals(resultCollateral.get("ICX"), expectedCollateral.get("ICX"));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalance"), BigInteger.valueOf(1000).multiply(ICX));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalanceUSD"),
                expectedCollateral.get("ICX").get("underlyingBalanceUSD"));

    }

    @Test
    /*
     user has borrow of 30ICX initially with 100 ICX as collateral and badDebt of 13ICX
     user has also borrow of 20 IUSDC
     */
    public void userLiquidationData_bad_debt_multiple_reserve() {
        Address user = sm.createAccount().getAddress();
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");
        BigInteger totalFeesUSD = BigInteger.ONE;

        BigInteger badDebt = BigInteger.valueOf(13).multiply(ICX);
        doReturn(badDebt).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.LIQUIDATION_MANAGER), eq("calculateBadDebt"),
                        any(), any(), any(), any()
                );

        Map<String, Object> uBasicReserveData = new java.util.HashMap<>();
        uBasicReserveData.put("compoundedBorrowBalance", BigInteger.valueOf(30).multiply(ICX));
        uBasicReserveData.put("underlyingBalance", BigInteger.valueOf(100).multiply(ICX));
        uBasicReserveData.put("originationFee", totalFeesUSD.multiply(BigInteger.TEN));

        doReturn(uBasicReserveData).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(0), user);

        Map<String, Object> uBasicReserveData2 = new java.util.HashMap<>();
        uBasicReserveData2.put("compoundedBorrowBalance", BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000_000)));
        uBasicReserveData2.put("underlyingBalance", BigInteger.valueOf(15).multiply(BigInteger.valueOf(1000_000)));
        uBasicReserveData2.put("originationFee", totalFeesUSD.multiply(BigInteger.TEN));

        doReturn(uBasicReserveData2).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve.get(1), user);

        Map<String, Object> config = new java.util.HashMap<>();
        config.put("decimals", BigInteger.valueOf(18));
        config.put("usageAsCollateralEnabled", true);
        config.put("baseLTVasCollateral", BigInteger.valueOf(50).multiply(ICX).divide(BigInteger.valueOf(100)));
        config.put("liquidationThreshold", BigInteger.valueOf(65).multiply(ICX).divide(BigInteger.valueOf(100)));
        doReturn(config).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve.get(0));


        Map<String, Object> config2 = new java.util.HashMap<>();
        config2.put("usageAsCollateralEnabled", true);
        config2.put("baseLTVasCollateral", BigInteger.valueOf(50).multiply(ICX).divide(BigInteger.valueOf(100)));
        config2.put("liquidationThreshold", BigInteger.valueOf(65).multiply(ICX).divide(BigInteger.valueOf(100)));
        config2.put("decimals", BigInteger.valueOf(6));
        doReturn(config2).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve.get(1));


        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");

        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "USDC", "USD");
        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        Map<String, Object> result = (Map<String, Object>) score.call("getUserLiquidationData", user);
        Map<String, Map<String, BigInteger>> resultBorrows = (Map<String, Map<String, BigInteger>>) result.get(
                "borrows");
        Map<String, Map<String, BigInteger>> resultCollateral = (Map<String, Map<String, BigInteger>>) result.get(
                "collaterals");

        Map<String, Object> expected_icx = liquidationDataCalculation("ICX", BigInteger.valueOf(30).multiply(ICX),
                BigInteger.valueOf(100).multiply(ICX), badDebt);
        Map<String, Map<String, BigInteger>> expectedBorrows = (Map<String, Map<String, BigInteger>>) expected_icx.get(
                "borrows");
        Map<String, Map<String, BigInteger>> expectedCollateral = (Map<String, Map<String, BigInteger>>) expected_icx.get(
                "collaterals");

        Map<String, Object> expected_iusdc = liquidationDataCalculation("USDC",
                BigInteger.valueOf(40).multiply(BigInteger.valueOf(1000_000)),
                BigInteger.valueOf(15).multiply(BigInteger.valueOf(1000_000)), badDebt);
        Map<String, Map<String, BigInteger>> expectedBorrowsUSDC = (Map<String, Map<String, BigInteger>>) expected_iusdc.get(
                "borrows");
        Map<String, Map<String, BigInteger>> expectedCollateralUSDC = (Map<String, Map<String, BigInteger>>) expected_iusdc.get(
                "collaterals");

        assertEquals(result.get("badDebt"), expected_icx.get("badDebt"));

        assertEquals(resultBorrows.get("ICX"), expectedBorrows.get("ICX"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalanceUSD"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalanceUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidateUSD"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidateUSD"));
        assertEquals(resultBorrows.get("ICX").get("maxAmountToLiquidate"),
                expectedBorrows.get("ICX").get("maxAmountToLiquidate"));
        assertEquals(resultBorrows.get("ICX").get("compoundedBorrowBalance"),
                expectedBorrows.get("ICX").get("compoundedBorrowBalance"));

        assertEquals(resultCollateral.get("ICX"), expectedCollateral.get("ICX"));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalance"), BigInteger.valueOf(100).multiply(ICX));
        assertEquals(resultCollateral.get("ICX").get("underlyingBalanceUSD"),
                expectedCollateral.get("ICX").get("underlyingBalanceUSD"));

        assertEquals(result.get("badDebt"), expected_iusdc.get("badDebt"));

        assertEquals(resultBorrows.get("USDC"), expectedBorrowsUSDC.get("USDC"));
        assertEquals(resultBorrows.get("USDC").get("compoundedBorrowBalanceUSD"),
                expectedBorrowsUSDC.get("USDC").get("compoundedBorrowBalanceUSD"));
        assertEquals(resultBorrows.get("USDC").get("maxAmountToLiquidateUSD"),
                expectedBorrowsUSDC.get("USDC").get("maxAmountToLiquidateUSD"));
        assertEquals(resultBorrows.get("USDC").get("maxAmountToLiquidate"),
                expectedBorrowsUSDC.get("USDC").get("maxAmountToLiquidate"));
        assertEquals(resultBorrows.get("USDC").get("compoundedBorrowBalance"),
                expectedBorrowsUSDC.get("USDC").get("compoundedBorrowBalance"));

        assertEquals(resultCollateral.get("USDC"), expectedCollateralUSDC.get("USDC"));
        assertEquals(resultCollateral.get("USDC").get("underlyingBalance"),
                BigInteger.valueOf(15).multiply(BigInteger.valueOf(1000_000)));
        assertEquals(resultCollateral.get("USDC").get("underlyingBalanceUSD"),
                expectedCollateralUSDC.get("USDC").get("underlyingBalanceUSD"));


    }

    @Test
    public Map<String, Object> reserveData_iusdc() {

        Address iusdc_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress();
        Address oToken_addr = MOCK_CONTRACT_ADDRESS.get(Contracts.oIUSDC).getAddress();
        Address dToken_addr = MOCK_CONTRACT_ADDRESS.get(Contracts.dIUSDC).getAddress();

        BigInteger lendingPercentage = BigInteger.valueOf(8).divide(BigInteger.TEN);
        BigInteger borrowingPercentage = BigInteger.valueOf(9);
        BigInteger totalLiquidity = BigInteger.valueOf(100).multiply(BigInteger.valueOf(1000_000));
        BigInteger availableLiquidity = BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000_000));
        BigInteger totalBorrows = BigInteger.valueOf(80).multiply(BigInteger.valueOf(1000_000));

        doReturn(Map.of(
                "decimals", BigInteger.valueOf(6),
                "totalLiquidity", totalLiquidity,
                "availableLiquidity", availableLiquidity,
                "totalBorrows", totalBorrows,
                "oTokenAddress", oToken_addr,
                "dTokenAddress", dToken_addr
        )).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", iusdc_reserve);

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "USDC", "USD");

        doReturn(lendingPercentage).when(scoreSpy)
                .call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", oToken_addr);

        doReturn(borrowingPercentage).when(scoreSpy)
                .call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", dToken_addr);

        // calculation
        BigInteger totalLiquidityUSD = reserveDataCalcualtion("USDC", ICX, totalLiquidity);
        BigInteger availableLiquidityUSD = reserveDataCalcualtion("USDC", ICX, availableLiquidity);
        BigInteger totalBorrowsUSD = reserveDataCalcualtion("USDC", ICX, totalBorrows);

        Map<String, Object> result = (Map<String, Object>) score.call("getReserveData", iusdc_reserve);

        assertEquals(result.get("exchangePrice"), ICX);
        assertEquals(result.get("totalLiquidityUSD"), totalLiquidityUSD);
        assertEquals(result.get("availableLiquidityUSD"), availableLiquidityUSD);
        assertEquals(result.get("totalBorrowsUSD"), totalBorrowsUSD);
        assertEquals(result.get("lendingPercentage"), lendingPercentage);
        assertEquals(result.get("borrowingPercentage"), borrowingPercentage);
        assertEquals(result.get("rewardPercentage"), borrowingPercentage.add(lendingPercentage));
        assertNull(result.get("sICXRate"));
        return result;
    }

    @Test
    public Map<String, Object> reserveData_icx() {

        Address icx_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Address oToken_addr = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        Address dToken_addr = MOCK_CONTRACT_ADDRESS.get(Contracts.dICX).getAddress();

        BigInteger lendingPercentage = BigInteger.valueOf(1).divide(BigInteger.TEN);
        BigInteger borrowingPercentage = BigInteger.valueOf(12);
        BigInteger totalLiquidity = BigInteger.valueOf(500).multiply(ICX);
        BigInteger availableLiquidity = BigInteger.valueOf(245).multiply(ICX);
        BigInteger totalBorrows = BigInteger.valueOf(255).multiply(ICX);

        doReturn(Map.of(
                "decimals", BigInteger.valueOf(18),
                "totalLiquidity", totalLiquidity,
                "availableLiquidity", availableLiquidity,
                "totalBorrows", totalBorrows,
                "oTokenAddress", oToken_addr,
                "dTokenAddress", dToken_addr
        )).when(scoreSpy)
                .call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", icx_reserve);

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");
        doReturn(ICX).when(scoreSpy).call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                "ICX", "USD");
        doReturn(ICX.divide(BigInteger.TEN)).when(scoreSpy).call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        doReturn(lendingPercentage).when(scoreSpy)
                .call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", oToken_addr);

        doReturn(borrowingPercentage).when(scoreSpy)
                .call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", dToken_addr);

        // calculation
        BigInteger totalLiquidityUSD = reserveDataCalcualtion("ICX", ICX, totalLiquidity);
        BigInteger availableLiquidityUSD = reserveDataCalcualtion("ICX", ICX, availableLiquidity);
        BigInteger totalBorrowsUSD = reserveDataCalcualtion("ICX", ICX, totalBorrows);

        Map<String, Object> result = (Map<String, Object>) score.call("getReserveData", icx_reserve);

        assertEquals(result.get("exchangePrice"), ICX);
        assertEquals(result.get("totalLiquidityUSD"), totalLiquidityUSD);
        assertEquals(result.get("availableLiquidityUSD"), availableLiquidityUSD);
        assertEquals(result.get("totalBorrowsUSD"), totalBorrowsUSD);
        assertEquals(result.get("lendingPercentage"), lendingPercentage);
        assertEquals(result.get("borrowingPercentage"), borrowingPercentage);
        assertEquals(result.get("rewardPercentage"), borrowingPercentage.add(lendingPercentage));
        assertEquals(result.get("sICXRate"), ICX.divide(BigInteger.TEN));

        return result;
    }

    @Test
    public void getAllReserveData() {
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");

        doReturn(reserveData_iusdc()).when(scoreSpy).getReserveData(reserve.get(1));
        doReturn(reserveData_icx()).when(scoreSpy).getReserveData(reserve.get(0));

        Map<String, Map<String, Object>> result =
                (Map<String, Map<String, Object>>) score.call("getAllReserveData");

        assertEquals(reserveData_icx(), result.get("ICX"));
        assertEquals(reserveData_iusdc(), result.get("USDC"));
    }

    @Test
    public void allReserveConfiguration() {
        List<Address> reserve = new ArrayList<>();
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());
        reserve.add(MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress());

        doReturn(reserve).when(scoreSpy).call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC).getAddress(), "USDC");
        score.invoke(owner, "setSymbol",
                MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress(), "ICX");

        doReturn(Map.of()).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(0));
        doReturn(Map.of()).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", reserve.get(1));

        Map<String, Map<String, Object>> result =
                (Map<String, Map<String, Object>>) score.call("getAllReserveConfigurationData");

        assertEquals(Map.of(), result.get("ICX"));
        assertEquals(Map.of(), result.get("USDC"));
    }

    @Test
    public void loadOrigination() {
        doReturn(BigInteger.valueOf(1)).when(scoreSpy)
                .call(BigInteger.class, Contracts.FEE_PROVIDER, "getLoanOriginationFeePercentage");

        assertEquals(BigInteger.valueOf(1), score.call("getLoanOriginationFeePercentage"));
    }

    @Test
    public void realTimeDebt() {
        Account user = sm.createAccount(100);
        Address icx_reserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();

        doReturn(Map.of(
                "currentBorrowBalance", BigInteger.valueOf(10).multiply(ICX),
                "originationFee", BigInteger.valueOf(1)
        )).when(scoreSpy).getUserReserveData(icx_reserve, user.getAddress());

        BigInteger result = (BigInteger) score.call("getRealTimeDebt", icx_reserve, user.getAddress());
        BigInteger expected = BigInteger.valueOf(10).multiply(ICX).add(BigInteger.valueOf(1));
        assertEquals(result, expected);
    }

    @Test
    public void userStakeInfo_returns_empty_list() {
        Account user = sm.createAccount(100);

        Map<String, Object> unstakedRecords = new HashMap<>();
        unstakedRecords.put("from", MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress());

        List<Map<String, Object>> unstakeDetails = new ArrayList<>();
        unstakeDetails.add(0, unstakedRecords);

        doReturn(unstakeDetails).when(scoreSpy)
                .call(List.class, Contracts.STAKING, "getUserUnstakeInfo", user.getAddress());

        List<Map<String, BigInteger>> result = (List<Map<String, BigInteger>>) score.call("getUserUnstakeInfo",
                user.getAddress());

        List<Map<String, BigInteger>> expected = new ArrayList<>();

        assertEquals(result, expected);

    }

    @Test
    void userStakeInfo() {
        Account user = sm.createAccount(100);

        Map<String, Object> unstakedRecords = new HashMap<>();
        unstakedRecords.put("from", MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress());
        unstakedRecords.put("amount", BigInteger.valueOf(100));
        unstakedRecords.put("unstakingBlockHeight", BigInteger.valueOf(10000));

        List<Map<String, Object>> unstakeDetails = new ArrayList<>();
        unstakeDetails.add(unstakedRecords);

        doReturn(unstakeDetails).when(scoreSpy)
                .call(List.class, Contracts.STAKING, "getUserUnstakeInfo", user.getAddress());

        List<Map<String, BigInteger>> result = (List<Map<String, BigInteger>>) score.call("getUserUnstakeInfo",
                user.getAddress());

        List<Map<String, BigInteger>> expected = new ArrayList<>();

        Map<String, BigInteger> expectedResponse = new HashMap<>();
        expectedResponse.put("amount", BigInteger.valueOf(100));
        expectedResponse.put("unstakingBlockHeight", BigInteger.valueOf(10000));

        expected.add(expectedResponse);

        assertEquals(result.size(), expected.size());
        assertEquals(result.get(0).get("amount"), expected.get(0).get("amount"));
        assertEquals(result.get(0).get("unstakingBlockHeight"), expected.get(0).get("unstakingBlockHeight"));

    }

}
