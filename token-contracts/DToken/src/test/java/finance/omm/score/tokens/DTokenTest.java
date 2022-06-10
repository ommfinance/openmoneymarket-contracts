package finance.omm.score.tokens;

import com.iconloop.score.test.TestBase;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.SupplyDetails;
import score.Address;
import score.Context;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DTokenTest  extends TestBase {
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

    
    private Score dToken;
    
    @BeforeAll
    public static void init() {
        owner.addBalance(Contracts.DTOKEN.getKey(), totalSupply);
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
        
        dToken = sm.deploy(owner, DTokenImpl.class, addressProviderAccount.getAddress(),DTokenImpl.TAG,Contracts.DTOKEN.getKey(),decimals,false);
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

        try(MockedStatic<Context> theMock = Mockito.mockStatic(Context.class)){
            theMock
            .when(() -> Context.call(BigInteger.class, lendingPoolCoreAccount.getAddress(),"getReserveLiquidityCumulativeIndex", reserveAddress))
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
        BigInteger balances = (BigInteger)dToken.call("principalBalanceOf",  lendingPoolAccount.getAddress());
        assertNotNull(balances);
        assertEquals(BigInteger.ZERO,balances);
    }
    
    @Test
    void getUserBorrowCumulativeIndexTest() {
        BigInteger userBorrowIndex = (BigInteger)dToken.call("getUserBorrowCumulativeIndex", lendingPoolCoreAccount.getAddress());
        assertNotNull(userBorrowIndex);
        assertEquals(BigInteger.ZERO, userBorrowIndex);
    }
    
    @Test
    void nameTest() {
        String name = (String) dToken.call("name");
        assertNotNull(name); 
        assertEquals(DTokenImpl.TAG, name);
    }
    
    @Test
    void symbolTest() {
        String symbol = (String)dToken.call("symbol");
        assertNotNull(symbol);
        assertEquals(Contracts.DTOKEN.getKey(), symbol);
    }
    
    @Test
    void decimalsTest() {
        BigInteger decimals = (BigInteger)dToken.call("decimals");
        assertNotNull(decimals);
        assertEquals(BigInteger.TEN, decimals);
    }
    
}
