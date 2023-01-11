package finance.omm.score.test.unit.LPStaker;

import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class LPStakerTest extends AbstractLPStakerTest {

    private final BigInteger id = BigInteger.valueOf(6);

    @Test
    void name() {
        assertEquals("Omm LP Staker", score.call("name"));
    }

    @Test
    void stakeLPToken() {

        Address stakedLp = MOCK_CONTRACT_ADDRESS.get(Contracts.STAKED_LP).getAddress();
        byte[] data = "{\"method\":\"stake\"}".getBytes();

        doNothing().when(scoreSpy).call(Contracts.DEX, "transfer", stakedLp, TEN, id, data);

        score.invoke(score.getAccount(), "stakeLP", id, TEN);

        verify(scoreSpy).call(Contracts.DEX, "transfer", stakedLp, TEN, id, data);
    }

    @Test
    void unstakeLPToken() {

        doNothing().when(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), TEN);
        score.invoke(score.getAccount(), "unstakeLP", id, TEN);

        verify(scoreSpy).call(Contracts.STAKED_LP, "unstake", id.intValue(), TEN);
    }

    @Test
    void transferLpToken() {

        doNothing().when(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(),
                TWO, id);
        score.invoke(score.getAccount(), "transferLp", alice.getAddress(), TWO, id);

        verify(scoreSpy).call(Contracts.DEX, "transfer", alice.getAddress(), TWO, id);
    }

    @Test
    void checkBalanceOfLpToken() {

        contextMock.when(mockScoreAddress()).thenReturn(score.getAddress());

        doReturn(TEN).when(scoreSpy).call(BigInteger.class, Contracts.DEX, "balanceOf",
                score.getAddress(), id);

        assertEquals(TEN, score.call("balanceOfLp", id));


    }

    @Test
    void transferFundsFromContract() {

        contextMock.when(mockCaller()).thenReturn(GOVERNANCE_SCORE.getAddress());

        Address ommAddress = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();
        doNothing().when(scoreSpy).call(ommAddress, "transfer", alice.getAddress(), BigInteger.TWO);

        score.invoke(GOVERNANCE_SCORE, "transferFunds", alice.getAddress(), BigInteger.TWO);

        verify(scoreSpy).call(ommAddress, "transfer", alice.getAddress(), BigInteger.TWO);

    }

    @Test
    void transferFundsException() {
        contextMock.when(mockCaller()).thenReturn(owner.getAddress());

        Executable callerNotGovernance = () ->
                score.invoke(owner, "transferFunds", alice.getAddress(), BigInteger.TWO);
        expectErrorMessage(callerNotGovernance,
                "LP Staker | SenderNotGovernanceError: sender is not equals to governance");

    }


    @Test
    void claimRewards() {
        contextMock.when(mockCaller()).thenReturn(owner.getAddress());
        doNothing().when(scoreSpy).call(Contracts.LENDING_POOL, "claimRewards", owner.getAddress());

        score.call("claimRewards");

        verify(scoreSpy).call(Contracts.LENDING_POOL, "claimRewards", owner.getAddress());
    }

    @Test
    void contractReceivesLPToken() {

        contextMock.when(mockCaller()).thenReturn(owner.getAddress());

        stakeLPToken();

        score.invoke(owner, "onIRC31Received", owner.getAddress(), owner.getAddress(), id, TEN, "lp token received".getBytes());
        verify(scoreSpy).LPTokenReceived(id, TEN, owner.getAddress());
    }

    @Test
    void contractReceivesReward() {
        Address ommToken = MOCK_CONTRACT_ADDRESS.get(Contracts.OMM_TOKEN).getAddress();
        score.invoke(owner, "tokenFallback", ommToken, TEN.multiply(ICX), "ommToken received".getBytes());
        verify(scoreSpy).FundReceived(ommToken, TEN.multiply(ICX));
    }

}
