package finance.omm.score.reward.test.unit;

import static finance.omm.utils.constants.TimeConstants.DAY_IN_SECONDS;
import static finance.omm.utils.constants.TimeConstants.SECOND;
import static finance.omm.utils.constants.TimeConstants.getBlockTimestampInSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.UserDetails;
import finance.omm.libs.test.VarargAnyMatcher;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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


public class RewardDistributionUnitTest extends RewardDistributionAbstractTest {

    @DisplayName("Add type")
    @Test
    void testAddType() {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addType"),
                        ArgumentMatchers.<Object>argThat(matcher));
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addType", "key-1", Boolean.FALSE);

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
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-1", "asset-name", asset,
                BigInteger.ZERO);

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
            score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-1", "asset-name-1",
                    asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(12);
            assets[1] = asset;
            score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-2", "asset-name-2",
                    asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(13);
            assets[2] = asset;
            score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-2", "asset-name-3",
                    asset.getAddress(), BigInteger.ZERO);

            asset = Account.newScoreAccount(14);
            assets[3] = asset;
            score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-3", "asset-name-4",
                    asset.getAddress(), BigInteger.ONE);

            users.add(sm.createAccount(10));
            users.add(sm.createAccount(10));
            users.add(sm.createAccount(10));

            doReturn(Boolean.TRUE).when(scoreSpy).isHandleActionEnabled();
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

            doReturn(BigInteger.ZERO).when(scoreSpy)
                    .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("calculateIntegrateIndex"),
                            ArgumentMatchers.<Object>argThat(matcher));

            SupplyDetails details = createSupplyDetails(200);

            doReturn(details).when(scoreSpy).fetchUserBalance(any(), any(), any());

            int index = 0;
            Account asset = assets[index];

            BigInteger bOMMbalance_1 = BigInteger.valueOf(200).multiply(ICX);
            BigInteger bOMMbalance_2 = BigInteger.valueOf(400).multiply(ICX);
            BigInteger totalbOMMbalance = bOMMbalance_1.add(bOMMbalance_2);

            doReturn(bOMMbalance_1).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_1._user);

            doReturn(bOMMbalance_2).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_2._user);

            doReturn(BigInteger.ZERO).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", details_3._user);

            doReturn(bOMMbalance_1, totalbOMMbalance, totalbOMMbalance).when(scoreSpy)
                    .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

            /*
            user 1
            workingbalance=min(supply*0.4+totalSupply*bOMMBalance/totalbOMMbalance*0.6,supply)
             */

            BigInteger workingBalance = details.principalUserBalance.multiply(FORTY).divide(HUNDRED)
                    .add(details.principalTotalSupply.multiply(bOMMbalance_1)
                            .divide(bOMMbalance_1)
                            .multiply(SIXTY).divide(HUNDRED))
                    .min(details.principalUserBalance);
            BigInteger workingTotal = workingBalance;
            score.invoke(asset, "handleAction", details_1);
            verify(scoreSpy).WorkingBalanceUpdated(details_1._user, asset.getAddress(), workingBalance,
                    workingTotal);

                /*
            user 2
             */
            workingBalance = details.principalUserBalance.multiply(FORTY).divide(HUNDRED)
                    .add(details.principalTotalSupply.multiply(bOMMbalance_2)
                            .divide(totalbOMMbalance)
                            .multiply(SIXTY).divide(HUNDRED))
                    .min(details.principalUserBalance);
            workingTotal = workingTotal.add(workingBalance);
            score.invoke(asset, "handleAction", details_2);
            verify(scoreSpy).WorkingBalanceUpdated(details_2._user, asset.getAddress(), workingBalance,
                    workingTotal);

            /*
            user 3 : no boost
             */
            workingBalance = details.principalUserBalance.multiply(FORTY).divide(HUNDRED)
                    .add(details.principalTotalSupply.multiply(BigInteger.ZERO)
                            .divide(totalbOMMbalance)
                            .multiply(SIXTY).divide(HUNDRED))
                    .min(details.principalUserBalance);
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

                doReturn(Boolean.TRUE).when(scoreSpy).isHandleActionEnabled();

                UserDetails details_1 = createUserDetail(0, 100);
                details_1._totalSupply = details_1._userBalance;

                UserDetails details_2 = createUserDetail(1, 100);
                details_2._totalSupply = details_1._totalSupply.add(details_2._userBalance);

                doReturn(BigInteger.ZERO).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER),
                                eq("calculateIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));

                doReturn(details).when(scoreSpy).fetchUserBalance(any(), any(), any());

                BigInteger bOMMBalance = BigInteger.valueOf(200).multiply(ICX);

                sm.getBlock().increase(999);

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
                clearInvocations(scoreSpy.assets);

                doReturn(ICX).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER),
                                eq("calculateIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));

                doReturn(details).when(scoreSpy).fetchUserBalance(any(), any(), any());

                Map<String, ?> result = (Map<String, ?>) score.call("getRewards", users.get(userIndex).getAddress());

                verify(scoreSpy.assets, never()).setAssetIndex(any(), any());
                verify(scoreSpy.assets, never()).setUserIndex(any(), any(), any());

                verify(scoreSpy, never()).AssetIndexUpdated(any(), any(), any());
                verify(scoreSpy, never()).UserIndexUpdated(any(), any(), any(), any());

                verifyGetRewards(result, weight);
            }

            private void verifyGetRewards(Map<String, ?> result, long weight) {

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
                doReturn(Boolean.TRUE).when(scoreSpy).isRewardClaimEnabled();

                doReturn(BigInteger.valueOf(bBalance).multiply(ICX)).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user.getAddress());
                doReturn(BigInteger.valueOf(1000).multiply(ICX)).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

                doReturn(ICX).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER),
                                eq("calculateIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));
                assert (bBalance < 1000);
                SupplyDetails details = new SupplyDetails();
                details.decimals = BigInteger.valueOf(0x12);
                details.principalUserBalance = BigInteger.valueOf(tokenBalance).multiply(ICX);
                details.principalTotalSupply = BigInteger.valueOf(10_000).multiply(ICX);
                doReturn(details).when(scoreSpy).fetchUserBalance(eq(user.getAddress()), any(), any());

                doNothing().when(scoreSpy)
                        .call(eq(Contracts.OMM_TOKEN), eq("transfer"), ArgumentMatchers.<Object>argThat(matcher));

                score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL), "claimRewards", user.getAddress());
                verifyAssetIndex(ICX);
                verifyUserIndex(ICX);

                BigInteger reward = BigInteger.valueOf(workingBalance * 4).multiply(ICX);
                verify(scoreSpy).RewardsClaimed(user.getAddress(), reward, "Asset rewards claimed");
                BigInteger claimedReward = (BigInteger) score.call("getClaimedReward", user.getAddress());
                assertEquals(reward, claimedReward);
                verifyWorkingBalanceUpdate(tokenBalance, bBalance, workingTotal - workingBalance);


            }

            @ParameterizedTest
            @MethodSource("finance.omm.score.reward.test.unit.RewardDistributionUnitTest#userRewards")
            void kick_user(int userIndex, long weight) {
                reset(scoreSpy);
                reset(scoreSpy.assets);
                Address user = users.get(userIndex).getAddress();

                doReturn(BigInteger.ONE).when(scoreSpy)
                        .call(BigInteger.class, Contracts.BOOSTED_OMM, "totalSupply");

                doReturn(ICX).when(scoreSpy)
                        .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER),
                                eq("calculateIntegrateIndex"),
                                ArgumentMatchers.<Object>argThat(matcher));

                SupplyDetails details = createSupplyDetails(200);

                doReturn(details).when(scoreSpy).fetchUserBalance(any(), any(), any());

                score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.BOOSTED_OMM), "onKick", user, BigInteger.ZERO,
                        "message".getBytes());
                Map<String, ?> result = (Map<String, ?>) score.call("getRewards", user);

                verifyGetRewards(result, weight);
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
            details._totalSupply = BigInteger.valueOf(1000 + balance).multiply(ICX);
            details._userBalance = BigInteger.valueOf(balance).multiply(ICX);
            details._user = account.getAddress();
            return details;
        }

        private SupplyDetails createSupplyDetails(long balance) {
            SupplyDetails details = new SupplyDetails();
            details.decimals = BigInteger.valueOf(18);
            details.principalUserBalance = BigInteger.valueOf(balance).multiply(ICX);
            details.principalTotalSupply = BigInteger.valueOf(1000 + balance).multiply(ICX);
            return details;
        }
    }

