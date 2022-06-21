
package finance.omm.libs.test.integration.scores;

import java.util.Map;
import score.Address;
import score.annotation.Payable;


public interface SystemInterface {

    Map<String, Object> getIISSInfo();

    Map<String, Object> queryIScore(Address address);

    Map<String, Object> getStake(Address address);

    Map<String, Object> getDelegation(Address address);

    @Payable
    void registerPRep(String name, String email, String country, String city, String website, String details,
            String p2pEndpoint);
}
