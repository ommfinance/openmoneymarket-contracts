package finance.omm.score.tokens;

import static finance.omm.score.tokens.DTokenImpl.TAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.Context;

public class DTokenTest extends TestBase {
    private static ServiceManager sm = getServiceManager();
    private static Account owner = sm.createAccount();

    private BigInteger decimals = BigInteger.valueOf(10);
    private static BigInteger totalSupply = BigInteger.valueOf(50000000000L);

    private AddressDetails lendingPoolDetails;
    private Account lendingPoolAccount = sm.createAccount();

    private AddressDetails lendingPoolDataProviderDetails;
    private Account lendingPoolDataProviderAccount = sm.createAccount();

    private AddressDetails reserveDetails;
    private Account reserveAccount = sm.createAccount();

    private AddressDetails rewardsDetails;
    private Account rewardsAccount = sm.createAccount();

    private AddressDetails liquidationManagerDetails;
    private Account liquidationManagerAccount = sm.createAccount();

    private AddressDetails lendingPoolCoreDetails;
    private Account lendingPoolCoreAccount = sm.createAccount();

    private Account addressProviderAccount = sm.createAccount();
    private Address reserveAddress = reserveAccount.getAddress();
    private Address lendingPoolCoreAddress = lendingPoolCoreAccount.getAddress();

    private Score dToken;

    private static final String DTOKEN = "dToken";

    @BeforeAll
    public static void init() {
        owner.addBalance(DTOKEN, totalSupply);
    }

    @BeforeEach
    public void setup() throws Exception {
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

        dToken = sm.deploy(owner, DTokenImpl.class, addressProviderAccount.getAddress(), TAG, DTOKEN, decimals);
    }

    @Test
    void testSetAddresses() {

        AddressDetails[] addressDetails = {lendingPoolDetails, lendingPoolDataProviderDetails};
        dToken.invoke(addressProviderAccount, "setAddresses", (Object) addressDetails);

        @SuppressWarnings("unchecked")
        Map<String, Address> addresses = (Map<String, Address>) dToken.call("getAddresses");

        assertNotNull(addresses);
        assertFalse(addresses.isEmpty());
        assertNotNull(addresses.get(lendingPoolDetails.name));
    }

    private void setAddressDetails() {
        AddressDetails[] addressDetails = {
                lendingPoolDetails,
                lendingPoolCoreDetails,
                lendingPoolDataProviderDetails,
                liquidationManagerDetails,
                reserveDetails,
                rewardsDetails};
        dToken.invoke(addressProviderAccount, "setAddresses", (Object) addressDetails);
    }

    @Test
    void ShouldGetZeroTotalSupply() {
        setAddressDetails();

        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {
            theMock
                    .when(() -> Context.call(BigInteger.class, lendingPoolCoreAccount.getAddress(),
                            "getReserveLiquidityCumulativeIndex", reserveAddress))
                    .thenReturn(BigInteger.ZERO);

            BigInteger totalSupply = (BigInteger) dToken.call("totalSupply");
            assertEquals(BigInteger.ZERO, totalSupply);
        }
    }

    @Test
    void ShouldGetPrincipalSupply() {
        Account userAccount = sm.createAccount();

        SupplyDetails principalSupply = (SupplyDetails) dToken.call("getPrincipalSupply", userAccount.getAddress());
        assertEquals(BigInteger.ZERO, principalSupply.principalUserBalance);
    }

    @Test
    void principalBalanceOfTest() {
        BigInteger balances = (BigInteger) dToken.call("principalBalanceOf", lendingPoolAccount.getAddress());
        assertNotNull(balances);
        assertEquals(BigInteger.ZERO, balances);
    }

    @Test
    void getUserBorrowCumulativeIndexTest() {
        BigInteger userBorrowIndex = (BigInteger) dToken.call("getUserBorrowCumulativeIndex", lendingPoolCoreAccount.getAddress());
        assertNotNull(userBorrowIndex);
        assertEquals(BigInteger.ZERO, userBorrowIndex);
    }

    @Test
    void nameTest() {
        String name = (String) dToken.call("name");
        assertNotNull(name);
        assertEquals(TAG, name);
    }

    @Test
    void symbolTest() {
        String symbol = (String) dToken.call("symbol");
        assertNotNull(symbol);
        assertEquals(DTOKEN, symbol);
    }

    @Test
    void decimalsTest() {
        BigInteger decimals = (BigInteger) dToken.call("decimals");
        assertNotNull(decimals);
        assertEquals(BigInteger.TEN, decimals);
    }

    @Test
    void ShouldMintOnBorrow() {
        Account userAccount = sm.createAccount();
        BigInteger amountToBorrow = BigInteger.TEN.multiply(BigInteger.TWO);
        BigInteger balanceIncrease = BigInteger.ZERO;
        setAddressDetails();

        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {

            theMock
                    .when(() -> Context.getCaller())
                    .thenReturn(lendingPoolCoreAccount.getAddress());

            theMock
                    .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,
                            "getReserveBorrowCumulativeIndex",
                            reserveAddress))
                    .thenReturn(BigInteger.ZERO);

