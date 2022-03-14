package finance.omm.score.reward.test.unit;

import static finance.omm.utils.constants.TimeConstants.SECOND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetail;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.UserDetails;
import finance.omm.libs.test.VarargAnyMatcher;
import finance.omm.score.core.reward.distribution.RewardDistributionImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import score.Address;


public class RewardDistributionUnitTest extends TestBase {

    private static final ServiceManager sm = getServiceManager();
    private Account owner;
    private Score score;
    private RewardDistributionImpl scoreSpy;
    private float floatWeight = 0.4f;
    private BigInteger weight = BigInteger.valueOf((long) (floatWeight * 100))
            .multiply(ICX)
            .divide(BigInteger.valueOf(100));


    private BigInteger TWO = BigInteger.TWO;
    private BigInteger THREE = BigInteger.valueOf(3);
    private BigInteger FOUR = BigInteger.valueOf(4);

    Address[] addresses = new Address[]{
            Account.newScoreAccount(1001).getAddress(),
            Account.newScoreAccount(1002).getAddress(),
            Account.newScoreAccount(1003).getAddress(),
            Account.newScoreAccount(1004).getAddress(),
            Account.newScoreAccount(1005).getAddress(),
            Account.newScoreAccount(1006).getAddress(),
            Account.newScoreAccount(1007).getAddress(),
            Account.newScoreAccount(1008).getAddress(),
            Account.newScoreAccount(1009).getAddress(),
            Account.newScoreAccount(1010).getAddress(),
            Account.newScoreAccount(1011).getAddress(),
    };


