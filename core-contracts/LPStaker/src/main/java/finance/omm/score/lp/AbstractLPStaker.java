package finance.omm.score.lp;


import finance.omm.core.score.interfaces.LPStaker;
import finance.omm.libs.address.AddressProvider;
import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public abstract class AbstractLPStaker extends AddressProvider implements LPStaker {
    public static final String TAG = "LP Staker";

    public final VarDB<Address> admin = Context.newVarDB("admin", Address.class);

    @EventLog(indexed = 2)
    public void TokenReceived(Address reserve, BigInteger value) {
    }

    @EventLog(indexed = 3)
    public void LPTokenReceived(Address from, BigInteger poolId, BigInteger value) {

    }

    public AbstractLPStaker(Address addressProvider) {
        super(addressProvider, false);
        if (admin.get() == null) {
            admin.set(Context.getCaller());
        }
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {

        this.LPTokenReceived(_from, _id, _value);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        this.TokenReceived(_from, _value);
    }

    protected void onlyOwner(String msg) {
        Address caller = Context.getCaller();
        if (!caller.equals(admin.get())) {
            throw OMMException.unknown(msg);
        }
    }
}