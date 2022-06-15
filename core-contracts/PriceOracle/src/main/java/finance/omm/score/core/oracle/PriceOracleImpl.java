package finance.omm.score.core.oracle;

import finance.omm.core.score.interfaces.PriceOracle;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.oracle.exception.PriceOracleException;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.pow10;

public class PriceOracleImpl extends AddressProvider implements PriceOracle {

    private static final String TAG = "Price Oracle Proxy";

    private static final String OMM_POOL = "ommPool";
    private static final String BALN = "BALN";
    private static final String OMM = "OMM";

    private VarDB<String> ommPool = Context.newVarDB(OMM_POOL,String.class);

    private final String[] STABLE_TOKEN = {"USDS","bnUSD"};


    public PriceOracleImpl(Address addressProvider) {
        super(addressProvider, false);
        ommPool.set(OMM);
    }

    @External(readonly = true)
    public String name() {
        return "Omm" + TAG;
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
            return getPrice(_base,_quote);
        }
    }

    private BigInteger getPrice(String _base, String _quote){
        for (String tokens : STABLE_TOKEN) {
            if (tokens.equals(_base)) {
                return BigInteger.ONE.multiply(pow10(18));
            }
        }
        if (_base.equals(BALN)){
            // will the returned value be biginter or integer
            return call(BigInteger.class,Contracts.DEX,"getBalnPrice");
        }
        else {
            Map<String, BigInteger> price = call(Map.class,Contracts.BAND_ORACLE,
                    "get_reference_data",_base,_quote);
            return price.get("rate");
        }

    }

    private BigInteger getOmmPrice(String _quote){
        BigInteger totalPrice = BigInteger.ZERO;
        BigInteger totalOmmSupply = BigInteger.ZERO;

        for (String[] token: Tokens ) {
            String name = token["name"];
            String priceOracleKey = token["priceOracleKey"];

            String _name = getOMMPool() + "/" + name;
            Integer poolId = call(Integer.class,Contracts.DEX,"lookupPid",_name);
            if (!(poolId == null)){ // _pool_id == 0
                continue;
            }
            Map<String , BigInteger> poolStats = call(Map.class,Contracts.DEX,"getPoolStats",poolId);

            BigInteger price = poolStats.get("price");
            BigInteger quoteDecimals = poolStats.get("quote_decimals");
            BigInteger baseDecimals = poolStats.get("base_decimals");
            BigInteger averageDecimals = quoteDecimals.multiply(BigInteger.valueOf(18)).divide(baseDecimals);

            // convert method from the OMM TOKENS
            BigInteger adjustedPrice = BigInteger.ZERO;
            BigInteger convertToExa = convertToExa(price,averageDecimals);

            if (name.equals("sICX") && priceOracleKey.equals("ICX")){
                BigInteger icxPrice = call(BigInteger.class,Contracts.DEX,"getPriceByName","sICX/ICX");
                adjustedPrice = exaMultiply(convertToExa,icxPrice);
            }
            else{
                adjustedPrice = convertToExa;
            }

            BigInteger convertedPrice = exaMultiply(adjustedPrice,getPrice(priceOracleKey,_quote));
            // till here

            BigInteger totalSupply = poolStats.get("base");

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
