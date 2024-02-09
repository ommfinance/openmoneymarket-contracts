package finance.omm.score.karma;

import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface KarmaTreasury {

    void pushManagement(Address newOwner);

    void pullManagement();

    void withdraw(Address token, Address destination, BigInteger amount);

    void withdrawLp(Address token, Address destination, BigInteger amount, @Optional BigInteger poolId);

    void toggleBondContract(Address bondContract);

    boolean bondContract(Address address);
}
