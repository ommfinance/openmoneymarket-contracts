package finance.omm.score.core.lendingpoolDataProvider;

import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.core.score.interfaces.LendingPoolDataProvider;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;

import java.math.BigInteger;

import finance.omm.score.core.lendingpoolDataProvider.exception.LendingPoolDataProviderException;
import score.Address;
import score.Context;
import score.DictDB;

import static finance.omm.utils.math.MathUtils.ICX;

public abstract class AbstractLendingPoolDataProvider extends AddressProvider
        implements LendingPoolDataProvider, Authorization<LendingPoolDataProviderException> {

    public static final String TAG = "Lending Pool Data Provider";
    public static BigInteger HEALTH_FACTOR_LIQUIDATION_THRESHOLD = ICX;

    public static final String SYMBOL = "symbol";

    public final DictDB<Address, String> symbol = Context.newDictDB(SYMBOL, String.class);

    public AbstractLendingPoolDataProvider(Address addressProvider, boolean _update) {
        super(addressProvider, _update);
    }

    protected BigInteger calculateHealthFactorFromBalancesInternal(BigInteger collateralBalanceUSD, BigInteger borrowBalanceUSD,
                                                                   BigInteger totalFeesUSD, BigInteger liquidationThreshold) {
        if (borrowBalanceUSD.equals(BigInteger.ZERO)) {
            return BigInteger.ONE.negate();
        }

        return exaDivide(exaMultiply(collateralBalanceUSD.subtract(totalFeesUSD), liquidationThreshold), borrowBalanceUSD);
    }

    protected BigInteger calculateBorrowingPowerFromBalancesInternal(BigInteger collateralBalanceUSD, BigInteger borrowBalanceUSD,
                                                                     BigInteger totalFeesUSD, BigInteger ltv) {
        if (collateralBalanceUSD.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }
        return exaDivide(borrowBalanceUSD, exaMultiply(collateralBalanceUSD.subtract(totalFeesUSD), ltv));
    }

    public <K> K call(Class<K> kClass, Address contract, String method, Object... params) {
        return Context.call(kClass, contract, method, params);
    }
}
