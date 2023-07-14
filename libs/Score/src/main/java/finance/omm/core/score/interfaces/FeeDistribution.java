package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.Optional;

import java.math.BigInteger;

@ScoreInterface(suffix = "Client")
public interface FeeDistribution {

    String name();

    BigInteger getFeeDistributionOf(Address address);

    BigInteger getCollectedFee(Address address);

    BigInteger getValidatorCollectedFee();

    BigInteger getAccumulatedFee(Address address);

    void setFeeDistribution(Address[] addresses, BigInteger[] weights);

    void tokenFallback(Address _from, BigInteger _value, byte[] _data);

    void claimRewards(@Optional Address receiverAddress);
}
