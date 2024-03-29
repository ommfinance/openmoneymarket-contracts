package finance.omm.score;

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;

import finance.omm.core.score.interfaces.WorkerToken;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.utils.db.EnumerableSet;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

public class WorkerTokenImpl extends AddressProvider implements WorkerToken {

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

    public WorkerTokenImpl(Address _addressProvider, BigInteger _initialSupply, BigInteger _decimals) {
        super(_addressProvider, false);
        if (this.totalSupply.get() == null) {
            Context.println(TAG + "| on install event:" + Context.getAddress());
            if (_initialSupply == null || _initialSupply.compareTo(ZERO) < 0) {
                Context.revert("Initial supply cannot be less than zero");
            }

            if (_decimals == null || _decimals.compareTo(ZERO) < 0) {
                Context.revert("Decimals cannot be less than zero");
            }

            BigInteger totalSupply = _initialSupply.multiply(MathUtils.pow(TEN, _decimals.intValue()));
            Context.println(TAG + "| total_supply " + totalSupply);

            this.totalSupply.set(totalSupply);
            this.decimals.set(_decimals);
            this.balances.set(Context.getCaller(), totalSupply);

            Context.println(TAG + "| Started Worker Token:" + Context.getAddress());
        } else {
            onUpdate();
        }

    }

    public void onUpdate() {
        Context.println(TAG + "| on update event:" + Context.getAddress());
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value,
            byte[] _data) {
    }

    @EventLog(indexed = 2)
    public void Distribution(String _recipient, Address _user, BigInteger _value) {}

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External(readonly = true)
    public String symbol() {
        return WORKER_TOKEN_SYMBOL;
    }

    @External(readonly = true)
    public BigInteger decimals() {
        return this.decimals.get();
    }

    @External(readonly = true)
    public BigInteger totalSupply() {
        return this.totalSupply.get();
    }

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        return this.balances.getOrDefault(_owner, ZERO);
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        if (_data == null || _data.length == 0) {
            _data = "None".getBytes();
        }

        this.wallets.add(_to);
        this._transfer(Context.getCaller(), _to, _value, _data);
    }

    public void _transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {

        // Checks the sending value and balance.
        if (_value.compareTo(ZERO) <= 0) {
            Context.revert(TAG + ": Transferring value should be greater than zero");
        }

        BigInteger balanceFrom = this.balances.getOrDefault(_from, ZERO);
        Context.println(TAG + "| transferring: " + _value + " - from sender balance: " + balanceFrom);

        if (balanceFrom.compareTo(_value) < 0) {
            Context.revert(TAG + " : Out of balance: " + _value);
        }

        this.balances.set(_from, balanceFrom.subtract(_value));
        this.balances.set(_to, this.balances.getOrDefault(_to, ZERO).add(_value));

        if (this.balanceOf(_from).equals(ZERO)) {
            this.wallets.remove(_from);
        }

        // Emits an event log `Transfer`
        this.Transfer(_from, _to, _value, _data);

        if (_to.isContract()) {
            // If the recipient is SCORE,
            // then calls `tokenFallback` to hand over control.
            Context.call(_to, "tokenFallback", _from, _value, _data);
        }
    }

    @External(readonly = true)
    public List<Address> getWallets() {
        return this.wallets.toList();
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        Context.require(_from.equals(getAddress(Contracts.REWARDS.getKey())),
                TAG + " Only rewards");
        Context.require(Context.getCaller().equals(getAddress(Contracts.OMM_TOKEN.getKey())),
                TAG + " Only OMM Token can be distributed to workers.");

        List<Address> workers = getWallets();
        BigInteger totalSupply = totalSupply();

        BigInteger remaining = _value;

        for (Address worker : workers) {
            BigInteger balance = balanceOf(worker);
            BigInteger share = balance.multiply(remaining).divide(totalSupply);
            call(Contracts.OMM_TOKEN, "transfer", worker, share);
            Distribution("worker", worker, share);
            remaining = remaining.subtract(share);
            totalSupply = totalSupply.subtract(balance);

            if (totalSupply.equals(BigInteger.ZERO) || remaining.equals(BigInteger.ZERO)) {
                break;
            }
        }
    }
}
