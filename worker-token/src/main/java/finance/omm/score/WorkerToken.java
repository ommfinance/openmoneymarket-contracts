package finance.omm.score;

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;


import java.math.BigInteger;
import java.util.List;

import finance.omm.score.utils.EnumerableSet;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class WorkerToken {

	private static final String TAG = "Worker Token";

	private static final String BALANCES = "balances";
	private static final String TOTAL_SUPPLY = "total_supply";
	private static final String DECIMALS = "decimals";
	private static final String WALLETS = "wallets";
	public static final String WORKER_TOKEN_SYMBOL = "OMMWT";

	private VarDB<BigInteger> totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
	private VarDB<BigInteger> decimals = Context.newVarDB(DECIMALS, BigInteger.class);
	private DictDB<Address, BigInteger> balances = Context.newDictDB(BALANCES, BigInteger.class);
	private EnumerableSet<Address> wallets = new EnumerableSet<>(WALLETS, Address.class);

	public WorkerToken(BigInteger _initialSupply, BigInteger _decimals, @Optional boolean _update) {
		//we mimic on_update py feature, updating java score will call <init> (constructor) method
		if(_update) {
			Context.println(TAG + "| on update event:" + Context.getAddress());
			onUpdate();
			return;
		}

		Context.println(TAG + "| on install event:" + Context.getAddress())
		;
		if (_initialSupply == null || _initialSupply.compareTo(ZERO) < 0) {
			Context.revert("Initial supply cannot be less than zero");
		}

		if (_decimals == null || _decimals.compareTo(ZERO) < 0) {
			Context.revert("Decimals cannot be less than zero");
		}

		BigInteger totalSupply = _initialSupply.multiply( pow( TEN , _decimals.intValue()) );
		Context.println(TAG+"| total_supply "+ totalSupply );

		this.totalSupply.set(totalSupply);
		this.decimals.set(_decimals);
		this.balances.set(Context.getCaller(), totalSupply);

		Context.println(TAG + "| Started Worker Token:" + Context.getAddress());

	}

	public void onUpdate() {}

	@EventLog(indexed=3)
	public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {}

	@External(readonly=true)
	public String name() {
		return "Omm "+TAG;
	}

	@External(readonly=true)
	public String symbol() {
		return WORKER_TOKEN_SYMBOL;
	}

	@External(readonly=true)
	public BigInteger decimals() {
		return this.decimals.get();
	}

	@External(readonly=true)
	public BigInteger totalSupply() {
		return this.totalSupply.get();
	}

	@External(readonly=true)
	public BigInteger balanceOf(Address _owner) {
		return this.balances.getOrDefault(_owner, ZERO);
	}

	@External
	public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
		if (_data == null || _data.length == 0){
			_data = "None".getBytes();
		}

		if (!wallets.contains(_to)) {
			this.wallets.add(_to);
		}
		this._transfer( Context.getCaller(), _to, _value, _data);
	}

	public void _transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {

		// Checks the sending value and balance.
		if (_value.compareTo(ZERO) <= 0) {
			Context.revert(TAG +": Transferring value should be greater than zero");
		}
		BigInteger balanceFrom = this.balances.getOrDefault(_from, ZERO);
		Context.println(TAG + "| transferring: " + _value + " - from sender balance: " + balanceFrom);
		if ( balanceFrom.compareTo(_value) < 0 ) {
			Context.revert(TAG +" : Out of balance: "+ _value);
		}

		this.balances.set(_from, balanceFrom.subtract(_value));
		this.balances.set(_to, this.balances.getOrDefault(_to, ZERO).add(_value));

		if (this.balanceOf(_from).equals(ZERO)) {
			this.wallets.remove(_from);
		}

		if (_to.isContract()) {
			// If the recipient is SCORE,
			//   then calls `tokenFallback` to hand over control.
			Context.call(_to, "tokenFallback", _from, _value, _data);
		}
		// Emits an event log `Transfer`
		this.Transfer(_from, _to, _value, _data);
	}

	@External(readonly=true)
	public List<Address> getWallets() {
		return arrayDbToList(this.wallets.getEntries());
	}

	private static BigInteger pow(BigInteger base, int exponent) {
		BigInteger result = BigInteger.ONE;
		for (int i = 0; i < exponent; i++) {
			result = result.multiply(base);
		}
		return result;
	}

	private <T> List<T> arrayDbToList(ArrayDB<T> arraydb) {
		@SuppressWarnings("unchecked")
		T[] addressList = (T[])new Object[arraydb.size()];

		for (int i=0; i< arraydb.size(); i++) {
			addressList[i] = arraydb.get(i);
		}
		return List.of(addressList);
	}
}
