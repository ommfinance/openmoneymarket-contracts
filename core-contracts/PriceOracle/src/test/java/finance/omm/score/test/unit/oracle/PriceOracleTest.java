package finance.omm.score.test.unit.oracle;

import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
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
    }

    @Test
    public void getReferenceData(){
        String OMM = "OMM";
        String USDS = "USDS";

        doReturn(BigInteger.valueOf(7)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/USDS"));
        doReturn(BigInteger.valueOf(6)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/sICX"));
        doReturn(BigInteger.valueOf(5)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("lookupPid"), eq("OMM/IUSDC"));
        doReturn(Map.of(
                "price", BigInteger.valueOf(10),
                "quote_decimals",BigInteger.valueOf(18),
                "base_decimals",BigInteger.valueOf(18),
                "base",BigInteger.valueOf(100)
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.DEX),eq("getPoolStats"),any());

        doReturn(Map.of(
                "rate",BigInteger.valueOf(3)
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.BAND_ORACLE), eq("get_reference_data"),any(),any());

        contextMock
                .when(() -> Context.call(BigInteger.class, MOCK_CONTRACT_ADDRESS.get(Contracts.DEX).getAddress(),
                        "getPriceByName" ,"sICX/ICX"))
                .thenReturn(BigInteger.valueOf(2));
        doReturn(BigInteger.valueOf(10)).when(scoreSpy).call(BigInteger.class, Contracts.DEX,"getBalnPrice");

        score.call("get_reference_data","OMM","USDS");
        score.call("get_reference_data","BALN","USDS");
        score.call("get_reference_data","bnUSD","USDS");
    }


}
