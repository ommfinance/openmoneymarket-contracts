package finance.omm.libs.test.integration;

import static finance.omm.libs.test.integration.Environment.SYSTEM_INTERFACE;
import static finance.omm.libs.test.integration.Environment.chain;
import static finance.omm.libs.test.integration.Environment.godClient;
import static finance.omm.libs.test.integration.Environment.preps;
import static finance.omm.libs.test.integration.ScoreIntegrationTest.createWalletWithBalance;
import static finance.omm.libs.test.integration.ScoreIntegrationTest.transfer;

import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.model.Score;
import finance.omm.libs.test.integration.scores.SystemInterface;
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

        setStakeOfPreps();
        setDelegationOfPreps();
        setBonderList();
        setBondsofPreps();
        setGodStake();

        this.addresses = new ScoreDeployer(this, contracts).deployContracts();

        ownerClient = new OMMClient(this, owner);
        testClient = new OMMClient(this, createWalletWithBalance(BigInteger.TEN.pow(24)));
    }

    public void setStakeOfPreps(){
        int count = 0;
        for (Entry<Address, String> prep : preps.entrySet()) {
            if (prep.getKey().equals(Address.fromString("hxb6b5791be0b5ef67063b3c10b840fb81514db2fd"))){
                continue;
            }
            if (count < 7) {
                KeyWallet wallet = KeyWallet.load(new Bytes(prep.getValue()));
                var client = new DefaultScoreClient(
                        chain.getEndpointURL(),
                        chain.networkId,
                        wallet,
                        DefaultScoreClient.ZERO_ADDRESS
                );
                SYSTEM_INTERFACE = new SystemInterfaceScoreClient(client);
                SYSTEM_INTERFACE.setStake(BigInteger.valueOf(100_000).multiply(BigInteger.TEN.pow(18)));
                count++;
            }
            else {
                break;
            }
        }
        System.out.println("stake of preps done ");
    }

    public void setDelegationOfPreps(){
        int count = 0;
        for (Entry<Address, String> prep : preps.entrySet()) {
            if (prep.getKey().equals(Address.fromString("hxb6b5791be0b5ef67063b3c10b840fb81514db2fd"))){
                continue;
            }
            if (count < 7) {
                KeyWallet wallet = KeyWallet.load(new Bytes(prep.getValue()));
                SystemInterface.Delegation[] delegations = new SystemInterface.Delegation[1];
                delegations[0] = new SystemInterface.Delegation();
                delegations[0].address = score.Address.fromString(prep.getKey().toString());
                delegations[0].value = BigInteger.valueOf(80_000).multiply(BigInteger.TEN.pow(18));
                var client = new DefaultScoreClient(
                        chain.getEndpointURL(),
                        chain.networkId,
                        wallet,
                        DefaultScoreClient.ZERO_ADDRESS
                );
                SYSTEM_INTERFACE = new SystemInterfaceScoreClient(client);
                SYSTEM_INTERFACE.setDelegation(delegations);
                count++;
            }
            else {
                break;
            }
        }

        System.out.println("delegation of preps done ");
    }

    public void setBonderList(){
        int count = 0;
        for (Entry<Address, String> prep : preps.entrySet()) {
            if (prep.getKey().equals(Address.fromString("hxb6b5791be0b5ef67063b3c10b840fb81514db2fd"))){
                continue;
            }
            if (count < 7) {
                KeyWallet wallet = KeyWallet.load(new Bytes(prep.getValue()));
                score.Address[] bonderList = new score.Address[1];
                bonderList[0] = score.Address.fromString(prep.getKey().toString());
                var client = new DefaultScoreClient(
                        chain.getEndpointURL(),
                        chain.networkId,
                        wallet,
                        DefaultScoreClient.ZERO_ADDRESS
                );
                SYSTEM_INTERFACE = new SystemInterfaceScoreClient(client);
                SYSTEM_INTERFACE.setBonderList(bonderList);
                count++;
            }
            else {
                break;
            }
        }

        System.out.println("bonder list done");
    }

    public void setBondsofPreps(){
        int count = 0;
        for (Entry<Address, String> prep : preps.entrySet()) {
            if (prep.getKey().equals(Address.fromString("hxb6b5791be0b5ef67063b3c10b840fb81514db2fd"))){
                continue;
            }
            if (count<7){
                KeyWallet wallet = KeyWallet.load(new Bytes(prep.getValue()));
                SystemInterface.Bond[] bond = new SystemInterface.Bond[1];
                bond[0] = new SystemInterface.Bond();
                bond[0].address = score.Address.fromString(prep.getKey().toString());
                bond[0].value = BigInteger.valueOf(10_000).multiply(BigInteger.TEN.pow(18));
                var client = new DefaultScoreClient(
                        chain.getEndpointURL(),
                        chain.networkId,
                        wallet,
                        DefaultScoreClient.ZERO_ADDRESS
                );
                SYSTEM_INTERFACE = new SystemInterfaceScoreClient(client);
                SYSTEM_INTERFACE.setBond(bond);
                count++;
        }
            else {
                break;
            }
        }

        System.out.println("bond of preps done ");
    }

    public void registerGodClient(){
        SYSTEM_INTERFACE = new SystemInterfaceScoreClient(godClient);
        ((SystemInterfaceScoreClient) SYSTEM_INTERFACE).registerPRep(
                BigInteger.valueOf(2000).multiply(BigInteger.TEN.pow(18)), "test",
                "kokoa@example.com",
                "USA",
                "New York", "https://icon.kokoa.com", "https://icon.kokoa.com/json/details.json",
                "localhost:9082");
        System.out.println("god client prep done");
    }




    public void setGodStake(){
        SYSTEM_INTERFACE = new SystemInterfaceScoreClient(godClient);
        SYSTEM_INTERFACE.setStake(BigInteger.valueOf(9_000_000).multiply(BigInteger.TEN.pow(18)));
        SystemInterface.Delegation[] delegation = new SystemInterface.Delegation[1];
        delegation[0] = new SystemInterface.Delegation();
        delegation[0].address = score.Address.fromString(chain.godWallet.getAddress().toString());
        delegation[0].value = BigInteger.valueOf(8000000).multiply(BigInteger.TEN.pow(18));
        SYSTEM_INTERFACE.setDelegation(delegation);

        score.Address[] addresses = new score.Address[1];
        addresses[0] = score.Address.fromString(chain.godWallet.getAddress().toString());
        SYSTEM_INTERFACE.setBonderList(addresses);

        SystemInterface.Bond[] bonds = new SystemInterface.Bond[1];
        bonds[0] = new SystemInterface.Bond();
        bonds[0].address = score.Address.fromString(chain.godWallet.getAddress().toString());
        bonds[0].value = BigInteger.valueOf(1000000).multiply(BigInteger.TEN.pow(18));
        SYSTEM_INTERFACE.setBond(bonds);
        System.out.println("god wallet setup done");
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
            Map<String, Object> result = SYSTEM_INTERFACE.getPReps(BigInteger.ONE, BigInteger.valueOf(100));
            List<Object> registeredPReps = (List<Object>) result.get("preps");
            if (registeredPReps.size() >= 100) {
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

    public OMMClient customClient(String privateKey){
        OMMClient client = new OMMClient(this,KeyWallet.load(new Bytes(privateKey)));
        ommClients.put(client.getAddress(),client);
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
