package finance.omm.score.test.unit.liquidation.manager;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import score.Address;

import java.math.BigInteger;

import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        score.invoke(notLendingPool,"liquidationCall",collateralAddr,reserveAddr,user,purchasAmount);
    }

}
