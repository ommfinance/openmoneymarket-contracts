package finance.omm.score.staking.test;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.score.staking.StakingImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.Context;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static finance.omm.score.staking.utils.Constant.HUNDRED_PERCENTAGE;
import static finance.omm.score.staking.utils.Constant.ONE_EXA;
import static finance.omm.score.staking.utils.Constant.SYSTEM_SCORE_ADDRESS;
import static finance.omm.score.staking.utils.Constant.TOTAL_STAKE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockedStatic.Verification;
import static org.mockito.Mockito.*;

class StakingTest extends TestBase {

    private static int scoreAccountCount = 1;
    public static final ServiceManager sm = getServiceManager();
    public static final Account owner = sm.createAccount();
    public static final Account alice = sm.createAccount();
    public static final Account sicx = Account.newScoreAccount(scoreAccountCount++);
    public static final Account feeDistribution = Account.newScoreAccount(scoreAccountCount++);
    public static final Account ommLendingPoolCore = Account.newScoreAccount(scoreAccountCount++);
    public static final Account ommDelegation = Account.newScoreAccount(scoreAccountCount++);
    private final MockedStatic<Context> contextMock = Mockito.mockStatic(Context.class, Mockito.CALLS_REAL_METHODS);

    private Score staking;
    private StakingImpl stakingSpy;

    Map<String, Object> prepsResponse = new HashMap<>();
    Map<String, Object> iissInfo = new HashMap<>();
    Map<String, Object> stake = new HashMap<>();
    Map<String, Object> iScore = new HashMap<>();

    Map<String, Object> prepDict = new HashMap<>();
    BigInteger nextPrepTerm;
    BigInteger unlockPeriod;
    List<Address> prepAddress = new ArrayList();