    private final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.REWARD_WEIGHT_CONTROLLER, Account.newScoreAccount(102));
        put(Contracts.DAO_FUND, Account.newScoreAccount(103));
        put(Contracts.WORKER_TOKEN, Account.newScoreAccount(104));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(105));
        put(Contracts.BOOSTED_OMM, Account.newScoreAccount(106));
    }};

    @BeforeEach
    void setup() throws Exception {

        owner = sm.createAccount(100);

        BigInteger bOMMCutOff = BigInteger.valueOf(sm.getBlock().getTimestamp());

        score = sm.deploy(owner, RewardDistributionImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress(), bOMMCutOff, weight);
        setAddresses();
        RewardDistributionImpl t = (RewardDistributionImpl) score.getInstance();
        scoreSpy = spy(t);
        mockAssets(scoreSpy, Mockito.spy(scoreSpy.assets));
//        mockAssets(scoreSpy, Mockito.spy(scoreSpy.legacyRewards));
        score.setInstance(scoreSpy);
        sm.getBlock().increase(1_000_000);
    }

    private void setAddresses() {
        AddressDetail[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetail ad = new AddressDetail();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetail[]::new);

        Object[] params = new Object[]{
                addressDetails
        };
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }

    @DisplayName("Add type")
    @Test
    void testAddType() {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addType"),
                        ArgumentMatchers.<Object>argThat(matcher));
        score.invoke(owner, "addType", "key-1", Boolean.FALSE);

        verify(scoreSpy).call(Contracts.REWARD_WEIGHT_CONTROLLER, "addType", "key-1", Boolean.FALSE);
        verify(scoreSpy).AddType("key-1", Boolean.FALSE);
    }

    @DisplayName("test add asset")
    @Test
    public void testAddAsset() {
        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addAsset"),
                        ArgumentMatchers.<Object>argThat(matcher));
        Address asset = Account.newScoreAccount(11).getAddress();
        score.invoke(owner, "addAsset", "type-1", "asset-name", asset, BigInteger.ZERO);

        verify(scoreSpy).call(Contracts.REWARD_WEIGHT_CONTROLLER, "addAsset", "type-1", asset, "asset-name");
        verify(scoreSpy).AssetAdded("type-1", "asset-name", asset, BigInteger.ZERO);

    }

    @Nested
    @DisplayName("handle action")
    class TestHandleAction {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        List<Account> users = new ArrayList<>();
        Account[] assets = new Account[4];

        @BeforeEach
        void setup() throws Exception {

            doNothing().when(scoreSpy)
                    .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addAsset"),
                            ArgumentMatchers.<Object>argThat(matcher));
            Account asset = Account.newScoreAccount(11);
            assets[0] = asset;
            score.invoke(owner, "addAsset", "type-1", "asset-name-1", asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(12);
            assets[1] = asset;
            score.invoke(owner, "addAsset", "type-2", "asset-name-2", asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(13);
            assets[2] = asset;
            score.invoke(owner, "addAsset", "type-2", "asset-name-3", asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(14);
            assets[3] = asset;
            score.invoke(owner, "addAsset", "type-3", "asset-name-4", asset.getAddress(), BigInteger.ONE);

            users.add(sm.createAccount(10));
            users.add(sm.createAccount(10));
            users.add(sm.createAccount(10));

        }

        @DisplayName("should fail for invalid asset")
        @Test
        void handleAction_shouldFailForInvalidAsset() {
            UserDetails details = createUserDetail(0, 100);
            Executable call = () -> score.invoke(users.get(1), "handleAction", details);
            expectErrorMessage(call, "Asset is null (" + users.get(1).getAddress() + ")");
        }

        @DisplayName("should able to call handle action")
        @Test
        void handleAction_shouldAbleToCallHandleAction() {

            UserDetails details_1 = createUserDetail(0, 200);
            UserDetails details_2 = createUserDetail(1, 200);
            UserDetails details_3 = createUserDetail(2, 400);

            int index = 0;
            Account asset = assets[index];

            BigInteger bOMMbalance_1 = BigInteger.valueOf(200).multiply(ICX);
            BigInteger bOMMbalance_2 = BigInteger.valueOf(400).multiply(ICX);

            doReturn(bOMMbalance_1, bOMMbalance_2, BigInteger.ZERO).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_1._user);

            doReturn(bOMMbalance_2).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_2._user);

            doReturn(BigInteger.ZERO).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_3._user);

            doReturn(bOMMbalance_1, bOMMbalance_1.add(bOMMbalance_2), bOMMbalance_1.add(bOMMbalance_2)).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

            /*
            user 1
             */
            BigInteger workingTotal = details_1._userBalance;
            score.invoke(asset, "handleAction", details_1);
            verify(scoreSpy).WorkingBalanceUpdated(details_1._user, asset.getAddress(), details_1._userBalance,
                    workingTotal);

                /*
            user 2
             */
            workingTotal = workingTotal.add(details_2._userBalance);
            score.invoke(asset, "handleAction", details_2);
            verify(scoreSpy).WorkingBalanceUpdated(details_2._user, asset.getAddress(), details_2._userBalance,
                    workingTotal);

            /*
            user 3 : no boost
             */
            BigInteger workingBalance = details_3._userBalance.multiply(weight).divide(ICX);
            workingTotal = workingTotal.add(workingBalance);
            score.invoke(asset, "handleAction", details_3);
            verify(scoreSpy).WorkingBalanceUpdated(details_3._user, asset.getAddress(), workingBalance,
                    workingTotal);
        }


        @Nested
        @DisplayName("rewards")
        class TestReward {

            SupplyDetails details = createSupplyDetails(100);


            @BeforeEach
            void setup() {
                UserDetails details_1 = createUserDetail(0, user1Balance);
                details_1._totalSupply = details_1._userBalance;
                UserDetails details_2 = createUserDetail(1, user2Balance);
                details_2._totalSupply = details_1._totalSupply.add(details_2._userBalance);

                doReturn(BigInteger.ZERO).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_2._user);

                for (Account asset : assets) {
                    doReturn(bOMMBalance, BigInteger.ZERO).when(scoreSpy)
                            .call(eq(BigInteger.class), eq(Contracts.BOOSTED_OMM), eq("balanceOf"), any());
                    doReturn(bOMMBalance.multiply(TWO)).when(scoreSpy)
                            .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

                    score.invoke(asset, "handleAction", details_1);
                    score.invoke(asset, "handleAction", details_2);
                }
                sm.getBlock().increase(999);
            }

            @ParameterizedTest
            @MethodSource("finance.omm.score.reward.test.unit.RewardDistributionUnitTest#userRewards")
            void getRewards_shouldReturnGroupRewards(int userIndex, long weight) {
                clearInvocations(scoreSpy);

                doReturn(ICX).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("getIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));

                doReturn(details).when(scoreSpy).fetchUserBalance(any(), any(), any());

                Map<String, ?> result = (Map<String, ?>) score.call("getRewards", users.get(userIndex).getAddress());

                verify(scoreSpy.assets, never()).setAssetIndex(any(), any());
                verify(scoreSpy.assets, never()).setUserIndex(any(), any(), any());

                verify(scoreSpy, never()).AssetIndexUpdated(any(), any(), any());
                verify(scoreSpy, never()).UserIndexUpdated(any(), any(), any(), any());
//                user1 working balance min(100*0.4+200*200/400*0.6,100)=100

                Map<String, BigInteger> type1 = (Map<String, BigInteger>) result.get("type-1");
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type1.get("total"));
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type1.get("asset-name-1"));

