//package finance.omm.gradle.plugin;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import finance.omm.gradle.plugin.utils.AddressUtils;
//import finance.omm.gradle.plugin.utils.Network;
//import finance.omm.gradle.plugin.utils.Score;
//import foundation.icon.icx.Wallet;
//import foundation.icon.jsonrpc.Address;
//import foundation.icon.jsonrpc.IconJsonModule;
//import foundation.icon.jsonrpc.JsonrpcClient;
//import foundation.icon.score.client.DefaultScoreClient;
//import java.io.IOException;
//import java.io.InputStream;
//import java.math.BigInteger;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import org.gradle.api.DefaultTask;
//import org.gradle.api.model.ObjectFactory;
//import org.gradle.api.provider.Property;
//import org.gradle.api.tasks.Input;
//import org.gradle.api.tasks.TaskAction;
//
//public class ConfigureOMMEnv extends DefaultTask {
//
//    private Properties properties = new Properties();
//    private final Property<String> configurationFile;
//    private final Property<String> keystore;
//    private final Property<String> password;
//
//    private JsonrpcClient client;
//    private Wallet wallet;
//    private Network network;
//    private Address addressProvider;
//
//    private static final List<String> ADDRESS_PROVIDER_PARAMS_NOT_REQUIRED = List.of(
//            "addressProvider", "workerToken"
//    );
//
//
//    public ConfigureOMMEnv() {
//        super();
//        ObjectFactory objectFactory = getProject().getObjects();
//        this.configurationFile = objectFactory.property(String.class);
//        this.keystore = objectFactory.property(String.class);
//        this.password = objectFactory.property(String.class);
//    }
//
//    @Input
//    public Property<String> getConfigurationFile() {
//        return configurationFile;
//    }
//
//    @Input
//    public Property<String> getKeystore() {
//        return keystore;
//    }
//
//    @Input
//    public Property<String> getPassword() {
//        return password;
//    }
//
//    @TaskAction
//    public void deploy() throws Exception {
//        List<Score> scores = readSCOREs();
//        loadConfiguration();
//
//        this.network = Network.getNetwork(properties.getProperty("NETWORK"));
//        client = new JsonrpcClient(network.getUrl());
//        client.mapper().registerModule(new IconJsonModule());
//        client.mapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        this.wallet = DefaultScoreClient.wallet(this.keystore.get(), this.password.get());
//        Map<String, Address> addresses = new HashMap<>();
//        for (Score score : scores) {
//            Address address = deploy(score);
//            if (score.getName().equals("addressProvider")) {
//                addressProvider = address;
//            } else {
//                addresses.put(score.getName(), address);
//            }
//        }
//
//        writeFile("addresses-" + network.name() + "-" + System.nanoTime() + ".json", addresses);
//
//    }
//
//    private void setAddresses(Map<String, Address> addresses) {
//        Map<String, Object> params = AddressUtils.getParamsForSetAddresses(addresses);
//
//        send(addressProvider, "setAddresses", params);
//    }
//
//
//    private void send(Address address, String method, Map<String, Object> params) {
//        DefaultScoreClient.send(client, network.getNid(), wallet,
//                DefaultScoreClient.DEFAULT_STEP_LIMIT,
//                address, BigInteger.ZERO, method, params,
//                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
//    }
//
//    private Address deploy(Score score) {
//        Map<String, Object> params = score.getParams();
//        if (!ADDRESS_PROVIDER_PARAMS_NOT_REQUIRED.contains(score.getName())) {
//            params.put("_addressProvider", addressProvider);
//        }
//        return DefaultScoreClient.deploy(client, network.getNid(), wallet,
//                DefaultScoreClient.DEFAULT_STEP_LIMIT,
//                DefaultScoreClient.ZERO_ADDRESS, score.getPath(), params,
//                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
//    }
//
//
//    private Address update(Score score) {
//        Map<String, Object> params = score.getParams();
//        if (!ADDRESS_PROVIDER_PARAMS_NOT_REQUIRED.contains(score.getName())) {
//            params.put("_addressProvider", addressProvider);
//        }
//        return DefaultScoreClient.deploy(client, network.getNid(), wallet,
//                DefaultScoreClient.DEFAULT_STEP_LIMIT,
//                score.getAddress(), score.getPath(), params,
//                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
//    }
//
//    private List<Score> readSCOREs() throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        InputStream is = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("contracts-sample.json");
//
//        List<Score> list = objectMapper.readValue(is, new TypeReference<>() {
//        });
//
//        list.sort((s1, s2) -> Float.compare(s1.getOrder(), s2.getOrder()));
//        return list;
//    }
//
//    private void loadConfiguration() throws IOException {
//        try (InputStream stream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream(configurationFile.get())) {
//            properties.load(stream);
//        }
//    }
//
//    private static void writeFile(String filePath, Map<String, Address> data) {
//        Path outFile = Paths.get(filePath);
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            String json = mapper.writeValueAsString(data);
//            Files.write(outFile, json.getBytes());
//        } catch (IOException e) {
//            throw new RuntimeException(e.getMessage());
//        }
//    }
//
//}
