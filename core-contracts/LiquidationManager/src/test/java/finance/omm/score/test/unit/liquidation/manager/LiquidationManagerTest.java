package finance.omm.score.test.unit.liquidation.manager;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.liquidation.manager.LiquidationManagerImpl.TAG;
import static finance.omm.utils.math.MathUtils.exaMultiply;
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

    private BigInteger _calculateBadDebt(BigInteger borrowed, BigInteger feeUsd,
                                        BigInteger collateral, BigInteger ltv){

        BigInteger actual = (BigInteger) score.call("calculateBadDebt",borrowed,feeUsd,collateral,ltv);
        return actual;
    }

    @Test
    public void liquidationCall(){
        BigInteger EXA = BigInteger.valueOf(1).multiply(BigInteger.TEN).pow(18);
        Account notLendingPool = sm.createAccount(100);
        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        BigInteger purchaseAmount = BigInteger.valueOf(30).multiply(EXA);
        BigInteger userCollateralBalance = null;

        //call from non lendingpool address
        Executable call = () -> score.invoke(notLendingPool,"liquidationCall",collateralAddr,reserveAddr,user,
                purchaseAmount);

        expectErrorMessage(call,TAG+ ": SenderNotLendingPoolError: (sender)" +
                notLendingPool.getAddress() + " (lending pool)"+ (MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL)).
                getAddress());


        String principalBase = "ICX";
        doReturn(principalBase).when(scoreSpy).
                call(eq(String.class) ,eq(Contracts.LENDING_POOL_DATA_PROVIDER),eq("getSymbol"),eq(reserveAddr));


        BigInteger principalPrice = BigInteger.valueOf(20).multiply(EXA);
        doReturn(principalPrice).when(scoreSpy).
                call(eq(BigInteger.class) ,eq(Contracts.PRICE_ORACLE),eq("get_reference_data"),eq(principalBase),any());


        Map<String, Object> userAccountData = new HashMap<>();
                userAccountData.put("totalCollateralBalanceUSD", BigInteger.valueOf(40).multiply(EXA));
                userAccountData.put("totalBorrowBalanceUSD", BigInteger.valueOf(40).multiply(EXA));
                userAccountData.put("totalFeesUSD", BigInteger.valueOf(40).multiply(EXA));
                userAccountData.put("currentLtv", BigInteger.valueOf(40).multiply(EXA));
                userAccountData.put("healthFactor", BigInteger.ONE);
        doReturn(userAccountData).when(scoreSpy).
                call(eq(Map.class ),eq(Contracts.LENDING_POOL_DATA_PROVIDER),eq("getUserAccountData"),eq(user));

        doReturn(
                Map.of("usageAsCollateralEnabled",false)
        ).when(scoreSpy).call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getReserveData",collateralAddr);

        call = () -> score.invoke(LENDING_POOL,"liquidationCall", collateralAddr,reserveAddr,user, purchaseAmount);
        expectErrorMessage(call,TAG + ": the reserve " + collateralAddr + " cannot be used as collateral");

        doReturn(
                Map.of("usageAsCollateralEnabled",true)
        ).when(scoreSpy).call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getReserveData",collateralAddr);


        call = () -> score.invoke(LENDING_POOL,"liquidationCall",
                collateralAddr,reserveAddr,user, purchaseAmount);
        expectErrorMessage(call,TAG + ": unsuccessful liquidation call,health factor of user is above 1" +
                "health factor of user " + BigInteger.ONE);


        userAccountData.put("healthFactorBelowThreshold", true);
        doReturn(userAccountData).when(scoreSpy).
                call(Map.class ,Contracts.LENDING_POOL_DATA_PROVIDER,"getUserAccountData",user);


        doReturn(userCollateralBalance).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserUnderlyingAssetBalance",collateralAddr,user);


        call = () -> score.invoke(LENDING_POOL,"liquidationCall",
                collateralAddr,reserveAddr,user, purchaseAmount);
        expectErrorMessage(call,TAG + ": unsuccessful liquidation call,user have no collateral balance" +
                "for collateral" + collateralAddr + "balance of user: " + user + " is " + userCollateralBalance);


        userCollateralBalance = BigInteger.valueOf(70).multiply(EXA);
        doReturn(userCollateralBalance).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserUnderlyingAssetBalance",collateralAddr,user);
        call = () -> score.invoke(LENDING_POOL,"liquidationCall",
                collateralAddr,reserveAddr,user, purchaseAmount);


        Map<String, BigInteger> userBorrowBalances = new HashMap<>();
        userBorrowBalances.put("principalBorrowBalance", BigInteger.valueOf(50));
        userBorrowBalances.put("borrowBalanceIncrease", BigInteger.valueOf(40));

        doReturn(userBorrowBalances).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances",reserveAddr,user);
        call = () -> score.invoke(LENDING_POOL,"liquidationCall",
                collateralAddr,reserveAddr,user, purchaseAmount);

        expectErrorMessage(call,TAG +": unsuccessful liquidation call,user have no borrow balance"+
                "for reserve" + reserveAddr + "borrow balance of user: " + user + " is " + userBorrowBalances);


        userBorrowBalances.put("compoundedBorrowBalance", BigInteger.valueOf(80));

        call = () -> score.invoke(LENDING_POOL,"liquidationCall",
                collateralAddr,reserveAddr,user, purchaseAmount);

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18)
        )).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",reserveAddr);

        doReturn(BigInteger.TEN).when(scoreSpy).call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserOriginationFee",reserveAddr,user);

        calculate(collateralAddr,reserveAddr);
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE),eq("updateStateOnLiquidation"),
                eq(reserveAddr), eq(collateralAddr), eq(user), any(), any(), any(), any(), any());


        Address collateralOtokenAddress = addresses[3];
        doReturn(collateralOtokenAddress).when(scoreSpy).call(eq(Address.class),eq(Contracts.LENDING_POOL_CORE),
                eq("getReserveOTokenAddress"),eq(collateralAddr));


        doNothing().when(scoreSpy).call(eq(collateralOtokenAddress),eq("burnOnLiquidation"),any(),any());
        doNothing().when(scoreSpy).call(eq(collateralOtokenAddress),eq("burnOnLiquidation"),any(),any());
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE),eq("liquidateFee"),any(), any(),
                    any());

        score.invoke(LENDING_POOL,"liquidationCall",collateralAddr,
                reserveAddr,user, purchaseAmount);
    }

    @Test
    public void calculateAvailableCollateralToLiquidate(){
        BigInteger EXA = BigInteger.valueOf(1).multiply(BigInteger.TEN).pow(18);
        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        BigInteger purchaseAmount = BigInteger.valueOf(30).multiply(EXA);
        BigInteger colleteralBalance = BigInteger.valueOf(70).multiply(EXA);

        calculate(collateralAddr,reserveAddr);
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

    public void calculate(Address collateralAddr,Address reserveAddr){
        BigInteger EXA = BigInteger.valueOf(1).multiply(BigInteger.TEN).pow(18);

        doReturn(Map.of(
                "liquidationBonus",BigInteger.valueOf(10).multiply(EXA))).when(scoreSpy).
                call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                        "getReserveConfigurationData",collateralAddr);

        doReturn("USDS").when(scoreSpy).
                call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",collateralAddr);

        doReturn("ICX").when(scoreSpy).
                call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",reserveAddr);

        doReturn(BigInteger.valueOf(20).multiply(EXA)).when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE,
                        "get_reference_data","ICX","USD");

        doReturn(BigInteger.valueOf(30).multiply(EXA)).when(scoreSpy).
                call(BigInteger.class, Contracts.PRICE_ORACLE,
                        "get_reference_data","USDS","USD");

        doReturn(BigInteger.valueOf(5).multiply(EXA)).when(scoreSpy).
                call(BigInteger.class,Contracts.STAKING,"getTodayRate");

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18)
        )).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",reserveAddr);

        doReturn(Map.of(
                "decimals",BigInteger.valueOf(18)
        )).when(scoreSpy).call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",collateralAddr);
    }

}
