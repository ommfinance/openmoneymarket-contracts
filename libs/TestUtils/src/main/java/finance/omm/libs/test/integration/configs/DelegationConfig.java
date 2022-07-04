package finance.omm.libs.test.integration.configs;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMMClient;

import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import score.Address;

import static finance.omm.utils.math.MathUtils.ICX;

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
                Contracts.LENDING_POOL_CORE.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.STAKING.getKey(),
                Contracts.sICX.getKey(),
                Contracts.GOVERNANCE.getKey(),
                Contracts.REWARDS.getKey(),
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.DAO_FUND.getKey()

        });
        ommClient.addressManager.addAddressToScore(Contracts.LENDING_POOL_CORE.getKey(), new String[]{
                Contracts.DELEGATION.getKey(),
                Contracts.STAKING.getKey(),
                Contracts.GOVERNANCE.getKey(),
                Contracts.LENDING_POOL.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.LENDING_POOL.getKey(), new String[]{
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BRIDGE_O_TOKEN.getKey(),
                Contracts.sICX.getKey(),
                Contracts.STAKING.getKey(),
                Contracts.LENDING_POOL_CORE.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.DAO_FUND.getKey(), new String[]{
                Contracts.DELEGATION.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.GOVERNANCE.getKey()
        });


        ommClient.addressManager.addAddressToScore(Contracts.BOOSTED_OMM.getKey(), new String[]{
                Contracts.DELEGATION.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.OMM_TOKEN.getKey(), new String[]{
                Contracts.REWARDS.getKey(),
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.WORKER_TOKEN.getKey(),
                Contracts.GOVERNANCE.getKey(),
                Contracts.DAO_FUND.getKey(),
                Contracts.LENDING_POOL.getKey()
        });
        ommClient.addressManager.addAddressToScore(Contracts.GOVERNANCE.getKey(),new String[]{
                Contracts.REWARDS.getKey(),
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.DELEGATION.getKey(),
                Contracts.DAO_FUND.getKey(),
                Contracts.LENDING_POOL_CORE.getKey()
        });


        ommClient.addressManager.addAddressToScore(Contracts.REWARDS.getKey(), new String[]{
                Contracts.REWARD_WEIGHT_CONTROLLER.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.DAO_FUND.getKey(),
                Contracts.WORKER_TOKEN.getKey(),
                Contracts.GOVERNANCE.getKey(),
        });
        ommClient.addressManager.addAddressToScore(Contracts.REWARD_WEIGHT_CONTROLLER.getKey(), new String[]{
                Contracts.REWARDS.getKey(),
                Contracts.OMM_TOKEN.getKey(),
                Contracts.BOOSTED_OMM.getKey(),
                Contracts.GOVERNANCE.getKey(),
        });

        ommClient.addressManager.addAddressToScore(Contracts.oICX.getKey(), new String[]{
                Contracts.LENDING_POOL.getKey()
        });

        ommClient.delegation.addAllContributors(Environment.preps.keySet().toArray(Address[]::new));
        ommClient.staking.setSicxAddress(addressMap.get(Contracts.sICX.getKey()));
        ommClient.staking.toggleStakingOn();

        ommClient.lendingPool.setFeeSharingTxnLimit(BigInteger.valueOf(50));

        ommClient.workerToken.transfer(ommClient.getAddress(), BigInteger.valueOf(100).multiply(ICX), "None".getBytes());
    }
}
