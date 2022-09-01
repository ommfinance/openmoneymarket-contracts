package finance.omm.libs.test.integration.configs;

import finance.omm.libs.test.integration.OMMClient;

public interface Config {

    void call(OMMClient ommClient);
}
