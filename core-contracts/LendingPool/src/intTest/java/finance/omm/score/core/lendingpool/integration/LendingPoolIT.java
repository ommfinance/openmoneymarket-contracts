package finance.omm.score.core.lendingpool.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;

import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import finance.omm.libs.test.integration.scores.LendingPoolScoreClient;
import finance.omm.score.core.lendingpool.exception.LendingPoolException;
import finance.omm.score.core.lendingpool.integration.config.lendingPoolConfig;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import score.Address;
import score.UserRevertedException;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


import static finance.omm.libs.test.AssertRevertedException.assertReverted;
import static finance.omm.libs.test.AssertRevertedException.assertUserRevert;
import static finance.omm.score.core.lendingpool.AbstractLendingPool.TAG;
import static finance.omm.utils.math.MathUtils.HALF_ICX;
import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


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
        reserveSetup();
    }

    @Test
    void testName() {
        assertEquals("Omm Lending Pool",ommClient.lendingPool.name());
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
    @Test
    void deposit_payble(){
        depositICX(ommClient,BigInteger.valueOf(10));
    }

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

    @Test
    void testTokenFallbackExceptions(){

        Address collateral = addressMap.get(Contracts.sICX.getKey());
        Address reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address user = testClient.getAddress();

        byte[] data = createByteArray("abcd",null, collateral,reserve,user);
        BigInteger value = BigInteger.TEN.multiply(ICX);
        byte[] finalData = data;
        assertUserRevert(LendingPoolException.unknown(TAG + " No valid method called, data: "+ data.toString()),
                ()->ommClient.lendingPool.tokenFallback(ommClient.getAddress(), value, finalData),null);

        data = createByteArray("liquidationCall",null, null,reserve,user);
        byte[] finalData1 = data;
        assertUserRevert(LendingPoolException.unknown(TAG + " Invalid data: Collateral: " + collateral +
                        " Reserve: "+reserve+ " User: "+ user),
                ()->ommClient.lendingPool.tokenFallback(ommClient.getAddress(), value, finalData1),null);

        data = createByteArray("liquidationCall",null, collateral,null,user);
        byte[] finalData2 = data;
        assertUserRevert(LendingPoolException.unknown(TAG + " Invalid data: Collateral: " + collateral +
                        " Reserve: "+reserve+ " User: "+ user),
                ()->ommClient.lendingPool.tokenFallback(ommClient.getAddress(), value, finalData2),null);

        data = createByteArray("liquidationCall",null, collateral,reserve,null);
        byte[] finalData3 = data;
        assertUserRevert(LendingPoolException.unknown(TAG + " Invalid data: Collateral: " + collateral +
                        " Reserve: "+reserve+ " User: "+ user),
                ()->ommClient.lendingPool.tokenFallback(ommClient.getAddress(), value, finalData3),null);
    }

    @Test
    void deposit_IUSDC(){

    }

    @Test
    void redeem_reserve_inactive(){
        depositICX(ommClient,BigInteger.valueOf(1000));

        Address icxAddr = addressMap.get(Contracts.sICX.getKey());

        ommClient.governance.setReserveActiveStatus(icxAddr,false);

        assertUserRevert(LendingPoolException.reserveNotActive("Reserve is not active, redeem unsuccessful"),
                ()->ommClient.lendingPool.redeem(addressMap.get(Contracts.oICX.getKey()),BigInteger.valueOf(50).
                        multiply(ICX),false), null);

    }

    @Test
    void redeem_more_than_liquidity(){
        depositICX(ommClient,BigInteger.valueOf(1000));
        // 1000 ICX deposited in reserve setup
        //total liquidity available = 2000
        Address icxAddr = addressMap.get(Contracts.oICX.getKey());

        BigInteger amount =BigInteger.valueOf(1000).multiply(ICX);
        BigInteger availableLiquidity = BigInteger.valueOf(2000).multiply(ICX);

        ommClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(1001).multiply(ICX),false);

        assertReverted(new RevertedException(1,"Amount " + amount + " is more than available liquidity " +
                        availableLiquidity), ()->ommClient.lendingPool.redeem(icxAddr,amount,false));
    }

    @Test
    void redeem_waitForUnstaking(){
        depositICX(ommClient,BigInteger.valueOf(1000));

        Address icxAddr = addressMap.get(Contracts.oICX.getKey());

        ommClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(50).multiply(ICX),true);

        assertUserRevert(LendingPoolException.unknown("Redeem with wait for unstaking failed: Invalid token"),
                ()->ommClient.lendingPool.redeem(icxAddr,BigInteger.valueOf(50).multiply(ICX),false),
                null);
    }

    @Test
    void redeem_success_test(){
        Address icxAddr = addressMap.get(Contracts.oICX.getKey());

        BigInteger amount =BigInteger.valueOf(500).multiply(ICX);

        ommClient.lendingPool.redeem(icxAddr,amount,false);

        assertEquals(ommClient.oICX.balanceOf(ommClient.getAddress()),BigInteger.valueOf(500).multiply(ICX));

        depositICX(testClient,BigInteger.valueOf(1000));

        testClient.lendingPool.redeem(icxAddr,amount,false);

        assertEquals(ommClient.oICX.balanceOf(testClient.getAddress()),BigInteger.valueOf(500).multiply(ICX));

    }


    @Test
    void stake(){
        assertReverted(new RevertedException(1, "Staking of OMM token no longer supported."),
                () -> ommClient.lendingPool.stake(BigInteger.valueOf(500).multiply(ICX)));

    }

    @Test
    void unstake(){
        BigInteger value = BigInteger.ONE.negate();
        assertReverted(new RevertedException(1, "Cannot unstake less than or equals to zero value to stake " + value),
                () -> ommClient.lendingPool.unstake(value.multiply(ICX)));

        BigInteger finalValue = BigInteger.valueOf(100);
        assertReverted(new RevertedException(1, "Cannot unstake,user dont have enough staked balance amount to unstake "
                        + value + " staked balance of user: " + ommClient.getAddress() + " is " + BigInteger.ZERO),
                () -> ommClient.lendingPool.unstake(finalValue.multiply(ICX)));

    }

    @Test
    void checkDepositWallets(){
        depositICX(testClient,BigInteger.valueOf(1000));

        List<Address> list= ommClient.lendingPool.getDepositWallets(1);

        assertEquals(2,list.size());
        assertEquals(ommClient.getAddress(),list.get(0));
        assertEquals(testClient.getAddress(),list.get(1));
    }

    @Test
    void checkBorrowWallets(){
        depositICX(testClient,BigInteger.valueOf(1000));

        List<Address> list= ommClient.lendingPool.getBorrowWallets(1);
        assertEquals(0,list.size());

        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());
        ommClient.lendingPool.borrow(iusdc_reserve,BigInteger.valueOf(100));
        testClient.lendingPool.borrow(iusdc_reserve,BigInteger.valueOf(100));

        assertEquals(2,list.size());
        assertEquals(ommClient.getAddress(),list.get(0));
        assertEquals(testClient.getAddress(),list.get(1));
    }




    @Test
    void deposit_tokenfallback(){
        byte[] data = createByteArray("deposit",null, null,
                null,null);
        ommClient.lendingPool.tokenFallback(ommClient.getAddress(), BigInteger.valueOf(1000).multiply(ICX), data);
    }

    @Test
    void repay_tokenfallback(){
        byte[] data = createByteArray("deposit",null, null,
                null,null);
        ommClient.lendingPool.tokenFallback(ommClient.getAddress(), BigInteger.valueOf(1000).multiply(ICX), data);
    }

    @Test
    void liquidation_tokenfallback(){
        Address collateral = addressMap.get(Contracts.sICX.getKey());
        Address reserve = addressMap.get(Contracts.IUSDC.getKey());
        Address user = testClient.getAddress();

        ommClient.dummyPriceOracle.set_reference_data("ICX", ICX);

        //100 deposit icx 40 borrow usdc
        depositICX(testClient, BigInteger.valueOf(1000));

        depositIUSDC(ommClient, BigInteger.valueOf(100));

        Address IUSDCAddr = addressMap.get(Contracts.IUSDC.getKey());
        BigInteger amountToBorrowIUSDC = BigInteger.valueOf(400).multiply(BigInteger.valueOf(1000_000));

        // test client borrows 40 IUSDC
        testClient.lendingPool.borrow(IUSDCAddr, amountToBorrowIUSDC);

        //0.5 icx
        ommClient.dummyPriceOracle.set_reference_data("ICX", HALF_ICX);

        byte[] data = createByteArray("liquidationCall", null, collateral, reserve, user);
        BigInteger value = BigInteger.valueOf(50).multiply(BigInteger.valueOf(1000_000));
        ommClient.lendingPool.tokenFallback(ommClient.getAddress(), value, data);


//        BigInteger balanceAfterLiq = ommClient.oICX.principalBalanceOf(testClient.getAddress());
//        BigInteger feeProvider = ommClient.sICX.balanceOf(addressMap.get("feeProvider"));
//        BigInteger repayEq = BigInteger.TEN.multiply(ICX);
//        BigInteger tenPercent = BigInteger.ONE.multiply(ICX);
//
//        assertEquals(prevBalance.subtract(feeProvider.add(repayEq).add(tenPercent)),balanceAfterLiq);
//        //10+10% of 10 oICX + fee provider
    }


/*
java -> fucntion returpns map(string,object)
{
'reserve': addr,
'sth': str,
'sth: bigint
}

function returna map
{
'reserve': addr.toString(),.
..
..
}

another contract

// dToken error in borrow
 */
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

    @Test
    void borrow_not_enough_liquidity(){ // not working
        //        depositIUSDC(ommClient,BigInteger.valueOf(10000));
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        testClient.lendingPool.deposit(BigInteger.valueOf(200));

//        assertUserRevert(LendingPoolException.unknown("Borrow error:Not enough available liquidity in the reserve"),
//                () -> testClient.lendingPool.borrow(iusdc_reserve,borrowAmt),null);

        testClient.lendingPool.borrow(iusdc_reserve,borrowAmt);

    }


    @Test
    void borrow_more_than_collateral(){ // not working
        BigInteger borrowAmt = BigInteger.valueOf(100);
        Address iusdc_reserve = addressMap.get(Contracts.IUSDC.getKey());

        testClient.lendingPool.deposit(BigInteger.valueOf(200));

        ommClient.lendingPool.borrow(iusdc_reserve,borrowAmt);

    }



}
