package finance.omm.libs.test.integration.configs;

import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.configs.versions.Release;
import finance.omm.libs.test.integration.configs.versions.Release_1_0_0;
import finance.omm.libs.test.integration.configs.versions.Release_1_1_0;
import finance.omm.libs.test.integration.configs.versions.Release_1_1_2;
import finance.omm.libs.test.integration.configs.versions.Release_1_1_5_1;
import finance.omm.libs.test.integration.configs.versions.Release_1_2_0;

public class BaseConfig implements Config {

    Release release;

    public BaseConfig() {

    }

    @Override
    public void call(OMMClient ommClient) {
        release = new Release_1_0_0(ommClient);
        release.nextRelease(new Release_1_1_0())
                .nextRelease(new Release_1_1_2())
                .nextRelease(new Release_1_1_5_1())
                .nextRelease(new Release_1_2_0());

        release.init();

        ommClient.governance.enableHandleActions();
        ommClient.governance.enableRewardClaim();
    }
}
