package finance.omm.score.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.text.NumberFormat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.libs.structs.UserDetails;
import finance.omm.utils.math.MathUtils;
import score.Address;
import score.Context;

public class OTokenImplTest extends TestBase {
    private static ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();

    private BigInteger decimals = BigInteger.valueOf(10);
    private static BigInteger totalSupply = BigInteger.valueOf(50000000000L);

    private Score oToken;
    private Account addressProviderAccount = sm.createAccount();
    private Account lendingPoolAccount = sm.createAccount();
    private Account lendingPoolCoreAccount = sm.createAccount();
    private Account lendingPoolDataProviderAccount = sm.createAccount();
    private Account liquidationManagerAccount = sm.createAccount();
    private Account reserveAccount = sm.createAccount();
    private Account rewardsAccount = sm.createAccount();

    private Address lendingPoolCoreAddress = lendingPoolCoreAccount.getAddress();
    private Address reserveAddress = reserveAccount.getAddress();

    private AddressDetails lendingPoolDetails;
    private AddressDetails lendingPoolCoreDetails;
    private AddressDetails lendingPoolDataProviderDetails;
    private AddressDetails liquidationManagerDetails;
    private AddressDetails reserveDetails;
    private AddressDetails rewardsDetails;

    @BeforeAll
    public static void init() {
        owner.addBalance(Contracts.oTOKEN.getKey(), totalSupply);
    }

    @BeforeEach
    public void setup() throws Exception {
        oToken = sm.deploy(owner, OTokenImpl.class, 
                addressProviderAccount.getAddress(), 
                OTokenImpl.TAG,
                Contracts.oTOKEN.getKey(),
                decimals,
                false);

        lendingPoolDetails = new AddressDetails();
        lendingPoolDetails.name = Contracts.LENDING_POOL.getKey();
        lendingPoolDetails.address = lendingPoolAccount.getAddress();

        lendingPoolCoreDetails = new AddressDetails();
        lendingPoolCoreDetails.name = Contracts.LENDING_POOL_CORE.getKey();
        lendingPoolCoreDetails.address = lendingPoolCoreAccount.getAddress();

        lendingPoolDataProviderDetails = new AddressDetails();
        lendingPoolDataProviderDetails.name = Contracts.LENDING_POOL_DATA_PROVIDER.getKey();
        lendingPoolDataProviderDetails.address = lendingPoolDataProviderAccount.getAddress();

        reserveDetails = new AddressDetails();
        reserveDetails.name = Contracts.RESERVE.getKey();
        reserveDetails.address = reserveAccount.getAddress();

        rewardsDetails = new AddressDetails();
        rewardsDetails.name = Contracts.REWARDS.getKey();
        rewardsDetails.address = rewardsAccount.getAddress();

        liquidationManagerDetails = new AddressDetails();
        liquidationManagerDetails.name = Contracts.LIQUIDATION_MANAGER.getKey();
        liquidationManagerDetails.address = liquidationManagerAccount.getAddress();
    }

    private void setAddressDetails() {
        AddressDetails[] addressDetails = {
                lendingPoolDetails,
                lendingPoolCoreDetails,
                lendingPoolDataProviderDetails,
                liquidationManagerDetails,
                reserveDetails,
                rewardsDetails};
        oToken.invoke(addressProviderAccount, "setAddresses", (Object) addressDetails);
    }

