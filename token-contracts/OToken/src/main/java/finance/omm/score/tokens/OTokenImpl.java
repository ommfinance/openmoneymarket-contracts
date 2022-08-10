package finance.omm.score.tokens;

import finance.omm.core.score.interfaces.OToken;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.libs.structs.UserDetails;
import finance.omm.utils.constants.AddressConstant;
import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.convertExaToOther;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.exaDivide;
import static finance.omm.utils.math.MathUtils.exaMultiply;


/**
 * Implementation of IRC2
 */
public class OTokenImpl extends AddressProvider implements OToken {

    public static final String TAG = "Omm oToken";
    private static final String NAME = "token_name";
    private static final String SYMBOL = "token_symbol";
    private static final String DECIMALS = "decimals";
    private static final String TOTAL_SUPPLY = "total_supply";
    private static final String BALANCES = "balances";
    private static final String USER_INDEXES = "user_indexes";
    private static final String HANDLE_ACTION_ENABLED = "handle_action_enabled";
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger N_ONE = BigInteger.ONE.negate();

    /*
    Variable Definition
    */

    private final VarDB<String> name = Context.newVarDB(NAME, String.class);
    private final VarDB<String> symbol = Context.newVarDB(SYMBOL, String.class);
    private final VarDB<BigInteger> decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    private final VarDB<BigInteger> totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    private final DictDB<Address, BigInteger> balances = Context.newDictDB(BALANCES, BigInteger.class);
    private final DictDB<Address, BigInteger> userIndexes = Context.newDictDB(USER_INDEXES, BigInteger.class);
    private final VarDB<Boolean> handleActionEnabled = Context.newVarDB(HANDLE_ACTION_ENABLED, Boolean.class);

    /**
     * Variable Initialization.
     *
     * @param _addressProvider: The address of the addressProvider SCORE.
     * @param _name:            The name of the token.
     * @param _symbol:          The symbol of the token.
     * @param _decimals:        The number of decimals. Set to 18 by default.
     */
    public OTokenImpl(Address _addressProvider, String _name, String _symbol, BigInteger _decimals) {
        super(_addressProvider, false);

        if (handleActionEnabled.get() == null) {
            handleActionEnabled.set(true);
        }

        if (totalSupply.get() == null) {
            if (_symbol.isEmpty()) {
                Context.revert("Invalid Symbol name");
            }

            if (_name.isEmpty()) {
                Context.revert("Invalid Token Name");
            }

            if (_decimals.compareTo(ZERO) < 0) {
                Context.revert("Decimals cannot be less than zero");
            }
            this.name.set(_name);
            this.symbol.set(_symbol);
            this.decimals.set(_decimals);
            this.totalSupply.set(ZERO);
        }
    }

    public void onUpdate() {
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    }

    @EventLog(indexed = 3)
    public void Redeem(Address _from, BigInteger _value, BigInteger _fromBalanceIncrease, BigInteger _fromIndex) {
    }

    @EventLog(indexed = 3)
    public void MintOnDeposit(Address _from, BigInteger _value, BigInteger _fromBalanceIncrease, BigInteger _fromIndex) {
    }

    @EventLog(indexed = 3)
    public void BurnOnLiquidation(Address _from, BigInteger _value, BigInteger _fromBalanceIncrease, BigInteger _fromIndex) {
    }

    @EventLog(indexed = 3)
    public void BalanceTransfer(Address _from, Address _to, BigInteger _value, BigInteger _fromBalanceIncrease,
                                BigInteger _toBalanceIncrease, BigInteger _fromIndex, BigInteger _toIndex) {
    }

    /**
     * Returns the name of the token
     */
    @External(readonly = true)
    public String name() {
        return this.name.get();
    }

    /**
     * Returns the symbol of the token
     */
    @External(readonly = true)
    public String symbol() {
        return this.symbol.get();
    }

    /**
     * Returns the number of decimals
     * For example, if the decimals = 2, a balance of 25 tokens
     * should be displayed to the user as (25 * 10 ** 2)
     * Tokens usually opt for value of 18. It is also the IRC2
     * uses by default. It can be changed by passing required
     * number of decimals during initialization.
     */
    @External(readonly = true)
    public BigInteger decimals() {
        return this.decimals.getOrDefault(ZERO);
    }

