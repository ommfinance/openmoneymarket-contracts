package finance.omm.libs.test.integration.configs.versions;

import java.math.BigInteger;

public class Release_1_3_0 extends Release {
    @Override
    public boolean init() {
        System.out.println("----init release v1.3.0----");

        ommClient.rewardWeightController.setStopDay(BigInteger.valueOf(1000000000L));

        return this.next(ommClient);
    }
}
