package finance.omm.score.core.lendingpoolDataProvider;

import finance.omm.libs.address.Contracts;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.score.core.lendingpoolDataProvider.exception.LendingPoolDataProviderException;
import score.Address;

import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import static finance.omm.utils.math.MathUtils.*;

public class LendingPoolDataProviderImpl extends AbstractLendingPoolDataProvider {

    public LendingPoolDataProviderImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setSymbol(Address _reserve, String _sym) {
        onlyOwnerOrElseThrow(LendingPoolDataProviderException.notOwner());
        symbol.set(_reserve, _sym);
    }

    @External(readonly = true)
    public String getSymbol(Address _reserve) {
        return symbol.getOrDefault(_reserve, "");
    }

    @External(readonly = true)
    public String[] getRecipients() {
        return call(String[].class, Contracts.REWARDS, "getRecipients");
    }

    @External(readonly = true)
    @SuppressWarnings("unchecked")
    public Map<String, BigInteger> getDistPercentages() {
        return call(Map.class, Contracts.REWARDS, "getAllDistributionPercentage");
    }

    @External(readonly = true)
    public Map<String, BigInteger> getReserveAccountData() {
        BigInteger todayRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger availableLiquidityBalanceUSD = BigInteger.ZERO;
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        for (Address reserve : reserves) {
            String symbol = this.symbol.get(reserve);
            Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                    "getReserveData", reserve);
            BigInteger reserveDecimals = (BigInteger) reserveData.getOrDefault("decimals", BigInteger.ZERO);
            BigInteger reservePrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                    symbol, "USD");
            if (symbol.equals("ICX")) {
                reservePrice = exaMultiply(reservePrice, todayRate);
            }
            BigInteger reserveTotalLiquidity = (BigInteger) reserveData.getOrDefault("totalLiquidity", BigInteger.ZERO);
            BigInteger reserveAvailableLiquidity = (BigInteger) reserveData.getOrDefault("availableLiquidity", BigInteger.ZERO);
            BigInteger reserveTotalBorrows = (BigInteger) reserveData.getOrDefault("totalBorrows", BigInteger.ZERO);

