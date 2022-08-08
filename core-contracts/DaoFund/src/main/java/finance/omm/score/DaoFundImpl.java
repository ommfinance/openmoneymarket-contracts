package finance.omm.score;

import finance.omm.core.score.interfaces.DAOFund;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;
import score.Address;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class DaoFundImpl extends AddressProvider implements DAOFund {

    public static final String TAG = "Dao Fund Manager";

    /**
     * _addressProvider: contract address of the provider _update: allow to mimic on update event call, default to
     * false
     */
    public DaoFundImpl(Address _addressProvider, @Optional boolean _update) {
        super(_addressProvider, _update);
        if (_update) {
            Context.println(TAG + "| on update event:" + Context.getAddress());
        } else {
            Context.println(TAG + "| on install event:" + Context.getAddress());
        }
        Context.println(TAG + "| Started Dao Fund:" + Context.getAddress());
    }

    @EventLog(indexed = 1)
    public void FundReceived(BigInteger _amount, Address _reserve) {
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void transferOmm(BigInteger _value, Address _address) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(
                        TAG + " | SenderNotGovernanceError: sender is not equals to governance"));
        Address ommAddress = getAddress(Contracts.OMM_TOKEN.getKey());

        if (ommAddress == null) {
            Context.revert(TAG + "| omm address was not set");
        }
        Context.call(ommAddress, "transfer", _address, _value);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        this.FundReceived(_value, Context.getCaller());
    }

}
