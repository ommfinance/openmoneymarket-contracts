package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import finance.omm.libs.test.ScoreIntegrationTest;
import finance.omm.libs.test.ScoreIntegrationTest.Faker;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
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