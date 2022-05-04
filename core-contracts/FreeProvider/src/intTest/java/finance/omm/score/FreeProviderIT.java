package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import finance.omm.core.score.interfaces.FreeProvider;
import finance.omm.core.score.interfaces.FreeProviderScoreClient;
import finance.omm.libs.test.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import score.Address;

public class FreeProviderIT implements ScoreIntegrationTest {

    Address addressProvider = Address.fromString("cxa755b2ef6eb46c1e817c636be3c21d26c81fe6cc");

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    @ScoreClient
    FreeProvider scoreClient = new FreeProviderScoreClient(client);

    @Test
    void test() {
        BigInteger result = scoreClient.getLoanOriginationFeePercentage();
        assertNotNull(result);
        assertEquals(BigInteger.ZERO, result);
    }
    
}
