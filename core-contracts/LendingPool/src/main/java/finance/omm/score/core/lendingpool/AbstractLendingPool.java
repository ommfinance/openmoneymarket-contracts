package finance.omm.score.core.lendingpool;

import com.eclipsesource.json.JsonObject;
import finance.omm.core.score.interfaces.LendingPool;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Authorization;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.lendingpool.exception.LendingPoolException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import scorex.util.ArrayList;

public abstract class AbstractLendingPool extends AddressProvider
        implements LendingPool, Authorization<LendingPoolException> {

    public static final String TAG = "Lending Pool";

    public static final String START_HEIGHT = "startHeight";
    public static final String TXN_COUNT = "txnCount";

    private static final BigInteger TERM_LENGTH = BigInteger.valueOf(43120L);
    private static final int BATCH_SIZE = 50;

    public static final String BORROW_WALLETS = "borrowWallets";
    public static final String DEPOSIT_WALLETS = "depositWallets";
    public static final String BORROW_INDEX = "borrowIndex";
    public static final String DEPOSIT_INDEX = "depositIndex";
    public static final String FEE_SHARING_USERS = "feeSharingUsers";
    public static final String FEE_SHARING_TXN_LIMIT = "feeSharingTxnLimit";
    public static final String BRIDGE_FEE_THRESHOLD = "bridgeFeeThreshold";
    public static final String LIQUIDATION_STATUS = "liquidationStatus";

    public final ArrayDB<Address> borrowWallets = Context.newArrayDB(BORROW_WALLETS, Address.class);
    public final ArrayDB<Address> depositWallets = Context.newArrayDB(DEPOSIT_WALLETS, Address.class);
    public final DictDB<Address, BigInteger> borrowIndex = Context.newDictDB(BORROW_INDEX, BigInteger.class);
    public final DictDB<Address, BigInteger> depositIndex = Context.newDictDB(DEPOSIT_INDEX, BigInteger.class);
    public final BranchDB<Address, DictDB<String, BigInteger>> feeSharingUsers = Context.newBranchDB(FEE_SHARING_USERS, BigInteger.class);
    public final VarDB<BigInteger> feeSharingTxnLimit = Context.newVarDB(FEE_SHARING_TXN_LIMIT, BigInteger.class);
    public final VarDB<BigInteger> bridgeFeeThreshold = Context.newVarDB(BRIDGE_FEE_THRESHOLD, BigInteger.class);
    public final VarDB<Boolean> liquidationStatus = Context.newVarDB(LIQUIDATION_STATUS,Boolean.class);

    public AbstractLendingPool(Address addressProvider) {
        super(addressProvider, false);
        if (bridgeFeeThreshold.get() == null) {
            bridgeFeeThreshold.set(BigInteger.ZERO);
            feeSharingTxnLimit.set(BigInteger.valueOf(50));
        }
    }

    /*EventLogs*/
    @EventLog(indexed = 3)
    public void Deposit(Address _reserve, Address _sender, BigInteger _amount){}

    @EventLog(indexed = 3)
    public void Borrow(Address _reserve, Address _user, BigInteger _amount,
                       BigInteger _borrowRate, BigInteger _borrowFee, BigInteger _borrowBalanceIncrease){}

    @EventLog(indexed = 3)
    public void RedeemUnderlying(Address _reserve, Address _user, BigInteger _amount) {}

    @EventLog(indexed = 3)
    public void Repay(Address _reserve, Address _user, BigInteger _paybackAmount, BigInteger _originationFee,
                      BigInteger _borrowBalanceIncrease) {}

    protected boolean hasUserDepositBridgeOToken(Address user) {
        BigInteger balance = call(BigInteger.class, Contracts.BRIDGE_O_TOKEN, "balanceOf", user);
        return balance.compareTo(bridgeFeeThreshold.get()) > 0;
    }

    protected boolean isFeeSharingEnabled(Address user) {
        BigInteger currentBlockHeight = BigInteger.valueOf(Context.getBlockHeight());
        DictDB<String, BigInteger> userFeeSharing = feeSharingUsers.at(user);
        if (hasUserDepositBridgeOToken(user)) {
            if (userFeeSharing.get(START_HEIGHT) == null) {
                userFeeSharing.set(START_HEIGHT, currentBlockHeight);
            }
            if (userFeeSharing.get(START_HEIGHT).add(TERM_LENGTH).compareTo(currentBlockHeight) > 0) {
                BigInteger count = userFeeSharing.getOrDefault(TXN_COUNT, BigInteger.ZERO);
                if (count.compareTo(feeSharingTxnLimit.get()) < 0) {
                    userFeeSharing.set(TXN_COUNT, count.add(BigInteger.ONE));
                    return true;
                }
            } else {
                userFeeSharing.set(START_HEIGHT, currentBlockHeight);
                userFeeSharing.set(TXN_COUNT, BigInteger.ONE);
                return true;
            }
        }
        return false;
    }

    protected void checkAndEnableFeeSharing() {
        if (isFeeSharingEnabled(Context.getCaller())) {
            Context.setFeeSharingProportion(100);
        }
    }

    protected void deposit(Address reserve, BigInteger amount, Address sender) {
        checkAndEnableFeeSharing();

        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);

        if (reserveData.isEmpty()) {
            throw LendingPoolException.unknown(TAG + "reserve data is empty :: " + reserve);
        }

        boolean isActive = (boolean) reserveData.get("isActive");
        if (!isActive) {
            throw LendingPoolException.reserveNotActive("Reserve is not active, deposit unsuccessful");
        }

        boolean isFreezed = (boolean) reserveData.get("isFreezed");
        if (isFreezed) {
            throw LendingPoolException.unknown("Reserve is frozen, deposit unsuccessful");
        }

        if ( depositIndex.get(sender) == null ) {
            depositWallets.add(sender);
            depositIndex.set(sender, BigInteger.valueOf(depositWallets.size()));
        }

        call(Contracts.LENDING_POOL_CORE, "updateStateOnDeposit", reserve, sender, amount);

        Address oToken = (Address) reserveData.get("oTokenAddress");
        call(oToken, "mintOnDeposit", sender, amount);

        BigInteger icxValue = Context.getValue();
        Address lendingPoolCore = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        if (reserve.equals(getAddress(Contracts.sICX.getKey())) && !icxValue.equals(BigInteger.ZERO)) {
            amount = (BigInteger) Context.call(icxValue, getAddress(Contracts.STAKING.getKey()),"stakeICX",
                    lendingPoolCore);

        } else {
            call(reserve, "transfer", lendingPoolCore, amount);
        }
        Deposit(reserve, sender, amount);
    }

    protected void redeemUnderlying(Address reserve, Address user, Address oToken,
                                    BigInteger amount, boolean waitForUnstaking) {

        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);

        boolean isActive = (boolean) reserveData.get("isActive");
        if (! isActive) {
            throw LendingPoolException.reserveNotActive("Reserve is not active, withdraw unsuccessful");
        }

        BigInteger availableLiquidity = (BigInteger) reserveData.get("availableLiquidity");
        if (availableLiquidity.compareTo(amount) < 0) {
            throw LendingPoolException.unknown("Amount " + amount + " is more than available liquidity " +
                    availableLiquidity);
        }

        call(Contracts.LENDING_POOL_CORE, "updateStateOnRedeem", reserve, user, amount);

        JsonObject data = new JsonObject();
        Address to = user;

        if ( waitForUnstaking ) {
            if (! oToken.equals(getAddress(Contracts.oICX.getKey()))) {
                throw LendingPoolException.unknown("Redeem with wait for unstaking failed: Invalid token");
            }
//            data = new JsonObject();
            data.add("method","unstake");
            data.add("user", user.toString());
            to = getAddress(Contracts.STAKING.getKey());
        }
        call(Contracts.LENDING_POOL_CORE, "transferToUser", reserve, to, amount, data.toString().getBytes());
        RedeemUnderlying(reserve, user, amount);
    }

    protected void repay(Address reserve, BigInteger amount, Address sender) {
        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);

        if (reserveData.isEmpty()) {
            throw LendingPoolException.unknown(TAG + "reserve data is empty :: " + reserve);
        }

        boolean isActive = (boolean) reserveData.get("isActive");
        if (!isActive) {
            throw LendingPoolException.reserveNotActive("Reserve is not active, withdraw unsuccessful");
        }

        Map<String, BigInteger> borrowData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBorrowBalances", reserve, sender);
        Map<String, BigInteger> userBasicReserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getUserBasicReserveData", reserve, sender);

        BigInteger compoundedBorrowBalance = borrowData.get("compoundedBorrowBalance");

        if (compoundedBorrowBalance.compareTo(BigInteger.ZERO) <= 0) {
            throw LendingPoolException.unknown("The user does not have any borrow pending");
        }


        BigInteger originationFee = userBasicReserveData.get("originationFee");
        BigInteger paybackAmount = compoundedBorrowBalance.add(originationFee);
        BigInteger returnAmount = BigInteger.ZERO;

        if (amount.compareTo(paybackAmount) < 0) {
            paybackAmount = amount;
        } else {
            returnAmount = amount.subtract(paybackAmount);
        }

        BigInteger borrowBalanceIncrease = borrowData.get("borrowBalanceIncrease");

        if (paybackAmount.compareTo(originationFee) <= 0) {
            call(Contracts.LENDING_POOL_CORE, "updateStateOnRepay", reserve, sender,
                    BigInteger.ZERO, paybackAmount, borrowBalanceIncrease, false);
            call(reserve, "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), paybackAmount);
            Repay(reserve, sender, BigInteger.ZERO, paybackAmount, borrowBalanceIncrease);
            return;
        }

        BigInteger paybackAmountMinusFees = paybackAmount.subtract(originationFee);
        call(Contracts.LENDING_POOL_CORE, "updateStateOnRepay", reserve, sender, paybackAmountMinusFees,
                originationFee, borrowBalanceIncrease, compoundedBorrowBalance.equals(paybackAmountMinusFees));

        if (originationFee.compareTo(BigInteger.ZERO) > 0) {
            call(reserve, "transfer", getAddress(Contracts.FEE_PROVIDER.getKey()), originationFee);
        }

        call(reserve, "transfer", getAddress(Contracts.LENDING_POOL_CORE.getKey()), paybackAmountMinusFees);

        if (returnAmount.compareTo(BigInteger.ZERO) > 0) {
            call(reserve, "transfer", sender, returnAmount);
        }
        Repay(reserve, sender, paybackAmountMinusFees, originationFee, borrowBalanceIncrease);
    }

    protected void liquidationCall(Address collateral, Address reserve, Address user,
                                   BigInteger purchaseAmount, Address sender) {
        if (!isLiquidationEnabled()){
            throw LendingPoolException.liquidationDisabled("Liquidation is not enabled,liquidation unsuccessful");
        }

        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", reserve);
        if (reserveData.isEmpty()) {
            throw LendingPoolException.unknown(TAG + "reserve data is empty :: " + reserve);
        }

        Map<String, Object> collateralData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", collateral);

        if (collateralData.isEmpty()) {
            throw LendingPoolException.unknown(TAG + "collateral data is empty :: " + collateral);
        }

        boolean isReserveActive = (boolean) reserveData.get("isActive");
        if (!isReserveActive) {
            throw LendingPoolException.reserveNotActive("Borrow reserve is not active,liquidation unsuccessful");
        }

        boolean isCollateralActive = (boolean) collateralData.get("isActive");
        if (!isCollateralActive) {
            throw LendingPoolException.reserveNotActive("Collateral reserve is not active,liquidation unsuccessful");
        }
        Map<String, BigInteger> liquidation = call(Map.class, Contracts.LIQUIDATION_MANAGER, "liquidationCall",
                collateral, reserve, user, purchaseAmount);
        BigInteger amountToLiquidate = liquidation.get("actualAmountToLiquidate");
        call(Contracts.LENDING_POOL_CORE, "transferToUser", collateral, sender,
                liquidation.get("maxCollateralToLiquidate"));
        call(reserve, "transfer", getAddress(Contracts.LENDING_POOL_CORE.getKey()), amountToLiquidate);
        if (purchaseAmount.compareTo(amountToLiquidate) > 0){
            call(reserve, "transfer", sender, purchaseAmount.subtract(amountToLiquidate));
        }
    }

    protected List<Address> getArrayItems(ArrayDB<Address> array, int index) {
        int length = array.size();
        int start = index * BATCH_SIZE;
        List<Address> list = new ArrayList<>();
        if (start >= length) {
            return list;
        }
        int end = start + BATCH_SIZE;
        if (end > length) {
            end = length;
        }
        for (int i = start; i < end; i++) {
            list.add(array.get(i));
        }
        return list;
    }

    public <K> K call(Class<K> kClass, Address contract, String method, Object... params) {
        return Context.call(kClass, contract, method, params);
    }
}