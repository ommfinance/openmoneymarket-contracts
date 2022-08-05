package finance.omm.score.core.lendingpool.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;

import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.lendingpool.exception.LendingPoolException;
import finance.omm.score.core.lendingpool.integration.config.lendingPoolConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;


import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.score.core.lendingpool.AbstractLendingPool.TAG;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LendingPoolIT implements ScoreIntegrationTest{

    private static OMMClient ommClient;
    private static OMMClient testClient;

    private static Map<String, foundation.icon.jsonrpc.Address> addressMap;

    @BeforeAll
    void setup() throws  Exception{
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        System.out.println("address map " + addressMap);
        Config config = new lendingPoolConfig();
        omm.runConfig(config);
        ommClient = omm.defaultClient();
        testClient = omm.testClient();
        mintToken();
    }

    @Test
    void testName() {
        assertEquals("OMM Lending Pool",ommClient.lendingPool.name());
    }
    @Test
    void deposit_icx(){
        BigInteger depositAmt = BigInteger.valueOf(100);
        ommClient.lendingPool.deposit(depositAmt);
    }

    @Test
    void deposit_token(){
        ommClient.iUSDC.transfer(testClient.getAddress(),
                BigInteger.valueOf(10).multiply(BigInteger.valueOf(100_000)),new byte[]{});
        System.out.println("bal "+testClient.iUSDC.balanceOf(testClient.getAddress()));
        depositIUSDC(testClient,BigInteger.valueOf(10));
    }

//    @Test
//    void deposit_payble(){
//        depositICX(ommClient,BigInteger.valueOf(10));
//    }

    @Test
    void borrow_more_than_available(){
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        assertUserRevert(LendingPoolException.unknown(TAG + "Amount requested " + borrowAmt +
                        " is more then the "),
                () -> testClient.lendingPool.borrow(iusdc_reserve,borrowAmt),null);

    }

    @Test
    void borrow_not_active_reserve(){
//        depositIUSDC(ommClient,BigInteger.valueOf(10000));
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        testClient.lendingPool.deposit(BigInteger.valueOf(200));


        ommClient.governance.setReserveActiveStatus(iusdc_reserve,false);
        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, borrow unsuccessful"),
                () -> testClient.lendingPool.borrow(iusdc_reserve,borrowAmt),null);

    }

    @Test
    void borrow_freeze_reserve(){
//        depositIUSDC(ommClient,BigInteger.valueOf(10000));
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        testClient.lendingPool.deposit(BigInteger.valueOf(200));

        ommClient.governance.setReserveFreezeStatus(iusdc_reserve,true);
        assertUserRevert(LendingPoolException.unknown("Reserve is frozen, borrow unsuccessful"),
                () -> testClient.lendingPool.borrow(iusdc_reserve,borrowAmt),null);

    }

    @Test
    void reserve_borrow_not_enabled(){
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        testClient.lendingPool.deposit(BigInteger.valueOf(200));

        ommClient.governance.updateBorrowingEnabled(iusdc_reserve,false);
        assertUserRevert(LendingPoolException.unknown("Borrow error:borrowing not enabled in the reserve"),
                () -> testClient.lendingPool.borrow(iusdc_reserve,borrowAmt),null);

    }


    /*
    deposit ICX as collateral
    */
    private void depositICX(OMMClient client, BigInteger amount){
        ((LendingPoolScoreClient)client.lendingPool).deposit(amount.multiply(ICX),amount.multiply(ICX));
    }

    /*
    deposit IUSDC as collateral
     */
    private void depositIUSDC(OMMClient client, BigInteger amount){
        byte[] data = createByteArray("deposit",amount,null,null,null);
        client.iUSDC.transfer(addressMap.get(Contracts.LENDING_POOL.getKey()),
                amount.multiply(BigInteger.valueOf(100_000)),data);
    }

    private void mintToken(){
        BigInteger amount = BigInteger.valueOf(100_000_000).multiply(ICX);
        ommClient.iUSDC.addIssuer(ommClient.getAddress());
        ommClient.iUSDC.approve(ommClient.getAddress(),amount);
        ommClient.iUSDC.mintTo(ommClient.getAddress(),amount);
    }

    private byte[] createByteArray(String methodName, BigInteger value,
                                   @Optional Address collateral, @Optional Address reserve, @Optional Address user) {

        JsonObject internalParameters = new JsonObject()
                .add("amount", String.valueOf(value))
                .add("_collateral", String.valueOf(collateral))
                .add("_reserve", String.valueOf(reserve))
                .add("_user", String.valueOf(user));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

    private void reserveSetup(){
        depositICX(ommClient,BigInteger.valueOf(1000));
        depositIUSDC(ommClient,BigInteger.valueOf(1000));
    }



}
