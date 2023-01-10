package finance.omm.score.core.lendingpoolDataProvider;

import static finance.omm.utils.math.MathUtils.EIGHTEEN;
import static finance.omm.utils.math.MathUtils.convertExaToOther;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;

import finance.omm.libs.address.Contracts;
import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.HashMap;

public class LendingPoolDataProviderImpl extends AbstractLendingPoolDataProvider {

    public LendingPoolDataProviderImpl(Address addressProvider) {
        super(addressProvider);
    }

    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void setSymbol(Address _reserve, String _sym) {
        onlyOwnerOrElseThrow(OMMException.unknown("Not an owner"));
        symbol.set(_reserve, _sym);
    }

    @External(readonly = true)
    public String getSymbol(Address _reserve) {
        return symbol.get(_reserve);
    }

    /**
     * Call getTypes of reward weight controller
     */
    @External(readonly = true)
    public String[] getRecipients() {
        return call(String[].class, Contracts.REWARDS, "getRecipients");
    }

    /**
     * Call getAllTypeWeight of reward weight controller
     */
    @External(readonly = true)
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
            Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", reserve);
            BigInteger reserveDecimals = (BigInteger) reserveData.get("decimals");
            BigInteger reservePrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol,
                    "USD");
            if (symbol.equals("ICX")) {
                reservePrice = exaMultiply(reservePrice, todayRate);
            }
            BigInteger reserveTotalLiquidity = (BigInteger) reserveData.get("totalLiquidity");
            BigInteger reserveAvailableLiquidity = (BigInteger) reserveData.get("availableLiquidity");
            BigInteger reserveTotalBorrows = (BigInteger) reserveData.get("totalBorrows");

            if (!reserveDecimals.equals(EIGHTEEN)) {
                reserveTotalLiquidity = convertToExa(reserveTotalLiquidity, reserveDecimals);
                reserveAvailableLiquidity = convertToExa(reserveAvailableLiquidity, reserveDecimals);
                reserveTotalBorrows = convertToExa(reserveTotalBorrows, reserveDecimals);
            }

            totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));
            availableLiquidityBalanceUSD = availableLiquidityBalanceUSD.add(
                    exaMultiply(reserveAvailableLiquidity, reservePrice));
            totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply(reserveTotalBorrows, reservePrice));
            boolean usageAsCollateralEnabled = (boolean) reserveData.get("usageAsCollateralEnabled");

            if (usageAsCollateralEnabled) {
                totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(
                        exaMultiply(reserveTotalLiquidity, reservePrice));
            }

        }
        return Map.of("totalLiquidityBalanceUSD", totalLiquidityBalanceUSD, "availableLiquidityBalanceUSD",
                availableLiquidityBalanceUSD, "totalBorrowsBalanceUSD", totalBorrowBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD);
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
            Map<String, Object> userBasicReserveData = new HashMap<>();
            userBasicReserveData.putAll(
                    call(Map.class, Contracts.LENDING_POOL_CORE, "getUserBasicReserveDataProxy", reserve, _user));
            if (userBasicReserveData.get("underlyingBalance").equals(BigInteger.ZERO) && userBasicReserveData.get(
                    "compoundedBorrowBalance").equals(BigInteger.ZERO)) {
                continue;
            }
            BigInteger reserveDecimals = (BigInteger) userBasicReserveData.get("decimals");
            // converting the user balances into 18 decimals
            if (!reserveDecimals.equals(EIGHTEEN)) {
                userBasicReserveData.put("underlyingBalance",
                        convertToExa((BigInteger) userBasicReserveData.get("underlyingBalance"), reserveDecimals));
                userBasicReserveData.put("compoundedBorrowBalance",
                        convertToExa((BigInteger) userBasicReserveData.get("compoundedBorrowBalance"), reserveDecimals));
                userBasicReserveData.put("originationFee",
                        convertToExa((BigInteger) userBasicReserveData.get("originationFee"), reserveDecimals));
            }

            String symbol = this.symbol.get(reserve);
            BigInteger reserveUnitPrice = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data",
                    symbol, "USD");
            if (symbol.equals("ICX")) {
                reserveUnitPrice = exaMultiply(reserveUnitPrice,todaySicxRate);
            }

            if (((BigInteger)userBasicReserveData.get("underlyingBalance")).compareTo(BigInteger.ZERO) > 0) {
                BigInteger liquidityBalanceUSD = exaMultiply(reserveUnitPrice,
                        (BigInteger) userBasicReserveData.get("underlyingBalance"));
                totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(liquidityBalanceUSD);

                BigInteger baseLTVasCollateral = (BigInteger) userBasicReserveData.get("baseLTVasCollateral");
                if ((boolean) userBasicReserveData.get("usageAsCollateralEnabled") &&
                        baseLTVasCollateral.compareTo(BigInteger.ZERO) > 0) {
                    totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(liquidityBalanceUSD);
                    currentLtv = currentLtv.add(exaMultiply(liquidityBalanceUSD,
                            (BigInteger) userBasicReserveData.get("baseLTVasCollateral")));
                    currentLiquidationThreshold = currentLiquidationThreshold.add(exaMultiply(liquidityBalanceUSD,
                            (BigInteger) userBasicReserveData.get("liquidationThreshold")));
                }

            }

            if (((BigInteger) userBasicReserveData.get("compoundedBorrowBalance")).compareTo(BigInteger.ZERO) > 0) {
                totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(
                        exaMultiply(reserveUnitPrice,
                                (BigInteger) userBasicReserveData.get("compoundedBorrowBalance")));
                totalFeesUSD = totalFeesUSD.add(exaMultiply(reserveUnitPrice,
                        (BigInteger) userBasicReserveData.get("originationFee")));

            }
        }

        if (totalCollateralBalanceUSD.compareTo(BigInteger.ZERO) > 0) {
            currentLtv = exaDivide(currentLtv, totalCollateralBalanceUSD);
            currentLiquidationThreshold = exaDivide(currentLiquidationThreshold, totalCollateralBalanceUSD);

        } else {
            currentLtv = BigInteger.ZERO;
            currentLiquidationThreshold = BigInteger.ZERO;
        }

        BigInteger healthFactor = calculateHealthFactorFromBalancesInternal(totalCollateralBalanceUSD,
                totalBorrowBalanceUSD, totalFeesUSD, currentLiquidationThreshold);
        boolean healthFactorBelowThreshold =
                healthFactor.compareTo(HEALTH_FACTOR_LIQUIDATION_THRESHOLD) < 0 && (!healthFactor.equals(
                        BigInteger.ONE.negate()));

        BigInteger borrowingPower = calculateBorrowingPowerFromBalancesInternal(totalCollateralBalanceUSD,
                totalBorrowBalanceUSD, totalFeesUSD, currentLiquidationThreshold);

        BigInteger borrowsAllowedUSD = exaMultiply(totalCollateralBalanceUSD.subtract(totalFeesUSD), currentLtv);
        BigInteger availableBorrowsUSD = borrowsAllowedUSD.subtract(totalBorrowBalanceUSD);

        if (availableBorrowsUSD.compareTo(BigInteger.ZERO) < 0) {
            availableBorrowsUSD = BigInteger.ZERO;
        }

        Map<String, Object> response = new HashMap<>();

        response.put("totalLiquidityBalanceUSD", totalLiquidityBalanceUSD);
        response.put("totalCollateralBalanceUSD", totalCollateralBalanceUSD);
        response.put("totalBorrowBalanceUSD", totalBorrowBalanceUSD );
        response.put("totalFeesUSD", totalFeesUSD);
        response.put("availableBorrowsUSD", availableBorrowsUSD);
        response.put("currentLtv", currentLtv);
        response.put("currentLiquidationThreshold", currentLiquidationThreshold );
        response.put("healthFactor", healthFactor);
        response.put("borrowingPower", borrowingPower);
        response.put("healthFactorBelowThreshold", healthFactorBelowThreshold);
        response.put("todaySicxRate", todaySicxRate);

        return response;
    }

    @External(readonly = true)
    public Map<String, BigInteger> getUserReserveData(Address _reserve, Address _user) {
        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", _reserve);
        Map<String, BigInteger> userReserveData = call(Map.class, Contracts.LENDING_POOL_CORE, "getUserReserveData",
                _reserve, _user);

        Address oTokenAddr = (Address) reserveData.get("oTokenAddress");

        BigInteger currentOTokenBalance = call(BigInteger.class, oTokenAddr, "balanceOf", _user);
        BigInteger principalOTokenBalance = call(BigInteger.class, oTokenAddr, "principalBalanceOf", _user);
        BigInteger userLiquidityCumulativeIndex = call(BigInteger.class, oTokenAddr, "getUserLiquidityCumulativeIndex",
                _user);

        Address dTokenAddr = (Address) reserveData.get("dTokenAddress");

        BigInteger principalBorrowBalance = call(BigInteger.class, dTokenAddr, "principalBalanceOf", _user);
        BigInteger currentBorrowBalance = call(BigInteger.class, dTokenAddr, "balanceOf", _user);

        BigInteger userBorrowCumulativeIndex = call(BigInteger.class, dTokenAddr, "getUserBorrowCumulativeIndex",
                _user);

        BigInteger borrowRate = (BigInteger) reserveData.get("borrowRate");
        BigInteger reserveDecimals = (BigInteger) reserveData.get("decimals");
        BigInteger liquidityRate = (BigInteger) reserveData.get("liquidityRate");

        BigInteger originationFee = userReserveData.get("originationFee");
        BigInteger lastUpdateTimestamp = userReserveData.get("lastUpdateTimestamp");

        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol, "USD");

        BigInteger todaySicxRate = null;
        if (symbol.equals("ICX")) {
            todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        }
        BigInteger currentOTokenBalanceUSD = exaMultiply(convertToExa(currentOTokenBalance, reserveDecimals), price);
        BigInteger principalOTokenBalanceUSD = exaMultiply(convertToExa(principalOTokenBalance, reserveDecimals),
                price);
        BigInteger currentBorrowBalanceUSD = exaMultiply(convertToExa(currentBorrowBalance, reserveDecimals), price);
        BigInteger principalBorrowBalanceUSD = exaMultiply(convertToExa(principalBorrowBalance, reserveDecimals),
                price);

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
    public boolean balanceDecreaseAllowed(Address _reserve, Address _user, BigInteger _amount) {
        Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", _reserve);
        BigInteger reserveLiquidationThreshold = (BigInteger) reserveConfiguration.get("liquidationThreshold");
        boolean reserveUsageAsCollateralEnabled = (boolean) reserveConfiguration.get("usageAsCollateralEnabled");
        if (!reserveUsageAsCollateralEnabled) {
            return true;
        }
        Map<String, Object> userAccountData = getUserAccountData(_user);
        BigInteger collateralBalanceUSD = (BigInteger) userAccountData.get("totalCollateralBalanceUSD");
        BigInteger borrowBalanceUSD = (BigInteger) userAccountData.get("totalBorrowBalanceUSD");

        if (borrowBalanceUSD.equals(BigInteger.ZERO)) {
            return true;
        }

        BigInteger totalFeesUSD = (BigInteger) userAccountData.get("totalFeesUSD");
        BigInteger currentLiquidationThreshold = (BigInteger) userAccountData.get("currentLiquidationThreshold");

        BigInteger decimals = (BigInteger) reserveConfiguration.get("decimals");
        if (!decimals.equals(EIGHTEEN)) {
            _amount = convertToExa(_amount, decimals);
        }
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol, "USD");
        if (symbol.equals("ICX")) {
            BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        }
        BigInteger amountToDecreaseUSD = exaMultiply(price, _amount);
        BigInteger collateralBalanceAfterDecreaseUSD = collateralBalanceUSD.subtract(amountToDecreaseUSD);
        // TODO: CHECK
        if (collateralBalanceAfterDecreaseUSD.equals(BigInteger.ZERO)) {
            return false;
        }

        BigInteger liquidationThresholdAfterDecrease = exaDivide(
                (exaMultiply(collateralBalanceUSD, currentLiquidationThreshold).subtract(
                        exaMultiply(amountToDecreaseUSD, reserveLiquidationThreshold))),
                collateralBalanceAfterDecreaseUSD);
        BigInteger healthFactorAfterDecrease = calculateHealthFactorFromBalancesInternal(
                collateralBalanceAfterDecreaseUSD, borrowBalanceUSD, totalFeesUSD, liquidationThresholdAfterDecrease);
        return healthFactorAfterDecrease.compareTo(HEALTH_FACTOR_LIQUIDATION_THRESHOLD) > 0;
    }

    @External(readonly = true)
    public BigInteger calculateCollateralNeededUSD(Address _reserve, BigInteger _amount, BigInteger _fee,
            BigInteger _userCurrentBorrowBalanceUSD, BigInteger _userCurrentFeesUSD, BigInteger _userCurrentLtv) {
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol, "USD");
        Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveConfiguration", _reserve);
        BigInteger decimals = (BigInteger) reserveConfiguration.get("decimals");
        if (!decimals.equals(EIGHTEEN)) {
            // TODO: CHECK
            _amount = convertToExa(_amount, decimals);
        }
        if (symbol.equals("ICX")) {
            BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            price = exaMultiply(price, todaySicxRate);
        }
        BigInteger requestedBorrowUSD = exaMultiply(price, _amount);

        return exaDivide(_userCurrentBorrowBalanceUSD.add(requestedBorrowUSD), _userCurrentLtv).add(
                _userCurrentFeesUSD);
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

        BigInteger badDebt = BigInteger.ZERO;

        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger currentLtv = BigInteger.ZERO;
        BigInteger currentLiquidationThreshold = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger totalFeesUSD = BigInteger.ZERO;

        Map<String, Map<String, BigInteger>> tempBorrows = new HashMap<>();
        Map<String, Object> collaterals = new HashMap<>();

        for (Address reserve : reserves) {
            Map<String, BigInteger> userReserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                    "getUserBasicReserveData", reserve, _user);
            BigInteger underlyingBalance = userReserveData.get("underlyingBalance");
            BigInteger compoundedBorrowBalance = userReserveData.get("compoundedBorrowBalance");
            if (underlyingBalance.equals(BigInteger.ZERO) && compoundedBorrowBalance.equals(BigInteger.ZERO)) {
                continue;
            }
            Map<String, Object> reserveConfiguration = call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve);

            BigInteger reserveDecimals = (BigInteger) reserveConfiguration.get("decimals");
            BigInteger userReserveUnderlyingBalance = convertToExa(underlyingBalance, reserveDecimals);
            BigInteger userBorrowBalance = convertToExa(compoundedBorrowBalance, reserveDecimals);

            BigInteger originationFee = convertToExa(userReserveData.get("originationFee"), reserveDecimals);
            String symbol = this.symbol.get(reserve);
            BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol, "USD");
            if (symbol.equals("ICX")) {
                BigInteger todaySicxRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
                price = exaMultiply(price, todaySicxRate);
            }

            if (userReserveUnderlyingBalance.compareTo(BigInteger.ZERO) > 0) {

                BigInteger liquidityBalanceUSD = exaMultiply(price, userReserveUnderlyingBalance);
                collaterals.put(symbol,
                        Map.of("underlyingBalance", userReserveData.get("underlyingBalance"), "underlyingBalanceUSD",
                                liquidityBalanceUSD));
                totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(liquidityBalanceUSD);

                if ((boolean) reserveConfiguration.get("usageAsCollateralEnabled")) {
                    totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(liquidityBalanceUSD);
                    currentLtv = currentLtv.add(exaMultiply(liquidityBalanceUSD,
                            (BigInteger) reserveConfiguration.get("baseLTVasCollateral")));
                    currentLiquidationThreshold = currentLiquidationThreshold.add(exaMultiply(liquidityBalanceUSD,
                            (BigInteger) reserveConfiguration.get("liquidationThreshold")));
                }

            }

            if (compoundedBorrowBalance.compareTo(BigInteger.ZERO) > 0) {
                BigInteger borrowBalanceInUSD = exaMultiply(price, userBorrowBalance);
                totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(borrowBalanceInUSD);
                totalFeesUSD = totalFeesUSD.add(exaMultiply(price, originationFee));
                tempBorrows.put(symbol,
                        Map.of("compoundedBorrowBalance", compoundedBorrowBalance, "compoundedBorrowBalanceUSD",
                                borrowBalanceInUSD, "price", price, "reserveDecimals", reserveDecimals));

            }
        }

        if (totalCollateralBalanceUSD.compareTo(BigInteger.ZERO) > 0) {
            currentLtv = exaDivide(currentLtv, totalCollateralBalanceUSD);
            currentLiquidationThreshold = exaDivide(currentLiquidationThreshold, totalCollateralBalanceUSD);
        } else {
            currentLtv = BigInteger.ZERO;
            currentLiquidationThreshold = BigInteger.ZERO;
        }

        BigInteger healthFactor = calculateHealthFactorFromBalancesInternal(totalCollateralBalanceUSD,
                totalBorrowBalanceUSD, totalFeesUSD, currentLiquidationThreshold);
        boolean healthFactorBelowThreshold =
                healthFactor.compareTo(HEALTH_FACTOR_LIQUIDATION_THRESHOLD) < 0 && (!healthFactor.equals(
                        BigInteger.ONE.negate()));
        if (healthFactorBelowThreshold) {
            badDebt = call(BigInteger.class, Contracts.LIQUIDATION_MANAGER, "calculateBadDebt", totalBorrowBalanceUSD,
                    totalFeesUSD, totalCollateralBalanceUSD, currentLtv);
        }

        Map<String, Map<String, BigInteger>> borrows = new HashMap<>();
        for (Map.Entry<String, Map<String, BigInteger>> entry : tempBorrows.entrySet()) {
            String symbol = entry.getKey();
            Map<String, BigInteger> borrowDetails = entry.getValue();
            BigInteger maxAmountToLiquidateUSD = borrowDetails.get("compoundedBorrowBalanceUSD");
            BigInteger maxAmountToLiquidate = borrowDetails.get("compoundedBorrowBalance");
            if (badDebt.compareTo(borrowDetails.get("compoundedBorrowBalanceUSD")) < 0) {
                maxAmountToLiquidateUSD = badDebt;
                maxAmountToLiquidate = convertExaToOther(exaDivide(badDebt, borrowDetails.get("price")),
                        borrowDetails.get("reserveDecimals").intValue());

            }

            borrows.put(symbol, Map.of("compoundedBorrowBalance", borrowDetails.get("compoundedBorrowBalance"),
                    "compoundedBorrowBalanceUSD", borrowDetails.get("compoundedBorrowBalanceUSD"),
                    "maxAmountToLiquidate", maxAmountToLiquidate, "maxAmountToLiquidateUSD", maxAmountToLiquidateUSD));
        }

        return Map.of("badDebt", badDebt, "borrows", borrows, "collaterals", collaterals);
    }


    @External(readonly = true)
    public Map<String, Object> getReserveData(Address _reserve) {
        Map<String, Object> reserveData = new HashMap<>();
        reserveData.putAll(call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveData", _reserve));
        String symbol = this.symbol.get(_reserve);
        BigInteger price = call(BigInteger.class, Contracts.PRICE_ORACLE, "get_reference_data", symbol, "USD");
        reserveData.put("exchangePrice", price);
        if (symbol.equals("ICX")) {
            BigInteger todayRate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
            reserveData.put("sICXRate", todayRate);
            price = exaMultiply(todayRate, price);
        }
        BigInteger reserveDecimals = (BigInteger) reserveData.get("decimals");
        reserveData.put("totalLiquidityUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.get("totalLiquidity"), reserveDecimals), price));
        reserveData.put("availableLiquidityUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.get("availableLiquidity"), reserveDecimals), price));
        reserveData.put("totalBorrowsUSD",
                exaMultiply(convertToExa((BigInteger) reserveData.get("totalBorrows"), reserveDecimals), price));
        BigInteger lendingPercentage = call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage",
                reserveData.get("oTokenAddress"));
        BigInteger borrowingPercentage = call(BigInteger.class, Contracts.REWARDS, "assetDistPercentage",
                reserveData.get("dTokenAddress"));
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
            reserveDetails.put(symbol.get(reserve),
                    call(Map.class, Contracts.LENDING_POOL_CORE, "getReserveConfiguration", reserve));
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

            if (fromAddress.equals(getAddress(Contracts.LENDING_POOL_CORE.getKey()))) {
                Map<String, BigInteger> record = new HashMap<>();
                record.put("amount", (BigInteger) unstakedRecords.get("amount"));
                record.put("unstakingBlockHeight", (BigInteger) unstakedRecords.get("unstakingBlockHeight"));
                response.add(record);
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
        return userReserveData.get("currentBorrowBalance").add(userReserveData.get("originationFee"));
    }
}
