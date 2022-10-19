package finance.omm.score.core.oracle;

import finance.omm.core.score.interfaces.PriceOracle;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.oracle.token.BaseToken;
import finance.omm.score.core.oracle.token.Token;
import finance.omm.score.core.oracle.token.sICXToken;
import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.exaMultiply;

public class PriceOracleImpl extends AddressProvider implements PriceOracle {

    private static final String TAG = "Price Oracle Proxy";

    private static final String OMM_POOL = "ommPool";
    private static final String BALN = "BALN";
    private static final String OMM = "OMM";

    private static final BigInteger ONE_USD = ICX;

    private final VarDB<String> ommPool = Context.newVarDB(OMM_POOL, String.class);

    private final String[] STABLE_TOKENS = {"USDS", "bnUSD"};


    public PriceOracleImpl(Address addressProvider) {
        super(addressProvider, false);
        if (ommPool.get() == null) {
            ommPool.set(OMM);
        }
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void setOMMPool(String _value) {
        checkOwner();
        ommPool.set(_value);
    }

    @External(readonly = true)
    public String getOMMPool() {
        return ommPool.get();
    }

    private BigInteger getPrice(String _base, String _quote) {
        for (String token : STABLE_TOKENS) {
            if (token.equals(_base)) {
                return ONE_USD;
            }
        }
        if (_base.equals(BALN)) {
            return call(BigInteger.class, Contracts.BALANCED_ORACLE, "getPriceInLoop",_base);
        } else {
            Map<String, BigInteger> price = call(Map.class, Contracts.BAND_ORACLE, "get_reference_data", _base, _quote);
            return price.get("rate");
        }
    }

    private BigInteger getOmmPrice(String _quote) {
        BigInteger totalPrice = BigInteger.ZERO;
        BigInteger totalOmmSupply = BigInteger.ZERO;

        Address dex = getAddress(Contracts.DEX.getKey());

        Token[] tokens = new Token[]{
                new BaseToken("USDS", "USDS"),
                new sICXToken("sICX", "ICX", dex),
                new BaseToken("IUSDC", "USDC")
        };

        for (Token token : tokens) {
            String name = token.getName();
            String priceOracleKey = token.getPriceOracleKey();

            String _name = getOMMPool() + "/" + name;
            BigInteger poolId = call(BigInteger.class, Contracts.DEX, "lookupPid", _name);
            if (poolId == null || poolId.equals(BigInteger.ZERO)) {
                continue;
            }
            Map<String, Object> poolStats = call(Map.class, Contracts.DEX, "getPoolStats", poolId);

            BigInteger price = (BigInteger) poolStats.get("price");
            BigInteger quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
            BigInteger baseDecimals = (BigInteger) poolStats.get("base_decimals");
            BigInteger averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);

            BigInteger adjustedPrice = token.convert(price, averageDecimals);
            BigInteger convertedPrice = exaMultiply(adjustedPrice, getPrice(priceOracleKey, _quote));

            BigInteger totalSupply = (BigInteger) poolStats.get("base");

            totalOmmSupply = totalOmmSupply.add(totalSupply);
            totalPrice = totalPrice.add(totalSupply.multiply(convertedPrice));
        }

        if (totalOmmSupply.equals(BigInteger.ZERO)) {
            return BigInteger.ONE.negate();
        }

        return totalPrice.divide(totalOmmSupply);
    }

    @External(readonly = true)
    public BigInteger get_reference_data(String _base, String _quote) {
        if (_base.equals(OMM)) {
            return getOmmPrice(_quote);
        } else {
            return getPrice(_base, _quote);
        }
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw OMMException.unknown("require owner access");
        }
    }
}
