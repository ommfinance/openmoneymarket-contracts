package finance.omm.score.tokens.utils;

import com.iconloop.score.token.irc2.IRC2Mintable;
import java.math.BigInteger;
import score.Context;

public class IRC2Token extends IRC2Mintable {

    public IRC2Token(BigInteger _totalSupply) {
        super("Omm Token", "OMM", 18);
        _mint(Context.getCaller(), _totalSupply);
    }
}
