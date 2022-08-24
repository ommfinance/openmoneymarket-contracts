package finance.omm.score.core.finance.omm.score.core.lendingpoolDataProvider.test.unit;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.score.core.lendingpoolDataProvider.LendingPoolDataProviderImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import scorex.util.HashMap;


import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.convertExaToOther;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;


public class AbstractLendingDataProviderTest extends TestBase {

    public static ServiceManager sm = getServiceManager();
    public static Account owner = sm.createAccount();
    public static Account notOwner = sm.createAccount();

    public Score score;

    public LendingPoolDataProviderImpl scoreSpy;

    public static final Map<Contracts, Account> MOCK_CONTRACT_ADDRESS = new HashMap<>() {{
        put(Contracts.ADDRESS_PROVIDER, Account.newScoreAccount(101));
        put(Contracts.IUSDC, Account.newScoreAccount(102));
        put(Contracts.sICX, Account.newScoreAccount(103));
        put(Contracts.OMM_TOKEN, Account.newScoreAccount(104));
        put(Contracts.oICX, Account.newScoreAccount(105));
        put(Contracts.oIUSDC, Account.newScoreAccount(106));
        put(Contracts.dICX, Account.newScoreAccount(107));
        put(Contracts.dIUSDC, Account.newScoreAccount(108));
        put(Contracts.LENDING_POOL_CORE, Account.newScoreAccount(109));

    }};

    @BeforeEach
    public void setup() throws Exception {

        owner = sm.createAccount(100);
        notOwner = sm.createAccount(100);

        score = sm.deploy(owner, LendingPoolDataProviderImpl.class,
                MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER).getAddress());
        setAddresses();

