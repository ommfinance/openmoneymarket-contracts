package finance.omm.libs.test.integration;

import static finance.omm.libs.test.integration.Environment.SYSTEM_INTERFACE;
import static finance.omm.libs.test.integration.Environment.chain;
import static finance.omm.libs.test.integration.Environment.godClient;
import static finance.omm.libs.test.integration.Environment.preps;
import static finance.omm.libs.test.integration.ScoreIntegrationTest.createWalletWithBalance;
import static finance.omm.libs.test.integration.ScoreIntegrationTest.transfer;

import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.model.Score;
import finance.omm.libs.test.integration.scores.SystemInterfaceScoreClient;
import finance.omm.libs.test.integration.utils.DefaultICONClient;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Bytes;
import foundation.icon.score.client.DefaultScoreClient;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import score.Address;

public class OMM {

    public KeyWallet owner;
    public OMMClient ownerClient;
    public OMMClient testClient;
    public DefaultScoreClient addressManager;

    public HashMap<Address, OMMClient> ommClients;

    public DefaultICONClient iconClient;

    private String contracts;
    private Map<String, foundation.icon.jsonrpc.Address> addresses;

    public OMM(String contracts) throws Exception {
        this.contracts = contracts;
        ommClients = new HashMap<>();
        owner = createWalletWithBalance(BigInteger.TEN.pow(24));
        iconClient = new DefaultICONClient(chain);

    }

    public void setupOMM() throws Exception {
        deployPrep();
        this.addresses = new ScoreDeployer(this, contracts).deployContracts();

        ownerClient = new OMMClient(this, owner);
        testClient = new OMMClient(this, createWalletWithBalance(BigInteger.TEN.pow(24)));
    }

    public void runConfig(Config config) {
        config.call(ownerClient);
    }


    public OMMClient defaultClient() {
        return ownerClient;
    }

    public OMMClient testClient() {
        return testClient;
    }


    public void send(foundation.icon.jsonrpc.Address address, String method, Map<String, Object> params) {
        iconClient.send(owner, address, BigInteger.ZERO, method, params, DefaultICONClient.DEFAULT_RESULT_TIMEOUT);
    }

    public foundation.icon.jsonrpc.Address deployAddressManager() {
        return iconClient.deploy(owner, DefaultICONClient.ZERO_ADDRESS, getScorePath("AddressManager"),
                new HashMap<>());
    }

    public String getScorePath(String key) {
        String path = System.getProperty(key);
        if (path == null) {
            throw new IllegalArgumentException("No such property: " + key);
        }
        return path;
    }

    public Callable<foundation.icon.jsonrpc.Address> deploy(Score score) {
        return () -> _deploy(score);
    }

    public foundation.icon.jsonrpc.Address _deploy(Score score) {
        return iconClient.deploy(owner, DefaultICONClient.ZERO_ADDRESS, score.getPath(), score.getParams());
    }

    public boolean isPRepRegistered() {
        try {
            SYSTEM_INTERFACE = new SystemInterfaceScoreClient(godClient);
            Map<String, Object> result = SYSTEM_INTERFACE.getPReps(BigInteger.ONE, BigInteger.TWO);
            List<Object> registeredPReps = (List<Object>) result.get("preps");
            if (registeredPReps.size() > 0) {
                return true;
            }
        } catch (Exception e) {

        }
        return false;
    }

    public void deployPrep() {
        if (isPRepRegistered()) {
            return;
        }
        try {

            for (Entry<Address, String> prep : preps.entrySet()) {
                KeyWallet wallet = KeyWallet.load(new Bytes(prep.getValue()));
                transfer(foundation.icon.jsonrpc.Address.of(wallet), BigInteger.TEN.pow(24));
                var client = new DefaultScoreClient(
                        chain.getEndpointURL(),
                        chain.networkId,
                        wallet,
                        DefaultScoreClient.ZERO_ADDRESS
                );
                SYSTEM_INTERFACE = new SystemInterfaceScoreClient(client);
                ((SystemInterfaceScoreClient) SYSTEM_INTERFACE).registerPRep(
                        BigInteger.valueOf(2000).multiply(BigInteger.TEN.pow(18)), prep.getKey().toString(),
                        "kokoa@example.com",
                        "USA",
                        "New York", "https://icon.kokoa.com", "https://icon.kokoa.com/json/details.json",
                        "localhost:9082");
            }
        } catch (Exception e) {

        }
    }

    public OMMClient newClient(BigInteger balance) throws Exception {
        OMMClient client = new OMMClient(this, createWalletWithBalance(balance));
        ommClients.put(client.getAddress(), client);
        return client;
    }

    public OMMClient newClient() throws Exception {
        return newClient(BigInteger.TEN.pow(24));
    }

    public OMMClient getClient(Address address) {
        return ommClients.get(address);
    }


    public Map<String, foundation.icon.jsonrpc.Address> getAddresses() {
        return this.addresses;
    }

    public foundation.icon.jsonrpc.Address getAddress(String key) {

        return this.addresses.get(key);
    }
}
