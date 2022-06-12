package finance.omm.score.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import finance.omm.core.score.interfaces.DToken;
import finance.omm.core.score.interfaces.DTokenScoreClient;
import finance.omm.libs.test.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import score.Address;

public class DTokenIT implements ScoreIntegrationTest {


    Address addressProvider = Address.fromString("cxa755b2ef6eb46c1e817c636be3c21d26c81fe6cc");

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    @ScoreClient
    DToken scoreClient = new DTokenScoreClient(client);

    @Test
    void ShouldGetDecimals() {

        BigInteger decimals = scoreClient.decimals();
        assertNotNull(decimals);
        assertEquals(BigInteger.valueOf(18), decimals);

    }  

}
