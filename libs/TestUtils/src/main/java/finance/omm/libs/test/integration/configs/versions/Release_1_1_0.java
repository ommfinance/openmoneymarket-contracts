package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.address.Contracts;

public class Release_1_1_0 extends Release {

    @Override
    public boolean init() {
        System.out.println("----init release v1.1.0----");
        ommClient.addressManager.addAddressToScore(Contracts.LENDING_POOL_CORE.getKey(), new String[]{
                Contracts.REWARDS.getKey()
        });

        ommClient.addressManager.addAddressToScore(Contracts.GOVERNANCE.getKey(), new String[]{
                Contracts.OMM_TOKEN.getKey()
        });

        return this.next(ommClient);
    }

}
