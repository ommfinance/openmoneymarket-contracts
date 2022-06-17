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
import static org.mockito.Mockito.doReturn;

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
        // collateral which user has
        // reserve which is going to be liquidate
        Account notLendingPool = sm.createAccount(100);

        Address collateralAddr = addresses[0];
        Address reserveAddr = addresses[1];
        Address user = addresses[2];
        BigInteger purchasAmount = BigInteger.valueOf(30);

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
                purchasAmount);

        expectErrorMessage(call,TAG + ": the reserve " + collateralAddr + " cannot be used as collateral");


    }

}