    Verification getIISSInfo = () -> Context.call(SYSTEM_SCORE_ADDRESS, "getIISSInfo");
    Verification getPreps = () -> Context.call(SYSTEM_SCORE_ADDRESS, "getPReps", BigInteger.ONE,
            BigInteger.valueOf(100L));
    Verification queryIscore = () -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("queryIScore"), any(Address.class));
    Verification getStake = () -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("getStake"), any(Address.class));
    Verification getUnstakeLockPeriod = () -> Context.call(SYSTEM_SCORE_ADDRESS, "estimateUnstakeLockPeriod");
    Verification claimIScore = () -> Context.call(SYSTEM_SCORE_ADDRESS, "claimIScore");

    BigInteger sicxBalance;
    BigInteger sicxTotalSupply;
    Verification sicxBalanceOf = () -> Context.call(eq(sicx.getAddress()), eq("balanceOf"), any(Address.class));
    Verification getSicxTotalSupply = () -> Context.call(sicx.getAddress(), "totalSupply");

    @BeforeEach
    void setUp() throws Exception {

        setupSystemScore();
        setupSicxScore();

        setupStakingScore();
        setupFeeDistribution();
    }

    private void setupFeeDistribution(){
        // Configure fee distribution contract
        staking.invoke(owner,"setFeeDistributionAddress",feeDistribution.getAddress());

        BigInteger feePercentage = new BigInteger("10").multiply(ONE_EXA);
        doReturn(feePercentage).when(stakingSpy).getFeePercentage();

        // Configure lending pool core contract
        staking.invoke(owner,"setOmmLendingPoolCore",ommLendingPoolCore.getAddress());
    }

    private void setupStakingScore() throws Exception {
        staking = sm.deploy(owner, StakingImpl.class,new BigInteger("10").multiply(ONE_EXA),
                new BigInteger("90").multiply(ONE_EXA),ommLendingPoolCore.getAddress(),feeDistribution.getAddress(),
                ommDelegation.getAddress());
        stakingSpy = (StakingImpl) spy(staking.getInstance());
        staking.setInstance(stakingSpy);

        // Configure Staking contract
        staking.invoke(owner, "setSicxAddress", sicx.getAddress());

        staking.invoke(owner,"toggleStakingOn");
    }

    void setupSystemScore() {
        // Write methods will have no effect
        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("setStake"),
                any(BigInteger.class))).thenReturn(null);
        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("setDelegation"),
                any(List.class))).thenReturn(null);
        contextMock.when(claimIScore).thenReturn(null);

        stake.put("unstakes", List.of());
        contextMock.when(getStake).thenReturn(stake);

        iScore.put("estimatedICX", BigInteger.ZERO);
        contextMock.when(queryIscore).thenReturn(iScore);

        setupGetPrepsResponse();
        contextMock.when(getPreps).thenReturn(prepsResponse);

        nextPrepTerm = BigInteger.valueOf(1000);
        iissInfo.put("nextPRepTerm", nextPrepTerm);
        contextMock.when(getIISSInfo).thenReturn(iissInfo);

        unlockPeriod = BigInteger.valueOf(8 * 43200L);
        contextMock.when(getUnstakeLockPeriod).thenReturn(Map.of("unstakeLockPeriod", unlockPeriod));

        prepDict.put("totalBlocks",BigInteger.ZERO);
        prepDict.put("validatedBlocks",BigInteger.ZERO);
        prepDict.put("power",BigInteger.ZERO);
        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("getPRep"),
                any(Address.class))).thenReturn(prepDict);
    }

    void setupSicxScore() {
        contextMock.when(() -> Context.call(eq(sicx.getAddress()), eq("mintTo"), any(Address.class),
                any(BigInteger.class), any(byte[].class))).thenReturn(null);
        contextMock.when(() -> Context.call(eq(sicx.getAddress()), eq("burn"), any(BigInteger.class))).thenReturn(null);

        sicxBalance = BigInteger.ZERO;
        contextMock.when(sicxBalanceOf).thenReturn(sicxBalance);

        sicxTotalSupply = BigInteger.ZERO;
        contextMock.when(getSicxTotalSupply).thenReturn(sicxTotalSupply);
    }

    void setupGetPrepsResponse() {
        prepsResponse.put("blockHeight", BigInteger.valueOf(123456L));
        List<Map<String, Object>> prepsList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            prepAddress.add(sm.createAccount().getAddress());
        }
        for (int i = 0; i < 100; i++) { // BigInteger.valueOf((int)(Math.random() * 1000)+1)
            Map<String, Object> prep = Map.of("address", prepAddress.get(i),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ZERO,
                    "commissionRate",BigInteger.ONE);
            prepsList.add(prep);
        }
        prepsResponse.put("preps", prepsList);
    }

    JSONObject getUnstakeJsonData() {
        JSONObject unstakeData = new JSONObject();
        unstakeData.put("method", "unstake");
        return unstakeData;
    }

    @Test
    void name() {
        assertEquals("Staked ICX Manager", staking.call("name"));
    }

    @Test
    void setAndGetBlockHeightWeek() {
        assertEquals(nextPrepTerm, staking.call("getBlockHeightWeek"));

        BigInteger newBlockHeight = BigInteger.valueOf(55555);
        staking.invoke(owner, "setBlockHeightWeek", newBlockHeight);
        assertEquals(newBlockHeight, staking.call("getBlockHeightWeek"));

        nextPrepTerm = newBlockHeight.add(BigInteger.valueOf(7 * 43200 + 19L));
        iissInfo.put("nextPRepTerm", nextPrepTerm);
        contextMock.when(getIISSInfo).thenReturn(iissInfo);
        sm.call(owner, ICX.multiply(BigInteger.valueOf(199L)), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);
        assertEquals(nextPrepTerm, staking.call("getBlockHeightWeek"));
    }

    @Test
    void testNewIscore() {
        assertEquals(ICX, staking.call("getTodayRate"));

        BigInteger extraICXBalance = BigInteger.valueOf(397L);
        BigInteger stakeAmount = BigInteger.valueOf(199L);
        contextMock.when(() -> Context.getBalance(staking.getAddress())).thenReturn(extraICXBalance.add(stakeAmount));

        sicxTotalSupply = BigInteger.valueOf(719L);
        contextMock.when(getSicxTotalSupply).thenReturn(sicxTotalSupply);

        doReturn(sicxTotalSupply).when(stakingSpy).getTotalStake();
        //noinspection ResultOfMethodCallIgnored
        contextMock.verify(() -> Context.newVarDB(eq(TOTAL_STAKE), eq(BigInteger.class)));

        sm.call(owner, stakeAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);

        assertEquals(extraICXBalance, staking.call("getLifetimeReward"));
        BigInteger newRate = sicxTotalSupply.add(extraICXBalance).multiply(ICX).divide(sicxTotalSupply);
        assertEquals(newRate, staking.call("getTodayRate"));

        contextMock.when(() -> Context.getBalance(staking.getAddress())).thenReturn(stakeAmount);
        sm.call(owner, stakeAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        assertEquals(extraICXBalance, staking.call("getLifetimeReward"));
    }

    @Test
    void toggleStakingOn() {
        assertEquals(true, staking.call("getStakingOn"));
        staking.invoke(owner, "toggleStakingOn");
        assertEquals(false, staking.call("getStakingOn"));
    }

    @Test
    void setAndGetSicxAddress() {
        assertEquals(sicx.getAddress(), staking.call("getSicxAddress"));

        Account newSicx = Account.newScoreAccount(scoreAccountCount++);
        staking.invoke(owner, "setSicxAddress", newSicx.getAddress());
        assertEquals(newSicx.getAddress(), staking.call("getSicxAddress"));
    }

    @Test
    void unstakeBatchLimit() {
        assertEquals(BigInteger.valueOf(200L), staking.call("getUnstakeBatchLimit"));
        staking.invoke(owner, "setUnstakeBatchLimit", BigInteger.valueOf(300L));
        assertEquals(BigInteger.valueOf(300L), staking.call("getUnstakeBatchLimit"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPrepList() {
        ArrayList<Address> expectedList = new ArrayList<>();
        List<Map<String, Object>> prepsList = (List<Map<String, Object>>) prepsResponse.get("preps");
        for (Map<String, Object> prepMap : prepsList) {
            expectedList.add((Address) prepMap.get("address"));
        }
        assertArrayEquals(expectedList.toArray(), ((List<Address>) staking.call("getPrepList")).toArray());
        assertEquals(100, ((List<Address>) staking.call("getPrepList")).size());

        Account newPrep = sm.createAccount();
        PrepDelegations delegation = new PrepDelegations();
        delegation._address = newPrep.getAddress();
        delegation._votes_in_per = HUNDRED_PERCENTAGE;

        // Providing user delegation by non sicx holder doesn't increase prep list
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation});
        assertArrayEquals(expectedList.toArray(), ((List<Address>) staking.call("getPrepList")).toArray());

        // Sicx holder's delegation increase prep list
        contextMock.when(sicxBalanceOf).thenReturn(BigInteger.TEN);
        expectedList.add(newPrep.getAddress());
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation});
        assertArrayEquals(expectedList.toArray(), ((List<Address>) staking.call("getPrepList")).toArray());

        //Removing the delegation should reduce the prep list
        expectedList.remove(newPrep.getAddress());
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{});
        assertArrayEquals(expectedList.toArray(), ((List<Address>) staking.call("getPrepList")).toArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUnstakingAmount() {
        assertEquals(BigInteger.ZERO, staking.call("getUnstakingAmount"));

        // Calling from contracts other than sicx fails
        Executable callNotFromSicx = () -> staking.invoke(owner, "tokenFallback", owner.getAddress(),
                ICX.multiply(BigInteger.ONE), new byte[0]);
        String expectedErrorMessage = "Staked ICX Manager: The Staking contract only accepts sICX tokens" +
                ".: " +
                sicx.getAddress().toString();
        expectErrorMessage(callNotFromSicx, expectedErrorMessage);

        // Calling without data fails
        Executable invalidData = () -> staking.invoke(sicx, "tokenFallback", owner.getAddress(),
                ICX.multiply(BigInteger.valueOf(919L)), new byte[0]);
        expectedErrorMessage = "Unexpected end of input at 1:1";
        expectErrorMessage(invalidData, expectedErrorMessage);

        JSONObject data = getUnstakeJsonData();

        // Trying to unstake with no total stake fails
        BigInteger unstakedAmount = ICX.multiply(BigInteger.valueOf(919L));
        Executable negativeTotalStake = () -> staking.invoke(sicx, "tokenFallback", owner.getAddress(), unstakedAmount,
                data.toString().getBytes());
        expectedErrorMessage = "Staked ICX Manager: Total staked amount can't be set negative";
        expectErrorMessage(negativeTotalStake, expectedErrorMessage);

        // Trying to unstake zero amount

        Executable zeroUnstake = () -> staking.invoke(sicx, "tokenFallback", owner.getAddress(), BigInteger.ZERO,
                "unstake".getBytes());
        expectErrorMessage(zeroUnstake,"Staked ICX Manager: The Staking contract cannot unstake value less than or equal to 0");

        //Successful unstake
        doReturn(unstakedAmount).when(stakingSpy).getTotalStake();
        staking.invoke(sicx, "tokenFallback", owner.getAddress(), unstakedAmount, data.toString().getBytes());
        // Since unit test doesn't reset the context if invocation is reverted, the number is double than expected
        assertEquals(unstakedAmount.multiply(BigInteger.TWO), staking.call("getUnstakingAmount"));

        BigInteger blockHeight = BigInteger.valueOf(sm.getBlock().getHeight());
        List<List<Object>> unstakeDetails = new ArrayList<>();
        unstakeDetails.add(List.of(BigInteger.ONE, unstakedAmount, owner.getAddress(), blockHeight.add(unlockPeriod),
                owner.getAddress()));

        assertArrayEquals(unstakeDetails.toArray(), ((List<List<Object>>) staking.call("getUnstakeInfo")).toArray());

        staking.invoke(sicx, "tokenFallback", owner.getAddress(), unstakedAmount, data.toString().getBytes());
        assertEquals(unstakedAmount.multiply(BigInteger.valueOf(3L)), staking.call("getUnstakingAmount"));
        unstakeDetails.add(List.of(BigInteger.TWO, unstakedAmount, owner.getAddress(),
                blockHeight.add(unlockPeriod.add(BigInteger.ONE)), owner.getAddress()));
        List<List<Object>> actualUnstakeDetails = (List<List<Object>>) staking.call("getUnstakeInfo");
        assertArrayEquals(unstakeDetails.toArray(), actualUnstakeDetails.toArray());
    }

    @Test
    void getTotalStake() {
        assertEquals(BigInteger.ZERO, staking.call("getTotalStake"));

        BigInteger totalStaked;
        // changes with iscore rewards, stake and unstake
        BigInteger stakeAmount = BigInteger.valueOf(199L);
        sm.call(owner, stakeAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        totalStaked = stakeAmount;
        assertEquals(totalStaked, staking.call("getTotalStake"));

        sm.call(owner, stakeAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        totalStaked = totalStaked.add(stakeAmount);
        assertEquals(totalStaked, staking.call("getTotalStake"));
        contextMock.verify(() -> Context.call(eq(sicx.getAddress()), eq("mintTo"), any(Address.class), eq(stakeAmount),
                any(byte[].class)), times(2));

        // Unstake same amount
        JSONObject data = getUnstakeJsonData();
        staking.invoke(sicx, "tokenFallback", owner.getAddress(), stakeAmount, data.toString().getBytes());
        totalStaked = totalStaked.subtract(stakeAmount);
        assertEquals(totalStaked, staking.call("getTotalStake"));

        // I-Score generated is added in total staked amount
        BigInteger extraICXBalance = ICX.multiply(BigInteger.valueOf(100_000L));
        contextMock.when(() -> Context.getBalance(staking.getAddress())).thenReturn(extraICXBalance.add(stakeAmount));
        contextMock.when(getSicxTotalSupply).thenReturn(totalStaked);
        Map<String, Object> unstakeList = new HashMap<>();
        unstakeList.put("unstake", stakeAmount);
        List<Map<String, Object>> unstakes = new ArrayList<>();
        unstakes.add(unstakeList);
        contextMock.when(getStake).thenReturn(Map.of("unstakes", unstakes));
        sm.call(owner, stakeAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        totalStaked = totalStaked.add(extraICXBalance).add(stakeAmount);

        assertEquals(extraICXBalance,staking.call("getLifetimeReward"));
        assertEquals(totalStaked, staking.call("getTotalStake"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getTopPreps() {
        List<Address> expectedList = new ArrayList<>();
        List<Map<String, Object>> prepsList = (List<Map<String, Object>>) prepsResponse.get("preps");
        for (Map<String, Object> prepMap : prepsList) {
            expectedList.add((Address) prepMap.get("address"));
        }
        assertArrayEquals(expectedList.toArray(), ((List<Address>) staking.call("getTopPreps")).toArray());

        // Top preps changes whenever week has passed
        setupGetPrepsResponse();
        contextMock.when(getPreps).thenReturn(prepsResponse);

        BigInteger oneWeekBlocks = BigInteger.valueOf(7 * 43200L);
        nextPrepTerm = BigInteger.valueOf(1000).add(oneWeekBlocks).add(BigInteger.TEN);
        iissInfo.put("nextPRepTerm", nextPrepTerm);
        contextMock.when(getIISSInfo).thenReturn(iissInfo);

        List<Address> newTopPreps = new ArrayList<>();
        prepsList = (List<Map<String, Object>>) prepsResponse.get("preps");
        for (Map<String, Object> prepMap : prepsList) {
            newTopPreps.add((Address) prepMap.get("address"));
        }
        assertNotSame(expectedList, newTopPreps);
        sm.call(owner, BigInteger.TEN, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        assertArrayEquals(newTopPreps.toArray(), ((List<Address>) staking.call("getTopPreps")).toArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getPrepDelegations() {
        Map<String, BigInteger> actualPrepDelegation = new HashMap<>();
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        Map<String, BigInteger> expectedPrepDelegations = new HashMap<>();

        assertEquals(expectedPrepDelegations, staking.call("getPrepDelegations"));
        assertEquals(Map.of(), staking.call("getActualPrepDelegations"));

        Account newPrep = sm.createAccount();
        PrepDelegations delegation = new PrepDelegations();
        delegation._address = newPrep.getAddress();
        delegation._votes_in_per = HUNDRED_PERCENTAGE;

        // All sicx is used to specify non-top prep
        BigInteger totalStaked = BigInteger.TEN;
        contextMock.when(sicxBalanceOf).thenReturn(BigInteger.TEN);
        doReturn(totalStaked).when(stakingSpy).getTotalStake();
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation});
        actualPrepDelegation.put(newPrep.getAddress().toString(), BigInteger.TEN);
        expectedPrepDelegations.put(topPreps.get(0).toString(), BigInteger.TEN);
        assertEquals(expectedPrepDelegations, staking.call("getPrepDelegations"));
        assertEquals(actualPrepDelegation, staking.call("getActualPrepDelegations"));

        // Stake ICX from a new user without any preference.
        BigInteger stakedAmount = BigInteger.valueOf(199L);
        totalStaked = totalStaked.add(stakedAmount);
        sm.call(alice, stakedAmount, staking.getAddress(), "stakeICX", new Address(new byte[Address.LENGTH]),
                new byte[0]);
        doReturn(totalStaked).when(stakingSpy).getTotalStake();

        Address topRankPrep = topPreps.get(0);
        expectedPrepDelegations.put(topRankPrep.toString(), stakedAmount.add(BigInteger.TEN));

        assertEquals(expectedPrepDelegations, staking.call("getPrepDelegations"));
        assertEquals(actualPrepDelegation, staking.call("getActualPrepDelegations"));

        // All preference to tenth prep from alice
        delegation._address = topPreps.get(10);
        contextMock.when(sicxBalanceOf).thenReturn(stakedAmount);
        staking.invoke(alice, "delegate", (Object) new PrepDelegations[]{delegation});
        expectedPrepDelegations.put(topPreps.get(0).toString(), BigInteger.TEN);
        expectedPrepDelegations.put(topPreps.get(10).toString(), stakedAmount);

        actualPrepDelegation.put(topPreps.get(10).toString(), stakedAmount);
        assertEquals(expectedPrepDelegations, staking.call("getPrepDelegations"));
        assertEquals(actualPrepDelegation, staking.call("getActualPrepDelegations"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAddressDelegation() {

        // Return empty map if icx is 0 and delegation null
        assertEquals(Map.of(), staking.call("getAddressDelegations", owner.getAddress()));

        // Return map with 0 value if icx is 0 and delegation yes
        BigInteger totalPrepsChosen = BigInteger.valueOf(7);
        BigInteger totalPercentage = HUNDRED_PERCENTAGE;
        PrepDelegations[] delegations = new PrepDelegations[7];
        for (int i = 0; i < 7; i++) {
            Account newPrep = sm.createAccount();
            PrepDelegations delegation = new PrepDelegations();
            delegation._address = newPrep.getAddress();
            BigInteger percentage = totalPercentage.divide(totalPrepsChosen);
            delegation._votes_in_per = percentage;
            delegations[i] = delegation;
            totalPercentage = totalPercentage.subtract(percentage);
            totalPrepsChosen = totalPrepsChosen.subtract(BigInteger.ONE);
        }
        staking.invoke(owner, "delegate", (Object) delegations);
        Map<String, BigInteger> expectedDelegation = new HashMap<>();
        Map<String, BigInteger> expectedUserDelegation = new HashMap<>();
        for (PrepDelegations delegation : delegations) {
            expectedDelegation.put(delegation._address.toString(), BigInteger.ZERO);
            expectedUserDelegation.put(delegation._address.toString(), delegation._votes_in_per);
        }
        assertEquals(expectedUserDelegation, staking.call("getActualUserDelegationPercentage", owner.getAddress()));
        assertEquals(expectedDelegation, staking.call("getAddressDelegations", owner.getAddress()));

        // Return as per delegation if icx is +ve and delegation yes
        BigInteger totalAmount = BigInteger.valueOf(199L).multiply(ICX);
        contextMock.when(sicxBalanceOf).thenReturn(totalAmount);
        doReturn(ONE_EXA).when(stakingSpy).getTodayRate();
        totalPercentage = HUNDRED_PERCENTAGE;
        BigInteger amountToDistribute = totalAmount.multiply(ONE_EXA).divide(ONE_EXA);

        Map<String, BigInteger> networkDelegationPercentage = (Map<String, BigInteger>) staking.call(
                "getActualUserDelegationPercentage",
                owner.getAddress());
        for (Map.Entry<String, BigInteger> delegation : networkDelegationPercentage.entrySet()) {
            BigInteger amount = delegation.getValue().multiply(amountToDistribute).divide(totalPercentage);
            expectedDelegation.put(delegation.getKey(), amount);
            amountToDistribute = amountToDistribute.subtract(amount);
            totalPercentage = totalPercentage.subtract(delegation.getValue());
        }
        assertEquals(expectedDelegation, staking.call("getAddressDelegations", owner.getAddress()));

        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{});
        assertEquals(Map.of(), staking.call("getActualUserDelegationPercentage", owner.getAddress()));
//
        // Return  distributed map  for omm validators if icx is +ve and delegation null
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        doReturn(Map.of(
                topPreps.get(4).toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100)),
                topPreps.get(5).toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100))
        )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore.getAddress());


        Map<String, BigInteger> defaultDelegationList = new HashMap<>();
        defaultDelegationList.put(topPreps.get(4).toString(),BigInteger.valueOf(995000000000000000L).multiply(BigInteger.valueOf(100)));
        defaultDelegationList.put(topPreps.get(5).toString(),BigInteger.valueOf(995000000000000000L).multiply(BigInteger.valueOf(100)));

        assertEquals(defaultDelegationList, staking.call("getAddressDelegations", alice.getAddress()));
    }

    @Test
    void checkForIscore() {

        Account newPrep = sm.createAccount();
        PrepDelegations delegation = new PrepDelegations();
        delegation._address = newPrep.getAddress();
        delegation._votes_in_per = HUNDRED_PERCENTAGE;

        iScore.put("estimatedICX", BigInteger.ZERO);
        contextMock.when(queryIscore).thenReturn(iScore);
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation});
        contextMock.verify(claimIScore, times(0));

        // claim iscore is called when there is estimated ICX
        iScore.put("estimatedICX", BigInteger.TEN);
        contextMock.when(queryIscore).thenReturn(iScore);
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation});
        contextMock.verify(claimIScore, times(1));
    }

    @Test
    void claimUnstakedICX() {
        BigInteger icxToClaim = BigInteger.valueOf(599L);
        BigInteger icxPayable = BigInteger.valueOf(401L);

        doReturn(BigInteger.ZERO).when(stakingSpy).claimableICX(any(Address.class));
        Executable claimZeroIcx = () -> staking.invoke(owner, "claimUnstakedICX", owner.getAddress());
        expectErrorMessage(claimZeroIcx, "Staked ICX Manager: No claimable icx to claim");

        doReturn(BigInteger.TEN).when(stakingSpy).claimableICX(any(Address.class));
        doReturn(BigInteger.TWO).when(stakingSpy).totalClaimableIcx();
        String expectedErrorMessage = "Staked ICX Manager: No sufficient icx to claim. Requested: 10 " +
                "Available: 2";
        Executable claimMoreThanAvailable = () -> staking.invoke(owner, "claimUnstakedICX", owner.getAddress());
        expectErrorMessage(claimMoreThanAvailable, expectedErrorMessage);

        doReturn(icxPayable).when(stakingSpy).claimableICX(any(Address.class));
        doReturn(icxToClaim).when(stakingSpy).totalClaimableIcx();

        contextMock.when(() -> Context.transfer(any(Address.class), any(BigInteger.class))).then(invocationOnMock -> null);

        staking.invoke(owner, "claimUnstakedICX", owner.getAddress());
        verify(stakingSpy).FundTransfer(owner.getAddress(), icxPayable,
                icxPayable + " ICX sent to " + owner.getAddress() + ".");
        contextMock.verify(() -> Context.transfer(owner.getAddress(), icxPayable));
    }

    @Test
    void delegate() {
        Account newPrep = sm.createAccount();
        PrepDelegations delegation = new PrepDelegations();
        delegation._address = newPrep.getAddress();
        delegation._votes_in_per = HUNDRED_PERCENTAGE;

        Executable duplicatePrep = () -> staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{delegation,
                delegation});
        String expectedErrorMessage = "Staked ICX Manager: You can not delegate same P-Rep twice in a " +
                "transaction.";
        expectErrorMessage(duplicatePrep, expectedErrorMessage);

        delegation._votes_in_per = BigInteger.TEN;
        Executable voteLessThanMinimum = () -> staking.invoke(owner, "delegate",
                (Object) new PrepDelegations[]{delegation});
        expectedErrorMessage = "Staked ICX Manager: You should provide delegation percentage more than 0" +
                ".001%.";
        expectErrorMessage(voteLessThanMinimum, expectedErrorMessage);

        delegation._votes_in_per = HUNDRED_PERCENTAGE.subtract(BigInteger.ONE);
        Executable totalLessThanHundred = () -> staking.invoke(owner, "delegate",
                (Object) new PrepDelegations[]{delegation});
        expectedErrorMessage = "Staked ICX Manager: Total delegations should be 100%.";
        expectErrorMessage(totalLessThanHundred, expectedErrorMessage);
    }

    @Test
    void unstake() {
        // stake
        sm.call(owner, ICX.multiply(BigInteger.valueOf(199L)), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);
        sm.call(alice, ICX.multiply(BigInteger.valueOf(599L)), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        // Unstake
        JSONObject data = getUnstakeJsonData();
        staking.invoke(sicx, "tokenFallback", owner.getAddress(), BigInteger.valueOf(100L), data.toString().getBytes());
        BigInteger ownerBlockHeight = BigInteger.valueOf(sm.getBlock().getHeight());
        staking.invoke(sicx, "tokenFallback", alice.getAddress(), BigInteger.valueOf(50L), data.toString().getBytes());
        BigInteger aliceBlockHeight = BigInteger.valueOf(sm.getBlock().getHeight());

        List<Map<String, Object>> ownerUnstakedDetails = new ArrayList<>();
        ownerUnstakedDetails.add(Map.of("amount", BigInteger.valueOf(100L), "from", owner.getAddress(), "blockHeight"
                , ownerBlockHeight.add(unlockPeriod), "sender", owner.getAddress()));
        assertEquals(ownerUnstakedDetails, staking.call("getUserUnstakeInfo", owner.getAddress()));

        List<Map<String, Object>> aliceUnstakedDetails = new ArrayList<>();
        aliceUnstakedDetails.add(Map.of("amount", BigInteger.valueOf(50L), "from", alice.getAddress(), "blockHeight"
                , aliceBlockHeight.add(unlockPeriod), "sender", alice.getAddress()));
        assertEquals(aliceUnstakedDetails, staking.call("getUserUnstakeInfo", alice.getAddress()));

        BigInteger totalUnstaked = BigInteger.valueOf(150L);
        assertEquals(totalUnstaked, staking.call("getUnstakingAmount"));

        contextMock.when(() -> Context.getBalance(any(Address.class))).thenReturn(BigInteger.TEN);
        Map<String, Object> unstakeList = new HashMap<>();
        unstakeList.put("unstake", totalUnstaked);
        List<Map<String, Object>> unstakes = new ArrayList<>();
        unstakes.add(unstakeList);
        contextMock.when(getStake).thenReturn(Map.of("unstakes", unstakes));

        sm.call(sm.createAccount(), BigInteger.TEN, staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);
        totalUnstaked = totalUnstaked.subtract(BigInteger.TEN);
        assertEquals(totalUnstaked, staking.call("getUnstakingAmount"));
        assertEquals(BigInteger.TEN, staking.call("claimableICX", owner.getAddress()));
        assertEquals(BigInteger.ZERO, staking.call("claimableICX", alice.getAddress()));
        assertEquals(BigInteger.TEN, staking.call("totalClaimableIcx"));

        contextMock.when(() -> Context.getBalance(any(Address.class))).thenReturn(BigInteger.valueOf(200L));
        sm.call(sm.createAccount(), BigInteger.valueOf(200L), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);
        totalUnstaked = totalUnstaked.subtract(BigInteger.valueOf(200L).min(totalUnstaked));
        assertEquals(totalUnstaked, staking.call("getUnstakingAmount"));
        assertEquals(BigInteger.valueOf(100L), staking.call("claimableICX", owner.getAddress()));
        assertEquals(BigInteger.valueOf(50L), staking.call("claimableICX", alice.getAddress()));
        assertEquals(BigInteger.valueOf(150L), staking.call("totalClaimableIcx"));
    }

    @Test
    void transferUpdateDelegations() {

    }

    @Test
    public void stakeICXOmmDelegationsIsPresent(){
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        Address ommLendingPoolCore= Account.newScoreAccount(1).getAddress();
        doReturn(ommLendingPoolCore).when(stakingSpy).getOmmLendingPoolCore();
        doReturn(Map.of(
                topPreps.get(4).toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100)),
                topPreps.get(5).toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100))
        )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore);

        Account caller = sm.createAccount();
        sm.call(caller, BigInteger.valueOf(12).multiply(ICX), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        Map<String,BigInteger> expectedDelegations = new HashMap<>();
        expectedDelegations.put(topPreps.get(4).toString(),BigInteger.valueOf(6).multiply(ICX));
        expectedDelegations.put(topPreps.get(5).toString(),BigInteger.valueOf(6).multiply(ICX));

        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));
        assertEquals(Map.of(),staking.call("getAddressDelegations",caller.getAddress()));
    }

    @Test
    public void stakeICXNotInTopPrep(){
        // the omm delegation preference does not lie in top prep list
        // the remaining delegation goes to topPrep with highest rank
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        Address ommLendingPoolCore= Account.newScoreAccount(1).getAddress();
        doReturn(ommLendingPoolCore).when(stakingSpy).getOmmLendingPoolCore();
        doReturn(Map.of(
                topPreps.get(4).toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100)),
                ommLendingPoolCore.toString(),BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100))
        )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore);

        Account caller = sm.createAccount();
        sm.call(caller, BigInteger.valueOf(12).multiply(ICX), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        Map<String,BigInteger> expectedDelegations = new HashMap<>();
        expectedDelegations.put(topPreps.get(4).toString(),BigInteger.valueOf(6).multiply(ICX));
        expectedDelegations.put(topPreps.get(0).toString(),BigInteger.valueOf(6).multiply(ICX));

        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));
        assertEquals(Map.of(),staking.call("getAddressDelegations",caller.getAddress()));

    }

    @Test
    public void stakeICX_to_userDelegations(){

        PrepDelegations[] prepDelegations = getPrepDelegations1(4);
        // prep delegation list
        // _data = new byte[0]

        Account caller = sm.createAccount();
        BigInteger amountToStake = BigInteger.valueOf(12).multiply(ICX);
        sm.call(caller, amountToStake, staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        assertEquals(amountToStake,staking.call("getTotalStake"));

        contextMock.when(() -> Context.call(sicx.getAddress(),"balanceOf",
                caller.getAddress())).thenReturn(amountToStake);

        staking.invoke(caller, "delegate", (Object) prepDelegations);

        Map<String, BigInteger> expectedDelegation = new HashMap<>();
        Map<String, BigInteger> actualUserDelegation = new HashMap<>();
        for (PrepDelegations delegation : prepDelegations) {
            expectedDelegation.put(delegation._address.toString(), BigInteger.valueOf(3).multiply(ICX));
            actualUserDelegation.put(delegation._address.toString(), BigInteger.valueOf(3).multiply(ICX));
        }

        assertEquals(expectedDelegation,staking.call("getPrepDelegations"));
        assertEquals(actualUserDelegation,staking.call("getAddressDelegations",caller.getAddress()));
    }

    @Test
    public void userDelegations_not_in_top_prep(){
        // delegates to only for omm Prep

        PrepDelegations delegation = new PrepDelegations();
        Address address = sm.createAccount().getAddress();
        delegation._address = address;
        delegation._votes_in_per = HUNDRED_PERCENTAGE;

        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        String ommPrep = topPreps.get(0).toString();
        String ommPrep2 = topPreps.get(1).toString();

        Address ommLendingPoolCore= Account.newScoreAccount(1).getAddress();
        doReturn(ommLendingPoolCore).when(stakingSpy).getOmmLendingPoolCore();
        doReturn(Map.of(
                ommPrep,BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100)),
                ommPrep2,BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100))
        )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore);

        Account caller = sm.createAccount();
        BigInteger amountToStake = BigInteger.valueOf(12).multiply(ICX);
        sm.call(caller, amountToStake, staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        Map<String, BigInteger> expectedDelegations = new HashMap<>();
        expectedDelegations.put(ommPrep,BigInteger.valueOf(6).multiply(ICX));
        expectedDelegations.put(ommPrep2,BigInteger.valueOf(6).multiply(ICX));
        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));


        Map<String, BigInteger> expectedOmmPreps = new HashMap<>();
        expectedOmmPreps.put(ommPrep,BigInteger.valueOf(6).multiply(ICX));
        expectedOmmPreps.put(ommPrep2,BigInteger.valueOf(6).multiply(ICX));
        assertEquals(expectedOmmPreps,staking.call("getPrepDelegations"));


        assertEquals(amountToStake,staking.call("getTotalStake"));

        contextMock.when(() -> Context.call(sicx.getAddress(),"balanceOf",
                caller.getAddress())).thenReturn(amountToStake);

        staking.invoke(caller, "delegate", (Object) new PrepDelegations[]{delegation});


        Map<String, BigInteger> actualUserDelegation = new HashMap<>();
        actualUserDelegation.put(address.toString(),amountToStake);
        assertEquals(actualUserDelegation,staking.call("getAddressDelegations",caller.getAddress()));

        expectedDelegations.put(ommPrep2,BigInteger.valueOf(6).multiply(ICX));
        expectedDelegations.put(ommPrep,BigInteger.valueOf(6).multiply(ICX));
        expectedDelegations.remove(address.toString());
        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));

    }

    @Test
    public void stakeIcx_full_flow(){
        // user delegation contains 1 topPrep,1 not a top prep
        // omm contributrs contain 2 preps in which 1 is not a top prep
        // remaining icx should be given to top prep with the highest rank

        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        Address address = sm.createAccount().getAddress();


        PrepDelegations delegation = new PrepDelegations();
        delegation._address = address;
        delegation._votes_in_per = HUNDRED_PERCENTAGE.divide(BigInteger.TWO);

        PrepDelegations delegations2 = new PrepDelegations();
        delegations2._address = topPreps.get(1);
        delegations2._votes_in_per = HUNDRED_PERCENTAGE.divide(BigInteger.TWO);

        Address ommLendingPoolCore= Account.newScoreAccount(1).getAddress();
        doReturn(ommLendingPoolCore).when(stakingSpy).getOmmLendingPoolCore();
        String ommPrep = topPreps.get(2).toString();
        String ommPrep2 = sm.createAccount().getAddress().toString();
        doReturn(Map.of(
                ommPrep,BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100)),
                ommPrep2,BigInteger.valueOf(500000000000000000L).multiply(BigInteger.valueOf(100))
        )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore);


        Account caller = sm.createAccount();
        BigInteger amountToStake = BigInteger.valueOf(10).multiply(ICX);
        sm.call(caller, amountToStake, staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

        assertEquals(amountToStake,staking.call("getTotalStake"));

        Map<String, BigInteger> expectedDelegations = new HashMap<>();
        expectedDelegations.put(topPreps.get(0).toString(),amountToStake.divide(BigInteger.TWO));
        expectedDelegations.put(topPreps.get(2).toString(),amountToStake.divide(BigInteger.TWO));

        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));

        contextMock.when(() -> Context.call(sicx.getAddress(),"balanceOf",
                caller.getAddress())).thenReturn(amountToStake);

        staking.invoke(caller, "delegate", (Object) new PrepDelegations[]{delegation,delegations2});

        Map<String, BigInteger> actualUserDelegation = new HashMap<>();
        actualUserDelegation.put(delegation._address.toString(),BigInteger.valueOf(5).multiply(ICX));
        actualUserDelegation.put(delegations2._address.toString(),BigInteger.valueOf(5).multiply(ICX));
        assertEquals(actualUserDelegation,staking.call("getAddressDelegations",caller.getAddress()));

        expectedDelegations = new HashMap<>();

        expectedDelegations.put(topPreps.get(0).toString(),BigInteger.valueOf(25).multiply(ICX).divide(BigInteger.TEN));
        expectedDelegations.put(topPreps.get(2).toString(),BigInteger.valueOf(25).multiply(ICX).divide(BigInteger.TEN));
