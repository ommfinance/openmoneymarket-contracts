package finance.omm.libs.test.integration.configs;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMMClient;
import foundation.icon.jsonrpc.Address;

import java.util.Map;
import java.util.stream.Collectors;

public class dTokenConfig implements Config{

    Map<String, score.Address> addressMap;

    public dTokenConfig(Map<String, Address> addressMap) {
        this.addressMap = addressMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> score.Address.fromString(entry.getValue().toString())));

    }
    @Override
    public void call(OMMClient ommClient) {
        ommClient.addressManager.addAddressToScore(Contracts.dICX.getKey(), new String[]{
                Contracts.LENDING_POOL_CORE.getKey(), Contracts.OMM_TOKEN.getKey()
        });
    }
}
