package finance.omm.score;

import static finance.omm.utils.math.MathUtils.exaMultiply;

import java.math.BigInteger;

import finance.omm.core.score.interfaces.FreeProvider;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class FreeProviderImpl extends AddressProvider implements FreeProvider {

    private static final String TAG = "Fee Provider";

    private static final String ORIGINATION_FEE_PERCENT = "originationFeePercent";

    private VarDB<BigInteger> _originationFeePercent = Context
            .newVarDB(ORIGINATION_FEE_PERCENT, BigInteger.class);

    public FreeProviderImpl(Address _addressProvider, @Optional boolean _update) {
        super(_addressProvider, _update);
        if (_update) {
            Context.println(TAG + "| on update event:" + Context.getAddress());
        } else {
            Context.println(TAG + "| on install event:" + Context.getAddress());
        }
        Context.println(TAG + "| Started Free Provider:" + Context.getAddress());
    }

    @EventLog(indexed = 3)
    public void FeeReceived(Address _from, BigInteger _value, byte[] _data, Address _sender) {
    }

    @External
    public void setLoanOriginationFeePercentage(BigInteger _percentage) {
        onlyOwner();
        this._originationFeePercent.set(_percentage);
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External(readonly = true)
    public BigInteger calculateOriginationFee(BigInteger _amount) {
        return exaMultiply(_amount, this.getLoanOriginationFeePercentage());
    }

    @External(readonly = true)
    public BigInteger getLoanOriginationFeePercentage() {
        return this._originationFeePercent.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        this.FeeReceived(_from, _value, _data, Context.getCaller());
    }

    @External
    public void transferFund(Address _token, BigInteger _value, Address _to) {
        onlyOrElseThrow(Contracts.GOVERNANCE, OMMException.unknown(
                TAG + " | SenderNotGovernanceError: sender is not equals to governance"));
        Context.call(_token, "transfer", _to, _value);
    }

    public void onlyOwner() {
        Address sender = Context.getCaller();
        Address owner = Context.getOwner();
        if (!sender.equals(owner)){
            Context.revert(TAG + ": SenderNotScoreOwnerError:  (sender)"+ sender + " (owner)"+owner);
        }
    }
}
