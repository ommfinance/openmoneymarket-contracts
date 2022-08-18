package finance.omm.test.lendingPoolCore;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.configs.Constant;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.test.lendingPoolCore.config.LendingPoolCoreConfig;
import finance.omm.utils.math.MathUtils;
import foundation.icon.jsonrpc.Address;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import score.UserRevertedException;
import scorex.util.ArrayList;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class LendingPoolCoreIntegrationTest implements ScoreIntegrationTest {
    private static OMMClient ownerClient;
    private static OMMClient alice;

    private static Map<String, Address> addressMap;

    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new LendingPoolCoreConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
    }

    @Test
    public void name() {
        String name = ownerClient.lendingPoolCore.name();
        assertEquals(name, "Omm Lending Pool Core");
    }

    @Test
    @Order(1)
    public void reserveInitializeChecks() {
        Address sicx = addressMap.get(Contracts.sICX.getKey());

        Map<String, Object> reserveData = getReserveData(sicx);

        Address oicx = addressMap.get(Contracts.oICX.getKey());
        Address dicx = addressMap.get(Contracts.dICX.getKey());

        assertEquals(oicx.toString(), reserveData.get("oTokenAddress"));
        assertEquals(dicx.toString(), reserveData.get("dTokenAddress"));

        assertEquals(BigInteger.ZERO, toBigInt((String) reserveData.get("liquidityRate")));
        assertEquals(BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) reserveData.get("borrowRate")));

        assertEquals(ICX, toBigInt((String) reserveData.get("liquidityCumulativeIndex")));
        assertEquals(ICX, toBigInt((String) reserveData.get("borrowCumulativeIndex")));

        assertEquals(new BigInteger("500000000000000000"), toBigInt((String) reserveData.get("baseLTVasCollateral")));
        assertEquals(new BigInteger("650000000000000000"), toBigInt((String) reserveData.get("liquidationThreshold")));
        assertEquals(new BigInteger("100000000000000000"), toBigInt((String) reserveData.get("liquidationBonus")));

        assertEquals("0x1", reserveData.get("borrowingEnabled"));
        assertEquals("0x1", reserveData.get("usageAsCollateralEnabled"));
        assertEquals("0x0", reserveData.get("isFreezed"));
        assertEquals("0x1", reserveData.get("isActive"));
    }

    @Test
    @Order(1)
    public void getReserves() {
        List<Address> list = new ArrayList<>();
        list.add(addressMap.get(Contracts.IUSDC.getKey()));
        list.add(addressMap.get(Contracts.sICX.getKey()));
        assertEquals(list, getReservesInternal());
    }

    @Test
    @Order(1)
    public void getReserveConstants() {
        Address iusdc = addressMap.get(Contracts.IUSDC.getKey());
        Map<String, Object> configData = ownerClient.lendingPoolCore.getReserveConstants(iusdc);
        assertEquals(iusdc.toString(), configData.get("reserve"));
        assertEquals(BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN),
                toBigInt((String) configData.get("optimalUtilizationRate")));
        assertEquals(BigInteger.valueOf(2).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) configData.get("baseBorrowRate")));
        assertEquals(BigInteger.valueOf(6).multiply(ICX).divide(BigInteger.valueOf(100)),
                toBigInt((String) configData.get("slopeRate1")));
        assertEquals(BigInteger.valueOf(2).multiply(MathUtils.ICX),
                toBigInt((String) configData.get("slopeRate2")));
    }

    private List<score.Address> getReservesInternal() {
        return ownerClient.lendingPoolCore.getReserves();
    }

    private BigInteger toBigInt(String inputString) {
        return new BigInteger(inputString.substring(2), 16);
    }
}
