package finance.omm.core.score.tokens;

import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

import static finance.omm.utils.checks.Check.only;
import static finance.omm.utils.checks.Check.onlyOwner;

public class IRC2Mintable extends IRC2Base {

    private final String MINTER = "admin";

    protected final VarDB<Address> minter = Context.newVarDB(MINTER, Address.class);

    public IRC2Mintable(String _tokenName, String _symbolName, BigInteger _decimals) {
        super(_tokenName, _symbolName, _decimals);
    }

    @External
    public void setMinter(Address _address) {
        onlyOwner();
        minter.set(_address);
    }

    @External(readonly = true)
    public Address getMinter() {
        return minter.get();
    }

    @External
    public void mint(BigInteger _amount, @Optional byte[] _data) {
        mintTo(Context.getCaller(), _amount, _data);
    }

    @External
    public void mintTo(Address _account, BigInteger _amount, @Optional byte[] _data) {
        only(minter);
        mintWithTokenFallback(_account, _amount, _data);
    }

    protected void mintWithTokenFallback(Address _to, BigInteger _amount, byte[] _data) {
        mint(_to, _amount);
        byte[] data = (_data == null) ? new byte[0] : _data;
        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", ZERO_ADDRESS, _amount, data);
        }
    }

}
