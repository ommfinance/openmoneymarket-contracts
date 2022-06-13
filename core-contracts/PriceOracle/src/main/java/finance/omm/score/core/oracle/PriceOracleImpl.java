package finance.omm.score.core.oracle;

import finance.omm.core.score.interfaces.PriceOracle;
import finance.omm.libs.address.AddressProvider;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;

public class PriceOracleImpl extends AddressProvider implements PriceOracle {

    private static final String TAG = "Price Oracle Proxy";

    public PriceOracleImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return null;
    }

    @External
    public void setOMMPool(String _value) {

    }

    @External(readonly = true)
    public String getOMMPool() {
        return null;
    }

    @External(readonly = true)
    public BigInteger get_reference_data(String _base, String _quote) {
        return null;
    }
}