//    @DisplayName("test distribute")
//    @Test
//    void testDistribute_forZEROday() {
//        Class<Map<String, ?>> clazz = (Class) Map.class;
//        Map<String, ?> result = new HashMap<>() {{
//            put("isValid", true);
//            put("amountToMint", BigInteger.ZERO);
//            put("day", BigInteger.ZERO);
//            put("timestamp", sm.getBlock().getTimestamp() / 1_000_000);
//        }};
//        doReturn(result).when(scoreSpy)
//                .call(eq(clazz), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("precompute"),
//                        any(BigInteger.class));
//        Executable call = () -> score.invoke(owner, "distribute");
//        expectErrorMessage(call, "no token to mint 0");
//
//
//    }

    @DisplayName("test distribute")
    @Test
    void testDistribute_forZEROmint() {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addType"),
                        ArgumentMatchers.<Object>argThat(matcher));

        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addType", "daoFund", Boolean.TRUE);

        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addType", "workerToken", Boolean.TRUE);

        Class<Map<String, ?>> clazz = (Class) Map.class;
        Map<String, ?> result = new HashMap<>() {{
            put("isValid", true);
            put("amountToMint", BigInteger.ZERO);
            put("day", BigInteger.ZERO);
            put("timestamp",getBlockTimestampInSecond().divide(DAY_IN_SECONDS).multiply(DAY_IN_SECONDS));
        }};
        doReturn(result).when(scoreSpy)
                .call(eq(clazz), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("precompute"),
                        any(BigInteger.class));

        mockTokenDistribution();

        doReturn(BigInteger.ZERO).when(scoreSpy)
                .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("calculateIntegrateIndex"),
                        any(Address.class),
                        eq(ICX), any(BigInteger.class), any(BigInteger.class));

        sm.getBlock().increase(86400);
        score.invoke(owner,"distribute");


        verify(scoreSpy,never()).OmmTokenMinted((BigInteger) result.get("day"), (BigInteger) result.get("amountToMint"),
                ((BigInteger) result.get("day")).subtract(BigInteger.ZERO));
        verify(scoreSpy).Distribution(eq("daoFund"), eq(MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress()),
                Mockito.any(BigInteger.class));
        verify(scoreSpy, never()).AssetIndexUpdated(any(), eq(BigInteger.ZERO),
                eq(BigInteger.ZERO));

    }

    @DisplayName("test distribute")
    @ParameterizedTest
    @MethodSource("distributeArgument")
    void testDistribute(Map<String, ?> response) {

        VarargAnyMatcher<Object> matcher = new VarargAnyMatcher<>();
        doNothing().when(scoreSpy)
                .call(eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("addType"),
                        ArgumentMatchers.<Object>argThat(matcher));
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addType", "workerToken", Boolean.TRUE);

        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addType", "daoFund", Boolean.TRUE);

        Class<Map<String, ?>> clazz = (Class) Map.class;
        BigInteger distribution = (BigInteger) response.get("amountToMint");
        doReturn(response).when(scoreSpy)
                .call(clazz, Contracts.REWARD_WEIGHT_CONTROLLER, "precompute",
                        BigInteger.ZERO);
        doNothing().when(scoreSpy).call(Contracts.OMM_TOKEN, "mint", distribution);
        mockTokenDistribution();

        BigInteger newIndex = ICX.divide(BigInteger.valueOf(1_000_000));
        doReturn(newIndex).when(scoreSpy)
                .call(eq(BigInteger.class), eq(Contracts.REWARD_WEIGHT_CONTROLLER), eq("calculateIntegrateIndex"),
                        any(Address.class),
                        eq(ICX), any(BigInteger.class), any(BigInteger.class));

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

        assertEquals(response.get("timestamp"), daoTime);
        assertEquals(response.get("timestamp"), workerTime);

        BigInteger scoreDay = (BigInteger) score.call("getDistributedDay");
        assertEquals(response.get("day"), scoreDay);
    }

    private void mockTokenDistribution() {
        doReturn(new Address[0]).when(scoreSpy).call(Address[].class, Contracts.WORKER_TOKEN, "getWallets");
        doReturn(BigInteger.ZERO).when(scoreSpy).call(BigInteger.class, Contracts.WORKER_TOKEN, "totalSupply");

        doNothing().when(scoreSpy)
                .call(eq(Contracts.OMM_TOKEN), eq("transfer"),
                        eq(MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress()),
                        any(BigInteger.class));

    }


    static Stream<Arguments> distributeArgument() {
        //                user1 working balance min(100*0.4+100*200/400*0.6,100)=70
        //                user2 working balance min(200*0.4+300*0/400*0.6,200)=80
        return Stream.of(
                Arguments.of(Map.of(
                        "isValid", true,
                        "amountToMint", ICX.multiply(BigInteger.valueOf(1_000_000)),
                        "day", BigInteger.ONE,
                        "timestamp", getBlockTimestampInSecond().divide(DAY_IN_SECONDS).multiply(DAY_IN_SECONDS)
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
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-1", "asset-name-1",
                asset.getAddress(), BigInteger.ONE);

        asset = Account.newScoreAccount(12);
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE), "addAsset", "type-2", "asset-name-2",
                asset.getAddress(), BigInteger.ZERO);

        Map result = (Map) score.call("getLiquidityProviders");
        System.out.println("result = " + result);
    }

}
