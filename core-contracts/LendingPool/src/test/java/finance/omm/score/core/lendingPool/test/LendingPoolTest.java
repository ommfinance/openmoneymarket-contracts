package finance.omm.score.core.lendingPool.test;

import com.eclipsesource.json.JsonObject;
import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.score.core.lendingpool.AbstractLendingPool.TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LendingPoolTest extends AbstractLendingPoolTest{
    Account OMM_TOKEN_ACCOUNT = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN);

    @Test
    public void name(){
        String expected = "Omm " + TAG;
        assertEquals(expected, score.call("name"));
    }

    /*
    getters and setters
     */
    @Test
    public void getters_and_setters(){
        BigInteger amount = BigInteger.valueOf(1);
        score.invoke(owner,"setBridgeFeeThreshold",amount);

        assertEquals(amount,score.call("getBridgeFeeThreshold"));

        BigInteger limit = BigInteger.valueOf(50);
        score.invoke(owner,"setFeeSharingTxnLimit",limit);
        assertEquals(limit,score.call("getFeeSharingTxnLimit"));

        Executable call = () -> score.invoke(notOwner,"setBridgeFeeThreshold",amount);
        expectErrorMessage(call,"require owner access");

        call =() -> score.invoke(notOwner,"setFeeSharingTxnLimit",amount);
        expectErrorMessage(call,"require owner access");
    }

    @Test
    public void check_fee_sharing(){
        mockFeeSharing();

        doReturn(BigInteger.valueOf(10).multiply(ICX)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), eq(owner.getAddress()));
        score.invoke(OMM_TOKEN_ACCOUNT,"isFeeSharingEnable",owner.getAddress());

        doReturn(BigInteger.valueOf(0)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), eq(notOwner.getAddress()));
        score.invoke(OMM_TOKEN_ACCOUNT,"isFeeSharingEnable",notOwner.getAddress());

        Executable call = () -> score.invoke(notOwner,"isFeeSharingEnable",owner.getAddress());
        expectErrorMessage(call,TAG + " Sender not OMM Token");
    }

    @Test
    public void redeem(){
        Address reserveAddress = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Address oICX = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        BigInteger amountToRedeem = BigInteger.valueOf(10).multiply(ICX);

        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), any());

        doReturn(oICX).when(scoreSpy).call(Address.class, Contracts.LENDING_POOL_CORE,
                "getReserveOTokenAddress", reserveAddress);

        doReturn(Map.of(
                "reserve",reserveAddress,
                "amountToRedeem",amountToRedeem
        )).when(scoreSpy).call(Map.class,oICX,"redeem",notOwner.getAddress(),amountToRedeem);


        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddress);
        Executable call = () -> score.invoke(notOwner,"redeem",reserveAddress,amountToRedeem,false);
        expectErrorMessage(call,"Reserve is not active, withdraw unsuccessful");


        doReturn(Map.of(
                "isActive",true,
                "availableLiquidity",BigInteger.ONE
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddress);
        call = () -> score.invoke(notOwner,"redeem",reserveAddress,amountToRedeem,false);
        expectErrorMessage(call,"Amount " + amountToRedeem + " is more than available liquidity " +
                BigInteger.ONE);


        doReturn(Map.of(
                "isActive",true,
                "availableLiquidity",BigInteger.valueOf(11).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddress);
        doNothing().when(scoreSpy).call(Contracts.LENDING_POOL_CORE, "updateStateOnRedeem",
                reserveAddress, notOwner.getAddress(),amountToRedeem );

        byte[] nullData = new JsonObject().toString().getBytes();
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("transferToUser"),
                eq(reserveAddress), eq(notOwner.getAddress()), eq(amountToRedeem),
                eq(nullData));
        score.invoke(notOwner,"redeem",reserveAddress,amountToRedeem,false);


        verify(scoreSpy,times(1)).RedeemUnderlying(reserveAddress,notOwner.getAddress(),
                amountToRedeem);
        verify(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("transferToUser"), eq(reserveAddress),
                eq(notOwner.getAddress()), eq(amountToRedeem), any());
        verify(scoreSpy,times(3)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddress);
        verify(scoreSpy,times(3)).call(Map.class,oICX,"redeem",notOwner.getAddress(),
                amountToRedeem);
        verify(scoreSpy).call(Contracts.LENDING_POOL_CORE, "updateStateOnRedeem",
                reserveAddress, notOwner.getAddress(), amountToRedeem);

    }

    @Test
    void redeem_when_unstaking_true(){

        Address reserveAddres = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Address oToken = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        BigInteger amountToRedeem = BigInteger.valueOf(10).multiply(ICX);

        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), any());

        doReturn(Map.of(
                "reserve",reserveAddres,
                "amountToRedeem",amountToRedeem
        )).when(scoreSpy).call(Map.class,oToken,"redeem",notOwner.getAddress(),amountToRedeem);

        doReturn(oToken).when(scoreSpy).call(Address.class, Contracts.LENDING_POOL_CORE,
                "getReserveOTokenAddress", reserveAddres);

        doReturn(Map.of(
                "isActive",true,
                "availableLiquidity",BigInteger.valueOf(11).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddres);
        doNothing().when(scoreSpy).call(Contracts.LENDING_POOL_CORE, "updateStateOnRedeem",
                reserveAddres, notOwner.getAddress(),amountToRedeem );


        byte[] data= jsonDataCompute(notOwner.getAddress());
        Address staking = MOCK_CONTRACT_ADDRESS.get(Contracts.STAKING).getAddress();
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("transferToUser"),
                eq(reserveAddres), eq(staking), eq(amountToRedeem),
                eq(data));
        score.invoke(notOwner,"redeem",reserveAddres,amountToRedeem,true);


        Address notoICX = MOCK_CONTRACT_ADDRESS.get(Contracts.oIUSDC).getAddress();
        doReturn(notoICX).when(scoreSpy).call(Address.class, Contracts.LENDING_POOL_CORE,
                "getReserveOTokenAddress", reserveAddres);

        doReturn(Map.of(
                "reserve",reserveAddres,
                "amountToRedeem",amountToRedeem
        )).when(scoreSpy).call(Map.class,notoICX,"redeem",notOwner.getAddress(),amountToRedeem);

        Executable call= () -> score.invoke(notOwner,"redeem",reserveAddres,amountToRedeem,true);
        expectErrorMessage(call,"Redeem with wait for unstaking failed: Invalid token");


        verify(scoreSpy,times(1)).RedeemUnderlying(reserveAddres,notOwner.getAddress(),
                amountToRedeem);
        verify(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("transferToUser"), eq(reserveAddres),
                eq(staking), eq(amountToRedeem), any());
        verify(scoreSpy,times(2)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserveAddres);
        verify(scoreSpy).call(Map.class,oToken,"redeem",notOwner.getAddress(),
                amountToRedeem);
        verify(scoreSpy).call(Map.class,notoICX,"redeem",notOwner.getAddress(),
                amountToRedeem);
        verify(scoreSpy,times(2)).call(Contracts.LENDING_POOL_CORE, "updateStateOnRedeem",
                reserveAddres, notOwner.getAddress(), amountToRedeem);
    }

    @Test
    void redeem_from_invalid_reserve(){
        Address invalidReserve = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        BigInteger amountToRedeem = BigInteger.valueOf(10).multiply(ICX);

        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(BigInteger.class, Contracts.BRIDGE_O_TOKEN,"balanceOf", owner.getAddress());

        Executable call = () -> score.invoke(owner,"redeem",invalidReserve,amountToRedeem,false);
        expectErrorMessage(call, "Lending Pool " + invalidReserve + " is not valid");

    }

    @Test
    void claimRewards(){
        feeSharingMock();
        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("claimRewards"),any());

        // user has not deposited bridge oToken
        score.invoke(notOwner,"claimRewards");

        // user has deposited bridge oToken
        score.invoke(owner,"claimRewards");

        verify(scoreSpy,times(2)).call(eq(Contracts.REWARDS),eq("claimRewards"),
                any(Address.class));

    }

    @Test
    void stake(){
        feeSharingMock();
        doNothing().when(scoreSpy).call(eq(Contracts.OMM_TOKEN),eq("stake"),any(BigInteger.class),
                any(Address.class));

        // user has not deposited bridge oToken
        score.invoke(notOwner,"stake",BigInteger.ONE);

        // user has not deposited bridge oToken
        score.invoke(owner,"stake",BigInteger.ONE);

        verify(scoreSpy,times(2)).call(eq(Contracts.OMM_TOKEN),eq("stake"),
                any(BigInteger.class),any(Address.class));

    }

    @Test
    void unstake(){
        feeSharingMock();
        doNothing().when(scoreSpy).call(eq(Contracts.OMM_TOKEN),eq("unstake"),any(),any());

        // user has not deposited bridge oToken
        score.invoke(notOwner,"unstake",BigInteger.ONE);

        // user has not deposited bridge oToken
        score.invoke(owner,"unstake",BigInteger.ONE);

        verify(scoreSpy,times(2)).call(eq(Contracts.OMM_TOKEN),eq("unstake"),
                any(BigInteger.class),any(Address.class));

    }

    @Test
    void borrow_should_fail(){
        Address sICXReserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        feeSharingMock();

        BigInteger borrowAmount = BigInteger.valueOf(100);
        doReturn(Map.of(
                "availableBorrows", borrowAmount
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", sICXReserve);

        Executable call= () -> score.invoke(notOwner,"borrow",sICXReserve,BigInteger.valueOf(1000));
        expectErrorMessage(call,TAG + "Amount requested 1000 is more then the 100");


        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICXReserve);

        call= () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Reserve is not active, borrow unsuccessful");


        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",true,
                "isFreezed",true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICXReserve);

        call= () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Reserve is frozen, borrow unsuccessful");


        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",true,
                "isFreezed",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICXReserve);
        doReturn(false).when(scoreSpy).call(Boolean.class,
                Contracts.LENDING_POOL_CORE, "isReserveBorrowingEnabled", sICXReserve);

        call= () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error:borrowing not enabled in the reserve");


        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",true,
                "isFreezed",false,
                "availableLiquidity",BigInteger.valueOf(10)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", sICXReserve);
        doReturn(true).when(scoreSpy).call(Boolean.class,
                Contracts.LENDING_POOL_CORE, "isReserveBorrowingEnabled", sICXReserve);

        call= () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error:Not enough available liquidity in the reserve");


        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",true,
                "isFreezed",false,
                "availableLiquidity", borrowAmount
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", sICXReserve);

        doReturn(Map.of(
                "totalCollateralBalanceUSD",BigInteger.valueOf(0),
                "totalBorrowBalanceUSD",BigInteger.valueOf(10),
                "totalFeesUSD",BigInteger.valueOf(2),
                "currentLtV",BigInteger.valueOf(65).multiply(ICX),
                "healthFactorBelowThreshold",true
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getUserAccountData"), any());

        call = () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error: The user does not have any collateral");


        doReturn(Map.of(
                "totalCollateralBalanceUSD",BigInteger.valueOf(2000),
                "totalBorrowBalanceUSD",BigInteger.valueOf(10),
                "totalFeesUSD",BigInteger.valueOf(2),
                "currentLtV",BigInteger.valueOf(65).multiply(ICX),
                "healthFactorBelowThreshold",true
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getUserAccountData"), any());

        call = () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error: Health factor is below threshold");


        doReturn(Map.of(
                "totalCollateralBalanceUSD",BigInteger.valueOf(2000),
                "totalBorrowBalanceUSD",BigInteger.valueOf(10),
                "totalFeesUSD",BigInteger.valueOf(2),
                "currentLtv",BigInteger.valueOf(65).multiply(ICX),
                "healthFactorBelowThreshold",false
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getUserAccountData"), any());
        doReturn(BigInteger.ZERO).when(scoreSpy).call(BigInteger.class, Contracts.FEE_PROVIDER,
                "calculateOriginationFee", borrowAmount);

        call = () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error: borrow amount is very small");


        doReturn(BigInteger.ONE).when(scoreSpy).call(BigInteger.class, Contracts.FEE_PROVIDER,
                "calculateOriginationFee", borrowAmount);
        doReturn(BigInteger.valueOf(2001)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("calculateCollateralNeededUSD"), any(), any(), any(),
                any(), any(), eq(BigInteger.valueOf(65).multiply(ICX)));

        call = () -> score.invoke(notOwner,"borrow",sICXReserve, borrowAmount);
        expectErrorMessage(call,"Borrow error: Insufficient collateral to cover new borrow");

        verify(scoreSpy,times(9)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICXReserve);
        verify(scoreSpy,times(6)).call(Boolean.class, Contracts.LENDING_POOL_CORE,
                "isReserveBorrowingEnabled", sICXReserve);
        verify(scoreSpy,times(4)).call(eq(Map.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getUserAccountData"), eq(notOwner.getAddress()));
        verify(scoreSpy,times(2)).call(BigInteger.class, Contracts.FEE_PROVIDER,
                "calculateOriginationFee", borrowAmount);
        verify(scoreSpy,times(1)).call(eq(BigInteger.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("calculateCollateralNeededUSD"), eq(sICXReserve), eq(borrowAmount),
                any(BigInteger.class), any(BigInteger.class), any(BigInteger.class), any(BigInteger.class));
    }

    @Test
    void borrow(){
        Address sICXReserve = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        feeSharingMock();

        doReturn(Map.of(
                "availableBorrows",BigInteger.valueOf(1000),
                "isActive",true,
                "isFreezed",false,
                "availableLiquidity",BigInteger.valueOf(100)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", sICXReserve);

        doReturn(true).when(scoreSpy).call(Boolean.class,
                Contracts.LENDING_POOL_CORE, "isReserveBorrowingEnabled", sICXReserve);

        doReturn(Map.of(
                "totalCollateralBalanceUSD",BigInteger.valueOf(2000),
                "totalBorrowBalanceUSD",BigInteger.valueOf(10),
                "totalFeesUSD",BigInteger.valueOf(2),
                "currentLtV",BigInteger.valueOf(65).multiply(ICX),
                "healthFactorBelowThreshold",false
        )).when(scoreSpy).call(eq(Map.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("getUserAccountData"), any());

        doReturn(BigInteger.ONE).when(scoreSpy).call(BigInteger.class, Contracts.FEE_PROVIDER,
                "calculateOriginationFee", BigInteger.valueOf(100));

        doReturn(BigInteger.valueOf(250)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.LENDING_POOL_DATA_PROVIDER),
                eq("calculateCollateralNeededUSD"), any(), any(), any(),
                any(), any(), any());
        doReturn(Map.of(
                "currentBorrowRate",BigInteger.valueOf(10).multiply(ICX),
                "balanceIncrease",BigInteger.valueOf(250)
        )).when(scoreSpy).call(eq(Map.class),eq( Contracts.LENDING_POOL_CORE), eq("updateStateOnBorrow"),
                eq(sICXReserve), any(), any(), any());

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("transferToUser"), any(),
                any(), any());

        score.invoke(notOwner,"borrow",sICXReserve,BigInteger.valueOf(100));

        verify(scoreSpy).call(Contracts.LENDING_POOL_CORE, "transferToUser",
                sICXReserve, notOwner.getAddress(), BigInteger.valueOf(100));
        verify(scoreSpy).call(eq(Map.class),eq( Contracts.LENDING_POOL_CORE), eq("updateStateOnBorrow"),
                eq(sICXReserve), any(), any(), any());
        verify(scoreSpy).Borrow(sICXReserve, notOwner.getAddress(),BigInteger.valueOf(100),
                BigInteger.valueOf(10).multiply(ICX) ,BigInteger.ONE,BigInteger.valueOf(250));
        verify(scoreSpy).call(eq(Map.class),eq( Contracts.LENDING_POOL_CORE), eq("updateStateOnBorrow"),
                eq(sICXReserve), any(), any(), any());
    }

    @Test
    void tokenFallback(){
        byte[] invalidData = new byte[]{};
        Executable call = () -> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), invalidData);
        expectErrorMessage(call,TAG +" Invalid data: " + invalidData.toString());

        byte[] invalidMethod = createByteArray("tokenFallback",null,null,null);
        call = () -> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), invalidMethod);
        expectErrorMessage(call,TAG + " No valid method called, data: "+ invalidMethod.toString());


        Address collateral = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        byte[] invalidParams = createByteArray_liquidation("liquidationCall",collateral);
        call = () -> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), invalidParams);
        expectErrorMessage(call,TAG +" Invalid data: Collateral: " + collateral +
                " User: "+ null);
    }



    @Test
    void liquidationCall(){
        Account collateralAccount = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX);
        Account reserveAccount =  MOCK_CONTRACT_ADDRESS.get(Contracts.IUSDC);
        Address collateral = collateralAccount.getAddress();
        Address reserve = reserveAccount.getAddress();

        byte[] liquidationCall = createByteArray("liquidationCall",collateral,reserve,
                notOwner.getAddress());

        doReturn(false).when(scoreSpy).isLiquidationEnabled();

        Executable call = () -> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), liquidationCall);
        expectErrorMessage(call,"Liquidation is not enabled,liquidation unsuccessful");

        doReturn(true).when(scoreSpy).isLiquidationEnabled();

        doReturn(Map.of()).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);
        call = () -> score.invoke(reserveAccount,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), liquidationCall);
        expectErrorMessage(call,TAG + "reserve data is empty :: " + reserve);

        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);

        doReturn(Map.of()).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);
        call = () -> score.invoke(reserveAccount,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), liquidationCall);
        expectErrorMessage(call,TAG + "collateral data is empty :: " + collateral);

        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);

        call = () -> score.invoke(reserveAccount,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), liquidationCall);
        expectErrorMessage(call,"Borrow reserve is not active,liquidation unsuccessful");



        doReturn(Map.of(
                "isActive",true)).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);
        doReturn(Map.of(
                "isActive",false)).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);
        call = () -> score.invoke(reserveAccount,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), liquidationCall);
        expectErrorMessage(call,"Collateral reserve is not active,liquidation unsuccessful");



        doReturn(Map.of(
                "isActive",true)).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);
        doReturn(Map.of(
                "actualAmountToLiquidate",BigInteger.TWO.multiply(ICX),
                "maxCollateralToLiquidate",BigInteger.TWO.multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.LIQUIDATION_MANAGER, "liquidationCall",
                collateral, reserve, notOwner.getAddress(), BigInteger.valueOf(10).multiply(ICX));
        doNothing().when(scoreSpy).call(Contracts.LENDING_POOL_CORE,"transferToUser", collateral,
                notOwner.getAddress(), BigInteger.TWO.multiply(ICX));
        doNothing().when(scoreSpy).call(reserve, "transfer",
                MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress() ,
                BigInteger.TWO.multiply(ICX));
        doNothing().when(scoreSpy).call(reserve, "transfer", notOwner.getAddress(), BigInteger.valueOf(8).
                multiply(ICX));

        score.invoke(reserveAccount,"tokenFallback",notOwner.getAddress(), BigInteger.valueOf(10).multiply(ICX),
                liquidationCall);

        verify(scoreSpy,times(4)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);
        verify(scoreSpy).call(reserve, "transfer", notOwner.getAddress(), BigInteger.valueOf(8).
                multiply(ICX));
        verify(scoreSpy).call(Contracts.LENDING_POOL_CORE,"transferToUser", collateral,
                notOwner.getAddress(), BigInteger.TWO.multiply(ICX));
        verify(scoreSpy).call(Map.class, Contracts.LIQUIDATION_MANAGER, "liquidationCall",
                collateral, reserve, notOwner.getAddress(), BigInteger.valueOf(10).multiply(ICX));

    }

    @Test
    void deposit_token(){
        byte[] depositMethod = createByteArray("deposit",null,null,null);
        Address sICX = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        BigInteger amount = BigInteger.valueOf(10).multiply(ICX);

        contextMock.when(caller()).thenReturn(sICX);

        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), eq(sICX));


        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);
        Executable call = ()-> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
               amount, depositMethod);
        expectErrorMessage(call,"Reserve is not active, deposit unsuccessful");


        doReturn(Map.of(
                "isActive",true,
                "isFreezed",true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);
        call = ()-> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                amount, depositMethod);
        expectErrorMessage(call,"Reserve is frozen, deposit unsuccessful");

        Address oTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();

        doReturn(Map.of(
                "isActive",true,
                "isFreezed",false,
                "oTokenAddress",oTokenAddr
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX); // any
        doNothing().when(scoreSpy). call(Contracts.LENDING_POOL_CORE, "updateStateOnDeposit", sICX,
                notOwner.getAddress(), amount); // any
        doNothing().when(scoreSpy).call(oTokenAddr, "mintOnDeposit", notOwner.getAddress(), amount);
        doNothing().when(scoreSpy).call(eq(sICX),eq("transfer"),any(Address.class),eq(amount));

        score.invoke(notOwner,"tokenFallback",notOwner.getAddress(), amount, depositMethod);


        verify(scoreSpy).Deposit(eq(sICX),eq(notOwner.getAddress()),any(BigInteger.class));
        verify(scoreSpy,times(3)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);
        verify(scoreSpy,times(1)).call(oTokenAddr, "mintOnDeposit", notOwner.getAddress(), amount);
        verify(scoreSpy).call(eq(sICX),eq("transfer"),any(Address.class),eq(amount));

    }

    @Test
    void deposit_icx(){
        BigInteger depositAmt = BigInteger.valueOf(10).multiply(ICX);
        contextMock.when(sendICX()).thenReturn(BigInteger.ONE);

        Executable call = () -> score.invoke(notOwner,"deposit",depositAmt);
        expectErrorMessage(call,TAG + " : Amount in param " +
                depositAmt + " doesnt match with the icx sent " + BigInteger.ONE + " to the Lending Pool");

        Address oTokenAddr = MOCK_CONTRACT_ADDRESS.get(Contracts.oICX).getAddress();
        Address sICX = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        Address staking = MOCK_CONTRACT_ADDRESS.get(Contracts.STAKING).getAddress();
        Address lendingPoolCore = MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress();

        contextMock.when(staking(depositAmt,staking,lendingPoolCore)).thenReturn(BigInteger.ONE);

        contextMock.when(sendICX()).thenReturn(BigInteger.valueOf(10).multiply(ICX));
        doReturn(BigInteger.valueOf(1).multiply(ICX)).when(scoreSpy).
                call(BigInteger.class, Contracts.STAKING, "getTodayRate");


        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.BRIDGE_O_TOKEN), eq("balanceOf"), eq(notOwner.getAddress()));

        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX); // any

        call = () -> score.invoke(notOwner,"deposit",depositAmt);
        expectErrorMessage(call,"Reserve is not active, deposit unsuccessful");

        doReturn(Map.of(
                "isActive",true,
                "isFreezed",true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX); // any

        call = () -> score.invoke(notOwner,"deposit",depositAmt);
        expectErrorMessage(call,"Reserve is frozen, deposit unsuccessful");

        doReturn(Map.of(
                "isActive",true,
                "isFreezed",false,
                "oTokenAddress",oTokenAddr
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX); // any
        doNothing().when(scoreSpy). call(Contracts.LENDING_POOL_CORE, "updateStateOnDeposit", sICX,
                notOwner.getAddress(), depositAmt); // any
        doNothing().when(scoreSpy).call(oTokenAddr, "mintOnDeposit", notOwner.getAddress(), depositAmt);


        score.invoke(notOwner,"deposit",depositAmt);
    }


    @Test
    void repay(){
        Address sICX = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        byte[] repayMethod = createByteArray("repay",null,null,null);
        contextMock.when(caller()).thenReturn(sICX);

        doReturn(Map.of(
                "isActive",false
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);

        Executable call = ()-> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), repayMethod);
        expectErrorMessage(call,"Reserve is not active, withdraw unsuccessful");

        doReturn(Map.of(
                "isActive",true
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);
        doReturn(Map.of(
                "compoundedBorrowBalance",BigInteger.ZERO
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances", sICX, notOwner.getAddress());
        call= () -> score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), repayMethod);
        expectErrorMessage(call,"The user does not have any borrow pending");



        doReturn(Map.of(
                "compoundedBorrowBalance",BigInteger.valueOf(20).multiply(ICX),
                "borrowBalanceIncrease",BigInteger.valueOf(7).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances", sICX, notOwner.getAddress());
        doReturn(Map.of(
                "originationFee",BigInteger.valueOf(102).multiply(ICX)
        )).when(scoreSpy).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", sICX, notOwner.getAddress());

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updateStateOnRepay"), eq(sICX),
                eq(notOwner.getAddress()), eq(BigInteger.ZERO), any(BigInteger.class), eq(BigInteger.valueOf(7).multiply(ICX)), eq(false));
        doNothing().when(scoreSpy).call(sICX, "transfer",
                MOCK_CONTRACT_ADDRESS.get(Contracts.FEE_PROVIDER).getAddress(), BigInteger.valueOf(10).multiply(ICX));
        score.invoke(notOwner,"tokenFallback",notOwner.getAddress(),
                BigInteger.valueOf(10).multiply(ICX), repayMethod);

        verify(scoreSpy,times(3)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", sICX);
        verify(scoreSpy,times(2)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances", sICX, notOwner.getAddress());
        verify(scoreSpy,times(2)).call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", sICX, notOwner.getAddress());
        verify(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updateStateOnRepay"), eq(sICX), eq(notOwner.
                getAddress()), eq(BigInteger.ZERO), any(BigInteger.class), eq(BigInteger.valueOf(7).
                multiply(ICX)), eq(false));
        verify(scoreSpy).call(sICX, "transfer", MOCK_CONTRACT_ADDRESS.get(Contracts.FEE_PROVIDER).getAddress(),
                BigInteger.valueOf(10).multiply(ICX));

    }

    private void feeSharingMock(){
        mockFeeSharing();
        // user has not deposited bridge oToken
        doReturn(BigInteger.ZERO.multiply(ICX)).when(scoreSpy).
                call(BigInteger.class, Contracts.BRIDGE_O_TOKEN, "balanceOf", notOwner.getAddress());

        // user has deposited bridge oToken
        doReturn(BigInteger.ONE.multiply(ICX)).when(scoreSpy).
                call(BigInteger.class, Contracts.BRIDGE_O_TOKEN, "balanceOf", owner.getAddress());

    }

    public MockedStatic.Verification staking(BigInteger icxValue,Address staking, Address lendingPoolCore) {
        return () -> Context.call(icxValue, staking,"stakeICX", lendingPoolCore);
    }


    public MockedStatic.Verification sendICX() {
        return () -> Context.getValue();
    }

    public MockedStatic.Verification caller() {
        return () -> Context.getCaller();
    }

    private byte[] createByteArray(String methodName,Address collateral, Address reserve, Address user ) {

        JsonObject internalParameters = new JsonObject()
                .add("_collateral", String.valueOf(collateral))
                .add("_reserve", String.valueOf(reserve))
                .add("_user", String.valueOf(user));

        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

    private byte[] createByteArray_liquidation(String methodName, Address collateral ) {

        JsonObject internalParameters = new JsonObject()
                .add("_collateral", String.valueOf(collateral));

        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

    private byte[] jsonDataCompute(Address user){
        JsonObject data = new JsonObject();
        data.add("method","unstake");
        data.add("user", user.toString());

        return data.toString().getBytes();
    }

}