//                user1 working balance min(100*0.4+100*200/400*0.6,100)=70
                Map<String, BigInteger> type2 = (Map<String, BigInteger>) result.get("type-2");
                assertEquals(BigInteger.valueOf(weight * 2).multiply(ICX), type2.get("total"));
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type2.get("asset-name-2"));
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type2.get("asset-name-3"));

                Map<String, BigInteger> type3 = (Map<String, BigInteger>) result.get("type-3");
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type3.get("total"));
                assertEquals(BigInteger.valueOf(weight).multiply(ICX), type3.get("asset-name-4"));

                assertEquals(BigInteger.valueOf(weight * 4).multiply(ICX), result.get("total"));
            }


            @ParameterizedTest
            @MethodSource("finance.omm.score.reward.test.unit.RewardDistributionUnitTest#claimRewards")
            void claimRewards_shouldReturnGroupRewards(int userIndex, Long tokenBalance, Long bBalance,
                    long workingBalance, long workingTotal) {
                clearInvocations(scoreSpy);
                Account user = users.get(userIndex);

                doReturn(BigInteger.valueOf(bBalance).multiply(ICX)).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user.getAddress());
                doReturn(BigInteger.valueOf(1000).multiply(ICX)).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

                doReturn(ICX).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("getIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));
                assert (bBalance < 1000);
                SupplyDetails details = new SupplyDetails();
                details.decimals = BigInteger.valueOf(0x12);
                details.principalUserBalance = BigInteger.valueOf(tokenBalance).multiply(ICX);
                details.principalTotalSupply = BigInteger.valueOf(10_000).multiply(ICX);
                doReturn(details).when(scoreSpy).fetchUserBalance(eq(user.getAddress()), any(), any());

                doNothing().when(scoreSpy)
                        .call(eq(Contracts.OMM_TOKEN), eq("transfer"), ArgumentMatchers.<Object>argThat(matcher));

                score.invoke(user, "claimRewards", user.getAddress());
                verifyAssetIndex(ICX);
                verifyUserIndex(ICX);

                BigInteger reward = BigInteger.valueOf(workingBalance * 4).multiply(ICX);
                verify(scoreSpy).RewardsClaimed(user.getAddress(), reward, "Asset rewards claimed");
                BigInteger claimedReward = (BigInteger) score.call("getClaimedReward", user.getAddress());
                assertEquals(reward, claimedReward);
                verifyWorkingBalanceUpdate(tokenBalance, bBalance, workingTotal - workingBalance);


            }

            private void verifyWorkingBalanceUpdate(Long tokenBalance, Long balance, Long workingTotalBalance) {
                ArgumentCaptor<BigInteger> workingBalance = ArgumentCaptor.forClass(BigInteger.class);
                ArgumentCaptor<BigInteger> workingTotal = ArgumentCaptor.forClass(BigInteger.class);
                verify(scoreSpy, times(4)).WorkingBalanceUpdated(any(), any(), workingBalance.capture(),
                        workingTotal.capture());
                long value = Math.min((long) (tokenBalance * floatWeight + 10_000 * balance / 1000 * (1 - floatWeight)),
                        tokenBalance);

                assertEquals(BigInteger.valueOf(value).multiply(ICX), workingBalance.getValue());
                assertEquals(BigInteger.valueOf(workingTotalBalance + value).multiply(ICX), workingTotal.getValue());
            }

            private void verifyAssetIndex(BigInteger newIndex) {
                ArgumentCaptor<Address> assetIdCapture = ArgumentCaptor.forClass(Address.class);
                ArgumentCaptor<BigInteger> indexCapture = ArgumentCaptor.forClass(BigInteger.class);

                verify(scoreSpy.assets, times(4)).setAssetIndex(assetIdCapture.capture(), indexCapture.capture());

                assertEquals(Arrays.asList(assets[0].getAddress(), assets[1].getAddress(), assets[2].getAddress(),
                        assets[3].getAddress()), assetIdCapture.getAllValues());
                assertEquals(Arrays.asList(newIndex, newIndex, newIndex, newIndex), indexCapture.getAllValues());

                ArgumentCaptor<BigInteger> oldIndex = ArgumentCaptor.forClass(BigInteger.class);
                assetIdCapture = ArgumentCaptor.forClass(Address.class);
                indexCapture = ArgumentCaptor.forClass(BigInteger.class);
                verify(scoreSpy, times(4)).AssetIndexUpdated(assetIdCapture.capture(), oldIndex.capture(),
                        indexCapture.capture());

                assertEquals(Arrays.asList(assets[0].getAddress(), assets[1].getAddress(), assets[2].getAddress(),
                                assets[3].getAddress()),
                        assetIdCapture.getAllValues());
                assertEquals(newIndex, indexCapture.getValue());
                assertEquals(BigInteger.ZERO, oldIndex.getValue());
            }

            private void verifyUserIndex(BigInteger asset) {
                ArgumentCaptor<Address> assetIdCapture = ArgumentCaptor.forClass(Address.class);
                ArgumentCaptor<BigInteger> indexCapture = ArgumentCaptor.forClass(BigInteger.class);
                verify(scoreSpy.assets, times(4)).setUserIndex(assetIdCapture.capture(), any(), indexCapture.capture());

                assertEquals(Arrays.asList(assets[0].getAddress(), assets[1].getAddress(), assets[2].getAddress(),
                                assets[3].getAddress()),
                        assetIdCapture.getAllValues());
                assertEquals(Arrays.asList(asset, asset, asset, asset), indexCapture.getAllValues());

                assetIdCapture = ArgumentCaptor.forClass(Address.class);
                indexCapture = ArgumentCaptor.forClass(BigInteger.class);
                ArgumentCaptor<BigInteger> oldIndex = ArgumentCaptor.forClass(BigInteger.class);
                verify(scoreSpy, times(4)).UserIndexUpdated(any(), assetIdCapture.capture(), oldIndex.capture(),
                        indexCapture.capture());

                assertEquals(Arrays.asList(assets[0].getAddress(), assets[1].getAddress(), assets[2].getAddress(),
                                assets[3].getAddress()),
                        assetIdCapture.getAllValues());
                assertEquals(Arrays.asList(asset, asset, asset, asset), indexCapture.getAllValues());
                assertEquals(Arrays.asList(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO),
                        oldIndex.getAllValues());
            }

        }

        private UserDetails createUserDetail(int index, long balance) {
            Account account = users.get(index);
            UserDetails details = new UserDetails();
            details._decimals = BigInteger.valueOf(18);
            details._totalSupply = BigInteger.valueOf(1000).multiply(ICX);
            details._userBalance = BigInteger.valueOf(balance).multiply(ICX);
            details._user = account.getAddress();
            return details;
        }
    }

    @DisplayName("test distribute")
    @Test
    void testDistribute_forZEROday() {
        Class<Map<String, ?>> clazz = (Class) Map.class;
        Map<String, ?> result = new HashMap<>() {{
            put("isValid", true);
            put("distribution", BigInteger.ZERO);
            put("day", BigInteger.ZERO);
        }};
        doReturn(result).when(scoreSpy)
                .call(eq(clazz), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("distributionDetails"),
                        any(BigInteger.class));
        score.invoke(owner, "distribute");

    }

    @DisplayName("test distribute")
    @ParameterizedTest
    @MethodSource("distributeArgument")
    void testDistribute(Map<String, ?> response) {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addType"),
                        ArgumentMatchers.<Object>argThat(matcher));
        score.invoke(owner, "addType", "workerToken", Boolean.TRUE);

        score.invoke(owner, "addType", "daoFund", Boolean.TRUE);

        Class<Map<String, ?>> clazz = (Class) Map.class;
        BigInteger distribution = (BigInteger) response.get("distribution");
        doReturn(response).when(scoreSpy)
                .call(clazz, Contracts.REWARD_WEIGHT_CONTROLLER, "distributionDetails",
                        BigInteger.ZERO);
        doNothing().when(scoreSpy).call(Contracts.OMM_TOKEN, "mint", distribution);
        mockTokenDistribution();

        BigInteger newIndex = ICX.divide(BigInteger.valueOf(1_000_000));
        doReturn(newIndex).when(scoreSpy)
                .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("getIntegrateIndex"),
                        any(Address.class),
                        eq(ICX), any(BigInteger.class));

        sm.getBlock().increase(86400);
        score.invoke(owner, "distribute");

        BigInteger timestamp = BigInteger.valueOf(sm.getBlock().getTimestamp()).divide(SECOND);

        verify(scoreSpy).OmmTokenMinted((BigInteger) response.get("day"), distribution,
                ((BigInteger) response.get("day")).subtract(BigInteger.ZERO));
        verify(scoreSpy).Distribution(eq("daoFund"), eq(MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress()),
                Mockito.any(BigInteger.class));
        verify(scoreSpy, times(2)).AssetIndexUpdated(any(), eq(BigInteger.ZERO),
                eq(newIndex));

        BigInteger daoIndex = (BigInteger) score.call("getAssetIndex",
                MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress());
        BigInteger workerTokenIndex = (BigInteger) score.call("getAssetIndex",
                MOCK_CONTRACT_ADDRESS.get(Contracts.WORKER_TOKEN).getAddress());

        assertEquals(newIndex, daoIndex);
        assertEquals(newIndex, workerTokenIndex);

        BigInteger daoTime = (BigInteger) score.call("getLastUpdatedTimestamp",
                MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress());
        BigInteger workerTime = (BigInteger) score.call("getLastUpdatedTimestamp",
                MOCK_CONTRACT_ADDRESS.get(Contracts.WORKER_TOKEN).getAddress());

        assertEquals(timestamp, daoTime);
        assertEquals(timestamp, workerTime);

        BigInteger scoreDay = (BigInteger) score.call("getDistributedDay");
        assertEquals((BigInteger) response.get("day"), scoreDay);
    }

    private void mockTokenDistribution() {
        doReturn(new Address[0]).when(scoreSpy).call(Address[].class, Contracts.WORKER_TOKEN, "getWallets");
        doReturn(BigInteger.ZERO).when(scoreSpy).call(BigInteger.class, Contracts.WORKER_TOKEN, "totalSupply");

        doNothing().when(scoreSpy)
                .call(eq(Contracts.OMM_TOKEN), eq("transfer"), eq(Contracts.DAO_FUND), any(BigInteger.class));

    }


    static Stream<Arguments> distributeArgument() {
        //                user1 working balance min(100*0.4+100*200/400*0.6,100)=70
        //                user2 working balance min(200*0.4+300*0/400*0.6,200)=80
        return Stream.of(
                Arguments.of(Map.of(
                        "isValid", true,
                        "distribution", ICX.multiply(BigInteger.valueOf(1_000_000)),
                        "day", BigInteger.TEN
                ))
        );
    }

    static Stream<Arguments> userRewards() {
        //                user1 working balance min(100*0.4+100*200/400*0.6,100)=70
        //                user2 working balance min(200*0.4+300*0/400*0.6,200)=80
        return Stream.of(
                Arguments.of(0, 100L), //with boost
                Arguments.of(1, 40L) //without boost
        );
    }


    static Stream<Arguments> claimRewards() {
        //                user1 working balance min(100*0.4+100*200/400*0.6,100)=70
        //                user2 working balance min(200*0.4+300*0/400*0.6,200)=80
        return Stream.of(
                //userIndex,userTokenBalance,userBoostedBalance,userWorkingBalance,workingTotalBalace
                Arguments.of(0, 100L, 300L, 100L, 140L), //with boost
                Arguments.of(1, 200L, 200L, 40L, 140L) //without boost
        );
    }


    @DisplayName("test get liquidity providers")
    @Test
    void testGetLiquidityProviders() {
        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addAsset"),
                        ArgumentMatchers.<Object>argThat(matcher));
        Account asset = Account.newScoreAccount(11);
        score.invoke(owner, "addAsset", "type-1", "asset-name-1", asset.getAddress(), BigInteger.ONE);

        asset = Account.newScoreAccount(12);
        score.invoke(owner, "addAsset", "type-2", "asset-name-2", asset.getAddress(), BigInteger.ZERO);

        Map result = (Map) score.call("getLiquidityProviders");
        System.out.println("result = " + result);
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }


    static void mockAssets(RewardDistributionImpl obj, Object value) throws Exception {
        Field field = obj.getClass().getField("assets");
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(obj, value);
    }

}
