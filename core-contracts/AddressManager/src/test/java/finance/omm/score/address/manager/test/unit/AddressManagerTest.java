package finance.omm.score.address.manager.test.unit;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.ReserveAddressDetails;
import finance.omm.score.core.addreess.manager.AddressManagerImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import  org.junit.jupiter.api.function.Executable;
import score.Address;
import scorex.util.HashMap;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class AddressManagerTest extends TestBase {

    private static final String name = "Address Provider";
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static final Account user = sm.createAccount();
    private Score score;

    AddressManagerImpl scoreSpy;

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.LENDING_POOL, Account.newScoreAccount(201));
        put(Contracts.LENDING_POOL_CORE, Account.newScoreAccount(202));
        put(Contracts.LENDING_POOL_DATA_PROVIDER, Account.newScoreAccount(203));
        put(Contracts.LIQUIDATION_MANAGER, Account.newScoreAccount(204));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(205));
        put(Contracts.oICX, Account.newScoreAccount(206));
        put(Contracts.oUSDs, Account.newScoreAccount(207));
        put(Contracts.oIUSDC, Account.newScoreAccount(208));
        put(Contracts.dICX, Account.newScoreAccount(209));
        put(Contracts.dUSDs, Account.newScoreAccount(210));
        put(Contracts.dIUSDC, Account.newScoreAccount(211));
        put(Contracts.DELEGATION, Account.newScoreAccount(212));
        put(Contracts.REWARDS, Account.newScoreAccount(213));
        put(Contracts.PRICE_ORACLE, Account.newScoreAccount(214));
        put(Contracts.STAKED_LP, Account.newScoreAccount(215));
        put(Contracts.GOVERNANCE, Account.newScoreAccount(216));
        put(Contracts.DAO_FUND, Account.newScoreAccount(217));
        put(Contracts.FEE_PROVIDER, Account.newScoreAccount(218));
        put(Contracts.STAKING,Account.newScoreAccount(219));
        put(Contracts.DEX,Account.newScoreAccount(220));
    }};

    @BeforeEach
    public void setup() throws Exception {
        score = sm.deploy(owner, AddressManagerImpl.class);
        AddressManagerImpl instance = (AddressManagerImpl) score.getInstance();
        scoreSpy = spy(instance);
        score.setInstance(scoreSpy);
        setAddresses();
    }

    private void setAddresses() {
        AddressDetails[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetails ad = new AddressDetails();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetails[]::new);

        Object[] params = new Object[]{
                addressDetails
        };
        score.invoke(owner,"setAddresses", params);
    }

    @Test
    void name(){
        String expectedName = "OMM " + name;
        assertEquals(expectedName, score.call("name"));
    }

    @DisplayName("should fail when not called by owner")
    @Test
    void addReserveAddress_shouldFailForOtherUser(){
        boolean overwrite = false;

        ReserveAddressDetails reserveAddressDetails = new ReserveAddressDetails();
        reserveAddressDetails.reserveName = "test_reserve_1";
        reserveAddressDetails.reserveAddress = Account.newScoreAccount(1).getAddress();

        reserveAddressDetails.dTokenName = "test_dtoken_1";
        reserveAddressDetails.dTokenAddress = Account.newScoreAccount(2).getAddress();

        reserveAddressDetails.oTokenName = "test_otoken_1";
        reserveAddressDetails.oTokenAddress = Account.newScoreAccount(3).getAddress();

        Executable call = () -> score.invoke(user,"addReserveAddress",reserveAddressDetails,overwrite);
        expectErrorMessage(call, "require owner access");
    }

    @Test
    void addReserverAddressTest(){
        boolean overwrite = true;

        ReserveAddressDetails reserveAddressDetails = new ReserveAddressDetails();
        reserveAddressDetails.reserveName = "test_reserve_1";
        reserveAddressDetails.reserveAddress = Account.newScoreAccount(1).getAddress();
        reserveAddressDetails.dTokenName = "test_dtoken_1";
        reserveAddressDetails.dTokenAddress = Account.newScoreAccount(2).getAddress();

        reserveAddressDetails.oTokenName = "test_otoken_1";
        reserveAddressDetails.oTokenAddress = Account.newScoreAccount(3).getAddress();

        score.invoke(owner,"addReserveAddress",reserveAddressDetails,overwrite);

        assertEquals(reserveAddressDetails.reserveAddress,score.call("getAddress",reserveAddressDetails.reserveName));
        assertEquals(reserveAddressDetails.dTokenAddress,score.call("getAddress",reserveAddressDetails.dTokenName));
        assertEquals(reserveAddressDetails.oTokenAddress,score.call("getAddress",reserveAddressDetails.oTokenName));

        Map<String,Address> result = new HashMap<>();
        result.put(reserveAddressDetails.reserveName,reserveAddressDetails.reserveAddress);
//
//        Map<String,Address> result2 = new HashMap<>();
//        result2.put(reserveAddressDetails.reserveName,reserveAddressDetails.reserveAddress);
//
//        //System.out.println(score.call("getReserveAddresses"));
//
////        assertEquals(result,score.call("getReserveAddresses"));
//        assertEquals(result,result2);

    }


    @DisplayName("should fail when not called by owner")
    @Test
    void setAddressesTest_shouldFailForOtherUsers(){
        String name = "test_address_name";
        Address address = Account.newScoreAccount(1).getAddress();
        AddressDetails[] addressDetails = new AddressDetails[] { new AddressDetails(name,address)};
        Object[] params = new Object[]{
              addressDetails
        };
        Executable call = () -> score.invoke(user,"setAddresses",params);
        expectErrorMessage(call, "require owner access");
    }

    // hash map key not matching
    @Test
    void getReserveAddressTest(){
        addReserverAddressTest();

        doNothing().when(scoreSpy).call(any(Contracts.class),eq("setAddresses"),any());
        score.invoke(owner,"setSCOREAddresses");

        Map<String,Address> system = Map.ofEntries(
                Map.entry("LendingPool",MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL).getAddress()),
                Map.entry("LendingPoolCore",MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress()),
                Map.entry("LendingPoolDataProvider",MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_DATA_PROVIDER).getAddress()),
                Map.entry("Staking",MOCK_CONTRACT_ADDRESS.get(Contracts.STAKING).getAddress()),
                Map.entry( "Governance",MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE).getAddress()),
                Map.entry("Delegation",MOCK_CONTRACT_ADDRESS.get(Contracts.DELEGATION).getAddress()),
                Map.entry("OmmToken",MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress()),
                Map.entry("Rewards",MOCK_CONTRACT_ADDRESS.get(Contracts.REWARDS).getAddress()),
                Map.entry("PriceOracle",MOCK_CONTRACT_ADDRESS.get(Contracts.PRICE_ORACLE).getAddress()),
                Map.entry("StakedLp",MOCK_CONTRACT_ADDRESS.get(Contracts.STAKED_LP).getAddress()),
                Map.entry("DEX",MOCK_CONTRACT_ADDRESS.get(Contracts.DEX).getAddress())
        );

        Map<String,Address> oToken = Map.of("test_otoken_1",Account.newScoreAccount(3).getAddress());
        Map<String,Address> dToken = Map.of("test_dtoken_1",Account.newScoreAccount(2).getAddress());
        Map<String,Address> collateral = Map.of("test_reserve_1",Account.newScoreAccount(1).getAddress());

        Map<String ,?> actual = Map.of(
                "collateral",collateral,
                "oTokens", oToken,
                "dTokens", dToken,
                "system", system
        );
        Map<String,?> expected = (Map<String, ?>) score.call("getAllAddresses");

        assertEquals(actual,expected);
        System.out.println(score.call("getAllAddresses"));

    }

    // when params is used score not found
    @Test
    void addAddressTest(){
        String to = Contracts.DELEGATION.getKey();
        String key = Contracts.OMM_TOKEN.getKey();
        Address value = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();

        AddressDetails [] addressDetails =new AddressDetails[]{ new AddressDetails(key,value)};
        Object[] params = new Object[]{
                addressDetails
        };
//        Address contract = (Address) score.call("getAddress",to);
        // when input is params scorenotfound
        doNothing().when(scoreSpy).call( any(Address.class),eq("setAddresses"),any());
        score.invoke(owner,"addAddress", to,key,value);
    }

    // Reverted(0) at starting of error message.
    @Test
    void addAddressToUnknownScore(){
        String to = Contracts.sICX.getKey();
        String key = Contracts.OMM_TOKEN.getKey();
        Address value = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();

        AddressDetails [] addressDetails =new AddressDetails[]{ new AddressDetails(key,value)};
        Object[] params = new Object[]{
                addressDetails
        };
        doNothing().when(scoreSpy).call( any(Address.class),eq("setAddresses"),any());

        Executable call = () ->   score.invoke(owner,"addAddress", to,key,value);
        expectErrorMessage(call, "Address Provider: score name " + to + " not matched." );

    }

    @Test
    void addAddressToScore(){
        String to = Contracts.DELEGATION.getKey();
        System.out.println(to);
        String[] name = new String[3];
        name[0] = Contracts.OMM_TOKEN.getKey();
        name[1] = Contracts.LENDING_POOL.getKey();
        name[2] = Contracts.REWARDS.getKey();
        doNothing().when(scoreSpy).call( any(Address.class),eq("setAddresses"),any());
        score.invoke(owner,"addAddressToScore",to,name);
    }

    // reverted
    @Test
    void addUnkownKeyToScore(){
        String to = "newKey";
        String[] name = new String[3];
        name[0] = Contracts.OMM_TOKEN.getKey();
        name[1] = Contracts.DELEGATION.getKey();
        name[2] = Contracts.REWARDS.getKey();
        doNothing().when(scoreSpy).call( any(Address.class),eq("setAddresses"),any());

        Executable call = () ->  score.invoke(owner,"addAddressToScore",to,name);
        expectErrorMessage(call, "Address Provider: score name " + to + " not matched." );
    }

    // reverted
    @Test
    void addWrongScorenameToList(){
        String to = Contracts.DELEGATION.getKey();
        String[] name = new String[3];
        name[0] = Contracts.OMM_TOKEN.getKey();
        name[1] = Contracts.LENDING_POOL.getKey();
        name[2] = Contracts.sICX.getKey();
        doNothing().when(scoreSpy).call( any(Address.class),eq("setAddresses"),any());

        Executable call = () ->  score.invoke(owner,"addAddressToScore",to,name);
        expectErrorMessage(call, "Address Provider: wrong score name in the list." );
    }

    public void expectErrorMessage(org.junit.jupiter.api.function.Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }
}
