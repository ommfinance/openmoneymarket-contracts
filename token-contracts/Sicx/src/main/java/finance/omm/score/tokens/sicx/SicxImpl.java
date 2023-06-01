package finance.omm.score.tokens.sicx;

import finance.omm.core.score.interfaces.Sicx;
import finance.omm.core.score.tokens.IRC2Burnable;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

import static finance.omm.utils.checks.Check.onlyOwner;

public class SicxImpl extends IRC2Burnable implements Sicx {
    private static final String TAG = "sICX";
    private static final String TOKEN_NAME = "Staked ICX";
    private static final String SYMBOL_NAME = "sICX";
    private static final BigInteger DECIMALS = BigInteger.valueOf(18);
    private static final String STAKING = "staking";
    public static final String STATUS_MANAGER = "status_manager";
    private static final String VERSION = "version";
    private static final String SICX_VERSION = "v1.0.0";

    private static final VarDB<Address> stakingAddress = Context.newVarDB(STAKING, Address.class);
    private final VarDB<Address> statusManager = Context.newVarDB(STATUS_MANAGER, Address.class);

    private final VarDB<String> currentVersion = Context.newVarDB(VERSION, String.class);

    public SicxImpl(Address _admin) {
        super(TOKEN_NAME, SYMBOL_NAME, DECIMALS);
        if (stakingAddress.get() == null) {
            stakingAddress.set(_admin);
        }

        if (currentVersion.getOrDefault("").equals(SICX_VERSION)) {
            Context.revert("Can't Update same version of code");
        }
        currentVersion.set(SICX_VERSION);
    }

    @External(readonly = true)
    public String version() {
        return currentVersion.getOrDefault("");
    }

    @External(readonly = true)
    public String getPeg() {
        return TAG;
    }

    @External
    public void setStaking(Address _address) {
        onlyOwner();
        stakingAddress.set(_address);
    }

    @External(readonly = true)
    public Address getStaking() {
        return stakingAddress.get();
    }

    @External
    public void setEmergencyManager(Address _address) {
        onlyOwner();
        statusManager.set(_address);
    }

    @External(readonly = true)
    public Address getEmergencyManager() {
        return statusManager.get();
    }

    @External(readonly = true)
    public BigInteger priceInLoop() {
        return (BigInteger) Context.call(stakingAddress.get(), "getTodayRate");
    }

    @External(readonly = true)
    public BigInteger lastPriceInLoop() {
        return priceInLoop();
    }

    @Override
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        if (!_to.equals(stakingAddress.get())) {
            Context.call(stakingAddress.get(), "transferUpdateDelegations", Context.getCaller(), _to, _value);
        }
        transfer(Context.getCaller(), _to, _value, _data);
    }


}
