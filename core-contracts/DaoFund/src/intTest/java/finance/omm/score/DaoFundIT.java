package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import finance.omm.libs.test.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import score.Address;
import score.annotation.External;

public class DaoFundIT implements ScoreIntegrationTest {

    Address addressProvider = Address.fromString("cxa755b2ef6eb46c1e817c636be3c21d26c81fe6cc");

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    @ScoreClient
    DaoFund scoreClient = new DaoFundScoreClient(client);

    @Test
    void testGetAddressProvider() {

        Address address = scoreClient.getAddressProvider();
        assertNotNull(address);
        assertEquals(addressProvider, address);

    }
}

interface DaoFund {
    @External(readonly = true)
    Address getAddressProvider();
}
