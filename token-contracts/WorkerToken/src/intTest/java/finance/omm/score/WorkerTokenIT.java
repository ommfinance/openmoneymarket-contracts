package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import finance.omm.core.score.interfaces.DaoFund;
import finance.omm.core.score.interfaces.WorkerToken;
import finance.omm.core.score.interfaces.WorkerTokenScoreClient;
import finance.omm.libs.test.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import score.Address;

public class WorkerTokenIT implements ScoreIntegrationTest {

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    @ScoreClient
    WorkerToken scoreClient = new WorkerTokenScoreClient(client);

    @Test
    void testTotalSupply() {
        BigInteger totalSupply = scoreClient.totalSupply();
        assertNotNull(totalSupply);
        assertEquals(new BigInteger("50000000000000"), totalSupply);
    }
}
