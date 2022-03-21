package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import finance.omm.core.score.interfaces.DaoFund;
import finance.omm.core.score.interfaces.DaoFundScoreClient;
import org.junit.jupiter.api.Test;

import finance.omm.libs.test.ScoreIntegrationTest;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import score.Address;

public class DaoFundIT implements ScoreIntegrationTest {

    Address addressProvider = Faker.address(foundation.icon.jsonrpc.Address.Type.CONTRACT);

    DefaultScoreClient client = DefaultScoreClient.of(System.getProperties());

    DefaultScoreClient clientWithTester = new DefaultScoreClient(client.endpoint(), client._nid(), tester,
            client._address());

    @ScoreClient
    DaoFund scoreClient = new DaoFundScoreClient(client);

    DaoFund scoreClientWithTester = new DaoFundScoreClient(clientWithTester);

    @Test
    void testGetAddressProvider(){

        Address address = scoreClient.getAddressProvider();
        assertNotNull(address);
        assertEquals(addressProvider, address);
        

    }
}