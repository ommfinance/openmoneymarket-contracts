package finance.omm.score.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import finance.omm.core.score.interfaces.OToken;
import finance.omm.core.score.interfaces.OTokenScoreClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import score.Address;

public class OTokenIT implements ScoreIntegrationTest {

    Address addressProvider = Address.fromString("cxa755b2ef6eb46c1e817c636be3c21d26c81fe6cc");

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    @ScoreClient
    OToken scoreClient = new OTokenScoreClient(client);

    @Test
    void ShouldGetDecimals() {

        BigInteger decimals = scoreClient.decimals();
        assertNotNull(decimals);
        assertEquals(BigInteger.valueOf(6), decimals);

    }   
}