    @External(readonly = true)
    public BigInteger principalTotalSupply() {
        return this.totalSupply.getOrDefault(ZERO);
    }

    /**
     * Returns the total number of tokens in existence
     */
    @External(readonly = true)
    public BigInteger totalSupply() {
        Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

        if (lendingPoolCoreAddress == null) {
            Context.revert(Contracts.LENDING_POOL_CORE.getKey() + " is not configured");
        }

        if (reserveAddress == null) {
            Context.revert(Contracts.RESERVE.getKey() + " is not configured");
        }

        BigInteger borrowIndex = Context.call(
                BigInteger.class,
                lendingPoolCoreAddress,
                "getReserveLiquidityCumulativeIndex",
                reserveAddress);

        BigInteger principalTotalSupply = this.principalTotalSupply();
        if (borrowIndex.equals(ZERO)) {
            return this.totalSupply.getOrDefault(ZERO);
        } else {
            BigInteger actualDecimals = this.decimals.getOrDefault(ZERO);
            BigInteger normalizedIncome = Context.call(BigInteger.class,
                    lendingPoolCoreAddress,
                    "getNormalizedIncome",
                    reserveAddress);
            BigInteger newBalance = exaDivide(
                    exaMultiply(
                            convertToExa(principalTotalSupply, actualDecimals),
                            normalizedIncome
                    ),
                    borrowIndex);
            return convertExaToOther(newBalance, actualDecimals.intValue());
        }
    }

    @External(readonly = true)
    public BigInteger getUserLiquidityCumulativeIndex(Address _user) {
        return this.userIndexes.getOrDefault(_user, ZERO);
    }

    protected BigInteger calculateCumulatedBalanceInternal(Address user, BigInteger balance) {

        BigInteger userIndex = this.userIndexes.getOrDefault(user, ZERO);

        if (userIndex.equals(ZERO)) {
            return balance;
        } else {
            Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
            Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

            if (lendingPoolCoreAddress == null) {
                Context.revert(Contracts.LENDING_POOL_CORE.getKey() + " is not configured");
            }

            if (reserveAddress == null) {
                Context.revert(Contracts.RESERVE.getKey() + " is not configured");
            }

            BigInteger actualDecimals = this.decimals.getOrDefault(ZERO);
            BigInteger newBalance = exaDivide(
                    exaMultiply(convertToExa(balance, actualDecimals),
                            Context.call(BigInteger.class,
                                    lendingPoolCoreAddress,
                                    "getNormalizedIncome",
                                    reserveAddress)),
                    userIndex);
            return convertExaToOther(newBalance, actualDecimals.intValue());
        }
    }

    protected Map<String, BigInteger> cumulateBalanceInternal(Address user) {
        BigInteger previousPrincipalBalance = principalBalanceOf(user);
        BigInteger balanceIncrease = this.balanceOf(user).subtract(previousPrincipalBalance);
        if (balanceIncrease.compareTo(ZERO) > 0) {
            this.mint(user, balanceIncrease);
        }

        Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

        if (lendingPoolCoreAddress == null) {
            Context.revert(Contracts.LENDING_POOL_CORE.getKey() + " is not configured");
        }

        if (reserveAddress == null) {
            Context.revert(Contracts.RESERVE.getKey() + " is not configured");
        }

        BigInteger userIndex = Context.call(BigInteger.class, lendingPoolCoreAddress, "getNormalizedIncome", reserveAddress);
        this.userIndexes.set(user, userIndex);
        return Map.of(
                "previousPrincipalBalance", previousPrincipalBalance,
                "principalBalance", previousPrincipalBalance.add(balanceIncrease),
                "balanceIncrease", balanceIncrease,
                "index", userIndex);
    }

    /**
     * This will always include accrued interest as a computed value
     */
    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        BigInteger currentPrincipalBalance = this.principalBalanceOf(_owner);

