package finance.omm.score.test.unit.LPInventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import finance.omm.libs.address.Contracts;
import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class LPInventoryTest extends AbstractLPInventoryTest {

    private final BigInteger id = BigInteger.valueOf(6);

    @Test
    void name() {
        assertEquals("Omm LP Inventory", score.call("name"));
    }

    @Test
    void changeAdmin() {
        assertEquals(owner.getAddress(), score.call("getAdmin"));

        // set admin condidate
        score.invoke(owner, "setAdmin", alice.getAddress());
        assertEquals(alice.getAddress(), score.call("getCandidate"));
        assertEquals(owner.getAddress(), score.call("getAdmin"));

        // claim admin
        score.invoke(alice, "claimAdminStatus");
        assertEquals(alice.getAddress(), score.call("getAdmin"));

        verify(scoreSpy).AdminCandidatePushed(alice.getAddress());
        verify(scoreSpy).AdminRoleClaimed(owner.getAddress(), alice.getAddress());
    }

    @Test
    void changeAdmin_candidate_not_set() {
        Executable adminStatus = () -> score.invoke(owner, "claimAdminStatus");
        expectErrorMessage(adminStatus, "LP Inventory | Candidate address is null");

        verify(scoreSpy, never()).AdminRoleClaimed(any(), any());
    }

    @Test
    void changeAdmin_not_claim_by_candidate() {

        score.invoke(owner, "setAdmin", alice.getAddress());
        assertEquals(alice.getAddress(), score.call("getCandidate"));
        assertEquals(owner.getAddress(), score.call("getAdmin"));

        Executable adminCalim = () -> score.invoke(owner, "claimAdminStatus");
        expectErrorMessage(adminCalim, "LP Inventory | The candidate's address and the caller do not match.");

        verify(scoreSpy).AdminCandidatePushed(alice.getAddress());
        verify(scoreSpy, never()).AdminRoleClaimed(any(), any());

    }

    @Test
    void changeAdmin_not_by_owner() {

        Executable changeAdmin = () -> score.invoke(alice, "setAdmin", alice.getAddress());
        expectErrorMessage(changeAdmin, "Only owner can set new admin");

        verify(scoreSpy, never()).AdminCandidatePushed(any());
    }

    @Test
    void stakeLPToken_not_by_owner() {
        Executable stake = () -> score.invoke(score.getAccount(), "stake", id, TEN);
        expectErrorMessage(stake, "Only owner can stake LP tokens");

    }

    @Test
    void stakeLPToken() {

        Address stakedLp = MOCK_CONTRACT_ADDRESS.get(Contracts.STAKED_LP).getAddress();
        byte[] data = "{\"method\":\"stake\"}".getBytes();

        doNothing().when(scoreSpy).call(Contracts.DEX, "transfer", stakedLp, TEN, id, data);

        score.invoke(owner, "stake", id, TEN);

        verify(scoreSpy).call(Contracts.DEX, "transfer", stakedLp, TEN, id, data);
    }

    @Test
    void unstakeLPToken() {

        doNothing().when(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), TEN);
        score.invoke(owner, "unstake", id, TEN);

        verify(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), TEN);
    }

    @Test
    void unstakeLPToken_not_by_owner() {

        doNothing().when(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), TEN);
        Executable unstakeLp = () -> score.invoke(alice, "unstake", id, TEN);
        expectErrorMessage(unstakeLp, "Only owner can unstake LP token");

    }

    @Test
    void transferLpToken() {
        byte[] data = "transfer LP".getBytes();
        BigInteger amount = TWO;

        contextMock.when(mockScoreAddress()).thenReturn(score.getAddress());
        doReturn(amount).when(scoreSpy)
                .call(BigInteger.class, Contracts.DEX, "balanceOf", score.getAddress(), id);

        doNothing().when(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(),
                amount, id, data);
        score.invoke(GOVERNANCE_SCORE, "transfer", alice.getAddress(), amount, id, data);

        verify(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(), amount, id, data);
        verify(scoreSpy, never()).call(eq(Contracts.STAKED_LP), eq("unstake"), any(), any());
    }

    @Test
    void transferLpToken_less_than_available_balance() {
        byte[] data = "transfer LP with unstake".getBytes();
        BigInteger amount = BigInteger.valueOf(10);

        contextMock.when(mockScoreAddress()).thenReturn(score.getAddress());

        // available balance -> 2
        doReturn(TWO).when(scoreSpy)
                .call(BigInteger.class, Contracts.DEX, "balanceOf", score.getAddress(), id);
        // transfer amount -> 10
        doNothing().when(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(),
                amount, id, data);
        // required amount -> 8
        doNothing().when(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), BigInteger.valueOf(8));
        score.invoke(GOVERNANCE_SCORE, "transfer", alice.getAddress(), amount, id, data);

        verify(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(), amount, id, data);
        verify(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), BigInteger.valueOf(8));
    }

    @Test
    void transferLpToken_not_by_governance() {
        byte[] data = "transfer LP".getBytes();

        Executable transferLp = () -> score.invoke(owner, "transfer", alice.getAddress(), TWO, id, data);
        expectErrorMessage(transferLp, "LP Inventory | SenderNotGovernanceError: sender is not equals to governance");

    }

    @Test
    void checkBalanceOfLpToken() {

        doReturn(Map.of(
                "poolID", id,
                "userTotalBalance", TEN,
                "userAvailableBalance", BigInteger.TWO,
                "userStakedBalance", BigInteger.valueOf(8),
                "totalStakedBalance", BigInteger.valueOf(100))
        ).when(scoreSpy).call(Map.class, Contracts.STAKED_LP, "balanceOf", owner.getAddress(), id);
        System.out.println(score.call("balanceOfLp", owner.getAddress(), id));

        verify(scoreSpy).call(Map.class, Contracts.STAKED_LP, "balanceOf", owner.getAddress(), id);

    }

    @Test
    void contractReceivesLPToken() {

        contextMock.when(mockCaller()).thenReturn(owner.getAddress());

        score.invoke(owner, "onIRC31Received", owner.getAddress(), owner.getAddress(), id, TEN,
                "lp token received".getBytes());
        verify(scoreSpy).LPTokenReceived(owner.getAddress(), id, TEN);
    }

    @Test
    void contractReceivesToken() {
        Address ommToken = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();
        score.invoke(owner, "tokenFallback", ommToken, TEN.multiply(ICX), "ommToken received".getBytes());
        verify(scoreSpy).TokenReceived(ommToken, TEN.multiply(ICX));
    }

}
