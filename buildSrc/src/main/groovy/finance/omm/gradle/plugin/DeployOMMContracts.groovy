package finance.omm.gradle.plugin

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import finance.omm.gradle.plugin.utils.AddressUtils
import finance.omm.gradle.plugin.utils.NameMapping
import finance.omm.gradle.plugin.utils.Network
import finance.omm.gradle.plugin.utils.Score
import foundation.icon.icx.Wallet
import foundation.icon.jsonrpc.Address
import foundation.icon.jsonrpc.IconJsonModule
import foundation.icon.jsonrpc.JsonrpcClient
import foundation.icon.score.client.DefaultScoreClient
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DeployOMMContracts extends DefaultTask {

    private final Property<String> env;
    private final Property<String> keystore;
    private final Property<String> password;
    private final Property<String> configFile;

    private JsonrpcClient client;
    private Wallet wallet;
    private Network network;


    DeployOMMContracts() {
        super();
        ObjectFactory objectFactory = getProject().getObjects();
        this.env = objectFactory.property(String.class);
        this.keystore = objectFactory.property(String.class);
        this.password = objectFactory.property(String.class);
        this.configFile = objectFactory.property(String.class);
    }

    @Input
    Property<String> getKeystore() {
        return keystore;
    }

    @Input
    Property<String> getPassword() {
        return password;
    }

    @Input
    Property<String> getEnv() {
        return env;
    }


    @Input
    Property<String> getConfigFile() {
        return configFile;
    }

    @TaskAction
    void deployContracts() throws Exception {
        List<Score> scores = readSCOREs();

        this.network = Network.getNetwork(this.env.get());
        client = new JsonrpcClient(network.getUrl());
        client.mapper().registerModule(new IconJsonModule());
        client.mapper().setSerializationInclusion(Include.NON_NULL);
        this.wallet = DefaultScoreClient.wallet(this.keystore.get(), this.password.get());
        Map<String, Address> addresses = ["owner": new Address(wallet.getAddress().toString())];
        logger.lifecycle('deploying contracts...')

        for (Score score : scores) {
            if (score.getPath() == null) {
                String module = NameMapping.valueOf(score.getName());
                score.path = project.tasks.getByPath(":$module:optimizedJar").outputJarName;
            }
            logger.lifecycle("deploying contract $score.name :: $score.path")
            Map<String, String> addressParams = score.getAddressParams();

            for (Map.Entry<String, String> entry : addressParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                score.addParams(key, addresses.get(value))
            }
            Address address = deploy(score);
            addresses.put(score.getName(), address);
        }
        setAddresses(addresses)
        writeFile(".deployment/addresses-" + network.name() + "-" + System.currentTimeMillis() + ".json", addresses);
    }


    private void setAddresses(Map<String, Address> addresses) {

        Map<String, Object> params = AddressUtils.getParamsForSetAddresses(addresses);

        send(addresses.get("addressProvider"), "setAddresses", params);
    }

    private void send(Address address, String method, Map<String, Object> params) {
        DefaultScoreClient.send(client, network.getNid(), wallet,
                DefaultScoreClient.DEFAULT_STEP_LIMIT,
                address, BigInteger.ZERO, method, params,
                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
    }


    private Address deploy(Score score) throws URISyntaxException {
        return _deploy(score, DefaultScoreClient.ZERO_ADDRESS);
    }

    private Address _deploy(Score score, Address zeroAddress) {
        Map<String, Object> params = score.getParams();
        return DefaultScoreClient.deploy(client, network.getNid(), wallet,
                DefaultScoreClient.DEFAULT_STEP_LIMIT,
                zeroAddress, score.getPath(), params,
                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
    }


    private Address update(Score score) throws URISyntaxException {
        return _deploy(score, score.getAddress());
    }

    private List<Score> readSCOREs() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream(configFile.get());

        List<Score> list = objectMapper.readValue(is, new TypeReference<List<Score>>() {
        });

        list.sort((s1, s2) -> Float.compare(s1.getOrder(), s2.getOrder()));
        return list;
    }


    private static void writeFile(String filePath, Map<String, Address> data) {
        Path outFile = Paths.get(filePath);
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(data);
            Files.write(outFile, json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