        return calculateCumulatedBalanceInternal(_owner, currentPrincipalBalance);
    }

    /**
     * This shows the state updated balance and includes the accrued interest upto the most recent computation
     * initiated by the user transaction
     */
    @External(readonly = true)
    public BigInteger principalBalanceOf(Address _user) {
        return this.balances.getOrDefault(_user, ZERO);
    }

    /**
     * The transfer is only allowed if transferring this amount of the underlying collateral doesn't bring the health
     * factor below 1
     */
    @External(readonly = true)
    public boolean isTransferAllowed(Address _user, BigInteger _amount) {
        Address lendingPoolDataProviderAddress = getAddress(Contracts.LENDING_POOL_DATA_PROVIDER.getKey());
        Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

        if (lendingPoolDataProviderAddress == null) {
            Context.revert(Contracts.LENDING_POOL_DATA_PROVIDER.getKey() + " is not configured");
        }

        if (reserveAddress == null) {
            Context.revert(Contracts.RESERVE.getKey() + " is not configured");
        }

        return Context.call(Boolean.class, lendingPoolDataProviderAddress, "balanceDecreaseAllowed",
                reserveAddress, _user, _amount);
    }

    @External(readonly = true)
    public SupplyDetails getPrincipalSupply(Address _user) {
        SupplyDetails supplyDetails = new SupplyDetails();
        supplyDetails.decimals = this.decimals();
        supplyDetails.principalUserBalance = this.principalBalanceOf(_user);
        supplyDetails.principalTotalSupply = this.principalTotalSupply();
        return supplyDetails;
    }

    @External
    public void enableHandleAction() {
        onlyOrElseThrow(Contracts.GOVERNANCE, OMMException.unknown("Only Governance contract can call this method"));
        handleActionEnabled.set(true);
    }

    @External
    public void disableHandleAction() {
        onlyOrElseThrow(Contracts.GOVERNANCE, OMMException.unknown("Only Governance contract can call this method"));
        handleActionEnabled.set(false);
    }

    @External(readonly = true)
    public boolean isHandleActionEnabled() {
        return handleActionEnabled.get();
    }

    /**
     * Redeems certain amount of tokens to get the equivalent amount of underlying asset.
     *
     * @param _user: The address of user redeeming assets.
     * @param _amount: The amount of oToken.
     */
    @External
    public Map<String, ?> redeem(Address _user, BigInteger _amount) {
        onlyLendingPool();

        BigInteger beforeTotalSupply = this.principalTotalSupply();

        if (_amount.compareTo(ZERO) <= 0 && !_amount.equals(N_ONE)) {
            Context.revert(TAG + ": Amount: " + _amount + " to redeem needs to be greater than zero");
        }

        Map<String, BigInteger> cumulated = this.cumulateBalanceInternal(_user);
        BigInteger currentBalance = cumulated.get("principalBalance");
        BigInteger balanceIncrease = cumulated.get("balanceIncrease");
        BigInteger index = cumulated.get("index");
        BigInteger amountToRedeem = _amount;
        if (_amount.equals(N_ONE)) {
            amountToRedeem = currentBalance;
        }
        if (amountToRedeem.compareTo(currentBalance) > 0) {
            Context.revert(TAG + ": Redeem amount: " + amountToRedeem + " is more than user balance " + currentBalance);
        }
        if (!this.isTransferAllowed(_user, amountToRedeem)) {
            Context.revert(TAG + ": Transfer of amount " + amountToRedeem + " to the user is not allowed");
        }
        this.burn(_user, amountToRedeem);

        if (currentBalance.equals(amountToRedeem)) {
            this.resetDataOnZeroBalanceInternal(_user);
            index = ZERO;
        }

        this.handleAction(_user, cumulated.get("previousPrincipalBalance"), beforeTotalSupply);

        this.Redeem(_user, amountToRedeem, balanceIncrease, index);
        return Map.of(
                "reserve", getAddress(Contracts.RESERVE.getKey()),
                "amountToRedeem", amountToRedeem
        );
    }

    protected void handleAction(Address _user, BigInteger _userBalance, BigInteger _totalSupply) {
        Context.require(handleActionEnabled.get(), "Handle Action Disabled.");

        Address rewardsAddress = getAddress(Contracts.REWARDS.getKey());
        if (rewardsAddress == null) {
            Context.revert(Contracts.REWARDS.getKey() + " is not configured");
        }

        UserDetails userDetails = new UserDetails();
        userDetails._user = _user;
        userDetails._userBalance = _userBalance;
        userDetails._totalSupply = _totalSupply;
        userDetails._decimals = this.decimals();

        Context.call(rewardsAddress, "handleAction", userDetails);
    }

    protected void resetDataOnZeroBalanceInternal(Address user) {
        this.userIndexes.set(user, ZERO);
    }

    @External
    public void mintOnDeposit(Address _user, BigInteger _amount) {
        onlyLendingPool();
        BigInteger beforeTotalSupply = this.principalTotalSupply();
        Map<String, BigInteger> cumulated = this.cumulateBalanceInternal(_user);

        BigInteger balanceIncrease = cumulated.get("balanceIncrease");
        BigInteger index = cumulated.get("index");

        this.mint(_user, _amount);
        this.handleAction(_user, cumulated.get("previousPrincipalBalance"), beforeTotalSupply);
        this.MintOnDeposit(_user, _amount, balanceIncrease, index);
    }

    @External
    public void burnOnLiquidation(Address _user, BigInteger _value) {

        onlyOrElseThrow(Contracts.LIQUIDATION_MANAGER,
                OMMException.unknown(TAG
                        + ":  SenderNotAuthorized: (sender)" + Context.getCaller()
                        + " (liquidation)" + getAddress(Contracts.LIQUIDATION_MANAGER.getKey()) + "}"));

        BigInteger beforeTotalSupply = this.principalTotalSupply();
        Map<String, BigInteger> cumulated = this.cumulateBalanceInternal(_user);
        BigInteger currentBalance = cumulated.get("principalBalance");
        BigInteger balanceIncrease = cumulated.get("balanceIncrease");
        BigInteger index = cumulated.get("index");
        this.burn(_user, _value);
        this.handleAction(_user, cumulated.get("previousPrincipalBalance"), beforeTotalSupply);
        if (currentBalance.equals(_value)) {
            this.resetDataOnZeroBalanceInternal(_user);
            index = ZERO;
        }
        this.BurnOnLiquidation(_user, _value, balanceIncrease, index);
    }

    protected Map<String, BigInteger> executeTransfer(Address from, Address to, BigInteger value) {
        BigInteger beforeTotalSupply = this.principalTotalSupply();
        Map<String, BigInteger> fromCumulated = this.cumulateBalanceInternal(from);
        Map<String, BigInteger> toCumulated = this.cumulateBalanceInternal(to);
        BigInteger fromBalance = fromCumulated.get("principalBalance");
        BigInteger fromBalanceIncrease = fromCumulated.get("balanceIncrease");
        BigInteger fromIndex = fromCumulated.get("index");
        BigInteger toBalanceIncrease = toCumulated.get("balanceIncrease");
        BigInteger toIndex = toCumulated.get("index");

        if (fromBalance.equals(value)) {
            this.resetDataOnZeroBalanceInternal(from);
            fromIndex = ZERO;
        }

        this.BalanceTransfer(from, to, value, fromBalanceIncrease, toBalanceIncrease, fromIndex, toIndex);
        return Map.of(
                "fromPreviousPrincipalBalance", fromCumulated.get("previousPrincipalBalance"),
                "toPreviousPrincipalBalance", toCumulated.get("previousPrincipalBalance"),
                "beforeTotalSupply", beforeTotalSupply
        );
    }

    protected void callRewards(BigInteger fromPrevious, BigInteger toPrevious, BigInteger totalPrevious,
                               Address from, Address to) {
        this.handleAction(from, fromPrevious, totalPrevious);
        this.handleAction(to, toPrevious, totalPrevious);
    }

    public void onlyLendingPool() {
        onlyOrElseThrow(Contracts.LENDING_POOL,
                OMMException.unknown(TAG
                        + ":  SenderNotAuthorized: (sender)" + Context.getCaller()
                        + " (lendingPool)" + getAddress(Contracts.LENDING_POOL.getKey()) + "}"));
    }

    /**
     * Transfers certain amount of tokens from sender to the receiver.
     *
     * @param _to:    The account to which the token is to be transferred.
     * @param _value: The no. of tokens to be transferred.
     * @param _data:  Any information or message
     */
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        if (_data == null || _data.length == 0) {
            _data = "None".getBytes();
        }
        this.transfer(Context.getCaller(), _to, _value, _data);
    }

    /**
     * Transfers certain amount of tokens from sender to the recipient.
     * This is an internal function.
     *
     * @param from:  The account from which the token is to be transferred.
     * @param to:    The account to which the token is to be transferred.
     * @param value: The no. of tokens to be transferred.
     * @param data:  Any information or message
     */
    protected void transfer(Address from, Address to, BigInteger value, byte[] data) {
        if (value.compareTo(ZERO) < 0) {
            Context.revert(TAG + ": Transferring value:" + value + " cannot be less than 0.");
        }

        BigInteger balanceFrom = this.balances.getOrDefault(from, ZERO);
        if (balanceFrom.compareTo(value) < 0) {
            Context.revert(TAG + " : Token transfer error:Insufficient balance: " + balanceFrom);
        }

        if (!this.isTransferAllowed(from, value)) {
            Context.revert(TAG + ":  Transfer error:Transfer cannot be allowed");
        }

        Map<String, BigInteger> previousBalances = this.executeTransfer(from, to, value);
        this.balances.set(from, balanceFrom.subtract(value));
        this.balances.set(to, this.balances.getOrDefault(to, ZERO).add(value));
        this.callRewards(previousBalances.get("fromPreviousPrincipalBalance"),
                previousBalances.get("toPreviousPrincipalBalance"), previousBalances.get("beforeTotalSupply"),
                from,
                to);

        // Emits an event log `Transfer`
        this.Transfer(from, to, value, data);

        if (to.isContract()) {
            /*
            If the recipient is SCORE,
            then calls `tokenFallback` to hand over control.
            */
            Context.call(to, "tokenFallback", from, value, data);
        }
    }

    /**
     * Creates amount number of tokens, and assigns to account
     * Increases the balance of that account and total supply.
     * This is an internal function.
     *
     * @param account: The account at which token is to be created.
     * @param amount:  Number of tokens to be created at the `account`.
     */
    protected void mint(Address account, BigInteger amount) {

        if (amount.compareTo(ZERO) < 0) {
            Context.revert(TAG + ": Invalid value: " + amount + " to mint");
        }

        this.totalSupply.set(this.totalSupply.get().add(amount));
        this.balances.set(account, this.balances.getOrDefault(account, ZERO).add(amount));

        // Emits an event log Mint
        this.Transfer(AddressConstant.ZERO_ADDRESS, account, amount, "mint".getBytes());
    }

    /**
     * Destroys `amount` number of tokens from `account`
     * Decreases the balance of that `account` and total supply.
     * This is an internal function.
     *
     * @param account: The account at which token is to be destroyed.
     * @param amount:  The `amount` of tokens of `account` to be destroyed.
     */
    protected void burn(Address account, BigInteger amount) {
        if (amount.equals(ZERO)) {
            return;
        }

        if (amount.compareTo(ZERO) < 0) {
            Context.revert(TAG + ": Invalid value: " + amount + " to burn");
        }
        BigInteger actualTotalSupply = this.totalSupply.get();
        BigInteger userBalance = this.balances.getOrDefault(account, ZERO);
        if (amount.compareTo(actualTotalSupply) > 0) {
            Context.revert(TAG + ": " + amount + " is greater than total supply :" + actualTotalSupply);
        }
        if (amount.compareTo(userBalance) > 0) {
            Context.revert(TAG + ": Cannot burn more than user balance. Amount to burn: " + amount + ", User Balance:" + userBalance);
        }

        this.totalSupply.set(actualTotalSupply.subtract(amount));
        this.balances.set(account, userBalance.subtract(amount));

        // Emits an event log Burn
        this.Transfer(account, AddressConstant.ZERO_ADDRESS, amount, "burn".getBytes());
    }

    /**
     * return total supply for reward distribution
     *
     * @return: total supply and its precision
     */
    @External(readonly = true)
    public TotalStaked getTotalStaked() {
        TotalStaked totalStaked = new TotalStaked();
        totalStaked.decimals = this.decimals();
        totalStaked.totalStaked = this.principalTotalSupply();
        return totalStaked;
    }
}
