package finance.omm.libs.test.integration.configs;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMMClient;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import score.Address;

public class DelegationConfig implements Config {

    Map<String, Address> addressMap;

    public DelegationConfig(Map<String, foundation.icon.jsonrpc.Address> addressMap) {
        this.addressMap = addressMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> Address.fromString(entry.getValue().toString())));

    }

    @Override
    public void call(OMMClient ommClient) {
        ommClient.addressManager.addAddressToScore(Contracts.DELEGATION.getKey(), new String[]{
                Contracts.LENDING_POOL_CORE.getKey(), Contracts.OMM_TOKEN.getKey()
        });

        ommClient.delegation.addAllContributors(Environment.preps.keySet().toArray(Address[]::new));


    }
}
