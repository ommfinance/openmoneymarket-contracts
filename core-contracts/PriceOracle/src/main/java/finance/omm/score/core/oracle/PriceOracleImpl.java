package finance.omm.score.core.oracle;

import finance.omm.core.score.interfaces.PriceOracle;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.oracle.exception.PriceOracleException;
import finance.omm.score.core.oracle.token.Token;
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

    private VarDB<String> ommPool = Context.newVarDB(OMM_POOL,String.class);

    private final String[] STABLE_TOKENS = {"USDS","bnUSD"};
    private static final Token[] TOKENS = new Token[] {
            new Token("USDS","USDS"),
            new Token("sICX","ICX"),
            new Token("IUSDC","USDC")
    };


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

    @External(readonly = true)
    public BigInteger get_reference_data(String _base, String _quote) {
        if (_base.equals(OMM)){
            return getOmmPrice(_quote);
        }
        else {
            return getPrice(_base, _quote);
        }
    }

    private BigInteger getPrice(String _base, String _quote){
        for (String token : STABLE_TOKENS) {
            if (token.equals(_base)) {
                return ICX;
            }
        }
        if (_base.equals(BALN)){
            return call(BigInteger.class, Contracts.DEX,"getBalnPrice");
        } else {
            Map<String, BigInteger> price = call(Map.class, Contracts.BAND_ORACLE,
                    "get_reference_data",_base,_quote);
            return price.getOrDefault("rate", BigInteger.ZERO);
        }
    }

    private BigInteger getOmmPrice(String _quote){
        BigInteger totalPrice = BigInteger.ZERO;
        BigInteger totalOmmSupply = BigInteger.ZERO;

        BigInteger price;
        BigInteger quoteDecimals;
        BigInteger baseDecimals;
        BigInteger averageDecimals;
        BigInteger adjustedPrice;
        BigInteger convertedPrice;
        BigInteger totalSupply;

        Address dex = getAddress(Contracts.DEX.getKey());

        for (Token token: TOKENS ) {
            String name = token.name;
            String priceOracleKey = token.priceOracleKey;

            String _name = getOMMPool() + "/" + name;
            BigInteger poolId = call(BigInteger.class, Contracts.DEX,"lookupPid", _name);
            if (poolId == null) {
                continue;
            }
            Map<String, Object> poolStats = call(Map.class, Contracts.DEX,"getPoolStats",poolId);

            price = (BigInteger) poolStats.get("price");
            quoteDecimals = (BigInteger) poolStats.get("quote_decimals");
            baseDecimals = (BigInteger) poolStats.get("base_decimals");
            averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);

            adjustedPrice = token.convert(dex, price, averageDecimals);
            convertedPrice = exaMultiply(adjustedPrice, getPrice(priceOracleKey, _quote));

            totalSupply = (BigInteger) poolStats.get("base");

            totalOmmSupply = totalOmmSupply.add(totalSupply);
            totalPrice = totalPrice.add(totalSupply.multiply(convertedPrice));
        }

        if ((totalOmmSupply.compareTo(BigInteger.ZERO)) == 0){
            return BigInteger.ONE.negate();
        }

        return totalPrice.divide(totalOmmSupply);
    }

    private void checkOwner() {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw PriceOracleException.notOwner();
        }
    }
}
