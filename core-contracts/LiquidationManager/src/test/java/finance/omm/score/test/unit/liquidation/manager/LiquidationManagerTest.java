package finance.omm.score.test.unit.liquidation.manager;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import score.Context;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.liquidation.manager.LiquidationManagerImpl.TAG;
import static finance.omm.utils.math.MathUtils.convertExaToOther;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LiquidationManagerTest extends AbstractLiquidationManagerTest {

    Account LENDING_POOL = MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL);

    @Test
    public void name(){
        String actual = (String) score.call("name");
        String expected = "Omm Liquidation Manager";
        assertEquals(expected, actual);
    }

    @Test
    public void calculateBadDebt(){
        BigInteger borrowed = BigInteger.valueOf(30);
        BigInteger feeUsd = borrowed.multiply(ONE).divide(HUNDRED);
        BigInteger collateral = HUNDRED;
        BigInteger ltv = TWENTY_FIVE.multiply(BigInteger.valueOf(10).pow(16));

        BigInteger actual = _calculateBadDebt(borrowed,feeUsd,collateral,ltv);

        BigInteger expected = borrowed.subtract(exaMultiply(collateral.subtract(feeUsd),ltv));

        assertEquals(actual,expected);
    }

    public BigInteger _calculateBadDebt(BigInteger borrowed, BigInteger feeUsd,
                                        BigInteger collateral, BigInteger ltv){

        BigInteger actual = (BigInteger) score.call("calculateBadDebt",borrowed,feeUsd,collateral,ltv);
        return actual;
    }

    @Test
    public void liquidationCall(){
        // collateral which user has
        // reserve which is going to be liquidate
        Account notLendingPool = sm.createAccount(100);

        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        Address user = addresses[2];
        BigInteger purchasAmount = BigInteger.valueOf(30);
        BigInteger userCollateralBalance = null;

        Executable call = () -> score.invoke(notLendingPool,"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        expectErrorMessage(call,TAG+ ": SenderNotLendingPoolError: (sender)" +
                notLendingPool.getAddress() + " (lending pool)"+ (MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL)).
                getAddress());

        String principalBase = "ICX";
        doReturn(principalBase).when(scoreSpy).
                call(BigInteger.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getSymbol",reserveAddr);
        BigInteger principalPrice = BigInteger.valueOf(100);
        doReturn(principalPrice).when(scoreSpy).
                call(BigInteger.class ,Contracts.PRICE_ORACLE,"get_reference_data",principalBase,"USD");

        Map<String, Object> userAccountData = new HashMap<>();
        userAccountData.put("totalLiquidityBalanceUSD", BigInteger.valueOf(100));
                userAccountData.put("totalCollateralBalanceUSD", BigInteger.valueOf(50));
                userAccountData.put("totalBorrowBalanceUSD", BigInteger.valueOf(40));
                userAccountData.put("totalFeesUSD", BigInteger.TEN);
                userAccountData.put("availableBorrowsUSD", BigInteger.valueOf(80));
                userAccountData.put("currentLtv", BigInteger.valueOf(20));
                userAccountData.put("currentLiquidationThreshold", BigInteger.valueOf(30));
                userAccountData.put("healthFactor", BigInteger.ONE);
                userAccountData.put("borrowingPower", BigInteger.valueOf(40));

        doReturn(userAccountData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getUserAccountData",user);

        Map<String, Object> collateralData = new HashMap<>();
                collateralData.put("exchangePrice", BigInteger.valueOf(100));
                collateralData.put("decimals", BigInteger.valueOf(50));
                collateralData.put("totalLiquidityUSD", BigInteger.valueOf(40));
                collateralData.put("availableLiquidityUSD", BigInteger.TEN);
                collateralData.put("totalBorrowsUSD", BigInteger.valueOf(80));
                collateralData.put("lendingPercentage", BigInteger.valueOf(20));
                collateralData.put("borrowingPercentage", BigInteger.valueOf(30));
                collateralData.put("oTokenAddress", BigInteger.ONE);
                collateralData.put("rewardPercentage", BigInteger.valueOf(40));

        doReturn(collateralData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getReserveData",collateralAddr);

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        expectErrorMessage(call,TAG + ": the reserve " + collateralAddr + " cannot be used as collateral");

        collateralData.put("usageAsCollateralEnabled",false);

        doReturn(collateralData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getReserveData",collateralAddr);

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);
        BigInteger userHealthFactor = (BigInteger) userAccountData.get("healthFactor");
        expectErrorMessage(call,TAG + ": unsuccessful liquidation call,health factor of user is above 1" +
                "health factor of user " + userHealthFactor);

        userAccountData.put("healthFactorBelowThreshold", true);
        doReturn(userAccountData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getUserAccountData",user);
        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        doReturn(userCollateralBalance).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserUnderlyingAssetBalance",collateralAddr,user);

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        expectErrorMessage(call,TAG + ": unsuccessful liquidation call,user have no collateral balance" +
                "for collateral" + collateralAddr + "balance of user: " + user + " is " + userCollateralBalance);

        userCollateralBalance = BigInteger.valueOf(100);
        doReturn(userCollateralBalance).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserUnderlyingAssetBalance",collateralAddr,user);

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        Map<String, BigInteger> userBorrowBalances = new HashMap<>();
        userBorrowBalances.put("principalBorrowBalance", BigInteger.valueOf(50));
        userBorrowBalances.put("borrowBalanceIncrease", BigInteger.valueOf(40));

        doReturn(userBorrowBalances).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances",reserveAddr,user);
        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        expectErrorMessage(call,TAG +": unsuccessful liquidation call,user have no borrow balance"+
                "for reserve" + reserveAddr + "borrow balance of user: " + user + " is " + userBorrowBalances);


        userBorrowBalances.put("compoundedBorrowBalance", BigInteger.valueOf(80));

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);

        BigInteger reserveDecimals = BigInteger.valueOf(20);

        Map<String, Object> reserveConfiguration = new HashMap<>();
        reserveConfiguration.put("decimals", reserveDecimals);
        reserveConfiguration.put("baseLTVasCollateral", BigInteger.valueOf(40));
        reserveConfiguration.put("liquidationThreshold", BigInteger.valueOf(30));
        reserveConfiguration.put("usageAsCollateralEnabled", true);
        reserveConfiguration.put("isActive", true);
        reserveConfiguration.put("borrowingEnabled", true);
        reserveConfiguration.put("liquidationBonus", BigInteger.valueOf(50));

        doReturn(reserveConfiguration).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",reserveAddr);

        BigInteger userOriginationFee = BigInteger.TEN;
        doReturn(userOriginationFee).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserOriginationFee",reserveAddr,user);

        BigInteger borrowBalance = userBorrowBalances.get("borrowBalanceIncrease");
        doNothing().when(scoreSpy).call(Contracts.LENDING_POOL_CORE,"updateStateOnLiquidation",
                reserveAddr, collateralAddr, user, borrowBalance);



        Address collateralOtokenAddress = addresses[3];
        doReturn(collateralOtokenAddress).when(scoreSpy).call(eq(Address.class),eq(Contracts.LENDING_POOL_CORE),
                eq("getReserveOTokenAddress"),eq(collateralAddr));

        doNothing().when(scoreSpy).call(eq(collateralOtokenAddress),eq("burnOnLiquidation"),any(),any());

        BigInteger feeLiquidated = BigInteger.valueOf(10);

        doNothing().when(scoreSpy).call(eq(collateralOtokenAddress),eq("burnOnLiquidation"),any(),any());
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE),eq("liquidateFee"),any(), any(),
                    any());

        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchasAmount);
    }

    @Test
    public void calculateAvailableCollateralToLiquidate(){
        BigInteger EXA = BigInteger.valueOf(1).multiply(BigInteger.TEN).pow(18);
        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        BigInteger purchaseAmount = BigInteger.valueOf(30).multiply(EXA);
        BigInteger colleteralBalance = BigInteger.valueOf(70).multiply(EXA);

        doReturn(Map.of(
                "liquidationBonus",BigInteger.valueOf(10))).when(scoreSpy).
                call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                        "getReserveConfigurationData",collateralAddr);

        doReturn("ICX").when(scoreSpy).
                call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",collateralAddr);

        doReturn("USDS").when(scoreSpy).
                call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",reserveAddr);

        doReturn(BigInteger.valueOf(20).multiply(EXA)).when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE,
                        "get_reference_data","ICX","USD");

        doReturn(BigInteger.valueOf(30)).when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE,
                        "get_reference_data","USDS","USD");

        doReturn(BigInteger.valueOf(5)).when(scoreSpy).
                call(BigInteger.class,Contracts.STAKING,"getTodayRate");

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18)
        )).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",reserveAddr);

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18)
        )).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",collateralAddr);

        score.call("calculateAvailableCollateralToLiquidate",
                collateralAddr,reserveAddr,purchaseAmount, colleteralBalance,false);

        verify(scoreSpy).call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                "getReserveConfigurationData",collateralAddr);
        verify(scoreSpy,times(2)).call(eq(String.class),eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getSymbol"),any(Address.class));
        verify(scoreSpy,times(2)).call(eq(BigInteger.class),eq(Contracts.PRICE_ORACLE),
                eq("get_reference_data"),any(String.class),eq("USD"));
        verify(scoreSpy).call(BigInteger.class,Contracts.STAKING, "getTodayRate");
        verify(scoreSpy).call(eq(Map.class),eq(Contracts.LENDING_POOL_CORE),
                eq("getReserveConfiguration"),eq(reserveAddr));
        verify(scoreSpy).call(eq(Map.class),eq(Contracts.LENDING_POOL_CORE),
                eq("getReserveConfiguration"),eq(collateralAddr));

    }

}
