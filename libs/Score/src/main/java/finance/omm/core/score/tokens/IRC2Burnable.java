package finance.omm.core.score.tokens;

import finance.omm.core.score.interfaces.token.IRC2BurnableInterface;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

import static finance.omm.utils.checks.Check.only;

public class IRC2Burnable extends IRC2Mintable implements IRC2BurnableInterface {

    public IRC2Burnable(String _name, String _symbol, @Optional BigInteger _decimals) {
        super(_name, _symbol, _decimals);
    }

    @External
    public void burn(BigInteger _amount) {
        burnFrom(Context.getCaller(), _amount);
    }

    @External
    public void burnFrom(Address _account, BigInteger _amount) {
        only(minter);
        super.burn(_account, _amount);
    }
}