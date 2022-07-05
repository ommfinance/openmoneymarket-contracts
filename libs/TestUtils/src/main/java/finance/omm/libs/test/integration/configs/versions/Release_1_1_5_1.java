package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.address.Contracts;

public class Release_1_1_5_1 extends Release {


    @Override
    public boolean init() {
        System.out.println("----init release v1.1.5.1----");
        ommClient.addressManager.addAddressToScore(Contracts.DELEGATION.getKey(), new String[]{
                Contracts.STAKING.getKey(),
                Contracts.sICX.getKey()
        });

        return this.next(ommClient);
    }

}