        LendingPoolDataProviderImpl lendingPoolDataProvider = (LendingPoolDataProviderImpl) score.getInstance();
        scoreSpy = spy(lendingPoolDataProvider);
        score.setInstance(scoreSpy);
    }

    private void setAddresses() {
        AddressDetails[] addressDetails = MOCK_CONTRACT_ADDRESS.entrySet().stream().map(e -> {
            AddressDetails ad = new AddressDetails();
            ad.address = e.getValue().getAddress();
            ad.name = e.getKey().toString();
            return ad;
        }).toArray(AddressDetails[]::new);

        Object[] params = new Object[]{
                addressDetails
        };
        score.invoke(MOCK_CONTRACT_ADDRESS.get(Contracts.ADDRESS_PROVIDER), "setAddresses", params);
    }

    public void expectErrorMessage(Executable contractCall, String errorMessage){
        AssertionError e = Assertions.assertThrows(AssertionError.class,contractCall);
        assertEquals(errorMessage,e.getMessage());
    }

    protected Map<String, BigInteger> reserveDataCalculation(String symbol, BigInteger reserveTotalLiquidity, BigInteger reserveAvailableLiquidity,
                                                           BigInteger reserveTotalBorrows, Boolean collateralEnabled){
        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger availableLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger reservePrice = ICX;
        BigInteger reserveDecimals = BigInteger.valueOf(6);

        if (symbol.equals("ICX")){
            reservePrice = exaMultiply(reservePrice,
                    BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN));
        }

        if (symbol.equals("USDC")){
            reserveTotalLiquidity = convertToExa(reserveTotalLiquidity, reserveDecimals);
            reserveAvailableLiquidity = convertToExa(reserveAvailableLiquidity, reserveDecimals);
            reserveTotalBorrows = convertToExa(reserveTotalBorrows, reserveDecimals);
        }



        totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));
        availableLiquidityBalanceUSD = availableLiquidityBalanceUSD.add(exaMultiply(reserveAvailableLiquidity, reservePrice));
        totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply(reserveTotalBorrows, reservePrice));
        totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(exaMultiply(reserveTotalLiquidity, reservePrice));

        if (!collateralEnabled){
            totalBorrowBalanceUSD = BigInteger.ZERO;
            totalCollateralBalanceUSD = BigInteger.ZERO;
        }

        return Map.of("totalLiquidityBalanceUSD", totalLiquidityBalanceUSD,
                "availableLiquidityBalanceUSD", availableLiquidityBalanceUSD,
                "totalBorrowsBalanceUSD", totalBorrowBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD);
    }


    protected Map<String, Object> userAccountDataCalculation(String symbol, BigInteger  underlyingBalance,
                                                           BigInteger compoundedBorrowBalance, BigInteger originationFee,
                                                           Boolean usageAsCollateralEnabled, BigInteger baseLTVasCollateral,
                                                           BigInteger liquidationThreshold) {
        BigInteger todayRate = BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN);
        BigInteger totalLiquidityBalanceUSD = BigInteger.ZERO;
        BigInteger totalCollateralBalanceUSD = BigInteger.ZERO;
        BigInteger currentLtv = BigInteger.ZERO;
        BigInteger currentLiquidationThreshold = BigInteger.ZERO;
        BigInteger totalBorrowBalanceUSD = BigInteger.ZERO;
        BigInteger totalFeesUSD = BigInteger.ZERO;
        BigInteger reserveDecimals = BigInteger.valueOf(18);
        BigInteger reserveUnitPrice = ICX;

        if (symbol.equals("USDC")) {
            reserveDecimals = BigInteger.valueOf(6);
            underlyingBalance = convertToExa(underlyingBalance, reserveDecimals);
            compoundedBorrowBalance = convertToExa(compoundedBorrowBalance, reserveDecimals);
            originationFee = convertToExa(originationFee, reserveDecimals);

        }

        if (symbol.equals("ICX")) {
            reserveUnitPrice = exaMultiply(reserveUnitPrice, todayRate);
        }

        if (underlyingBalance.compareTo(BigInteger.ZERO) > 0) {
            BigInteger liquidityBalanceUSD = exaMultiply(reserveUnitPrice, underlyingBalance);
            totalLiquidityBalanceUSD = totalLiquidityBalanceUSD.add(liquidityBalanceUSD);
            if (usageAsCollateralEnabled) {
                totalCollateralBalanceUSD = totalCollateralBalanceUSD.add(liquidityBalanceUSD);
                currentLtv = currentLtv.add(exaMultiply(liquidityBalanceUSD, baseLTVasCollateral));
                currentLiquidationThreshold = currentLiquidationThreshold.add(
                        exaMultiply(liquidityBalanceUSD, liquidationThreshold));
            }

        }

        if (compoundedBorrowBalance.compareTo(BigInteger.ZERO) > 0) {
            totalBorrowBalanceUSD = totalBorrowBalanceUSD.add(exaMultiply(reserveUnitPrice, compoundedBorrowBalance));
            totalFeesUSD = totalFeesUSD.add(exaMultiply(reserveUnitPrice, originationFee));
        }

        if (totalCollateralBalanceUSD.compareTo(BigInteger.ZERO) > 0) {
            currentLtv = exaDivide(currentLtv, totalCollateralBalanceUSD);
            currentLiquidationThreshold = exaDivide(currentLiquidationThreshold, totalCollateralBalanceUSD);

        } else {
            currentLtv = BigInteger.ZERO;
            currentLiquidationThreshold = BigInteger.ZERO;
        }

        BigInteger borrowsAllowedUSD = exaMultiply(totalCollateralBalanceUSD.subtract(totalFeesUSD), currentLtv);
        BigInteger availableBorrowsUSD = borrowsAllowedUSD.subtract(totalBorrowBalanceUSD);

        if (availableBorrowsUSD.compareTo(BigInteger.ZERO) < 0){
            availableBorrowsUSD = BigInteger.ZERO;
        }

        return Map.of(
                "totalLiquidityBalanceUSD", totalLiquidityBalanceUSD,
                "totalCollateralBalanceUSD", totalCollateralBalanceUSD,
                "totalBorrowBalanceUSD", totalBorrowBalanceUSD,
                "totalFeesUSD", totalFeesUSD,
                "availableBorrowsUSD", availableBorrowsUSD,
                "currentLtv", currentLtv,
                "currentLiquidationThreshold", currentLiquidationThreshold
        );

    }

    protected BigInteger availableBorrow(BigInteger availableBorrowICX, BigInteger availableBorrowIUSDC){

        BigInteger availableBorrowsUSD = availableBorrowICX.add(availableBorrowIUSDC);
        if (availableBorrowICX.equals(BigInteger.ZERO) || availableBorrowIUSDC.equals(BigInteger.ZERO)){
            availableBorrowsUSD = BigInteger.ZERO;
        }

        return  availableBorrowsUSD;
    }

    protected BigInteger healthFactor(BigInteger totalBorrowBalanceUSD,BigInteger totalCollateralBalanceUSD,
                                      BigInteger totalFeesUSD,BigInteger liquidationThreshold){
        BigInteger healthFactor;
        if (totalBorrowBalanceUSD.equals(BigInteger.ZERO)){
            healthFactor = BigInteger.ONE.negate();
        }
        else {
            healthFactor = exaDivide(exaMultiply(totalCollateralBalanceUSD.subtract(totalFeesUSD), liquidationThreshold)
                    , totalBorrowBalanceUSD);
        }

        return  healthFactor;
    }

    protected BigInteger borrowingPower(BigInteger totalCollateralBalanceUSD,BigInteger totalBorrowBalanceUSD,BigInteger totalFeesUSD, BigInteger currentLiquidationThreshold){
        BigInteger borrowingPower ;
        if (totalCollateralBalanceUSD.equals(BigInteger.ZERO)){
            borrowingPower = BigInteger.ZERO;
        }
        else {
            borrowingPower = exaDivide(totalBorrowBalanceUSD, exaMultiply(totalCollateralBalanceUSD.subtract(totalFeesUSD),
                    currentLiquidationThreshold.divide(BigInteger.TWO)));
        }

        return borrowingPower;
    }

    protected BigInteger collateralNeededUSDCalculation(String symbol,BigInteger amount,BigInteger userCurrentBorrow,
                                                      BigInteger userCurrentFee,BigInteger userCurrentLTV){
        BigInteger decimals = BigInteger.valueOf(18);
        BigInteger price = ICX;

        if (symbol.equals("USDC")){
            decimals = BigInteger.valueOf(6);
            amount = convertToExa(amount,decimals);
        }

        if (symbol.equals("ICX")){
            price = exaMultiply(price,BigInteger.valueOf(1).multiply(ICX).divide(BigInteger.TEN));
        }


        BigInteger requestedBorrowUSD= exaMultiply(price,amount);

        return exaDivide(userCurrentBorrow.add(requestedBorrowUSD),
                userCurrentLTV).add(userCurrentFee);
    }

    protected BigInteger reserveDataCalcualtion(String symbol,BigInteger price, BigInteger value){
        BigInteger reserveDecimals = BigInteger.valueOf(18);

        if (symbol.equals("ICX")){
            price = exaMultiply(ICX.divide(BigInteger.TEN),price);
        }

        if (symbol.equals("USDC")){
            reserveDecimals = BigInteger.valueOf(6);
        }

        return exaMultiply(convertToExa(value, reserveDecimals), price);
    }


    protected Map<String, Object> liquidationDataCalculation(String symbol, BigInteger compoundedBorrowBalance,
                                                           BigInteger underLyingBalance,BigInteger badDebt){
        BigInteger reserveDecimals = BigInteger.valueOf(18);
        BigInteger price = ICX;
        BigInteger userBorrowBalance= BigInteger.ZERO;
        BigInteger userReserveUnderlyingBalance = BigInteger.ZERO;

        Map<String, Object> borrows = new HashMap<>();
        Map<String, Object> collaterals = new HashMap<>();

        if (symbol.equals("ICX")){
            price = exaMultiply(price,ICX.divide(BigInteger.TEN));
            userBorrowBalance = convertToExa(compoundedBorrowBalance, reserveDecimals);
            userReserveUnderlyingBalance = convertToExa(underLyingBalance,reserveDecimals);
        }

        if (symbol.equals("USDC")){
            reserveDecimals = BigInteger.valueOf(6);
            userBorrowBalance = convertToExa(compoundedBorrowBalance, reserveDecimals);
            userReserveUnderlyingBalance = convertToExa(underLyingBalance,reserveDecimals);
        }

        if (userBorrowBalance.compareTo(BigInteger.ZERO) > 0) {
            BigInteger maxAmountToLiquidateUSD;
            BigInteger maxAmountToLiquidate;
            if (badDebt.compareTo(exaMultiply(price, userBorrowBalance)) > 0) {
                maxAmountToLiquidateUSD = exaMultiply(price, userBorrowBalance);
                maxAmountToLiquidate = compoundedBorrowBalance;
            } else {
                maxAmountToLiquidateUSD = badDebt;
                maxAmountToLiquidate = convertExaToOther(exaDivide(badDebt, price), reserveDecimals.intValue());
            }
            borrows.put(symbol, Map.of(
                    "compoundedBorrowBalance", compoundedBorrowBalance,
                    "compoundedBorrowBalanceUSD", exaMultiply(price, userBorrowBalance),
                    "maxAmountToLiquidate", maxAmountToLiquidate,
                    "maxAmountToLiquidateUSD", maxAmountToLiquidateUSD

            ));
        }
        if (userReserveUnderlyingBalance.compareTo(BigInteger.ZERO) > 0) {
            collaterals.put(symbol, Map.of(
                    "underlyingBalance", underLyingBalance,
                    "underlyingBalanceUSD", exaMultiply(price, userReserveUnderlyingBalance)
            ));
        }
        return Map.of(
                "badDebt", badDebt,
                "borrows", borrows,
                "collaterals", collaterals
        );
    }



}
