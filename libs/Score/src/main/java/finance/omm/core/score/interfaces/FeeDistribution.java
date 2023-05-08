package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.Address;

import java.math.BigInteger;

@ScoreInterface(suffix = "Client")
public interface FeeDistribution {

    String name();

    BigInteger getFeeDistributionOf(Address address);

    BigInteger getFeeDistributed(Address address);

    void setFeeDistribution(Address[] addresses, BigInteger[] weights);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    void claimValidatorsRewards(Address receiverAddress);
}