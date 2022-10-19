package finance.omm.score.test.unit.oracle;

import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Context;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class PriceOracleTest extends AbstractPriceOracleTest{

    @Test
    public void name() {
        String expected = "Omm Price Oracle Proxy";
        assertEquals(expected, score.call("name"));
    }

    @Test
    public void setAndGetPool(){
        score.invoke(owner,"setOMMPool","OMM/USDS");

        assertEquals("OMM/USDS",score.call("getOMMPool"));

        Executable call = () -> score.invoke(notOwner,"setOMMPool","OMM/USDS");
        expectErrorMessage(call,"require owner access");
    }

    @Test
    public void tokenPrice(){

        doReturn(BigInteger.valueOf(6)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/sICX"));
        doReturn(Map.of(
                "price",  BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.valueOf(100)),
                "quote_decimals",BigInteger.valueOf(18),
                "base_decimals",BigInteger.valueOf(18),
                "base",BigInteger.valueOf(8000_000).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.DEX,"getPoolStats",BigInteger.valueOf(6));

        doReturn(Map.of(
                "rate",BigInteger.valueOf(3).multiply(ICX)
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.BAND_ORACLE), eq("get_reference_data"),any(),any());

        contextMock
                .when(() -> Context.call(BigInteger.class, MOCK_CONTRACT_ADDRESS.get(Contracts.DEX).getAddress(),
                        "getPriceByName" ,"sICX/ICX"))
                .thenReturn(BigInteger.valueOf(1).multiply(ICX));

        doReturn(BigInteger.valueOf(10)).when(scoreSpy).call(BigInteger.class, Contracts.BALANCED_ORACLE,
                "getLastPriceInLoop","BALN");

        BigInteger icxPrice= getPrice("ICX");
        BigInteger price = icxPrice.divide(BigInteger.valueOf(8000_000).multiply(ICX));

        assertEquals(price,score.call("get_reference_data","OMM","sICX"));
        assertEquals(BigInteger.valueOf(10),score.call("get_reference_data","BALN","USD"));
        assertEquals(ICX,score.call("get_reference_data","bnUSD","USD"));
        assertEquals(ICX,score.call("get_reference_data","USDS","USD"));

    }

    @Test
    public void ommPrice(){
        String OMM = "OMM";
        String USD = "USD";

        doReturn(BigInteger.valueOf(8)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/USDS"));
        doReturn(BigInteger.valueOf(7)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/sICX"));
        doReturn(BigInteger.valueOf(6)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/IUSDC"));


        // poolStat for USDS
        doReturn(Map.of(
                "price", BigInteger.valueOf(1).multiply(ICX),
                "quote_decimals",BigInteger.valueOf(18),
                "base_decimals",BigInteger.valueOf(18),
                "base",BigInteger.valueOf(7600_000).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.DEX,"getPoolStats",BigInteger.valueOf(8));

        // poolStat for sICX
        doReturn(Map.of(
                "price", BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.valueOf(100)),
                "quote_decimals",BigInteger.valueOf(18),
                "base_decimals",BigInteger.valueOf(18),
                "base",BigInteger.valueOf(8000_000).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.DEX,"getPoolStats",BigInteger.valueOf(7));

        // poolStat for IUSDC
        doReturn(Map.of(
                "price", BigInteger.valueOf(1).multiply(ICX),
                "quote_decimals",BigInteger.valueOf(6),
                "base_decimals",BigInteger.valueOf(18),
                "base",BigInteger.valueOf(7200_000).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.DEX,"getPoolStats",BigInteger.valueOf(6));

        contextMock
                .when(() -> Context.call(BigInteger.class, MOCK_CONTRACT_ADDRESS.get(Contracts.DEX).getAddress(),
                        "getPriceByName" ,"sICX/ICX"))
                .thenReturn(BigInteger.valueOf(1).multiply(ICX));

        // price from ICX
        doReturn(Map.of(
                "rate",BigInteger.valueOf(3).multiply(ICX)
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.BAND_ORACLE), eq("get_reference_data"),
                eq("ICX"),eq("USD"));

        // price of USDC
        doReturn(Map.of(
                "rate",BigInteger.valueOf(9).multiply(ICX)
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.BAND_ORACLE), eq("get_reference_data"),
                eq("USDC"),eq("USD"));


        BigInteger USDSprice = getPrice("USDS");
        BigInteger icxPrice= getPrice("ICX");
        BigInteger iusdcPrice = getPrice("IUSDC");

        BigInteger totalPrice = icxPrice.add(iusdcPrice).add(USDSprice);
        BigInteger totalSupply = BigInteger.valueOf(7600_000).multiply(ICX).
                add(BigInteger.valueOf(8000_000).multiply(ICX)).
                add(BigInteger.valueOf(7200_000).multiply(ICX));


        BigInteger ommPrice =  totalPrice.divide(totalSupply);

        assertEquals(ommPrice,score.call("get_reference_data",OMM,USD));
    }


}