//        expectedDelegations.put(topPreps.get(2).toString(),BigInteger.ZERO);
        expectedDelegations.put(delegations2._address.toString(),BigInteger.valueOf(5).multiply(ICX));
//        expectedDelegations.put(address.toString(),BigInteger.valueOf(5).multiply(ICX));

        assertEquals(expectedDelegations,staking.call("getPrepDelegations"));


    }

    private PrepDelegations[] getPrepDelegations1(int n) {
        // 100 % n = 0
        PrepDelegations[] delegations = new PrepDelegations[n];
        BigInteger val = HUNDRED_PERCENTAGE.divide(BigInteger.valueOf(n));
        List<Map<String, Object>> prepList = (List<Map<String, Object>>) prepsResponse.get("preps");
        for (int i = 0; i < n; i++) {
            PrepDelegations prepDelegation = new PrepDelegations();
            prepDelegation._address= (Address) prepList.get(i).get("address");
            prepDelegation._votes_in_per=val;
            delegations[i] = prepDelegation;
        }
        return delegations;

    }

    @Test
    void stakeICX() {
        Map<String, Object> prepDict = new HashMap<>();
        prepDict.put("totalBlocks",BigInteger.valueOf(100));
        prepDict.put("validatedBlocks",BigInteger.valueOf(80));
        prepDict.put("power",BigInteger.valueOf(10));

        Account newPrep = sm.createAccount();
        Address prep = newPrep.getAddress();

        Address prep2 = sm.createAccount().getAddress();
        Address prep3 = sm.createAccount().getAddress();
        Address prep4 = sm.createAccount().getAddress();
        // prep delegation list
        // _data = new byte[0]
        doReturn(Map.of(
                prep.toString(),ICX.divide(BigInteger.valueOf(4)),
                prep2.toString(),ICX.divide(BigInteger.valueOf(4)),
                prep3.toString(),ICX.divide(BigInteger.valueOf(4)),
                prep4.toString(),ICX.divide(BigInteger.valueOf(4))
        )).when(stakingSpy).getActualUserDelegationPercentage(any(Address.class));

        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("getPRep"),
                any(Address.class))).thenReturn(prepDict);

        sm.call(sm.createAccount(), BigInteger.valueOf(12).multiply(ICX), staking.getAddress(), "stakeICX",
                new Address(new byte[Address.LENGTH]), new byte[0]);

    }

    @Test
    void updateValidPreps(){

        // initially all top Preps are valid preps
        List<Address> validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrep,prepAddress);

        /* out of 18 topPreps
         * 2 preps -> null jailFlag, null commissionRate
         * 2 preps -> null jailFlag, valid commissionRate
         * 2 preps -> not jailed, valid commissionRate
         * 2 preps ->  not jailed, null commissionRate
         *
         * 2 preps ->  jailed, valid commissionRate
         * 2 preps ->  jailed, invalid commissionRate
         * 2 preps ->  jailed, null commissionRate
         * 2 preps -> null jailFlag, invalid commissionRate
         * 2 preps ->  not jailed, invalid commissionRate */

        List<Address> prepAddr = new ArrayList<>();
        Map<String,Object> prepResponse = new HashMap<>();
        prepResponse.put("blockHeight", BigInteger.valueOf(123456L));
        List<Map<String, Object>> prepsList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10));
            prepsList.add(prep);
            prepAddr.add(prepAddress.get(i));
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+2),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "commissionRate",BigInteger.TEN);
            prepsList.add(prep);
            prepAddr.add(prepAddress.get(i+2));
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+4),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ZERO,
                    "commissionRate",BigInteger.TEN);
            prepsList.add(prep);
            prepAddr.add(prepAddress.get(i+4));
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+6),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ZERO);
            prepsList.add(prep);
            prepAddr.add(prepAddress.get(i+6));
        }

        // invalid Preps
        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+8),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ONE,
                    "commissionRate",BigInteger.TEN);
            prepsList.add(prep);
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+10),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ONE,
                    "commissionRate",BigInteger.valueOf(50));
            prepsList.add(prep);
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+12),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ONE);
            prepsList.add(prep);
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+14),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "commissionRate",BigInteger.valueOf(50));
            prepsList.add(prep);
        }

        for (int i = 0; i < 2; i++) {
            Map<String, Object> prep = Map.of("address",prepAddress.get(i+16),
                    "totalBlocks", BigInteger.valueOf(1000),
                    "validatedBlocks",BigInteger.valueOf(950),
                    "power", BigInteger.valueOf(10),
                    "jailFlags",BigInteger.ONE,
                    "commissionRate",BigInteger.valueOf(50));
            prepsList.add(prep);
        }

        prepResponse.put("preps", prepsList);

        contextMock.when(getPreps).thenReturn(prepResponse);
        staking.invoke(owner,"updatePreps");
        validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrep,prepAddr);
        assertEquals(validPrep.size(),8);


    }

    @Test
    void claimIscore_withChangeInValidPreps(){

        List<Address> validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrep,prepAddress);

        iScore.put("estimatedICX", BigInteger.TEN);
        contextMock.when(queryIscore).thenReturn(iScore);

        // all preps except prepAddress.get(0) is jailed
        Map<String, Object> prepDict = Map.of("jailFlags",BigInteger.TEN,
                "commissionRate",BigInteger.valueOf(50));
        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("getPRep"),
                any(Address.class))).thenReturn(prepDict);

        prepDict = new HashMap<>();
        contextMock.when(() -> Context.call(SYSTEM_SCORE_ADDRESS, "getPRep",
                prepAddress.get(0))).thenReturn(prepDict);

        contextMock.when(sicxBalanceOf).thenReturn(BigInteger.TEN.multiply(ICX));
        doReturn(BigInteger.TEN.multiply(ICX)).when(stakingSpy).getTotalStake();
        staking.invoke(owner, "delegate", (Object) new PrepDelegations[]{});

        validPrep = (List<Address>) staking.call("getValidPreps");

        assertEquals(validPrep.size(),1);
        assertEquals(validPrep.get(0),prepAddress.get(0));

        Map<String,BigInteger> expectedPrepDelegation = Map.of(
                prepAddress.get(0).toString(),BigInteger.valueOf(10).multiply(ICX)
        );

        assertEquals(expectedPrepDelegation, staking.call("getPrepDelegations"));

    }

    @Test
    void stakeICX_checkDelegation_forValidPreps(){
        List<Address> validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrep,prepAddress);
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        assertEquals(topPreps.size(),100);

        sm.call(alice, BigInteger.TEN.multiply(ICX), staking.getAddress(), "stakeICX",alice.getAddress(), new byte[0]);

        Map<String,BigInteger> expectedPrepDelegation = Map.of(
                prepAddress.get(0).toString(),BigInteger.valueOf(10).multiply(ICX)
        );
        assertEquals(expectedPrepDelegation, staking.call("getPrepDelegations"));
        assertEquals(expectedPrepDelegation, staking.call("getbOMMDelegations"));
        assertEquals(Map.of(), staking.call("getActualPrepDelegations"));
    }

    @Test
    void freeICX_withFewValidPreps(){
        List<Address> validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrep,prepAddress);
        List<Address> topPreps = (List<Address>) staking.call("getTopPreps");
        assertEquals(topPreps.size(),100);

        // change valid preps when claimming iScore
        iScore.put("estimatedICX", BigInteger.TEN);
        contextMock.when(queryIscore).thenReturn(iScore);

        // mock all preps as invalid
        Map<String, Object> prepDict = Map.of("jailFlags",BigInteger.TEN,
                "commissionRate",BigInteger.valueOf(50));
        contextMock.when(() -> Context.call(eq(SYSTEM_SCORE_ADDRESS), eq("getPRep"),
                any(Address.class))).thenReturn(prepDict);

        // mock 4 prep as valid Preps
        prepDict = new HashMap<>();
        contextMock.when(() -> Context.call(SYSTEM_SCORE_ADDRESS, "getPRep",
                prepAddress.get(0))).thenReturn(prepDict);

        prepDict = Map.of("jailFlags",BigInteger.ZERO,
                "commissionRate",BigInteger.valueOf(0));
        contextMock.when(() -> Context.call(SYSTEM_SCORE_ADDRESS, "getPRep",
                prepAddress.get(1))).thenReturn(prepDict);

        prepDict = Map.of("jailFlags",BigInteger.ZERO,
                "commissionRate",BigInteger.TEN);
        contextMock.when(() -> Context.call(SYSTEM_SCORE_ADDRESS, "getPRep",
                prepAddress.get(2))).thenReturn(prepDict);

        prepDict = Map.of(
                "commissionRate",BigInteger.valueOf(5));
        contextMock.when(() -> Context.call(SYSTEM_SCORE_ADDRESS, "getPRep",
                prepAddress.get(3))).thenReturn(prepDict);

        List<Address> validPrepList = new ArrayList<>();
        validPrepList.add(prepAddress.get(0));
        validPrepList.add(prepAddress.get(1));
        validPrepList.add(prepAddress.get(2));
        validPrepList.add(prepAddress.get(3));


        // setting omm lending pool core delegations
        // 3 preps out of 5 are valid preps
        doReturn(Map.of(
                prepAddress.get(1).toString(),BigInteger.valueOf(300000000000000000L).multiply(BigInteger.valueOf(100)),
                prepAddress.get(2).toString(),BigInteger.valueOf(200000000000000000L).multiply(BigInteger.valueOf(100)),
                prepAddress.get(3).toString(),BigInteger.valueOf(200000000000000000L).multiply(BigInteger.valueOf(100)),
                prepAddress.get(4).toString(),BigInteger.valueOf(200000000000000000L).multiply(BigInteger.valueOf(100)),
                prepAddress.get(5).toString(),BigInteger.valueOf(100000000000000000L).multiply(BigInteger.valueOf(100))
                )).when(stakingSpy).getActualUserDelegationPercentage(ommLendingPoolCore.getAddress());

        sm.call(alice, BigInteger.TEN.multiply(ICX), staking.getAddress(), "stakeICX",alice.getAddress(), new byte[0]);

        assertEquals(Map.of(),staking.call("getActualPrepDelegations"));

        Map<String,BigInteger> expectedPrepDelegation = Map.of(
                prepAddress.get(1).toString(),BigInteger.valueOf(3).multiply(ICX),
                prepAddress.get(2).toString(),BigInteger.valueOf(2).multiply(ICX),
                prepAddress.get(3).toString(),BigInteger.valueOf(2).multiply(ICX),
                prepAddress.get(0).toString(),BigInteger.valueOf(3).multiply(ICX)
        );

        assertEquals(expectedPrepDelegation,staking.call("getPrepDelegations"));
        assertEquals(expectedPrepDelegation,staking.call("getbOMMDelegations"));

        validPrep = (List<Address>) staking.call("getValidPreps");
        assertEquals(validPrepList,validPrep);
    }

    @Test
    void delegation_withFewValidPreps(){
        freeICX_withFewValidPreps();


        // alice delegates to preps

        PrepDelegations delegation = new PrepDelegations();
        delegation._address = prepAddress.get(0);
        delegation._votes_in_per = BigInteger.valueOf(30).multiply(ONE_EXA);

        PrepDelegations delegation2 = new PrepDelegations();
        delegation2._address = prepAddress.get(1);
        delegation2._votes_in_per = BigInteger.valueOf(30).multiply(ONE_EXA);

        PrepDelegations delegation3 = new PrepDelegations();
        delegation3._address = prepAddress.get(4);
        delegation3._votes_in_per = BigInteger.valueOf(40).multiply(ONE_EXA);

        contextMock.when(sicxBalanceOf).thenReturn(BigInteger.TEN.multiply(ICX));
        doReturn(BigInteger.TEN.multiply(ICX)).when(stakingSpy).getTotalStake();
        staking.invoke(alice, "delegate", (Object) new PrepDelegations[]{
                delegation,delegation2,delegation3});

        Map<String,BigInteger> expectedUserDelegation = Map.of(
                prepAddress.get(0).toString(),BigInteger.valueOf(3).multiply(ICX),
                prepAddress.get(1).toString(),BigInteger.valueOf(3).multiply(ICX),
                prepAddress.get(4).toString(),BigInteger.valueOf(4).multiply(ICX)
        );

        assertEquals(expectedUserDelegation,staking.call("getActualPrepDelegations"));

        Map<String,BigInteger> expectedPrepDelegation = Map.of(
                prepAddress.get(0).toString(),BigInteger.valueOf(42).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(1).toString(),BigInteger.valueOf(42).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(2).toString(),BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(3).toString(),BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN)
        );
        assertEquals(expectedPrepDelegation,staking.call("getPrepDelegations"));

        Map<String,BigInteger> expectedBommDelegation = Map.of(
                prepAddress.get(0).toString(),BigInteger.valueOf(12).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(1).toString(),BigInteger.valueOf(12).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(2).toString(),BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN),
                prepAddress.get(3).toString(),BigInteger.valueOf(8).multiply(ICX).divide(BigInteger.TEN)
        );
        assertEquals(expectedBommDelegation,staking.call("getbOMMDelegations"));
    }

    @AfterEach
    void closeMock() {
        contextMock.close();
    }

    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

}