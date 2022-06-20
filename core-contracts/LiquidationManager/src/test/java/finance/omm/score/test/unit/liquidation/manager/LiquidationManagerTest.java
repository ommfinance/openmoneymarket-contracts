package finance.omm.score.test.unit.liquidation.manager;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.liquidation.manager.LiquidationManagerImpl.TAG;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        BigInteger actual = (BigInteger) score.call("calculateBadDebt",borrowed,feeUsd,collateral,ltv);

        BigInteger expected = borrowed.subtract(exaMultiply(collateral.subtract(feeUsd),ltv));

        assertEquals(actual,expected);

    }

    @Test
    public void liquidationCall(){
        Account notLendingPool = sm.createAccount(100);

        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        Address user = addresses[2];
        BigInteger purchaseAmount = BigInteger.valueOf(30);

        Executable call = () -> score.invoke(notLendingPool,"liquidationCall",collateralAddr,reserveAddr,user,
                purchaseAmount);

        expectErrorMessage(call,TAG+ ": SenderNotLendingPoolError: (sender)" +
                notLendingPool.getAddress() + " (lending pool)"+ (MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL)).
                getAddress());

        String principalBase = "ICX";
        doReturn(principalBase).when(scoreSpy).
                call(BigInteger.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getSymbol",reserveAddr);
        BigInteger principalPrice = BigInteger.valueOf(100);
        doReturn(principalPrice).when(scoreSpy).
                call(BigInteger.class ,Contracts.PRICE_ORACLE,"get_reference_data",principalBase,"USD");

        Map<String, Object> userAccountData = Map.of("totalLiquidityBalanceUSD", BigInteger.valueOf(100),
                                                        "totalCollateralBalanceUSD", BigInteger.valueOf(50),
                                                        "totalBorrowBalanceUSD", BigInteger.valueOf(40),
                                                        "totalFeesUSD", BigInteger.TEN,
                                                        "availableBorrowsUSD", BigInteger.valueOf(80),
                                                        "currentLtv", BigInteger.valueOf(20),
                                                        "currentLiquidationThreshold", BigInteger.valueOf(30),
                                                        "healthFactor", BigInteger.ONE,
                                                        "borrowingPower", BigInteger.valueOf(40),
                                                        "healthFactorBelowThreshold", true);
        doReturn(userAccountData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getUserAccountData",user);

        Map<String, BigInteger> collateralData = Map.of("exchangePrice", BigInteger.valueOf(100),
                                                    "decimals", BigInteger.valueOf(50),
                                                    "totalLiquidityUSD", BigInteger.valueOf(40),
                                                    "availableLiquidityUSD", BigInteger.TEN,
                                                    "totalBorrowsUSD", BigInteger.valueOf(80),
                                                    "lendingPercentage", BigInteger.valueOf(20),
                                                    "borrowingPercentage", BigInteger.valueOf(30),
                                                    "oTokenAddress", BigInteger.ONE,
                                                    "rewardPercentage", BigInteger.valueOf(40));
        doReturn(collateralData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getReserveData",collateralAddr);

        call = () -> score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL),"liquidationCall",collateralAddr,reserveAddr,user,
                purchaseAmount);

        expectErrorMessage(call,TAG + ": the reserve " + collateralAddr + " cannot be used as collateral");


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
