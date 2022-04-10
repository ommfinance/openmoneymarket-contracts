package finance.omm.gradle.plugin


import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import finance.omm.gradle.plugin.utils.Action
import finance.omm.gradle.plugin.utils.Network
import foundation.icon.icx.Wallet
import foundation.icon.jsonrpc.Address
import foundation.icon.score.client.DefaultScoreClient
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class ConfigureOMMEnv extends DefaultTask {


    private static final BigInteger ICX = BigInteger.TEN.pow(18);

    private Properties _properties = new Properties();

    private Map<String, Object> properties = new HashMap<>();

    private final Property<String> keystore;
    private final Property<String> password;
    private final Property<String> actionsFilePath;
    private final Property<String> addressesFilePath;
    private final Property<String> propertiesFile;

    private DefaultICONClient client;
    private Wallet wallet;
    private Network network;

    private Map<String, Address> addresses;

    public static String getTaskName() {
        return "executeOMMActions";
    }

    ConfigureOMMEnv() {
        super();
        ObjectFactory objectFactory = getProject().getObjects();
        this.keystore = objectFactory.property(String.class);
        this.password = objectFactory.property(String.class);
        this.actionsFilePath = objectFactory.property(String.class);
        this.addressesFilePath = objectFactory.property(String.class);
        this.propertiesFile = objectFactory.property(String.class);
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
    Property<String> getActionsFile() {
        return actionsFilePath;
    }

    void setKeystore(String keystore) {
        this.keystore.set(keystore);
    }

    void setPassword(String password) {
        this.password.set(password)
    }

    void setActionsFile(String actionFile) {
        this.actionsFilePath.set(actionFile)
    }

    void setContractAddressFile(String contractAddress) {
        this.addressesFilePath.set(contractAddress)
    }

    void setPropertiesFile(String configFile) {
        this.propertiesFile.set(configFile)
    }


    @TaskAction
    void configure() throws Exception {

        loadAddresses();

        List<Action> actions = loadActions();

        init()
        client = new DefaultICONClient(this.network)

        this.wallet = DefaultScoreClient.wallet(this.keystore.get(), this.password.get());


        logger.lifecycle('executing contract configurations...')

        for (Action action : actions) {
            logger.lifecycle("executing action $action.contract :: $action.method")

            JsonNode node = action.getArgs();
            Map<String, Object> params = new HashMap<>();
            buildParams(node, params);
            action.setParams(params);

            execute(action);
        }


    }

    private void buildParams(JsonNode node, Map<String, Object> params) {
        Iterator<Map.Entry<String, JsonNode>> iter = node.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> entry = iter.next();
            String key = entry.getKey();
            switch (key) {
                case "__address_args__":
                    JsonNode addressNodes = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> addressIter = addressNodes.fields();
                    while (addressIter.hasNext()) {
                        Map.Entry<String, JsonNode> nodeEntry = iter.next();
                        params.put(nodeEntry.getKey(), getAddress(nodeEntry.getValue().textValue()));
                    }
                    break;
                case "__property_args__":
                    JsonNode propertyNode = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> propertyIter = propertyNode.fields();
                    while (propertyIter.hasNext()) {
                        Map.Entry<String, JsonNode> nodeEntry = iter.next();
                        params.put(nodeEntry.getKey(), getMapProperty(nodeEntry.getValue().textValue()));
                    }
                    break;
                case "__args__":
                    JsonNode argsNode = entry.getValue();
                    Map<String, Object> _params = new HashMap<>();
                    buildParams(argsNode, _params);
                    break;
                default:
                    params.put(entry.getKey(), parse(entry.getValue()));
            }

        }
    }


    private Object parse(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iter = jsonNode.fields();
            Map<String, Object> params = new HashMap<>();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                JsonNode value = entry.getValue();
                if (value.isValueNode()) {
                    params.put(entry.getKey(), value.textValue());
                } else {
                    buildParams(entry.getValue(), params);
                }
            }
            return params;
        } else if (jsonNode.isArray()) {
            Iterator<JsonNode> node = jsonNode.elements();
            List<Object> list = new ArrayList<>();
            while (node.hasNext()) {
                list.add(parse(node.next()));
            }
            return list;
        }
        return jsonNode.textValue();
    }

    private void execute(Action action) {
        client.send(wallet,
                getAddress(action.contract), BigInteger.ZERO, action.method, action.params,
                DefaultScoreClient.DEFAULT_RESULT_TIMEOUT);
    }

    private Address getAddress(String key) {
        return this.addresses.get(key)
    }

    private Object getMapProperty(String key) {
        return this.properties.get(key);
    }


    private List<Action> loadActions() throws IOException {
        logger.lifecycle('loading actions...')
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream(this.actionsFilePath.get());
        if (is == null) {
            throw new RuntimeException(this.actionsFilePath.get() + " file not found")
        }

        ObjectMapper objectMapper = new ObjectMapper();

        List<Action> list = objectMapper.readValue(is, new TypeReference<List<Action>>() {
        });

        list.sort((s1, s2) -> Float.compare(s1.getOrder(), s2.getOrder()));
        return list;
    }

    private void loadAddresses() {
        logger.lifecycle('loading addresses...')
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream(this.addressesFilePath.get());
        if (is == null) {
            throw new RuntimeException(this.addressesFilePath.get() + " file not found")
        }
        ObjectMapper objectMapper = new ObjectMapper();

        this.addresses = objectMapper.readValue(is, new TypeReference<HashMap<String, Address>>() {
        });

    }


    private void init() {
        logger.lifecycle('loading properties...')
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream(this.propertiesFile.get());
        if (is == null) {
            throw new RuntimeException(this.propertiesFile.get() + " file not found")
        }
        _properties.load(is)
        this.network = Network.getNetwork(_properties.getProperty("NETWORK"));



        List<String> preps = _properties.getProperty("DEFAULT_PREPS_LIST").split("--;--").toList();
        properties.put("DEFAULT_PREPS_LIST", preps)
        BigInteger timestamp = _properties.getProperty("TIMESTAMP") as BigInteger
        properties.put("START_TIMESTAMP", timestamp)
        Address WORKER_WALLET_ADDRESS = new Address(_properties.getProperty("WORKER_WALLET_ADDRESS"))
        properties.put("WORKER_WALLET_ADDRESS", WORKER_WALLET_ADDRESS)
//            lending and borrow reward
        BigInteger LENDING_BORROW_PERCENTAGE = getPercentageValue("LENDING_BORROW_PERCENTAGE")
        properties.put("LENDING_BORROW_PERCENTAGE", LENDING_BORROW_PERCENTAGE)
//                    # LP staking reward
        BigInteger LP_PERCENTAGE = getPercentageValue("LP_PERCENTAGE")
        properties.put("LP_PERCENTAGE", LP_PERCENTAGE)
        //                    # OMM staking reward
        BigInteger OMM_STAKING_PERCENTAGE = getPercentageValue("OMM_STAKING_PERCENTAGE")
        properties.put("OMM_STAKING_PERCENTAGE", OMM_STAKING_PERCENTAGE)
//                    # DAO reward percentage
        BigInteger DAO_DIST_PERCENTAGE = getPercentageValue("DAO_DIST_PERCENTAGE")
        properties.put("DAO_DIST_PERCENTAGE", DAO_DIST_PERCENTAGE)
//                    # Worker reward
        BigInteger WORKER_DIST_PERCENTAGE = getPercentageValue("WORKER_DIST_PERCENTAGE")
        properties.put("WORKER_DIST_PERCENTAGE", WORKER_DIST_PERCENTAGE)

        assert (LENDING_BORROW_PERCENTAGE + LP_PERCENTAGE + OMM_STAKING_PERCENTAGE + DAO_DIST_PERCENTAGE + WORKER_DIST_PERCENTAGE) / ICX == 1

        BigInteger ICX_EMISSION = getPercentageValue("ICX_PERCENTAGE")
        BigInteger OICX_EMISSION = getPercentageValue("OICX_EMISSION") * ICX_EMISSION / ICX as BigInteger
        properties.put("OICX_EMISSION", OICX_EMISSION)
        BigInteger DICX_EMISSION = getPercentageValue("DICX_EMISSION") * ICX_EMISSION / ICX as BigInteger
        properties.put("DICX_EMISSION", DICX_EMISSION)

        BigInteger USDS_EMISSION = getPercentageValue("USDS_PERCENTAGE")
        BigInteger OUSDS_EMISSION = getPercentageValue("OUSDS_EMISSION") * USDS_EMISSION / ICX as BigInteger
        properties.put("OUSDS_EMISSION", OUSDS_EMISSION)
        BigInteger DUSDS_EMISSION = getPercentageValue("DUSDS_EMISSION") * USDS_EMISSION / ICX as BigInteger
        properties.put("DUSDS_EMISSION", DUSDS_EMISSION)

        BigInteger IUSDC_EMISSION = getPercentageValue("IUSDC_PERCENTAGE")
        BigInteger OIUSDC_EMISSION = getPercentageValue("OIUSDC_EMISSION") * IUSDC_EMISSION / ICX as BigInteger
        properties.put("OIUSDC_EMISSION", OIUSDC_EMISSION)
        BigInteger DIUSDC_EMISSION = getPercentageValue("DIUSDC_EMISSION") * IUSDC_EMISSION / ICX as BigInteger
        properties.put("DIUSDC_EMISSION", DIUSDC_EMISSION)

        BigInteger bnUSD_EMISSION = getPercentageValue("bnUSD_PERCENTAGE")
        BigInteger ObnUSD_EMISSION = getPercentageValue("ObnUSD_EMISSION") * bnUSD_EMISSION / ICX as BigInteger
        properties.put("ObnUSD_EMISSION", ObnUSD_EMISSION)
        BigInteger DbnUSD_EMISSION = getPercentageValue("DbnUSD_EMISSION") * bnUSD_EMISSION / ICX as BigInteger
        properties.put("DbnUSD_EMISSION", DbnUSD_EMISSION)

        BigInteger BALN_EMISSION = getPercentageValue("BALN_PERCENTAGE")
        BigInteger OBALN_EMISSION = getPercentageValue("OBALN_EMISSION") * BALN_EMISSION / ICX as BigInteger
        properties.put("OBALN_EMISSION", OBALN_EMISSION)
        BigInteger DBALN_EMISSION = getPercentageValue("DBALN_EMISSION") * BALN_EMISSION / ICX as BigInteger
        properties.put("DBALN_EMISSION", DBALN_EMISSION)


        BigInteger OMM_EMISSION = getPercentageValue("OMM_PERCENTAGE")
        BigInteger OOMM_EMISSION = getPercentageValue("OOMM_EMISSION") * OMM_EMISSION / ICX as BigInteger
        properties.put("OOMM_EMISSION", OOMM_EMISSION)
        BigInteger DOMM_EMISSION = getPercentageValue("DOMM_EMISSION") * OMM_EMISSION / ICX as BigInteger
        properties.put("DOMM_EMISSION", DOMM_EMISSION)

        assert (OICX_EMISSION + DICX_EMISSION + OUSDS_EMISSION + DUSDS_EMISSION + OIUSDC_EMISSION + DIUSDC_EMISSION + ObnUSD_EMISSION + DbnUSD_EMISSION + OBALN_EMISSION + DBALN_EMISSION + OOMM_EMISSION + DOMM_EMISSION) / ICX == 1

//            #LP and OMM staking reward

        BigInteger OMM_SICX_DIST_PERCENTAGE = BigInteger.valueOf(100 * ICX / 3 as Long)
//getPercentageValue("OMM_SICX_DIST_PERCENTAGE")
        properties.put("OMM_SICX_DIST_PERCENTAGE", OMM_SICX_DIST_PERCENTAGE)
        BigInteger OMM_USDS_DIST_PERCENTAGE = BigInteger.valueOf(100 * ICX / 3 as Long)
//getPercentageValue("OMM_USDS_DIST_PERCENTAGE")
        properties.put("OMM_USDS_DIST_PERCENTAGE", OMM_USDS_DIST_PERCENTAGE)
        BigInteger OMM_USDC_DIST_PERCENTAGE = ICX - OMM_SICX_DIST_PERCENTAGE - OMM_USDS_DIST_PERCENTAGE
// getPercentageValue("OMM_USDC_DIST_PERCENTAGE")
        properties.put("OMM_USDC_DIST_PERCENTAGE", OMM_USDC_DIST_PERCENTAGE)

        BigInteger OMM_DIST_PERCENTAGE = getPercentageValue("OMM_DIST_PERCENTAGE")
        properties.put("OMM_DIST_PERCENTAGE", OMM_DIST_PERCENTAGE)

        assert (OMM_SICX_DIST_PERCENTAGE + OMM_USDS_DIST_PERCENTAGE + OMM_USDC_DIST_PERCENTAGE) / ICX == 1

        BigInteger DEPOSIT_ICX_AMOUNT = new BigInteger(_properties.getProperty("DEPOSIT_ICX_AMOUNT")) * ICX
        properties.put("DEPOSIT_ICX_AMOUNT", DEPOSIT_ICX_AMOUNT)
        println "{ _properties.getProperty()} = ${_properties.getProperty("FEE_SHARING_TX_LIMIT")}"

        BigInteger FEE_SHARING_TX_LIMIT = _properties.getProperty("FEE_SHARING_TX_LIMIT") as BigInteger * ICX
        properties.put("FEE_SHARING_TX_LIMIT", FEE_SHARING_TX_LIMIT)
        BigInteger LOAN_ORIGINATION_PERCENTAGE = getPercentageValue("LOAN_ORIGINATION_PERCENTAGE")
        properties.put("LOAN_ORIGINATION_PERCENTAGE", LOAN_ORIGINATION_PERCENTAGE)
        BigInteger MINIMUM_OMM_STAKE = _properties.getProperty("MINIMUM_OMM_STAKE") as BigInteger * ICX
        properties.put("MINIMUM_OMM_STAKE", MINIMUM_OMM_STAKE)
        BigInteger OMM_UNSTAKING_PERIOD = _properties.getProperty("OMM_UNSTAKING_PERIOD") as BigInteger
        properties.put("OMM_UNSTAKING_PERIOD", OMM_UNSTAKING_PERIOD)
        BigInteger BORROW_THRESHOLD = getPercentageValue("BORROW_THRESHOLD")
        properties.put("BORROW_THRESHOLD", BORROW_THRESHOLD)


    }

    BigInteger getPercentageValue(String key) {
        def value = _properties.getProperty(key) as Double
        return BigInteger.valueOf(value * ICX / 100 as long)
    }

}