            dToken.invoke(lendingPoolCoreAccount, "mintOnBorrow",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            BigInteger userBalance = (BigInteger) dToken.call("principalBalanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertEquals(amountToBorrow, userBalance);

            BigInteger totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(amountToBorrow, totalSupply);
        }
    }

    @Test
    void ShouldBurnOnRepay() {
        Account userAccount = sm.createAccount();
        BigInteger amountToBorrow = BigInteger.TEN.multiply(BigInteger.TWO);
        BigInteger balanceIncrease = BigInteger.ZERO;
        setAddressDetails();

        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {

            theMock
                    .when(() -> Context.getCaller())
                    .thenReturn(lendingPoolCoreAccount.getAddress());

            theMock
                    .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,
                            "getReserveBorrowCumulativeIndex",
                            reserveAddress))
                    .thenReturn(BigInteger.ZERO);

            dToken.invoke(lendingPoolCoreAccount, "mintOnBorrow",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            BigInteger userBalance = (BigInteger) dToken.call("principalBalanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertEquals(amountToBorrow, userBalance);

            BigInteger totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(amountToBorrow, totalSupply);

            dToken.invoke(lendingPoolAccount, "burnOnRepay",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            userBalance = (BigInteger) dToken.call("principalBalanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertEquals(BigInteger.ZERO, userBalance);

            totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(BigInteger.ZERO, totalSupply);
        }
    }

    @Test
    void ShouldBurnOnLiquidation() {
        Account userAccount = sm.createAccount();
        BigInteger amountToBorrow = BigInteger.TEN.multiply(BigInteger.TWO);
        BigInteger balanceIncrease = BigInteger.ZERO;
        setAddressDetails();

        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {

            theMock
                    .when(() -> Context.getCaller())
                    .thenReturn(lendingPoolCoreAccount.getAddress());

            theMock
                    .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,
                            "getReserveBorrowCumulativeIndex",
                            reserveAddress))
                    .thenReturn(BigInteger.ZERO);

            dToken.invoke(lendingPoolCoreAccount, "mintOnBorrow",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            BigInteger userBalance = (BigInteger) dToken.call("principalBalanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertEquals(amountToBorrow, userBalance);

            BigInteger totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(amountToBorrow, totalSupply);

            dToken.invoke(lendingPoolAccount, "burnOnLiquidation",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            userBalance = (BigInteger) dToken.call("principalBalanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertEquals(BigInteger.ZERO, userBalance);

            totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(BigInteger.ZERO, totalSupply);
        }
    }

    @Test
    void ShouldGetBalanceOfUserWithBorrowedAmount() {
        Account userAccount = sm.createAccount();
        BigInteger amountToBorrow = BigInteger.valueOf(50000000000l);
        BigInteger balanceIncrease = BigInteger.valueOf(25000000000l);
        setAddressDetails();

        try (MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)) {

            theMock
                    .when(() -> Context.getCaller())
                    .thenReturn(lendingPoolCoreAccount.getAddress());

            theMock
                    .when(() -> Context.call(BigInteger.class, lendingPoolCoreAddress,
                            "getReserveBorrowCumulativeIndex",
                            reserveAddress))
                    .thenReturn(BigInteger.ONE);

            dToken.invoke(lendingPoolCoreAccount, "mintOnBorrow",
                    userAccount.getAddress(),
                    amountToBorrow,
                    balanceIncrease);

            theMock
                    .when(() -> Context.call(BigInteger.class,
                            lendingPoolCoreAccount.getAddress(),
                            "getNormalizedDebt",
                            reserveAddress))
                    .thenReturn(BigInteger.ONE);

            BigInteger userBalance = (BigInteger) dToken.call("balanceOf", userAccount.getAddress());
            assertNotNull(userBalance);
            assertTrue(userBalance.compareTo(amountToBorrow) > 0);

            BigInteger totalSupply = (BigInteger) dToken.call("principalTotalSupply");
            assertNotNull(totalSupply);
            assertEquals(amountToBorrow.add(balanceIncrease), totalSupply);

            TotalStaked ts = (TotalStaked) dToken.call("getTotalStaked");
            assertNotNull(ts);
            assertEquals(BigInteger.valueOf(75000000000L), ts.totalStaked);
            assertEquals(decimals, ts.decimals);
        }
    }

    @Test
    void ShouldFailToTransfer() {
        Account userAccount = sm.createAccount();
        Account userAccountTo = sm.createAccount();
        try {
            dToken.invoke(userAccount, "transfer", userAccountTo.getAddress(), BigInteger.TEN, "tacos".getBytes());
        } catch (AssertionError e) {
            assertEquals("Reverted(0): Omm dToken : Transfer not allowed in debt token", e.getMessage());
        }
    }
}
