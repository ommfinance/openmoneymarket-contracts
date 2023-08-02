package finance.omm.score.core;

import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.pow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.structs.PrepICXDelegations;
import finance.omm.score.core.delegation.DelegationImpl;
import finance.omm.utils.constants.AddressConstant;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.Context;

public class DelegationTest extends TestBase {

    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    private static Score delegationScore;
    static DelegationImpl scoreSpy;

    static Address contributor1;
    static Address contributor2;

    static MockedStatic<Context> contextMock;

    @BeforeAll
    protected static void init() {
        contextMock = Mockito.mockStatic(Context.class, Mockito.CALLS_REAL_METHODS);
    }


    @BeforeEach
    public void setup() throws Exception {
        delegationScore = sm.deploy(owner, DelegationImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        DelegationImpl t = (DelegationImpl) delegationScore.getInstance();
        scoreSpy = spy(t);
        delegationScore.setInstance(scoreSpy);
        setAddresses();
        contributor1 = sm.createAccount().getAddress();
        contributor2 = sm.createAccount().getAddress();
    }

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new java.util.HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.BOOSTED_OMM, Account.newScoreAccount(102));
        put(Contracts.LENDING_POOL_CORE, Account.newScoreAccount(103));
        put(Contracts.STAKING, Account.newScoreAccount(104));
        put(Contracts.sICX, Account.newScoreAccount(105));
    }};

    private static void setAddresses() {
        AddressDetails[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetails ad = new AddressDetails();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetails[]::new);

        Object[] params = new Object[]{
                addressDetails
        };
        delegationScore.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }

    @DisplayName("should return correct name")
    @Test
    void name() {
        String delegationScoreName = "OMM Delegation";
        assertEquals(delegationScoreName, delegationScore.call("name"));
    }

    @DisplayName("owner should be able to set vote threshold")
    @Test
    void ownerSetVoteThreshold() {
        BigInteger voteThreshold = BigInteger.valueOf(1000);
        delegationScore.invoke(owner, "setVoteThreshold", voteThreshold);
        BigInteger actualVoteThreshold = (BigInteger) delegationScore.call("getVoteThreshold");
        assertEquals(voteThreshold, actualVoteThreshold);
    }

    @DisplayName("general user should not be able to set vote threshold")
    @Test
    void othersSetVoteThreshold() {
        Account user = sm.createAccount();
        BigInteger voteThreshold = BigInteger.valueOf(1000);
        Executable call = () -> delegationScore.invoke(user, "setVoteThreshold", voteThreshold);
        expectErrorMessage(call, "require owner access");
    }

    @DisplayName("should be able to add single contributor")
    @Test
    void addContributor() {
        Account contributor1 = sm.createAccount();
        Account contributor2 = sm.createAccount();

        List<Address> contributorsList = new ArrayList<>();
        contributorsList.add(contributor1.getAddress());
        contributorsList.add(contributor2.getAddress());

        delegationScore.invoke(owner, "addContributor", contributor1.getAddress());
        delegationScore.invoke(owner, "addContributor", contributor2.getAddress());

        List<Address> contributors = (List<Address>) delegationScore.call("getContributors");

        assertEquals(contributors, contributorsList);
    }

    @DisplayName("should be able to add multiple contributor")
    @Test
    void addAllContributors() {
        Account contributor1 = sm.createAccount();
        Account contributor2 = sm.createAccount();

        List<Address> contributorsList = new ArrayList<>();
        contributorsList.add(contributor1.getAddress());
        contributorsList.add(contributor2.getAddress());

        Object[] params = new Object[]{
                contributorsList.toArray(Address[]::new)
        };

        delegationScore.invoke(owner, "addAllContributors", params);
        List<Address> contributors = (List<Address>) delegationScore.call("getContributors");

        assertEquals(contributors, contributorsList);
    }

    @DisplayName("should be able to remove contributors")
    @Test
    void removeContributor() {
        Account contributor1 = sm.createAccount();
        Account contributor2 = sm.createAccount();

        List<Address> contributorsList = new ArrayList<>();
        contributorsList.add(contributor1.getAddress());
        contributorsList.add(contributor2.getAddress());

        Object[] params = new Object[]{
                contributorsList.toArray(Address[]::new)
        };

        delegationScore.invoke(owner, "addAllContributors", params);
        delegationScore.invoke(owner, "removeContributor", contributor1.getAddress());

        List<Address> newContributors = (List<Address>) delegationScore.call("getContributors");
        contributorsList.remove(contributor1.getAddress());

        assertEquals(contributorsList, newContributors);
    }

    void initialize() {
        Mockito.reset();
        BigInteger voteThreshold = BigInteger.TEN.pow(18);
        delegationScore.invoke(owner, "setVoteThreshold", voteThreshold);

        List<Address> contributorsList = new ArrayList<>();
        contributorsList.add(contributor1);
        contributorsList.add(contributor2);

        Object[] params = new Object[]{
                contributorsList.toArray(Address[]::new)
        };

        delegationScore.invoke(owner, "addAllContributors", params);
    }

    private PrepDelegations[] getPrepDelegations1(int n) {
        // 100 % n = 0
        PrepDelegations[] delegations = new PrepDelegations[n];
        int val = 100 / n;

        for (int i = 0; i < n; i++) {
            Address prep = sm.createAccount().getAddress();
            BigInteger votesInPer = BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16));
            PrepDelegations prepDelegation = new PrepDelegations(prep, votesInPer);
            delegations[i] = prepDelegation;
        }
        return delegations;
    }

    @DisplayName("invalid preps")
    @Test
    void invalidPreps() {
        initialize();
        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        prepDetails = Map.of("status", BigInteger.ONE);

        PrepDelegations[] prepDelegations = getPrepDelegations1(4);

        contextMock
                .when(() -> Context.call(Map.class, AddressConstant.ZERO_SCORE_ADDRESS,
                        "getPRep", prepDelegations[0]._address))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        Executable call = () -> delegationScore.invoke(owner, "updateDelegations", prepDelegations, owner.getAddress());
        String expectedErrorMessage = "Delegation" + ": Invalid prep: " + prepDelegations[0]._address;
        expectErrorMessage(call, expectedErrorMessage);
    }


    @DisplayName("clear previous delegation preference of user")
    @Test
        /*
         * User who delegated to some preps tries to clear their delegation preferences
         */
    void clearPrevious() {

        Account user1 = sm.createAccount();

        initialize();
        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());

        BigInteger balance = (BigInteger) delegationScore.call("getWorkingBalance", user1.getAddress());
        assertEquals(BigInteger.ZERO, balance);

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        BigInteger prevTotal = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        assertEquals(prevTotal, BigInteger.ZERO);

        PrepDelegations[] delegations = getPrepDelegations1(4);
        delegationScore.invoke(user1, "updateDelegations", delegations, user1.getAddress());
        BigInteger betweenTotal = (BigInteger) delegationScore.call("getWorkingTotalSupply");

        delegationScore.invoke(user1, "clearPrevious", user1.getAddress());
        BigInteger afterTotal = (BigInteger) delegationScore.call("getWorkingTotalSupply");

        assertEquals(betweenTotal, afterTotal);
        assertEquals(afterTotal, workingBalance);

        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", user1.getAddress());
        PrepDelegations[] defDelegations = (PrepDelegations[]) delegationScore
                .call("distributeVoteToContributors");

        // check for default delegations
        assertArrayEquals(scoreDelegations, defDelegations);
        assertEquals(true, delegationScore.call("userDefaultDelegation", user1.getAddress()));
    }

    @DisplayName("kick user after voting period")
    @Test
    void kick() {
        Account user = sm.createAccount();
        initialize();

        Executable call = () -> delegationScore.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.BOOSTED_OMM), "onKick",
                user.getAddress(), BigInteger.ONE, "temp".getBytes());
        expectErrorMessage(call, user.getAddress() + " OMM locking has not expired");

        // should be kicked if boosted omm balance = 0
        doReturn(ICX).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());
        delegationScore.invoke(user, "updateDelegations", new PrepDelegations[0], user.getAddress());

        // prep delegations should be zero

        delegationScore.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.BOOSTED_OMM), "onKick",
                user.getAddress(), BigInteger.ZERO, "temp".getBytes());
        verify(scoreSpy).UserKicked(user.getAddress(), "temp".getBytes());

        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", user.getAddress());
        // percentageDelegations should be preserved
        for (PrepDelegations pd : scoreDelegations) {
            assertEquals(pd._votes_in_per, BigInteger.valueOf(50).multiply(pow(
                    BigInteger.valueOf(10), 16)));
        }

        Map<String, BigInteger> prepVotes = (Map<String, BigInteger>)
                delegationScore.call("userPrepVotes", user.getAddress());
        // userPrepVotes should be equal to zero.
        for (Map.Entry<String, BigInteger> entry : prepVotes.entrySet()) {
            assertEquals(BigInteger.ZERO, entry.getValue());
        }
    }

    @DisplayName("users delegate to default delegation")
    @Test
        /*
         * Tested Conditions
         * User calls updateDelegation method multiple times and set default delegation preferences
         * Check if correct delegation preference is set for both users
         * A prep changed their delegation preference
         * Check if new data is correct
         */
    void defaultDelegation() {
        initialize();
        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(owner, "updateDelegations", new PrepDelegations[0], owner.getAddress());
        delegationScore.invoke(owner, "updateDelegations", new PrepDelegations[0], owner.getAddress());

        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", owner.getAddress());
        PrepDelegations[] defDelegations = (PrepDelegations[]) delegationScore
                .call("distributeVoteToContributors");

        // check for default delegations
        assertArrayEquals(scoreDelegations, defDelegations);

        BigInteger total = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        assertEquals(BigInteger.TEN.pow(18), total);

        boolean isDefault = (boolean) delegationScore.call("userDefaultDelegation", owner.getAddress());
        assertEquals(true, isDefault);
    }

    @DisplayName("user should be able to update their delegation")
    @Test
        /*
         * Tested Conditions
         * User calls updateDelegation method
         * Check os correct delegation preference is set
         * Check if each prep is getting correct number of votes
         * Check if user prep votes is returning correct data
         */
    void updateDelegationsMethod() {
        initialize();

        PrepDelegations[] delegations = getPrepDelegations1(4);

        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(owner, "updateDelegations", delegations, owner.getAddress());
        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", owner.getAddress());
        assertArrayEquals(delegations, scoreDelegations);

        int val = 100 / 4;
        BigInteger defaultVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                workingBalance);

        for (PrepDelegations prepDelegation : delegations) {
            Address prep = prepDelegation._address;

            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, defaultVote);
        }

        Map<String, BigInteger> userPreps = (Map<String, BigInteger>) delegationScore.call("userPrepVotes",
                owner.getAddress());

        for (Map.Entry<String, BigInteger> entry : userPreps.entrySet()) {
            Address prep = Address.fromString(entry.getKey());
            BigInteger userVote = entry.getValue();
            BigInteger prepVote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(userVote, prepVote);
        }
    }

    @DisplayName("user changes their delegation preferences")
    @Test
        /*
         * Tested Conditions
         * User calls updateDelegation method multiple times
         * Check if correct delegation preference is set each time
         * Check if votes delegated to prep at first were replaced and set to new preps
         * Check computeDelegationPercentages method
         */
    void changeDelegationPreferences() {
        initialize();

        PrepDelegations[] delegations = getPrepDelegations1(2);

        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(owner, "updateDelegations", delegations, owner.getAddress());
        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", owner.getAddress());
        assertArrayEquals(delegations, scoreDelegations);

        int val = 100 / 2;
        BigInteger defaultVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                BigInteger.TEN.pow(18));

        for (PrepDelegations prepDelegation : delegations) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, defaultVote);
        }

        // user now change their preferences

        PrepDelegations[] newDelegations = getPrepDelegations1(4);
        val = 100 / 4;
        defaultVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                BigInteger.TEN.pow(18));
        delegationScore.invoke(owner, "updateDelegations", newDelegations, owner.getAddress());
        PrepDelegations[] newScoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", owner.getAddress());

        assertArrayEquals(newDelegations, newScoreDelegations);

        for (PrepDelegations prepDelegation : newDelegations) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, defaultVote);
        }

        // vote to previous should be set to 0
        for (PrepDelegations prepDelegation : delegations) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, BigInteger.ZERO);
        }

        PrepDelegations[] observedDelegation = (PrepDelegations[]) delegationScore.call("computeDelegationPercentages");
        PrepDelegations[] expectedDelegation = new PrepDelegations[4];

        for (int i = 0; i < newDelegations.length; i++) {
            PrepDelegations prepDelegation = newDelegations[i];
            Address prep = prepDelegation._address;
            BigInteger vote = prepDelegation._votes_in_per.multiply(BigInteger.valueOf(100));
            expectedDelegation[i] = new PrepDelegations(prep, vote);
        }
        assertArrayEquals(expectedDelegation, observedDelegation);
        boolean isDefault = (boolean) delegationScore.call("userDefaultDelegation", owner.getAddress());
        assertEquals(false, isDefault);
    }

    @DisplayName("multiple users update their delegations")
    @Test
        /*
         * Tested Conditions
         * Multiple user calls updateDelegation method
         * Check if correct delegation preference is set for both users
         * A prep changed their delegation preference
         * Check if new data is correct
         */
    void updateDelegationsMultipleUsers() {
        initialize();

        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();

        PrepDelegations[] delegation1 = getPrepDelegations1(5);
        PrepDelegations[] delegation2 = getPrepDelegations1(4);

        BigInteger workingBalance1 = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(18));
        BigInteger workingBalance2 = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18));

        doReturn(workingBalance1).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());
        doReturn(workingBalance2).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user2.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(user1, "updateDelegations", delegation1, user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", delegation2, user2.getAddress());

        // for user 1, delegate to 5 preps
        int val = 100 / 5;
        BigInteger defaultVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                workingBalance1);

        for (PrepDelegations prepDelegation : delegation1) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, defaultVote);
        }

        // for user 2, delegate to 4 preps
        val = 100 / 4;
        defaultVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                workingBalance2);

        for (PrepDelegations prepDelegation : delegation2) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, defaultVote);
        }

        // now user 2 updates their delegation preference to mach user 1's preference
        delegationScore.invoke(user2, "updateDelegations", delegation1, user2.getAddress());

        for (PrepDelegations prepDelegation : delegation2) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            System.out.println(prep.toString() + " >> " + vote.toString());
        }

        val = 100 / 5;
        BigInteger expectedVote = exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                workingBalance2).add(exaMultiply(BigInteger.valueOf(val).multiply(BigInteger.valueOf(10).pow(16)),
                workingBalance1));

        for (PrepDelegations prepDelegation : delegation1) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            System.out.println(prep.toString() + " >> " + vote.toString());
            assertEquals(vote, expectedVote);
        }

        for (PrepDelegations prepDelegation : delegation2) {
            Address prep = prepDelegation._address;
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            assertEquals(vote, BigInteger.ZERO);
        }
    }

    @DisplayName("user provides more than 100 delegation preferences")
    @Test
    /*
     * Tested Conditions
     * Check when user tries to delegate to more than 100 preps
     */
    public void moreThanHundredDelegation() {

        PrepDelegations[] delegations = new PrepDelegations[101];

        for (int i = 0; i < 101; i++) {
            Address prep = sm.createAccount().getAddress();
            BigInteger votesInPer = BigInteger.valueOf(1).multiply(BigInteger.valueOf(10).pow(16));
            delegations[i] = new PrepDelegations(prep, votesInPer);
        }

        BigInteger workingBalance = BigInteger.TEN.pow(18);

        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        Executable call = () -> delegationScore.invoke(owner, "updateDelegations", delegations, owner.getAddress());

        String expectedErrorMessage = "Delegation" +
                " updating delegation unsuccessful, more than 100 preps provided by user" +
                " delegations provided " + 101;
        expectErrorMessage(call, expectedErrorMessage);
    }

    @DisplayName("sum of percentage not equal to 100")
    @Test
    /*
     * Tested Conditions
     * Check when the votes do not equal to 100%
     */
    public void updateDelegationsNot100() {

        PrepDelegations[] delegations = new PrepDelegations[5];

        for (int i = 0; i < 5; i++) {
            Address prep = sm.createAccount().getAddress();
            BigInteger votesInPer = BigInteger.valueOf(25).multiply(BigInteger.valueOf(10).pow(16));
            delegations[i] = new PrepDelegations(prep, votesInPer);
        }

        BigInteger workingBalance = BigInteger.TEN.pow(18);

        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", owner.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        Executable call = () -> delegationScore.invoke(owner, "updateDelegations", delegations, owner.getAddress());

        String expectedErrorMessage = "Delegation" +
                ": updating delegation unsuccessful,sum of percentages not equal to 100 " +
                "sum total of percentages 1250000000000000000 delegation preferences 5";
        expectErrorMessage(call, expectedErrorMessage);
    }

    @DisplayName("compute delegation percentages method")
    @Test
    public void computeDelegationPercentages() {
        initialize();

        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();

        PrepDelegations[] delegation = getPrepDelegations1(4);

        BigInteger workingBalance1 = BigInteger.valueOf(200).multiply(BigInteger.TEN.pow(18));
        BigInteger workingBalance2 = BigInteger.valueOf(800).multiply(BigInteger.TEN.pow(18));
        doReturn(workingBalance1).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());
        doReturn(workingBalance2).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user2.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(user1, "updateDelegations", delegation, user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", delegation, user2.getAddress());

        PrepDelegations[] prepVoteCalculation = new PrepDelegations[4];

        for (int i = 0; i < delegation.length; i++) {
            PrepDelegations prepDelegation = delegation[i];
            Address prep = prepDelegation._address;
            BigInteger votes = prepDelegation._votes_in_per.multiply(BigInteger.valueOf(100));
            prepVoteCalculation[i] = new PrepDelegations(prep, votes);
        }

        PrepDelegations[] prepDelegationsScore = (PrepDelegations[])
                delegationScore.call("computeDelegationPercentages");

        assertArrayEquals(prepVoteCalculation, prepDelegationsScore);
    }

    public BigInteger exaHelper(int n) {
        return BigInteger.valueOf(n).multiply(BigInteger.TEN.pow(16));
    }

    @DisplayName("multiple users, all default delegation")
    @Test
    public void multipleUsersDefaultDelegation() {
        Mockito.reset();
        initialize();

        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();
        Account user3 = sm.createAccount();
        Account user4 = sm.createAccount();
        Account user5 = sm.createAccount();

        BigInteger workingBalance = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(18));
        doReturn(workingBalance).when(scoreSpy)
                .call(eq(BigInteger.class), eq(Contracts.BOOSTED_OMM), eq("balanceOf"), any());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        // update delegation preferences
        delegationScore.invoke(user1, "updateDelegations", new PrepDelegations[0], user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", new PrepDelegations[0], user2.getAddress());
        delegationScore.invoke(user3, "updateDelegations", new PrepDelegations[0], user3.getAddress());
        delegationScore.invoke(user4, "updateDelegations", new PrepDelegations[0], user4.getAddress());
        delegationScore.invoke(user5, "updateDelegations", new PrepDelegations[0], user5.getAddress());

        BigInteger total = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        BigInteger expectedTotal = workingBalance.multiply(BigInteger.valueOf(5));
        assertEquals(expectedTotal, total);

        // compute delegation percentages

        // staking 50%
        BigInteger staking50Per = BigInteger.valueOf(50).multiply(BigInteger.TEN.pow(18));

        PrepDelegations[] prepVoteCalculation = new PrepDelegations[2];
        prepVoteCalculation[0] = new PrepDelegations(contributor1, staking50Per);
        prepVoteCalculation[1] = new PrepDelegations(contributor2, staking50Per);

        PrepDelegations[] prepDelegationsScore = (PrepDelegations[])
                delegationScore.call("computeDelegationPercentages");

        assertArrayEquals(prepVoteCalculation, prepDelegationsScore);

    }

    @DisplayName("multiple users, multiple preps, different percentage values")
    @Test
    public void multipleInstances() {
        // initialize
        Mockito.reset();
        initialize();

        // preps
        List<Address> preps = new scorex.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Account prep = sm.createAccount();
            preps.add(prep.getAddress());
        }

        // users
        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();
        Account user3 = sm.createAccount();
        Account user4 = sm.createAccount();
        Account user5 = sm.createAccount();

        // delegation details

        PrepDelegations[] delegations1 = new PrepDelegations[5];
        PrepDelegations[] delegations2 = new PrepDelegations[4];
        PrepDelegations[] delegations3 = new PrepDelegations[3];
        PrepDelegations[] delegations4 = new PrepDelegations[1];

        // user5 will delegate to default contributors, so no initialization necessary

        // delegation for user 1
        delegations1[0] = new PrepDelegations(preps.get(1), exaHelper(10));
        delegations1[1] = new PrepDelegations(preps.get(2), exaHelper(15));
        delegations1[2] = new PrepDelegations(preps.get(3), exaHelper(25));
        delegations1[3] = new PrepDelegations(preps.get(4), exaHelper(5));
        delegations1[4] = new PrepDelegations(preps.get(5), exaHelper(45));

        // delegation for user 2
        delegations2[0] = new PrepDelegations(preps.get(2), exaHelper(35));
        delegations2[1] = new PrepDelegations(preps.get(5), exaHelper(30));
        delegations2[2] = new PrepDelegations(preps.get(6), exaHelper(20));
        delegations2[3] = new PrepDelegations(preps.get(7), exaHelper(15));

        // delegation for user 2
        delegations3[0] = new PrepDelegations(preps.get(8), exaHelper(40));
        delegations3[1] = new PrepDelegations(preps.get(3), exaHelper(40));
        delegations3[2] = new PrepDelegations(preps.get(0), exaHelper(20));

        // delegation for user 4
        delegations4[0] = new PrepDelegations(preps.get(0), exaHelper(100));

        BigInteger workingBalance1 = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(18));
        BigInteger workingBalance2 = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18));
        BigInteger workingBalance3 = BigInteger.valueOf(2000).multiply(BigInteger.TEN.pow(18));

        /*
         * User 1 -> 500  - 1 (10) |  2 (15) |  3 (25) |  4 (05)  |  5 (45)
         * User 2 -> 1000 - 2 (35) |  5 (30) |  6 (20) |  7 (15)  |  -
         * User 3 -> 2000 - 8 (40) |  3 (40) |  0 (20) |  -       |  -
         * User 4 -> 500  - 0(100) |  -      |     -   |  -       |  -
         * User 5 -> 1000 -  default delegation | 50/50
         */

        doReturn(workingBalance1).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());
        doReturn(workingBalance2).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user2.getAddress());
        doReturn(workingBalance3).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user3.getAddress());
        doReturn(workingBalance1).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user4.getAddress());
        doReturn(workingBalance2).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user5.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        // update delegation preferences
        delegationScore.invoke(user1, "updateDelegations", delegations1, user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", delegations2, user2.getAddress());
        delegationScore.invoke(user3, "updateDelegations", delegations3, user3.getAddress());
        delegationScore.invoke(user4, "updateDelegations", delegations4, user4.getAddress());
        delegationScore.invoke(user5, "updateDelegations", new PrepDelegations[0], user5.getAddress());

        // total votes check
        BigInteger total = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        BigInteger expectedTotal = workingBalance3.add(workingBalance2)
                .add(workingBalance2)
                .add(workingBalance1)
                .add(workingBalance1);
        assertEquals(expectedTotal, total);

        // default delegations check
        assertEquals(false, (boolean) delegationScore.call("userDefaultDelegation", user1.getAddress()));
        assertEquals(false, (boolean) delegationScore.call("userDefaultDelegation", user2.getAddress()));
        assertEquals(false, (boolean) delegationScore.call("userDefaultDelegation", user3.getAddress()));
        assertEquals(false, (boolean) delegationScore.call("userDefaultDelegation", user4.getAddress()));
        assertEquals(true, (boolean) delegationScore.call("userDefaultDelegation", user5.getAddress()));

        // prepVotes method check

        /*
         * 0 -> 1 * 500 + .2 * 2000 = 900
         * 1 -> .1 * 500 = 50
         * 2 -> .15 * 500 + .35 * 1000 = 425
         * 3 -> .25 * 500 + .40 * 2000 = 925
         * 4 -> .05 * 500 = 25
         * 5 -> .45 * 500 + .30 * 1000 = 525
         * 6 -> .20 * 1000 = 200
         * 7 -> .15 * 1000 = 150
         * 8 -> .40 * 2000 = 800
         * d1-> .50 * 1000 = 500
         * d2-> .50 * 1000 = 500
         */

        // from above calculation
        Map<String, BigInteger> prepVoteCalc = new HashMap<>();

        prepVoteCalc.put(preps.get(0).toString(), exaHelper(900 * 100));
        prepVoteCalc.put(preps.get(1).toString(), exaHelper(50 * 100));
        prepVoteCalc.put(preps.get(2).toString(), exaHelper(425 * 100));
        prepVoteCalc.put(preps.get(3).toString(), exaHelper(925 * 100));
        prepVoteCalc.put(preps.get(4).toString(), exaHelper(25 * 100));
        prepVoteCalc.put(preps.get(5).toString(), exaHelper(525 * 100));
        prepVoteCalc.put(preps.get(6).toString(), exaHelper(200 * 100));
        prepVoteCalc.put(preps.get(7).toString(), exaHelper(150 * 100));
        prepVoteCalc.put(preps.get(8).toString(), exaHelper(800 * 100));
        prepVoteCalc.put(contributor1.toString(), exaHelper(500 * 100));
        prepVoteCalc.put(contributor2.toString(), exaHelper(500 * 100));

        // from score
        Map<String, BigInteger> prepVoteMap = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            Address prep = preps.get(i);
            BigInteger vote = (BigInteger) delegationScore.call("prepVotes", prep);
            prepVoteMap.put(prep.toString(), vote);
        }

        BigInteger c1Vote = (BigInteger) delegationScore.call("prepVotes", contributor1);
        prepVoteMap.put(contributor1.toString(), c1Vote);

        BigInteger c2Vote = (BigInteger) delegationScore.call("prepVotes", contributor2);
        prepVoteMap.put(contributor2.toString(), c2Vote);

        assertEquals(prepVoteCalc, prepVoteMap);

        // userPrepVotes check
        // user 1
        Map<String, BigInteger> userPrep1 = new HashMap<>();
        userPrep1.put(preps.get(1).toString(), exaHelper((int) (100 * 500 * 0.1)));
        userPrep1.put(preps.get(2).toString(), exaHelper((int) (100 * 500 * 0.15)));
        userPrep1.put(preps.get(3).toString(), exaHelper((int) (100 * 500 * 0.25)));
        userPrep1.put(preps.get(4).toString(), exaHelper((int) (100 * 500 * 0.05)));
        userPrep1.put(preps.get(5).toString(), exaHelper((int) (100 * 500 * 0.45)));

        Map<String, BigInteger> userPrep1Score = (Map<String, BigInteger>)
                delegationScore.call("userPrepVotes", user1.getAddress());

        assertEquals(userPrep1, userPrep1Score);

        // user 3
        Map<String, BigInteger> userPrep3 = new HashMap<>();
        userPrep3.put(preps.get(3).toString(), exaHelper((int) (100 * 2000 * 0.4)));
        userPrep3.put(preps.get(8).toString(), exaHelper((int) (100 * 2000 * 0.4)));
        userPrep3.put(preps.get(0).toString(), exaHelper((int) (100 * 2000 * 0.2)));

        Map<String, BigInteger> userPrep3Score = (Map<String, BigInteger>)
                delegationScore.call("userPrepVotes", user3.getAddress());
        assertEquals(userPrep3, userPrep3Score);

        // get user delegation details check

        // user2
        // User 2 -> 1000 - 2 (35) |  5 (30) |  6 (20) |  7 (15)  |  -
        PrepDelegations[] userDelegations2 = new PrepDelegations[4];
        userDelegations2[0] = new PrepDelegations(preps.get(2), exaHelper((int) (100 * 0.35)));
        userDelegations2[1] = new PrepDelegations(preps.get(5), exaHelper((int) (100 * 0.30)));
        userDelegations2[2] = new PrepDelegations(preps.get(6), exaHelper((int) (100 * 0.20)));
        userDelegations2[3] = new PrepDelegations(preps.get(7), exaHelper((int) (100 * 0.15)));

        PrepDelegations[] userDelegations2Score = (PrepDelegations[]) delegationScore.call(
                "getUserDelegationDetails", user2.getAddress());

        assertArrayEquals(userDelegations2, userDelegations2Score);

        // user4
        PrepDelegations[] userDelegations4 = new PrepDelegations[1];
        userDelegations4[0] = new PrepDelegations(preps.get(0), exaHelper(100));
        PrepDelegations[] userDelegations4Score = (PrepDelegations[]) delegationScore.call(
                "getUserDelegationDetails", user4.getAddress());
        assertArrayEquals(userDelegations4, userDelegations4Score);

        /*
         * 0 -> 1 * 500 + .2 * 2000 = 900
         * 1 -> .1 * 500 = 50
         * 2 -> .15 * 500 + .35 * 1000 = 425
         * 3 -> .25 * 500 + .40 * 2000 = 925
         * 4 -> .05 * 500 = 25
         * 5 -> .45 * 500 + .30 * 1000 = 525
         * 6 -> .20 * 1000 = 200
         * 7 -> .15 * 1000 = 150
         * 8 -> .40 * 2000 = 800
         */

        PrepDelegations[] prepDelegationsScore = (PrepDelegations[])
                delegationScore.call("computeDelegationPercentages");
        PrepDelegations[] computedDelegation = new PrepDelegations[9];

        // 1 and 4 won't be in computeDelegationPercentages as they do not exceed threshold
        // dust votes set to max delegation

        computedDelegation[0] = new PrepDelegations(preps.get(2), exaDivide(toExa(425), total));
        computedDelegation[1] = new PrepDelegations(preps.get(3), exaDivide(toExa(1000), total)); // added here
        computedDelegation[2] = new PrepDelegations(preps.get(5), exaDivide(toExa(525), total));
        computedDelegation[3] = new PrepDelegations(preps.get(6), exaDivide(toExa(200), total));
        computedDelegation[4] = new PrepDelegations(preps.get(7), exaDivide(toExa(150), total));
        computedDelegation[5] = new PrepDelegations(preps.get(8), exaDivide(toExa(800), total));
        computedDelegation[6] = new PrepDelegations(preps.get(0), exaDivide(toExa(900), total));
        computedDelegation[7] = new PrepDelegations(contributor1, exaDivide(toExa(500), total));
        computedDelegation[8] = new PrepDelegations(contributor2, exaDivide(toExa(500), total));

