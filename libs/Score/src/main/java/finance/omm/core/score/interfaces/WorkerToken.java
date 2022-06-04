package finance.omm.core.score.interfaces;

import java.util.List;

import finance.omm.core.score.interfaces.token.IRC2;
import score.Address;

public interface WorkerToken extends IRC2{

    List<Address> getWallets();
}
