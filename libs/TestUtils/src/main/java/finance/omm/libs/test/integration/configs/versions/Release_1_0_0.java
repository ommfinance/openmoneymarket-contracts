package finance.omm.libs.test.integration.configs.versions;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.governance.ReserveAttributes;
import finance.omm.libs.structs.governance.ReserveConstant;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.configs.Constant;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import score.Address;

import static finance.omm.utils.math.MathUtils.ICX;

public class Release_1_0_0 extends Release {

    public Release_1_0_0(OMMClient ommClient) {
        this.ommClient = ommClient;
        this.addressMap = ommClient.getContractAddresses();
    }

    @Override
    public boolean init() {
        System.out.println("----init release v1.0.0----");
        initAddresses();
        this.ommClient.feeProvider.setLoanOriginationFeePercentage(Constant.LOAN_ORIGINATION_FEE_PERCENTAGE);

        this.ommClient.lendingPoolDataProvider.setSymbol(addressMap.get("IUSDC"), "USDC");
        this.ommClient.lendingPoolDataProvider.setSymbol(addressMap.get("sICX"), "ICX");

        ommClient.delegation.addAllContributors(Environment.preps.keySet().toArray(Address[]::new));
        ommClient.ommToken.setMinimumStake(Constant.MINIMUM_OMM_STAKE);
        ommClient.ommToken.setUnstakingPeriod(Constant.UNSTAKING_PERIOD);
        ommClient.staking.toggleStakingOn();
        ommClient.lendingPool.setFeeSharingTxnLimit(BigInteger.valueOf(50));
        ommClient.staking.setSicxAddress(addressMap.get(Contracts.sICX.getKey()));

        ommClient.dummyPriceOracle.set_reference_data("ICX",
                BigInteger.valueOf(3).multiply(ICX).divide(BigInteger.TEN));

        ommClient.dummyPriceOracle.set_reference_data("USDC",ICX);

      /*
        initialized reserves
         */
        ReserveAttributes usdcAttribute = new ReserveAttributes();

        usdcAttribute.reserveAddress = addressMap.get("IUSDC");
        usdcAttribute.oTokenAddress = addressMap.get("oIUSDC");
        usdcAttribute.dTokenAddress = addressMap.get("dIUSDC");
        usdcAttribute.decimals = 6;
        initializeReserve(usdcAttribute);

        ReserveAttributes sicxAttribute = new ReserveAttributes();

        sicxAttribute.reserveAddress = addressMap.get("sICX");
        sicxAttribute.oTokenAddress = addressMap.get("oICX");
        sicxAttribute.dTokenAddress = addressMap.get("dICX");
        sicxAttribute.decimals = 18;
        initializeReserve(sicxAttribute);


        /*
        set reserve constants
         */
        initReserveConstants();

        return next(ommClient);
    }


    private void initializeReserve(ReserveAttributes attribute) {
        attribute.lastUpdateTimestamp = BigInteger.ZERO;
        attribute.liquidityRate = BigInteger.ZERO;
        attribute.borrowRate = BigInteger.ZERO;
        attribute.liquidityCumulativeIndex = MathUtils.ICX;
        attribute.borrowCumulativeIndex = MathUtils.ICX;
        attribute.baseLTVasCollateral = new BigInteger("500000000000000000");
        attribute.liquidationThreshold = new BigInteger("650000000000000000");
        attribute.liquidationBonus = new BigInteger("100000000000000000");
        attribute.borrowingEnabled = true;
        attribute.usageAsCollateralEnabled = true;
        attribute.isFreezed = false;
        attribute.isActive = true;

        ommClient.governance.initializeReserve(attribute);
        ommClient.governance.updateBorrowThreshold(attribute.reserveAddress, Constant.BORROW_THRESHOLD);
    }

