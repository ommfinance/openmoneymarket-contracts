package finance.omm.score.core.liquidation.manager;

import finance.omm.core.score.interfaces.LiquidationManager;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.liquidation.manager.exception.LiquidationManagerException;
import score.Address;
import score.Context;
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

public class LiquidationManagerImpl extends AddressProvider implements LiquidationManager,
        Authorization<LiquidationManagerException> {

    public static final String TAG = "Liquidation Manager";

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
    public BigInteger calculateBadDebt(BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD,
                                       BigInteger _totalCollateralBalanceUSD, BigInteger _ltv) {
        BigInteger badDebtUSD = _totalBorrowBalanceUSD.subtract(
                exaMultiply(_totalCollateralBalanceUSD.subtract(_totalFeesUSD),_ltv));

        if (badDebtUSD.compareTo(ZERO)<0){
            badDebtUSD = ZERO;
        }
        return badDebtUSD;
    }

    public Map<String, BigInteger> calculateAvailableCollateralToLiquidate(
            Address _collateral, Address _reserve, BigInteger _purchaseAmount, BigInteger _userCollateralBalance,
            Boolean _fee) {
        BigInteger liquidationBonus;
        if (_fee){
            liquidationBonus = ZERO;
        }
        else {
            Map<String, BigInteger> collateralConfigs = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                    "getReserveConfigurationData",_collateral);
            liquidationBonus = collateralConfigs.get("liquidationBonus");
        }
        String collateralBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_collateral);
        String principalBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_reserve);

        BigInteger collateralPrice = call(BigInteger.class, Contracts.PRICE_ORACLE,
                "get_reference_data",collateralBase,"USD");
        BigInteger principalPrice = call(BigInteger.class, Contracts.PRICE_ORACLE,
                "get_reference_data",principalBase,"USD");

        if (collateralBase.equals("ICX")){
            BigInteger sicxRate = call(BigInteger.class,Contracts.STAKING,"getTodayRate");
            collateralPrice = exaMultiply(collateralPrice,sicxRate);
        }

        if (principalBase.equals("ICX")){
            BigInteger sicxRate = call(BigInteger.class,Contracts.STAKING,"getTodayRate");
            collateralPrice = exaMultiply(principalPrice,sicxRate);
        }

        Map<String, BigInteger> reserveConfiguration = call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",_reserve);
        BigInteger reserveDecimals = reserveConfiguration.get("decimals");
        reserveConfiguration = call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",_collateral);
        BigInteger collateralDecimals = reserveConfiguration.get("decimals");

        BigInteger userCollateralUSD = exaMultiply(
                convertToExa(_userCollateralBalance, collateralDecimals), collateralPrice);
        BigInteger purchaseAmountUSD = exaMultiply(convertToExa(_purchaseAmount, reserveDecimals), principalPrice);

        BigInteger maxCollateralToLiquidate = convertExaToOther(
                exaDivide(exaMultiply(purchaseAmountUSD, ICX.add(liquidationBonus)), collateralPrice),
                collateralDecimals.intValue());

        BigInteger collateralAmount, principalAmountNeeded;
        if (maxCollateralToLiquidate.compareTo(_userCollateralBalance)>0){
            collateralAmount = _userCollateralBalance;
            principalAmountNeeded = convertExaToOther(
                    exaDivide(exaDivide(userCollateralUSD, ICX.add(liquidationBonus)), principalPrice),
                    reserveDecimals.intValue());

        }
        else {
            collateralAmount = maxCollateralToLiquidate;
            principalAmountNeeded = _purchaseAmount;
        }

        return Map.of("collateralAmount", collateralAmount,
                "principalAmountNeeded", principalAmountNeeded);

    }

    public static BigInteger calculateCurrentLiquidationThreshold(
            BigInteger _totalBorrowBalanceUSD, BigInteger _totalFeesUSD, BigInteger _totalCollateralBalanceUSD){
        if (_totalCollateralBalanceUSD.compareTo(ZERO)==0){
            return ZERO;
        }
        return exaDivide(_totalBorrowBalanceUSD,_totalCollateralBalanceUSD.subtract(_totalFeesUSD));
    }

    @External
    public Map<String, BigInteger> liquidationCall(
            Address _collateral, Address _reserve, Address _user, BigInteger _purchaseAmount) {

        onlyContractOrElseThrow(Contracts.LENDING_POOL,
                LiquidationManagerException.unauthorized(TAG+ ": SenderNotLendingPoolError: (sender)" +
                        Context.getCaller() + " (lending pool)"+ getAddress(Contracts.LENDING_POOL.getKey())));

        String principalBase = call(String.class,Contracts.LENDING_POOL_DATA_PROVIDER, "getSymbol",_reserve);
        BigInteger principalPrice = call(BigInteger.class, Contracts.PRICE_ORACLE,
                "get_reference_data",principalBase,"USD");
        Map<String, BigInteger> userAccountData = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                "getUserAccountData",_user);
        Map<String, BigInteger> collateralData = call(Map.class,Contracts.LENDING_POOL_DATA_PROVIDER,
                "getReserveData",_collateral);

        BigInteger liquidatedCollateralForFee = ZERO;
        BigInteger feeLiquidated = ZERO;

        if (!(collateralData.containsKey("usageAsCollateralEnabled"))){
            Context.revert(TAG + ": the reserve " + _collateral + " cannot be used as collateral");
        }
        BigInteger userHealthFactor = userAccountData.get("healthFactor");
        if (!(userAccountData.containsKey("healthFactorBelowThreshold"))){
            Context.revert(TAG + ": unsuccessful liquidation call,health factor of user is above 1" +
                    "health factor of user " + userHealthFactor);
        }

        BigInteger userCollateralBalance = call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserUnderlyingAssetBalance",_collateral,_user);
        if (userCollateralBalance == null){
            Context.revert(TAG + ": unsuccessful liquidation call,user have no collateral balance" +
                    "for collateral" + _collateral + "balance of user: " + _user + " is " + userCollateralBalance);
        }

        Map<String, BigInteger> userBorrowBalances = call(Map.class,Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances",_reserve,_user);
        if (!(userBorrowBalances.containsKey("compoundedBorrowBalance"))){
            Context.revert(TAG +": unsuccessful liquidation call,user have no borrow balance"+
                    "for reserve" + _reserve + "borrow balance of user: " + _user + " is " + userBorrowBalances);
        }
        BigInteger maxPrincipalAmountToLiquidateUSD = calculateBadDebt(userAccountData.get("totalBorrowBalanceUSD"),
                                                                        userAccountData.get("totalFeesUSD"),
                                                                        userAccountData.get("totalCollateralBalanceUSD"),
                                                                        userAccountData.get("currentLtv"));
        BigInteger maxPrincipalAmountToLiquidate = exaDivide(maxPrincipalAmountToLiquidateUSD,principalPrice);
//
        Map<String, ?> reserveConfiguration = call(Map.class,Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration",_reserve);
        Integer reserveDecimals = (Integer) reserveConfiguration.get("decimals");

        // converting the user balances into 18 decimals
        if (reserveDecimals != 18) {
           maxPrincipalAmountToLiquidate = convertExaToOther(maxPrincipalAmountToLiquidate,reserveDecimals);
        }
        BigInteger actualAmountToLiquidate = ZERO;
        if (_purchaseAmount.compareTo(maxPrincipalAmountToLiquidate) >0){
            actualAmountToLiquidate = maxPrincipalAmountToLiquidate;
        }
        else {
            actualAmountToLiquidate = _purchaseAmount;
        }
//
        Map<String,BigInteger> liquidationDetails= calculateAvailableCollateralToLiquidate(
                _collateral,_reserve,actualAmountToLiquidate,userCollateralBalance,false);
        BigInteger maxCollateralToLiquidate = liquidationDetails.get("'collateralAmount'");
        BigInteger principalAmountNeeded = liquidationDetails.get("principalAmountNeeded");
        BigInteger userOriginationFee = call(BigInteger.class,Contracts.LENDING_POOL_CORE,
                "getUserOriginationFee",_reserve,_user);
        if (userOriginationFee.compareTo(ZERO) > 0){
            Map<String, BigInteger> feeLiquidationDetails = calculateAvailableCollateralToLiquidate(
                    _collateral, _reserve, userOriginationFee, userCollateralBalance.subtract(maxCollateralToLiquidate),
                    true);
           liquidatedCollateralForFee = feeLiquidationDetails.get("collateralAmount");
           feeLiquidated = feeLiquidationDetails.get("principalAmountNeeded");
        }

        if (principalAmountNeeded.compareTo(actualAmountToLiquidate)<0){
            actualAmountToLiquidate = principalAmountNeeded;
        }
        call(Contracts.LENDING_POOL_CORE,"updateStateOnLiquidation",
                _reserve, _collateral, _user, actualAmountToLiquidate, maxCollateralToLiquidate,
                feeLiquidated, liquidatedCollateralForFee, userBorrowBalances.get("borrowBalanceIncrease"));

        Address collateralOtokenAddress = call(Address.class,Contracts.LENDING_POOL_CORE,
                "getReserveOTokenAddress",_collateral);

        call(collateralOtokenAddress,"burnOnLiquidation",_user,maxCollateralToLiquidate);

        if (feeLiquidated.compareTo(ZERO) >0){
            call(collateralOtokenAddress,"burnOnLiquidation",_user,liquidatedCollateralForFee);
            call(Contracts.LENDING_POOL_CORE,"liquidateFee",_collateral, liquidatedCollateralForFee,
                    getAddress(Contracts.FEE_PROVIDER.getKey()));

            OriginationFeeLiquidated(_collateral, _reserve, _user, feeLiquidated, liquidatedCollateralForFee);
        }

        LiquidationCall(_collateral, _reserve, _user, actualAmountToLiquidate, maxCollateralToLiquidate,
                userBorrowBalances.get("borrowBalanceIncrease"), Context.getOrigin());

        return Map.of(
                "maxCollateralToLiquidate",maxCollateralToLiquidate,
                "actualAmountToLiquidate",actualAmountToLiquidate );
    }
}