//        computedDelegation[9] = new PrepDelegations(preps.get(1), exaDivide(toExa(50), total));
//        computedDelegation[10] = new PrepDelegations(preps.get(4), exaDivide(toExa(25), total));

        assertArrayEquals(computedDelegation, prepDelegationsScore);
    }


    public BigInteger toExa(int n) {
        return BigInteger.valueOf(n * 100L).multiply(BigInteger.TEN.pow(18));
    }

    @DisplayName("call external methods multiple times")
    @Test
    public void multipleTimes() {
        Account user1 = sm.createAccount();

        initialize();
        BigInteger workingBalance = BigInteger.TEN.pow(18);

        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        BigInteger prevTotal = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        assertEquals(prevTotal, BigInteger.ZERO);

        PrepDelegations[] delegations1 = getPrepDelegations1(4);
        delegationScore.invoke(user1, "updateDelegations", delegations1, user1.getAddress());
        PrepDelegations[] delegations2 = getPrepDelegations1(5);
        delegationScore.invoke(user1, "updateDelegations", delegations2, user1.getAddress());
        PrepDelegations[] delegations3 = getPrepDelegations1(2);
        delegationScore.invoke(user1, "updateDelegations", delegations3, user1.getAddress());
        delegationScore.invoke(user1, "updateDelegations", delegations1, user1.getAddress());
        delegationScore.invoke(user1, "clearPrevious", user1.getAddress());
        delegationScore.invoke(user1, "clearPrevious", user1.getAddress());
        delegationScore.invoke(user1, "updateDelegations", delegations3, user1.getAddress());
        delegationScore.invoke(user1, "clearPrevious", user1.getAddress());

        BigInteger newTotal = (BigInteger) delegationScore.call("getWorkingTotalSupply");
        assertEquals(BigInteger.TEN.pow(18), newTotal);
    }

    @DisplayName("Test user delegations details in ICX")
    @Test
    public void userICXDelegationDetails() {

        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();

        initialize();
        List<Address> preps = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            preps.add(sm.createAccount().getAddress());
        }
        PrepDelegations[] delegations1 = new PrepDelegations[3];

        // delegation for user 1
        delegations1[0] = new PrepDelegations(preps.get(0), exaHelper(10));
        delegations1[1] = new PrepDelegations(preps.get(1), exaHelper(40));
        delegations1[2] = new PrepDelegations(preps.get(2), exaHelper(50));

        BigInteger workingBalance = BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18));
        doReturn(workingBalance).when(scoreSpy)
                .call(eq(BigInteger.class), eq(Contracts.BOOSTED_OMM), eq("balanceOf"), any());

        // 2 * 10 ** 18
        BigInteger twoExa = BigInteger.TWO.multiply(BigInteger.TEN.pow(18));
        doReturn(twoExa).when(scoreSpy).
                call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        doReturn(BigInteger.valueOf(1000L).multiply(BigInteger.TEN.pow(18))).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.sICX), eq("balanceOf"), any());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(user1, "updateDelegations", delegations1, user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", delegations1, user2.getAddress());

        List<PrepICXDelegations> user1IcxDelegations = (List<PrepICXDelegations>)
                delegationScore.call("getUserICXDelegation", user1.getAddress());

        /*
         * total sicx balance :> 1000
         * total icx balance :> 2000
         * user1 staked :> 100
         * total staked :> 100
         * delegations :>
         *          | 0  | 1  | 2  | 3  |
         * User 1   | 10 | 40 | 50 | -  |
         */
        for (int i = 0; i < delegations1.length; i++) {
            BigInteger voteInPer = delegations1[i]._votes_in_per;
            BigInteger voteInIcx = exaMultiply(BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(18)), voteInPer);
            assertEquals(voteInIcx, user1IcxDelegations.get(i)._votes_in_icx);
        }
    }

    @DisplayName("Test multiple user delegations details in ICX")
    @Test
    public void multipleUserICXDelegationDetails() {

        Account user1 = sm.createAccount();
        Account user2 = sm.createAccount();
        Account user3 = sm.createAccount();
        Account user4 = sm.createAccount();

        initialize();
        List<Address> preps = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            preps.add(sm.createAccount().getAddress());
        }
        PrepDelegations[] delegations1 = new PrepDelegations[3];
        PrepDelegations[] delegations2 = new PrepDelegations[2];

        // delegation for user 1
        delegations1[0] = new PrepDelegations(preps.get(0), exaHelper(10));
        delegations1[1] = new PrepDelegations(preps.get(1), exaHelper(40));
        delegations1[2] = new PrepDelegations(preps.get(2), exaHelper(50));

        // delegation for user 2
        delegations2[0] = new PrepDelegations(preps.get(0), exaHelper(60));
        delegations2[1] = new PrepDelegations(preps.get(3), exaHelper(40));

        BigInteger workingBalance = BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18));
        BigInteger workingBalanceX = BigInteger.valueOf(1700).multiply(BigInteger.TEN.pow(18));

        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user2.getAddress());
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user3.getAddress());
        doReturn(workingBalanceX).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user4.getAddress());

        // 2 * 10 ** 18
        BigInteger twoExa = BigInteger.TWO.multiply(BigInteger.TEN.pow(18));
        doReturn(twoExa).when(scoreSpy).
                call(BigInteger.class, Contracts.STAKING, "getTodayRate");

        doReturn(BigInteger.valueOf(1000L).multiply(BigInteger.TEN.pow(18))).when(scoreSpy).
                call(eq(BigInteger.class), eq(Contracts.sICX), eq("balanceOf"), any());

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);
        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(user1, "updateDelegations", delegations1, user1.getAddress());
        delegationScore.invoke(user2, "updateDelegations", delegations2, user2.getAddress());
        delegationScore.invoke(user3, "updateDelegations", new PrepDelegations[0], user3.getAddress());
        delegationScore.invoke(user4, "updateDelegations", new PrepDelegations[0], user4.getAddress());

        List<PrepICXDelegations> user1IcxDelegations = (List<PrepICXDelegations>)
                delegationScore.call("getUserICXDelegation", user1.getAddress());
        List<PrepICXDelegations> user2IcxDelegations = (List<PrepICXDelegations>)
                delegationScore.call("getUserICXDelegation", user2.getAddress());
        List<PrepICXDelegations> user3IcxDelegations = (List<PrepICXDelegations>)
                delegationScore.call("getUserICXDelegation", user3.getAddress());
        /*
         * User1 staked: 100 OMM
         * User2 staked: 100 OMM
         * User3 staked: 100 OMM
         * Total staked: 2000 OMM
         * ICX balance: 2000
         */

        for (int i = 0; i < delegations1.length; i++) {
            BigInteger voteInPer = delegations1[i]._votes_in_per;
            BigInteger voteInIcx = exaMultiply(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)), voteInPer);
            assertEquals(voteInIcx, user1IcxDelegations.get(i)._votes_in_icx);
        }

        for (int i = 0; i < delegations2.length; i++) {
            BigInteger voteInPer = delegations2[i]._votes_in_per;
            BigInteger voteInIcx = exaMultiply(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)), voteInPer);
            assertEquals(voteInIcx, user2IcxDelegations.get(i)._votes_in_icx);
        }

        // for user 3
        BigInteger expected = exaMultiply(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)), exaHelper(50));
        assertEquals(expected, user3IcxDelegations.get(0)._votes_in_icx);
        assertEquals(expected, user3IcxDelegations.get(1)._votes_in_icx);
    }

    @DisplayName("Test user delgates by bOMM and sicx at once")
    @Test
    public void delgationAtOnce(){
        initialize();
        Account user1 = sm.createAccount();
        PrepDelegations[] prepDelegations = getPrepDelegations1(10);

        Map<String, ?> prepDetails = Map.of("status", BigInteger.ZERO);

        contextMock
                .when(() -> Context.call(eq(Map.class), eq(AddressConstant.ZERO_SCORE_ADDRESS),
                        eq("getPRep"), any()))
                .thenReturn(prepDetails);

        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());
        doNothing().when(scoreSpy).call(eq(Contracts.STAKING), eq("delegateForUser"), any(),any());

        BigInteger workingBalance = BigInteger.TEN.pow(18);
        doReturn(workingBalance).when(scoreSpy)
                .call(BigInteger.class, Contracts.BOOSTED_OMM, "balanceOf", user1.getAddress());

        delegationScore.invoke(user1,"updateDelegationAtOnce", (Object) prepDelegations);

        PrepDelegations[] scoreDelegations = (PrepDelegations[]) delegationScore
                .call("getUserDelegationDetails", user1.getAddress());
        assertArrayEquals(prepDelegations, scoreDelegations);
    }

    @Test
    public void initializeVoteToContributors() {
        initialize();
        Account user = sm.createAccount();
        doNothing().when(scoreSpy).call(eq(Contracts.LENDING_POOL_CORE), eq("updatePrepDelegations"), any());

        delegationScore.invoke(owner, "initializeVoteToContributors");
        // calling this function again
        Executable call = () -> delegationScore.invoke(owner, "initializeVoteToContributors");

        expectErrorMessage(call, "Delegation : This method cannot be called again.");
    }


    public void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }
}