    private void initAddresses() {
        ExecutorService exec = Executors.newFixedThreadPool(10);
        Map<String, Future<?>> futures = new HashMap<>();

        Future<?> setLendingPoolAddresses = exec.submit(
                () -> ommClient.addressManager.setLendingPoolAddresses());
        futures.put("setLendingPoolAddresses", setLendingPoolAddresses);

        Future<?> setLendingPoolCoreAddresses = exec.submit(
                () -> ommClient.addressManager.setLendingPoolCoreAddresses());
        futures.put("setLendingPoolCoreAddresses", setLendingPoolCoreAddresses);

        Future<?> setLendingPoolDataProviderAddresses = exec.submit(
                () -> ommClient.addressManager.setLendingPoolDataProviderAddresses());
        futures.put("setLendingPoolDataProviderAddresses", setLendingPoolDataProviderAddresses);

        Future<?> setLiquidationManagerAddresses = exec.submit(
                () -> ommClient.addressManager.setLiquidationManagerAddresses());
        futures.put("setLiquidationManagerAddresses", setLiquidationManagerAddresses);

        Future<?> setOmmTokenAddresses = exec.submit(
                () -> ommClient.addressManager.setOmmTokenAddresses());
        futures.put("setOmmTokenAddresses", setOmmTokenAddresses);
        Future<?> setoICXAddresses = exec.submit(
                () -> ommClient.addressManager.setoICXAddresses());
        futures.put("setoICXAddresses", setoICXAddresses);

        Future<?> setoIUSDCAddresses = exec.submit(
                () -> ommClient.addressManager.setoIUSDCAddresses());
        futures.put("setoIUSDCAddresses", setoIUSDCAddresses);

        Future<?> setdICXAddresses = exec.submit(
                () -> ommClient.addressManager.setdICXAddresses());
        futures.put("setdICXAddresses", setdICXAddresses);

        Future<?> setdIUSDCAddresses = exec.submit(
                () -> ommClient.addressManager.setdIUSDCAddresses());
        futures.put("setdIUSDCAddresses", setdIUSDCAddresses);

        Future<?> setDelegationAddresses = exec.submit(
                () -> ommClient.addressManager.setDelegationAddresses());
        futures.put("setDelegationAddresses", setDelegationAddresses);

        Future<?> setRewardAddresses = exec.submit(
                () -> ommClient.addressManager.setRewardAddresses());
        futures.put("setRewardAddresses", setRewardAddresses);

        Future<?> setGovernanceAddresses = exec.submit(
                () -> ommClient.addressManager.setGovernanceAddresses());
        futures.put("setGovernanceAddresses", setGovernanceAddresses);

        Future<?> setStakedLpAddresses = exec.submit(
                () -> ommClient.addressManager.setStakedLpAddresses());
        futures.put("setStakedLpAddresses", setStakedLpAddresses);

        Future<?> setPriceOracleAddress = exec.submit(
                () -> ommClient.addressManager.setPriceOracleAddress());
        futures.put("setPriceOracleAddress", setPriceOracleAddress);

        Future<?> setDaoFundAddresses = exec.submit(
                () -> ommClient.addressManager.setDaoFundAddresses());
        futures.put("setDaoFundAddresses", setDaoFundAddresses);

        Future<?> setFeeProviderAddresses = exec.submit(
                () -> ommClient.addressManager.setFeeProviderAddresses());
        futures.put("setFeeProviderAddresses", setFeeProviderAddresses);

        for (Entry<String, Future<?>> future : futures.entrySet()) {
            try {
                future.getValue().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(future.getKey() + " = " + e.getMessage());
            }
        }

    }


    private void initReserveConstants() {
        ReserveConstant usdcReserve = new ReserveConstant();
        usdcReserve.reserve = addressMap.get("IUSDC");
        initReserve(usdcReserve);

        ReserveConstant sicxReserve = new ReserveConstant();
        sicxReserve.reserve = addressMap.get("sICX");
        initReserve(sicxReserve);

        ReserveConstant[] reserveConstants = new ReserveConstant[]{
                usdcReserve, sicxReserve
        };
        ommClient.governance.setReserveConstants(reserveConstants);
    }

    private void initReserve(ReserveConstant usdcReserve) {
        usdcReserve.optimalUtilizationRate = BigInteger.valueOf(8)
                .multiply(ICX)
                .divide(BigInteger.TEN);
        usdcReserve.baseBorrowRate = BigInteger.valueOf(2)
                .multiply(ICX)
                .divide(BigInteger.valueOf(100));
        usdcReserve.slopeRate1 = BigInteger.valueOf(6)
                .multiply(ICX)
                .divide(BigInteger.valueOf(100));
        usdcReserve.slopeRate2 = BigInteger.valueOf(2)
                .multiply(ICX);
    }
}
