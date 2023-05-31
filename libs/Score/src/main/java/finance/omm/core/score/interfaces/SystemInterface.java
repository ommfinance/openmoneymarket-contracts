package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.Keep;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

//@ScoreClient
@ScoreInterface
public interface SystemInterface {
    public class Delegation{
        @Keep
        public Address address;
        @Keep
        public BigInteger value;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Delegation that = (Delegation) o;
            return address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }
    }
    Map<String, Object> getIISSInfo();

    Map<String, Object> queryIScore(Address address);

    Map<String, Object> getStake(Address address);

    Map<String, Object> getDelegation(Address address);

    Map<String, Object> getPReps(BigInteger startRanking, BigInteger endRanking);

    Map<String, Object> getPRep(Address address);

    @Payable
    void registerPRep(String name, String email, String country, String city, String website, String details, String p2pEndpoint);
}
