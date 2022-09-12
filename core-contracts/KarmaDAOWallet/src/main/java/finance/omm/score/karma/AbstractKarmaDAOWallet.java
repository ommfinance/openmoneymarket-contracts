package finance.omm.score.karma;

import finance.omm.core.score.interfaces.KarmaDAOWallet;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.utils.exceptions.OMMException.OMMWalletException;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public abstract class AbstractKarmaDAOWallet extends AddressProvider implements KarmaDAOWallet,
        Authorization<OMMWalletException> {

    //poolId -> bond contract
    public final DictDB<BigInteger, Address> bondContracts = Context.newDictDB("bondContracts", Address.class);
    //poolId -> treasury contract
    public final DictDB<BigInteger, Address> treasuryContracts = Context.newDictDB("treasuryContracts", Address.class);

    public final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    public final VarDB<Address> candidate = Context.newVarDB("admin-candidate", Address.class);

    public AbstractKarmaDAOWallet(Address addressProvider) {
        super(addressProvider, false);
        if (admin.get() == null) {
            admin.set(Context.getCaller());
        }
    }

    @EventLog(indexed = 1)
    public void AdminCandidatePushed(Address newAdmin) {}

    @EventLog(indexed = 1)
    public void AdminStatusClaimed(Address newAdmin) {}

    @EventLog(indexed = 2)
    public void FundTransferred(BigInteger poolId, BigInteger value) {}

    @EventLog(indexed = 2)
    public void FundWithdrawn(BigInteger poolId, BigInteger value) {}

    @EventLog(indexed = 2)
    public void LPWithdrawn(BigInteger poolId, BigInteger value) {}

    @EventLog(indexed = 2)
    public void LPTokenRemoved(BigInteger poolId, BigInteger value) {}

    @EventLog(indexed = 2)
    public void TokenTransferred(Address token, Address to, BigInteger value) {}

    @EventLog(indexed = 1)
    public void FundReceived(Address reserve, BigInteger value) {
    }


    @EventLog(indexed = 2)
    public void LPTokenReceived(BigInteger poolId, BigInteger value, Address from) {

    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        this.FundReceived(Context.getCaller(), _value);
    }


    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        this.LPTokenReceived(_id, _value, Context.getCaller());
    }

}
