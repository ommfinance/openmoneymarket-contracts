package finance.omm.score.test.unit.oracle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceOracleTest extends AbstractPriceOracleTest{

    @Test
    public void name() {
        String actual = (String) score.call("name");
        String expected = "Omm Price Oracle Proxy";
        assertEquals(expected, actual);
    }


}
