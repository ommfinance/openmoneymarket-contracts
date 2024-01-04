package finance.omm.score.staking.utils;

import finance.omm.score.staking.db.DelegationListDBSdo;
import score.Address;

import java.math.BigInteger;
import java.util.List;

public class Constant {
    public static final String TAG = "Staked ICX Manager";
    public static final Address SYSTEM_SCORE_ADDRESS = getSystemScoreAddress();
    public static final BigInteger ONE_EXA = pow10(18);
    public static final BigInteger HUNDRED = BigInteger.valueOf(100L);
    public static final BigInteger HUNDRED_PERCENTAGE = HUNDRED.multiply(ONE_EXA);
    public static final BigInteger TOP_PREP_COUNT = HUNDRED;
    public static final BigInteger DEFAULT_UNSTAKE_BATCH_LIMIT = BigInteger.valueOf(200L);
    public static final BigInteger MAX_ITERATION_LOOP = HUNDRED;
    public static final BigInteger DEFAULT_DECIMAL_VALUE = BigInteger.valueOf(18L);
    public static final BigInteger MINIMUM_DELEGATION_PERCENTAGE = pow10(15);
    public static final BigInteger BLOCKS_IN_A_WEEK = BigInteger.valueOf(7 * 43200L);

    public static final String SICX_SUPPLY = "sICX_supply";
    public static final String RATE = "_rate";
    public static final String SICX_ADDRESS = "sICX_address";
    public static final String BLOCK_HEIGHT_WEEK = "_block_height_week";
    public static final String BLOCK_HEIGHT_DAY = "_block_height_day";
    public static final String TOTAL_STAKE = "_total_stake";
    public static final String DAILY_REWARD = "_daily_reward";
    public static final String TOTAL_LIFETIME_REWARD = "_total_lifetime_reward";
    public static final String DISTRIBUTING = "_distributing";
    public static final String LINKED_LIST_VAR = "_linked_list_var";
    public static final String TOP_PREPS = "_top_preps";
    public static final String VALID_PREPS = "_valid_preps";
    public static final String PREP_LIST = "_prep_list";
    public static final String ADDRESS_DELEGATIONS = "_address_delegations";
    public static final String PREP_DELEGATIONS = "_prep_delegations";
    public static final String TOTAL_UNSTAKE_AMOUNT = "_total_unstake_amount";
    public static final String UNSTAKE_BATCH_LIMIT = "_unstake_batch_limit";
    public static final String STAKING_ON = "staking_on";
    public static final String ICX_PAYABLE = "icx_payable";
    public static final String ICX_TO_CLAIM = "icx_to_claim";
    public static final String UNSTAKE_DICT = "unstake_dict";
    public static final String USER_DELEGATION_PERCENTAGE = "user_delegation_percentage";
    public static final String STATUS_MANAGER = "status_manager";
    public static final String VERSION = "version";
    public static final String PREP_DELEGATION_ICX = "prep_delegation_icx";

    public static final String PRODUCTIVITY = "productivity";
    public static final String FEE_PERCENTAGE ="fee_percentage";
    public static final String FEE_ADDRESS ="fee_distribution_address";
    public static final String OMM_LENDING_POOL_CORE ="omm_lending_pool_address";
    public static final String OMM_DELEGATION ="omm_delegation_address";

    public static DelegationListDBSdo DEFAULT_DELEGATION_LIST = getDelegationList();

    private static BigInteger pow10(int exponent) {
        BigInteger result = BigInteger.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(BigInteger.TEN);
        }
        return result;
    }

    private static Address getSystemScoreAddress() {
        byte[] rawAddress = new byte[Address.LENGTH];
        rawAddress[0] = 1;
        return new Address(rawAddress);
    }

    private static DelegationListDBSdo getDelegationList() {
        DelegationListDBSdo delegationList = new DelegationListDBSdo();
        delegationList.setDelegationList(List.of());
        return delegationList;
    }
}