    @Test
    void ShouldGetZeroTotalSupply() {
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){
            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,"getReserveLiquidityCumulativeIndex", reserveAddress))
            .thenReturn(BigInteger.ZERO);

            BigInteger totalSupply = (BigInteger) oToken.call("totalSupply");
            assertEquals(BigInteger.ZERO, totalSupply);
        }
    }

    @Test
    void ShouldGetZeroTotalStaked() {
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){
            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,"getReserveLiquidityCumulativeIndex", reserveAddress))
            .thenReturn(BigInteger.ZERO);

            TotalStaked totalStaked = (TotalStaked) oToken.call("getTotalStaked");
            assertEquals(BigInteger.ZERO, totalStaked.totalStaked);
        }
    }

    @Test
    void ShouldGetPrincipalSupply() {
        Account userAccount = sm.createAccount();

        SupplyDetails principalSupply = (SupplyDetails) oToken.call("getPrincipalSupply", userAccount.getAddress());
        assertEquals(BigInteger.ZERO, principalSupply.principalUserBalance);
    }

    @Test
    void ShouldMintOnDeposit() {
        Account userAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            BigInteger amountToDeposit = BigInteger.valueOf(500);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userAccount.getAddress(), amountToDeposit);

            BigInteger userBalance = (BigInteger)oToken.call("balanceOf", userAccount.getAddress());
            assertEquals(amountToDeposit, userBalance);
     
            BigInteger principalTotalSupply = (BigInteger) oToken.call("principalTotalSupply");
            assertEquals(amountToDeposit, principalTotalSupply);

        }
    }

    @Test
    void ShouldBurnOnLiquidation() {
        Account userAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            //mint some balance then the user can liquidate it
            BigInteger amountToDeposit = BigInteger.valueOf(500);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userAccount.getAddress(), amountToDeposit);

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(liquidationManagerAccount.getAddress());
            //liquidate half of balance
            BigInteger amountToLiquidate = BigInteger.valueOf(250);
            oToken.invoke(liquidationManagerAccount, "burnOnLiquidation", userAccount.getAddress(), amountToLiquidate);

            BigInteger userBalance = (BigInteger)oToken.call("balanceOf", userAccount.getAddress());
            assertEquals(amountToDeposit.subtract(amountToLiquidate), userBalance);
        }    
    }

    @Test
    void ShouldTransfer() {
        Account userFromAccount = sm.createAccount();
        Account userToAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            //mint some balance then the user can transfer some tokens
            BigInteger amountToDeposit = BigInteger.valueOf(500);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userFromAccount.getAddress(), amountToDeposit);

            //transfer half of balance
            BigInteger amountToTransfer = BigInteger.valueOf(250);

            theMock
            .when(() -> Context.call(boolean.class,
                    lendingPoolDataProviderAccount.getAddress(),
                    "balanceDecreaseAllowed",
                    reserveAddress, userFromAccount.getAddress(), amountToTransfer) )
            .thenReturn(true);


            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(userFromAccount.getAddress());

            oToken.invoke(userFromAccount, "transfer", userToAccount.getAddress(), amountToTransfer, "tacos".getBytes());

            BigInteger userFromBalance = (BigInteger)oToken.call("balanceOf", userFromAccount.getAddress());
            assertEquals(amountToDeposit.subtract(amountToTransfer), userFromBalance);
            BigInteger userToBalance = (BigInteger)oToken.call("balanceOf", userToAccount.getAddress());
            assertEquals(amountToTransfer, userToBalance);
        }
    }

    @Test
    void ShouldRedeem() {
        Account userAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            //mint some balance then the user can redeem some tokens
            BigInteger amountToDeposit = BigInteger.valueOf(500);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userAccount.getAddress(), amountToDeposit);

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            theMock
            .when(() -> Context.call(
                    Mockito.eq(rewardsAccount.getAddress()), 
                    Mockito.eq("handleAction"), 
                    Mockito.any(UserDetails.class)) )
            .thenReturn(Void.TYPE);

            //transfer half of balance
            BigInteger amountToRedeem = BigInteger.valueOf(50);

            theMock
            .when(() -> Context.call(boolean.class,
                    lendingPoolDataProviderAccount.getAddress(),
                    "balanceDecreaseAllowed",
                    reserveAddress, userAccount.getAddress(), amountToRedeem) )
            .thenReturn(true);

            oToken.invoke(userAccount, "redeem", userAccount.getAddress(), amountToRedeem);

            BigInteger userBalance = (BigInteger)oToken.call("balanceOf", userAccount.getAddress());
            assertEquals(amountToDeposit.subtract(amountToRedeem), userBalance);
            
        }
    }

    @Test
    void ShouldGetNonZeroTotalSupply() {
        Account userAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){
            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.ZERO);

            //mint some balance so the total supply is > 0
            BigInteger amountToDeposit = BigInteger.valueOf(50000000000l);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userAccount.getAddress(), amountToDeposit);

            theMock
            .when(() -> Context.call(BigInteger.class,
                    lendingPoolCoreAccount.getAddress(),
                    "getReserveLiquidityCumulativeIndex", 
                    reserveAddress))
            .thenReturn(BigInteger.ONE);

            theMock
            .when(() -> Context.call(BigInteger.class,
                    lendingPoolCoreAccount.getAddress(),
                    "getNormalizedDebt",
                    reserveAddress) )
            .thenReturn(BigInteger.ONE);

            BigInteger totalSupply = (BigInteger) oToken.call("totalSupply");
            assertNotNull(totalSupply);
            assertEquals(amountToDeposit, totalSupply);
        }
    }

    @Test
    void ShouldGetBalanceOf() {
        Account userAccount = sm.createAccount();
        setAddressDetails();

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){

            theMock
            .when(() -> Context.getCaller() )
            .thenReturn(lendingPoolAccount.getAddress());

            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress) )
            .thenReturn(BigInteger.TWO);

            //mint some tokens to get balance after it
            BigInteger amountToDeposit = BigInteger.valueOf(50000000000l);
            oToken.invoke(lendingPoolAccount, "mintOnDeposit", userAccount.getAddress(), amountToDeposit);

            BigInteger userBalance = (BigInteger)oToken.call("balanceOf", userAccount.getAddress());
            assertEquals(amountToDeposit, userBalance);
     
            BigInteger principalTotalSupply = (BigInteger) oToken.call("principalTotalSupply");
            assertEquals(amountToDeposit, principalTotalSupply);

        }
    }
}
