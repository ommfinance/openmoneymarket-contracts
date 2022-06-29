package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.address.Contracts;

public class Release_1_2_0 extends Release {


    @Override
    public boolean init() {
        System.out.println("----init release v1.2.0----");
        ommClient.addressManager.addAddressToScore(Contracts.DELEGATION.getKey(), new String[]{
                Contracts.STAKING.getKey(),
                Contracts.sICX.getKey()
        });

        return this.next(ommClient);
    }

}
