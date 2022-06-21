package finance.omm.libs.test.integration.configs;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMMClient;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import score.Address;

public class RewardDistributionConfig implements Config {

    Map<String, Address> addressMap;

    public RewardDistributionConfig(Map<String, foundation.icon.jsonrpc.Address> addressMap) {
        this.addressMap = addressMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> Address.fromString(entry.getValue().toString())));

    }

    @Override
    public void call(OMMClient ommClient) {
        ommClient.addressManager.addAddressToScore(Contracts.REWARDS.getKey(), new String[]{
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.DAO_FUND.getKey(),
                Contracts.sICX.getKey(),
                Contracts.oICX.getKey(),
                Contracts.dICX.getKey(),
        });
        ommClient.addressManager.addAddressToScore(Contracts.REWARD_WEIGHT_CONTROLLER.getKey(), new String[]{
                Contracts.REWARDS.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.DAO_FUND.getKey(),
                Contracts.sICX.getKey(),
                Contracts.oICX.getKey(),
                Contracts.dICX.getKey(),
        });


    }
}