            if (reserveDecimals.compareTo(BigInteger.valueOf(18)) != 0) {
                reserveTotalLiquidity = convertToExa(reserveTotalLiquidity, reserveDecimals);
                reserveAvailableLiquidity = convertToExa(reserveAvailableLiquidity, reserveDecimals);
                reserveTotalBorrows = convertToExa(reserveAvailableLiquidity, reserveDecimals);
            }
            totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));
            availableLiquidityBalanceUSD = availableLiquidityBalanceUSD.add(exaMultiply(reserveAvailableLiquidity, reservePrice));
            totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply(reserveTotalBorrows, reservePrice));
            Boolean usageAsCollateralEnabled = (Boolean) reserveData.get("usageAsCollateralEnabled");
            if (usageAsCollateralEnabled.equals(true)) {
                totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));
            }

        }
        return Map.of(
                "totalLiquidityBalanceUSD", totalLiquidityBalanceUSD,
                "availableLiquidityBalanceUSD", availableLiquidityBalanceUSD,
                "totalBorrowsBalanceUSD", totalBorrowBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD
        );
    }

    @External(readonly = true)
    public Map<String, Object> getUserAccountData(Address _user) {
        BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger currentLtv = BigInteger.ZERO;
        BigInteger currentLiquidationThreshold = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger totalFeesUSD = BigInteger.ZERO;
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");
        for (Address reserve : reserves) {
            Map<String, BigInteger> userBasicReserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getUserBasicReserveData", reserve, _user);
            if (userBasicReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO).equals(BigInteger.ZERO)
                    && userBasicReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO).equals(BigInteger.ZERO)) {
                continue;
            }
            Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE,
                    "getReserveConfiguration", reserve);
            BigInteger reserveDecimals = (BigInteger) reserveConfiguration.getOrDefault("decimals", BigInteger.ZERO);
            // converting the user balances into 18 decimals
            if (reserveDecimals.compareTo(BigInteger.valueOf(18)) != 0) {
                userBasicReserveData.put("underlyingBalance", convertToExa(userBasicReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO),
                        reserveDecimals));
                userBasicReserveData.put("compoundedBorrowBalance", convertToExa(userBasicReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO),
                        reserveDecimals));
                userBasicReserveData.put("originationFee", convertToExa(userBasicReserveData.getOrDefault("originationFee", BigInteger.ZERO),
                        reserveDecimals));
            }

            String symbol = this.symbol.get(reserve);
            reserveConfiguration.put("reserveUnitPrice", call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                    symbol, "USD"));
            if (symbol.equals("ICX")) {
                reserveConfiguration.put("reserveUnitPrice", exaMultiply((BigInteger) reserveConfiguration.getOrDefault("reserveUnitPrice", BigInteger.ZERO),
                        todaySicxRate));
            }

            if (userBasicReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO).compareTo(BigInteger.ZERO) > 0) {
                BigInteger liquidityBalanceUSD = exaMultiply((BigInteger) reserveConfiguration.getOrDefault("reserveUnitPrice", BigInteger.ZERO),
                        userBasicReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO));
                totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(liquidityBalanceUSD);
                if (reserveConfiguration.get("usageAsCollateralEnabled").equals(true)) {
                    totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(liquidityBalanceUSD);
                    currentLtv = currentLtv.add(exaMultiply(liquidityBalanceUSD, (BigInteger) reserveConfiguration.getOrDefault("baseLTVasCollateral", BigInteger.ZERO)));
                    currentLiquidationThreshold = currentLiquidationThreshold.add(exaMultiply(liquidityBalanceUSD,
                            (BigInteger) reserveConfiguration.getOrDefault("liquidationThreshold", BigInteger.ZERO)));
                }

            }

            if (userBasicReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO).compareTo(BigInteger.ZERO) > 0) {
                totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply((BigInteger) reserveConfiguration.getOrDefault("reserveUnitPrice", BigInteger.ZERO),
                        userBasicReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO)));
                totalFeesUSD = totalFeesUSD.add(exaMultiply((BigInteger) reserveConfiguration.getOrDefault("reserveUnitPrice", BigInteger.ZERO),
                        userBasicReserveData.getOrDefault("originationFee", BigInteger.ZERO)));

            }
        }

        if (totalCollateralBalanceUSD.compareTo(BigInteger.ZERO) > 0) {
            currentLtv = exaDivide(currentLtv, totalCollateralBalanceUSD);
            currentLiquidationThreshold = exaDivide(currentLiquidationThreshold, totalCollateralBalanceUSD);

        } else {
            currentLtv = BigInteger.ZERO;
            currentLiquidationThreshold = BigInteger.ZERO;
        }
        BigInteger healthFactor = calculateHealthFactorFromBalancesInternal(totalCollateralBalanceUSD, totalBorrowBalanceUSD,
                totalFeesUSD, currentLiquidationThreshold);
        Boolean healthFactorBelowThreshold = healthFactor.compareTo(HEALTH_FACTOR_LIQUIDATION_THRESHOLD) < 0 && healthFactor != BigInteger.valueOf(-1);

        BigInteger borrowingPower = calculateBorrowingPowerFromBalancesInternal(totalCollateralBalanceUSD,
                totalBorrowBalanceUSD,
                totalFeesUSD, currentLiquidationThreshold);
        BigInteger borrowsAllowedUSD = exaMultiply(totalCollateralBalanceUSD.subtract(totalFeesUSD), currentLtv);
        BigInteger availableBorrowsUSD = borrowsAllowedUSD.subtract(totalBorrowBalanceUSD);
        if (availableBorrowsUSD.compareTo(BigInteger.ZERO) < 0) {
            availableBorrowsUSD = BigInteger.ZERO;
        }
        return Map.of(
                "totalLiquidityBalanceUSD", totalCollateralBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD,
                "totalBorrowBalanceUSD", totalBorrowBalanceUSD,
                "totalFeesUSD", totalFeesUSD,
                "availableBorrowsUSD", availableBorrowsUSD,
                "currentLtv", currentLtv,
                "currentLiquidationThreshold", currentLiquidationThreshold,
                "healthFactor", healthFactor,
                "borrowingPower", borrowingPower,
                "healthFactorBelowThreshold", healthFactorBelowThreshold
        );

    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user) {
        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", _reserve);
        Map<String, BigInteger> userReserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserReserveData", _reserve, _user);
        BigInteger currentOTokenBalance = call(BigInteger.class, (Address) reserveData.get("oTokenAddress"),
                "balanceOf", _user);
        BigInteger principalOTokenBalance = call(BigInteger.class, (Address) reserveData.get("oTokenAddress"),
                "principalBalanceOf", _user);
        BigInteger userLiquidityCumulativeIndex = call(BigInteger.class, (Address) reserveData.get("oTokenAddress"),
                "getUserLiquidityCumulativeIndex", _user);
        BigInteger principalBorrowBalance = call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "principalBalanceOf", _user);
        BigInteger currentBorrowBalance = call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "balanceOf", _user);
        BigInteger borrowRate = (BigInteger) reserveData.getOrDefault("borrowRate", BigInteger.ZERO);
        BigInteger reserveDecimals = (BigInteger) reserveData.getOrDefault("decimals", BigInteger.ZERO);
        BigInteger liquidityRate = (BigInteger) reserveData.getOrDefault("liquidityRate", BigInteger.ZERO);
        BigInteger originationFee = (BigInteger) reserveData.getOrDefault("originationFee", BigInteger.ZERO);
        BigInteger userBorrowCumulativeIndex = call(BigInteger.class, (Address) reserveData.get("dTokenAddress"),
                "getUserBorrowCumulativeIndex", _user);
        BigInteger lastUpdateTimestamp = (BigInteger) userReserveData.getOrDefault("lastUpdateTimestamp", BigInteger.ZERO);
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                symbol, "USD");
        BigInteger todaySicxRate;
        if (symbol.equals("ICX")) {
            todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        } else {
            todaySicxRate = null;
        }
        BigInteger currentOTokenBalanceUSD = exaMultiply(convertToExa(currentOTokenBalance, reserveDecimals), price);
        BigInteger principalOTokenBalanceUSD = exaMultiply(convertToExa(principalOTokenBalance, reserveDecimals), price);
        BigInteger currentBorrowBalanceUSD = exaMultiply(convertToExa(currentBorrowBalance, reserveDecimals), price);
        BigInteger principalBorrowBalanceUSD = exaMultiply(convertToExa(principalBorrowBalance, reserveDecimals), price);
        Map<String, BigInteger> response = new HashMap<>();
        response.put("currentOTokenBalance", currentOTokenBalance);
        response.put("currentOTokenBalanceUSD", currentOTokenBalanceUSD);
        response.put("principalOTokenBalance", principalOTokenBalance);
        response.put("principalOTokenBalanceUSD", principalOTokenBalanceUSD);
        response.put("currentBorrowBalance", currentBorrowBalance);
        response.put("currentBorrowBalanceUSD", currentBorrowBalanceUSD);
        response.put("principalBorrowBalance", principalBorrowBalance);
        response.put("principalBorrowBalanceUSD", principalBorrowBalanceUSD);
        response.put("userLiquidityCumulativeIndex", userLiquidityCumulativeIndex);
        response.put("borrowRate", borrowRate);
        response.put("liquidityRate", liquidityRate);
        response.put("originationFee", originationFee);
        response.put("userBorrowCumulativeIndex", userBorrowCumulativeIndex);
        response.put("lastUpdateTimestamp", lastUpdateTimestamp);
        response.put("exchangeRate", price);
        response.put("decimals", reserveDecimals);
        if (todaySicxRate != null) {
            response.put("sICXRate", todaySicxRate);
        }
        return response;
    }


    @External(readonly = true)
    public Boolean balanceDecreaseAllowed(Address _reserve, Address _user, BigInteger _amount) {
        Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", _reserve);
        BigInteger reserveLiquidationThreshold = (BigInteger) reserveConfiguration.getOrDefault("liquidationThreshold", BigInteger.ZERO);
        Boolean reserveUsageAsCollateralEnabled = (Boolean) reserveConfiguration.getOrDefault("usageAsCollateralEnabled", BigInteger.ZERO);
        if (reserveUsageAsCollateralEnabled.equals(false)) {
            return true;
        }
        Map<String, Object> userAccountData = getUserAccountData(_user);
        BigInteger collateralBalanceUSD = (BigInteger) userAccountData.getOrDefault("totalCollateralBalanceUSD", BigInteger.ZERO);
        BigInteger borrowBalanceUSD = (BigInteger) userAccountData.getOrDefault("totalBorrowBalanceUSD", BigInteger.ZERO);
        BigInteger totalFeesUSD = (BigInteger) userAccountData.getOrDefault("totalFeesUSD", BigInteger.ZERO);
        BigInteger currentLiquidationThreshold = (BigInteger) userAccountData.getOrDefault("currentLiquidationThreshold", BigInteger.ZERO);
        BigInteger decimals = (BigInteger) reserveConfiguration.getOrDefault("decimals", BigInteger.ZERO);
        if (decimals.compareTo(BigInteger.valueOf(18)) != 0) {
            _amount = convertToExa(_amount, decimals);
        }
        if (borrowBalanceUSD.compareTo(BigInteger.ZERO) == 0) {
            return true;
        }
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                symbol, "USD");
        if (symbol.equals("ICX")) {
            BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        }
        BigInteger amountToDecreaseUSD = exaMultiply(price, _amount);
        BigInteger collateralBalanceAfterDecreaseUSD = collateralBalanceUSD.subtract(amountToDecreaseUSD);
        if (collateralBalanceAfterDecreaseUSD.compareTo(BigInteger.ZERO) == 0) {
            return false;
        }
        BigInteger liquidationThresholdAfterDecrease = exaDivide((exaMultiply(collateralBalanceUSD, currentLiquidationThreshold).subtract(exaMultiply(
                amountToDecreaseUSD, reserveLiquidationThreshold))), collateralBalanceAfterDecreaseUSD);
        BigInteger healthFactorAfterDecrease = calculateHealthFactorFromBalancesInternal(collateralBalanceAfterDecreaseUSD,
                borrowBalanceUSD, totalFeesUSD,
                liquidationThresholdAfterDecrease);
        return healthFactorAfterDecrease.compareTo(HEALTH_FACTOR_LIQUIDATION_THRESHOLD) >= 0;
    }

    @External(readonly = true)
    public BigInteger calculateCollateralNeededUSD(Address _reserve, BigInteger _amount, BigInteger _fee,
                                                   BigInteger _userCurrentBorrowBalanceUSD,
                                                   BigInteger _userCurrentFeesUSD, BigInteger _userCurrentLtv) {
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                symbol, "USD");
        Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", _reserve);
        BigInteger decimals = (BigInteger) reserveConfiguration.getOrDefault("decimals", BigInteger.ZERO);
        if (!decimals.equals(BigInteger.valueOf(18))) {
            _amount = _amount.multiply(ICX);
        }
        if (symbol.equals("ICX")) {
            BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        }
        BigInteger requestedBorrowUSD = exaMultiply(price, _amount);
        BigInteger collateralNeededInUSD = exaDivide(_userCurrentBorrowBalanceUSD.add(requestedBorrowUSD),
                _userCurrentLtv).add(_userCurrentFeesUSD);

        return collateralNeededInUSD;
    }


    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> getUserAllReserveData(Address _user) {
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");

        Map<String, Map<String, BigInteger>> userReserveDetails = new HashMap<>();
        for (Address reserve : reserves) {
            userReserveDetails.put(symbol.get(reserve), getUserReserveData(reserve, _user));
        }
        return userReserveDetails;
    }

    @External(readonly = true)
    public Map<String, Object> getUserLiquidationData(Address _user) {
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");
        Map<String, Object> userAccountData = getUserAccountData(_user);
        BigInteger badDebt = BigInteger.ZERO;
        if (userAccountData.get("healthFactorBelowThreshold").equals(true)) {
            badDebt = call(BigInteger.class, Contracts.LIQUIDATION_MANAGER, "calculateBadDebt",
                    userAccountData.getOrDefault("totalBorrowBalanceUSD", BigInteger.ZERO),
                    userAccountData.getOrDefault("totalFeesUSD", BigInteger.ZERO),
                    userAccountData.getOrDefault("totalCollateralBalanceUSD", BigInteger.ZERO),
                    userAccountData.getOrDefault("currentLtv", BigInteger.ZERO)
            );
        }
        Map<String, Object> borrows = new HashMap<>();
        Map<String, Object> collaterals = new HashMap<>();

        for (Address reserve : reserves) {
            Map<String, BigInteger> userReserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getUserBasicReserveData", reserve, _user);
            Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve);
            BigInteger reserveDecimals = (BigInteger) reserveConfiguration.getOrDefault("decimals", BigInteger.ZERO);
            BigInteger userBorrowBalance = convertToExa(userReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO),
                    reserveDecimals);
            BigInteger userReserveUnderlyingBalance = convertToExa(userReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO),
                    reserveDecimals);
            String symbol = this.symbol.get(reserve);
            BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                    symbol, "USD");
            if (symbol.equals("ICX")) {
                BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
                price = exaMultiply(price, todaySicxRate);
            }
            if (userBorrowBalance.compareTo(BigInteger.ZERO) > 0) {
                BigInteger maxAmountToLiquidateUSD;
                BigInteger maxAmountToLiquidate;
                if (badDebt.compareTo(exaMultiply(price, userBorrowBalance)) > 0) {
                    maxAmountToLiquidateUSD = exaMultiply(price, userBorrowBalance);
                    maxAmountToLiquidate = userReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO);
                } else {
                    maxAmountToLiquidateUSD = badDebt;
                    maxAmountToLiquidate = convertExaToOther(exaDivide(badDebt, price), reserveDecimals.intValue());
                }
                borrows.put(symbol, Map.of(
                        "compoundedBorrowBalance", userReserveData.getOrDefault("compoundedBorrowBalance", BigInteger.ZERO),
                        "compoundedBorrowBalanceUSD", exaMultiply(price, userBorrowBalance),
                        "maxAmountToLiquidate", maxAmountToLiquidate,
                        "maxAmountToLiquidateUSD", maxAmountToLiquidateUSD

                ));
            }
            if (userReserveUnderlyingBalance.compareTo(BigInteger.ZERO) > 0) {
                collaterals.put(symbol, Map.of(
                        "underlyingBalance", userReserveData.getOrDefault("underlyingBalance", BigInteger.ZERO),
                        "underlyingBalanceUSD", exaMultiply(price, userReserveUnderlyingBalance)
                ));
            }
        }
        return Map.of(
                "badDebt", badDebt,
                "borrows", borrows,
                "collaterals", collaterals
        );
    }


    @External(readonly = true)
    public Map<String, Map<String, Object>> liquidationList(BigInteger _index) {
        List<Address> wallets = call(List.class, Contracts.LENDING_POOL_CORE, "getBorrowWallets", _index);
        Map<String, Map<String, Object>> userLiquidationDetails = new HashMap<>();
        for (Address wallet : wallets) {
            BigInteger healthFactor = (BigInteger) getUserAccountData(wallet).get("healthFactor");
            if (healthFactor.compareTo(ICX) < 0) {
                userLiquidationDetails.put(wallet.toString(), getUserLiquidationData(wallet));
            }
        }
        return userLiquidationDetails;
    }

    @External(readonly = true)
    public Map<String, Object> getReserveData(Address _reserve) {
        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", _reserve);
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                symbol, "USD");
        reserveData.put("exchangePrice", price);
        if (symbol.equals("ICX")) {
            BigInteger todayRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            reserveData.put("sICXRate", todayRate);
            price = exaMultiply(todayRate, price);
        }
        BigInteger reserveDecimals = (BigInteger) reserveData.getOrDefault("decimals", BigInteger.ZERO);
        reserveData.put("totalLiquidityUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.getOrDefault("totalLiquidity", BigInteger.ZERO), reserveDecimals),
                        price));
        reserveData.put("availableLiquidityUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.getOrDefault("availableLiquidity", BigInteger.ZERO), reserveDecimals),
                        price));
        reserveData.put("totalBorrowsUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.getOrDefault("totalBorrows", BigInteger.ZERO), reserveDecimals),
                        price));
        BigInteger lendingPercentage = call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", (Address) reserveData.get("oTokenAddress"));
        BigInteger borrowingPercentage = call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage", (Address) reserveData.get("dTokenAddress"));
        reserveData.put("lendingPercentage", lendingPercentage);
        reserveData.put("borrowingPercentage", borrowingPercentage);
        reserveData.put("rewardPercentage", borrowingPercentage.add(lendingPercentage));
        return reserveData;
    }

    @External(readonly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getAllReserveData() {
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");
        Map<String, Map<String, Object>> reserveDetails = new HashMap<>();
        for (Address reserve : reserves) {
            reserveDetails.put(symbol.get(reserve), getReserveData(reserve));
        }
        return reserveDetails;
    }

    @External(readonly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReserveConfigurationData(Address _reserve) {
        return call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", _reserve);
    }

    @External(readonly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getAllReserveConfigurationData() {
        List<Address> reserves = call(List.class, Contracts.LENDING_POOL_CORE, "getReserves");
        Map<String, Map<String, Object>> reserveDetails = new HashMap<>();
        for (Address reserve : reserves) {
            reserveDetails.put(symbol.get(reserve), call(Map.class, Contracts.LENDING_POOL_CORE,
                    "getReserveConfiguration", reserve));
        }
        return reserveDetails;
    }

    @External(readonly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, BigInteger>> getUserUnstakeInfo(Address _address) {
        List<Map<String, Object>> unstakeDetails = call(List.class, Contracts.STAKING, "getUserUnstakeInfo", _address);
        List<Map<String, BigInteger>> response = new ArrayList<>();
        for (Map<String, Object> unstakedRecords : unstakeDetails) {
            Address fromAddress = (Address) unstakedRecords.get("from");
            if (fromAddress.equals(getAddress(String.valueOf(Contracts.LENDING_POOL_CORE)))) {
                response.add(Map.of(
                        "amount", (BigInteger) unstakedRecords.get("amount"),
                        "unstakingBlockHeight", (BigInteger) unstakedRecords.get("blockHeight")
                ));
            }
        }
        return response;
    }

    @External(readonly = true)
    public BigInteger getLoanOriginationFeePercentage() {
        return call(BigInteger.class, Contracts.FEE_PROVIDER, "getLoanOriginationFeePercentage");
    }

    @External(readonly = true)
    public BigInteger getRealTimeDebt(Address _reserve, Address _user) {
        Map<String, BigInteger> userReserveData = getUserReserveData(_reserve, _user);
        return userReserveData.getOrDefault("currentBorrowBalance", BigInteger.ZERO)
                .add(userReserveData.getOrDefault("originationFee", BigInteger.ZERO));
    }
}
