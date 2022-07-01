package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.test.integration.OMMClient;
import java.util.Map;
import score.Address;

public abstract class Release {

    private Release nextRelease;
    protected Map<String, Address> addressMap;
    protected OMMClient ommClient;

    public Release nextRelease(Release nextRelease) {
        this.nextRelease = nextRelease;
        return nextRelease;
    }


    public abstract boolean init();


    protected boolean next(OMMClient ommClient) {
        if (nextRelease == null) {
            return true;
        }
        nextRelease.ommClient = ommClient;
        nextRelease.addressMap = ommClient.getContractAddresses();
        return nextRelease.init();
    }

}
