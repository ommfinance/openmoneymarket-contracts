package finance.omm.score;

import java.math.BigInteger;

import finance.omm.commons.Addresses;
import score.Address;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class DaoFund extends Addresses {

	public static final String TAG = "Dao Fund Manager";

	/**
	 * _addressProvider: contract address of the provider 
	 * _update: allow to mimic on update event call, default to false
	 * */
	public DaoFund(Address _addressProvider, @Optional boolean _update) {
		super(_addressProvider, _update);
		if(_update) {
			Context.println(TAG + "| on update event:" + Context.getAddress());
		}else {
			Context.println(TAG + "| on install event:" + Context.getAddress());
		}
		Context.println(TAG + "| Started Dao Fund:" + Context.getAddress());
	}

	@EventLog(indexed=1)
	public void FundReceived(BigInteger _amount, Address _reserve) {}

	@External(readonly=true)
	public String name() {
		return "Omm "+TAG;
	}

	@External
	public void transferOmm(BigInteger _value, Address _address) {
		onlyGovernance();
		Address ommAddress = getAddress(OMM_TOKEN);
		if(ommAddress == null) {
			Context.revert(TAG + "| omm address was not set");
		}
		Context.call(ommAddress, "transfer", _address, _value);
	}

	@External
	public void tokenFallback(Address _from, BigInteger _value, @Optional byte[] _data) {
		this.FundReceived(_value, Context.getCaller());
	}

	@Override
	public String getTag() {
		return TAG;
	}
}
