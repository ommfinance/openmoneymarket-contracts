
package finance.omm.libs.test.integration.scores;

import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.annotation.Payable;


public interface SystemInterface {

    public class Delegation{
        public Address address;
        public BigInteger value;
    }

    public class Bond{
        public Address address;
        public BigInteger value;
    }

    Map<String, Object> getIISSInfo();

    Map<String, Object> queryIScore(Address address);

    Map<String, Object> getStake(Address address);

    Map<String, Object> getDelegation(Address address);

    Map<String, Object> getPReps(BigInteger startRanking, BigInteger endRanking);

    @Payable
    void registerPRep(String name, String email, String country, String city, String website, String details,
            String p2pEndpoint);


    void setDelegation(Delegation[] delegations);
    void setBond(Bond[] bonds);
    void setBonderList(Address[] bonderList);
    void setStake(BigInteger value);

}
