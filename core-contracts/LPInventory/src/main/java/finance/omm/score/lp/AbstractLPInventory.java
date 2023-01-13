package finance.omm.score.lp;

import finance.omm.core.score.interfaces.LPInventory;
import finance.omm.libs.address.AddressProvider;
import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public abstract class AbstractLPInventory extends AddressProvider implements LPInventory {

    public static final String TAG = "LP Inventory";

    public final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    public final VarDB<Address> candidate = Context.newVarDB("admin-candidate", Address.class);

    @EventLog(indexed = 2)
    public void TokenReceived(Address reserve, BigInteger value) {
    }

    @EventLog(indexed = 3)
    public void LPTokenReceived(Address from, BigInteger poolId, BigInteger value) {
    }

    @EventLog
    public void AdminCandidatePushed(Address oldAdmin, Address newAdmin) {
    }

    @EventLog
    public void AdminRoleClaimed(Address oldAdmin, Address newAdmin) {
    }

    public AbstractLPInventory(Address addressProvider) {
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

    protected void onlyAdmin(String msg) {
        Address caller = Context.getCaller();
        if (!caller.equals(admin.get())) {
            throw OMMException.unknown(msg);
        }
    }
}
