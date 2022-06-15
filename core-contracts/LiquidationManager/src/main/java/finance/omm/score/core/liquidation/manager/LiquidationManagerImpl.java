package finance.omm.score.core.liquidation.manager;

import finance.omm.core.score.interfaces.LiquidationManager;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.convertExaToOther;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static java.math.BigInteger.ZERO;

public class LiquidationManagerImpl extends AddressProvider implements LiquidationManager {

    public static final String TAG = "Liquidation Manager";
    public static final BigInteger ZERO = BigInteger.ZERO;

    @EventLog(indexed = 3)
    public void OriginationFeeLiquidated(Address _collateral, Address _reserve, Address _user,
                                         BigInteger _feeLiquidated, BigInteger _liquidatedCollateralForFee){

    }

    @EventLog(indexed = 3)
    public void LiquidationCall(Address _collateral, Address _reserve, Address _user, BigInteger _purchaseAmount,
                                BigInteger _liquidatedCollateralForFee, BigInteger _accruedBorrowInterest,
                                Address _liquidator){

    }
    public LiquidationManagerImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "Omm " +TAG;
    }

    @External(readonly = true)
    public BigInteger calculateBadDebt(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger _totalCollateralBalanceUSD, BigInteger _ltv) {
        BigInteger badDebtUSD = _totalBorrowBalanceUSD.subtract(exaMultiply(_totalCollateralBalanceUSD.subtract(_totalFeesUSD),_ltv));

        if (badDebtUSD.compareTo(ZERO)<0){
            badDebtUSD = ZERO;
        }
        return badDebtUSD;
    }

    public Map<String, BigInteger> calculateAvailableCollateralToLiquidate(Address _collateral, Address _reserve, BigInteger _purchaseAmount, BigInteger _userCollateralBalance, Boolean _fee) {
        BigInteger liquidationBonus;
        if (_fee){
            liquidationBonus = ZERO;
        }
        else {
            Map<String, BigInteger> collateralConfigs = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getReserveConfigurationData",_collateral);
            liquidationBonus = collateralConfigs.getOrDefault("getSymbol",ZERO);
        }
        String collateralBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_collateral);
        String principalBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_reserve);

        BigInteger collateralPrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",collateralBase,"USD");
        BigInteger principalPrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",principalBase,"USD");

        if (collateralBase == "ICX"){
            BigInteger sicxRate = call(BigInteger.class,Contracts.STAKING,"getTodayRate");
            collateralPrice = exaMultiply(collateralPrice,sicxRate);
        }

        if (principalBase == "ICX"){
            BigInteger sicxRate = call(BigInteger.class,Contracts.STAKING,"getTodayRate");
            collateralPrice = exaMultiply(principalPrice,sicxRate);
        }

        Map<String, BigInteger> reserveConfiguration = call(Map.class,Contracts.LENDING_POOL_CORE,"getReserveConfiguration",_reserve);
        BigInteger reserveDecimals = reserveConfiguration.get("decimals");
        reserveConfiguration = call(Map.class,Contracts.LENDING_POOL_CORE,"getReserveConfiguration",_collateral);
        BigInteger collateralDecimals = reserveConfiguration.get("decimals");

        BigInteger userCollateralUSD = exaMultiply(convertToExa(_userCollateralBalance, collateralDecimals), collateralPrice);
        BigInteger purchaseAmountUSD = exaMultiply(convertToExa(_purchaseAmount, reserveDecimals), principalPrice);

        BigInteger maxCollateralToLiquidate = convertExaToOther(
                exaDivide(exaMultiply(purchaseAmountUSD, ICX.add(liquidationBonus)), collateralPrice), collateralDecimals.intValue());

        BigInteger collateralAmount, principalAmountNeeded;
        if (maxCollateralToLiquidate.compareTo(_userCollateralBalance)>0){
            collateralAmount = _userCollateralBalance;
            principalAmountNeeded = convertExaToOther(
                    exaDivide(exaDivide(userCollateralUSD, ICX.add(liquidationBonus)), principalPrice), reserveDecimals.intValue());

        }
        else {
            collateralAmount = maxCollateralToLiquidate;
            principalAmountNeeded = _purchaseAmount;
        }

        return Map.of("collateralAmount", collateralAmount,
                "principalAmountNeeded", principalAmountNeeded);

    }

    public static BigInteger calculateCurrentLiquidationThreshold(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger _totalCollateralBalanceUSD){
        if (_totalCollateralBalanceUSD.compareTo(ZERO)==0){
            return ZERO;
        }
        return exaDivide(_totalBorrowBalanceUSD,_totalCollateralBalanceUSD.subtract(_totalFeesUSD));
    }

    @External
    public Map<String, BigInteger> liquidationCall(Address _collateral, Address _reserve, Address _user, BigInteger _purchaseAmount) {
        String principalBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_reserve);
        BigInteger principalPrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",principalBase,"USD");
        Map<String, BigInteger> userAccountData = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getUserAccountData",_user);
        Map<String, BigInteger> collateralData = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getReserveData",_collateral);

        BigInteger liquidatedCollateralForFee = ZERO;
        BigInteger feeLiquidated = ZERO;


        return null;
    }
}