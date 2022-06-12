package finance.omm.core.score.interfaces;

import foundation.icon.score.client.ScoreInterface;
import score.Address;

@ScoreInterface(suffix = "Client")
public interface StakedLP {

    void addPool(int _poolID, Address asset);

    void removePool(int _poolID);
}
