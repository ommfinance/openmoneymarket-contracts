package finance.omm.libs.address;

public enum Contracts {
    USDS("USDS"),
    sICX("sICX"),
    IUSDC("IUSDC"),
    oICX("oICX"),
    oUSDs("oUSDS"),
    oIUSDC("oIUSDC"),
    dICX("dICX"),
    dUSDs("dUSDS"),
    dIUSDC("dIUSDC"),
    LENDING_POOL("lendingPool"),
    LENDING_POOL_DATA_PROVIDER("lendingPoolDataProvider"),
    STAKING("staking"),
    DELEGATION("delegation"),
    OMM_TOKEN("ommToken"),
    REWARDS("rewards"),
    REWARD_WEIGHT_CONTROLLER("rewardWeightController"),
    PRICE_ORACLE("priceOracle"),
    LENDING_POOL_CORE("lendingPoolCore"),
    LIQUIDATION_MANAGER("liquidationManager"),
    FEE_PROVIDER("feeProvider"),
    BRIDGE_O_TOKEN("bridgeOToken"),
    GOVERNANCE("governance"),
    ADDRESS_PROVIDER("addressProvider"),
    RESERVE("reserve"),
    WORKER_TOKEN("workerToken"),
    DAO_FUND("daoFund"),
    BAND_ORACLE("bandOracle"),
    STAKED_LP("stakedLP"),
    DEX("dex");


    private String key;

    Contracts(String key) {
        this.key = key;
    }

    public String toString() {
        return this.key;
    }

    public String getKey() {
        return key;
    }
}
