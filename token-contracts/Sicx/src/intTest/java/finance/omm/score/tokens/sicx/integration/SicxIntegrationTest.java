package finance.omm.score.tokens.sicx.integration;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.StakingScoreClient;
import finance.omm.score.tokens.sicx.integration.config.sICXConfig;
import foundation.icon.jsonrpc.Address;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SicxIntegrationTest implements ScoreIntegrationTest {

    private static OMMClient ommClient;
    private static OMMClient testClient;

    private static Map<String, Address> addressMap;

    @BeforeAll
    void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new sICXConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        ommClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));


    }

    @Test
    @Order(1)
    void name() {
        assertEquals("sICX", testClient.sICX.getPeg());
    }

    @Test
    @Order(2)
    void getSymbol() {
        assertEquals("sICX", testClient.sICX.symbol());
    }

    @Test
    @Order(3)
    void setAndGetStaking() {
        Address stakingScore = addressMap.get(Contracts.STAKING.getKey());
        ommClient.sICX.setStaking(stakingScore);
        assertEquals(stakingScore, testClient.sICX.getStaking());

    }

    @Test
    @Order(4)
    void setMinterAddress() {
        Address stakingScore = addressMap.get(Contracts.STAKING.getKey());
        ommClient.sICX.setMinter(stakingScore);
        assertEquals(stakingScore, testClient.sICX.getMinter());
    }


    @Test
    @Order(5)
    void mint() {
        BigInteger value = BigInteger.valueOf(20).multiply(ICX);
        BigInteger previousSupply = testClient.sICX.totalSupply();
        BigInteger previousBalance = testClient.sICX.balanceOf(ommClient.getAddress());

        ((StakingScoreClient) ommClient.staking).stakeICX(value, null, null);
        assertEquals(previousSupply.add(value), testClient.sICX.totalSupply());
        assertEquals(previousBalance.add(value), testClient.sICX.balanceOf(ommClient.getAddress()));
    }

    @Test
    @Order(6)
    void transfer() {
        BigInteger value = BigInteger.valueOf(5).multiply(ICX);
        BigInteger previousSupply = testClient.sICX.totalSupply();
        BigInteger previousOwnerBalance = testClient.sICX.balanceOf(ommClient.getAddress());
        BigInteger previousTesterBalance = testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString()));
        ommClient.sICX.transfer(Address.fromString(tester.getAddress().toString()), value, null);
        assertEquals(previousSupply, testClient.sICX.totalSupply());
        assertEquals(previousTesterBalance.add(value), testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString())));
        assertEquals(previousOwnerBalance.subtract(value), testClient.sICX.balanceOf(ommClient.getAddress()));
    }

    @Test
    @Order(7)
    void burn() {
        BigInteger previousSupply = testClient.sICX.totalSupply();
        BigInteger previousOwnerBalance = testClient.sICX.balanceOf(ommClient.getAddress());
        BigInteger previousTesterBalance = testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString()));

        Address staking = addressMap.get(Contracts.STAKING.getKey());
        JSONObject data = new JSONObject();
        data.put("method", "unstake");
        BigInteger value = BigInteger.TEN.multiply(ICX);
        ommClient.sICX.transfer(staking, value, data.toString().getBytes());
        assertEquals(previousSupply.subtract(value), testClient.sICX.totalSupply());
        assertEquals(previousTesterBalance, testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString())));
        assertEquals(previousOwnerBalance.subtract(value), testClient.sICX.balanceOf(ommClient.getAddress()));
    }

    @Test
    @Order(8)
    void mintTo() {
        ommClient.sICX.setMinter(ommClient.getAddress());
        BigInteger previousSupply = testClient.sICX.totalSupply();
        BigInteger previousOwnerBalance = testClient.sICX.balanceOf(ommClient.getAddress());
        BigInteger previousTesterBalance = testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString()));
        BigInteger value = BigInteger.valueOf(20).multiply(ICX);
        ommClient.sICX.mintTo(Address.fromString(tester.getAddress().toString()), value, null);
        assertEquals(previousSupply.add(value), testClient.sICX.totalSupply());
        assertEquals(previousTesterBalance.add(value), testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString())));
        assertEquals(previousOwnerBalance, testClient.sICX.balanceOf(ommClient.getAddress()));
    }

    @Test
    @Order(9)
    void burnFrom() {
        ommClient.sICX.setMinter(ommClient.getAddress());
        BigInteger previousSupply = testClient.sICX.totalSupply();
        BigInteger previousOwnerBalance = testClient.sICX.balanceOf(ommClient.getAddress());
        BigInteger previousTesterBalance = testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString()));
        BigInteger value = BigInteger.TEN.multiply(ICX);
        ommClient.sICX.burnFrom(Address.fromString(tester.getAddress().toString()), value);
        assertEquals(previousSupply.subtract(value), testClient.sICX.totalSupply());
        assertEquals(previousTesterBalance.subtract(value), testClient.sICX.balanceOf(Address.fromString(tester.getAddress().toString())));
        assertEquals(previousOwnerBalance, testClient.sICX.balanceOf(ommClient.getAddress()));
    }


}
