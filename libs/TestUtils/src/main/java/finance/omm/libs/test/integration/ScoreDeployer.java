package finance.omm.libs.test.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import finance.omm.libs.test.integration.model.Score;
import foundation.icon.jsonrpc.Address;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ScoreDeployer {

    private String contracts;
    private OMM omm;

    public ScoreDeployer(OMM omm, String contracts) {
        this.omm = omm;
        this.contracts = contracts;
    }

    public Map<String, foundation.icon.jsonrpc.Address> deployContracts() throws IOException, InterruptedException {
        Map<Float, List<Score>> scores = readSCOREs();

        ExecutorService exec = Executors.newFixedThreadPool(10);

        Map<String, foundation.icon.jsonrpc.Address> addresses = new HashMap<>();

        addresses.put("addressProvider", omm.deployAddressManager());

        for (Entry<Float, List<Score>> entry : scores.entrySet()) {
            Map<String, Future<Address>> result = new HashMap<>();
            for (Score score : entry.getValue()) {
                score.setPath(omm.getScorePath(score.getContract()));
                System.out.println("deploying contract " + score.getName() + " :: " + score.getPath());
                Thread.sleep(200);
                Map<String, String> addressParams = score.getAddressParams();

                for (Map.Entry<String, String> params : addressParams.entrySet()) {
                    String key = params.getKey();
                    String value = params.getValue();
                    score.addParams(key, addresses.get(value));
                }

                result.put(score.getName(), exec.submit(omm.deploy(score)));
            }

            for (Entry<String, Future<Address>> futureEntry : result.entrySet()) {
                try {
                    foundation.icon.jsonrpc.Address address = futureEntry.getValue().get();
                    String name = futureEntry.getKey();
                    addresses.put(name, address);
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("futureEntry = " + futureEntry.getKey());
                    e.printStackTrace();
                }
            }
        }

        exec.shutdown();

        setAddresses(addresses);
        return addresses;
    }

    private void setAddresses(Map<String, Address> addresses) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map.Entry<String, foundation.icon.jsonrpc.Address> entry : addresses.entrySet()) {
            if (!entry.getKey().equals("addressProvider") && !entry.getKey().equals("owner")) {
                details.add(Map.of(
                        "name", entry.getKey(), "address", entry.getValue()
                ));
            }
        }
        Map<String, Object> params = Map.of("_addressDetails", details);

        omm.send(addresses.get("addressProvider"), "setAddresses", params);
    }

    private Map<Float, List<Score>> readSCOREs() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream(this.contracts);

        List<Score> list = objectMapper.readValue(is, new TypeReference<List<Score>>() {
        });

        return list.stream()
                .sorted((x, y) -> Float.compare(y.getOrder(), x.getOrder()))
                .collect(Collectors.groupingBy(Score::getOrder, Collectors.toList()));

    }

}
