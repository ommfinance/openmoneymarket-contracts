package finance.omm.score.tokens;

import java.math.BigInteger;

import finance.omm.core.score.interfaces.DToken;
import finance.omm.libs.address.AddressProvider;
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

import static finance.omm.utils.math.MathUtils.*;
import finance.omm.libs.address.Contracts;


/**
Implementation of IRC2
**/
public class DTokenImpl extends AddressProvider implements DToken {

    public static final String TAG = "DToken";
    public static final BigInteger ZERO = BigInteger.ZERO;

    private static final String NAME = "token_name";
    private static final String SYMBOL = "token_symbol";
    private static final String DECIMALS = "decimals";
    private static final String TOTAL_SUPPLY = "total_supply";
    private static final String BALANCES = "balances";
    private static final String USER_INDEXES = "user_indexes";
    private static final String HANDLE_ACTION_ENABLED = "handle_action_enabled";

    /**
    Variable Definition
    **/
    private final VarDB<String> _name = Context.newVarDB(NAME, String.class);
    private final VarDB<String> _symbol = Context.newVarDB(SYMBOL, String.class);
    private final VarDB<BigInteger> _decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    private final VarDB<BigInteger> _totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    private final DictDB<Address, BigInteger> _balances = Context.newDictDB(BALANCES, BigInteger.class);
    private final DictDB<Address, BigInteger> _userIndexes = Context.newDictDB(USER_INDEXES, BigInteger.class);
    private final VarDB<Boolean> _handleActionEnabled = Context.newVarDB(HANDLE_ACTION_ENABLED, Boolean.class);

    /***
    Variable Initialization.
    :param _addressProvider: the address of addressProvider
    :param _name: The name of the token.
    :param _symbol: The symbol of the token.
    :param _decimals: The number of decimals. Set to 18 by default.
    ***/
    public DTokenImpl(Address _addressProvider, String _name, String _symbol, BigInteger _decimals, boolean _update) {
        super(_addressProvider, _update);

        if (_handleActionEnabled.get() == null) {
            _handleActionEnabled.set(true);
        }

        if (_totalSupply.get() == null) {
            if (_symbol.isEmpty()) {
                Context.revert("Invalid Symbol name");
            }

            if (_name.isEmpty()) {
                Context.revert("Invalid Token Name");
            }

            if (_decimals.compareTo(ZERO) < 0) {
                Context.revert("Decimals cannot be less than zero");
            }

            this._name.set(_name);
            this._symbol.set(_symbol);
            this._decimals.set(_decimals);
            this._totalSupply.set(ZERO);
        }
    }

    public void onUpdate() {
        Context.println(TAG + "| updating dtoken");
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {        
    }

    @EventLog(indexed=3)
    public void MintOnBorrow(Address _from, BigInteger _value, BigInteger  _fromBalanceIncrease, BigInteger _fromIndex) {        
    }

    @EventLog(indexed=3)
    public void BurnOnRepay(Address _from, BigInteger _value, BigInteger _fromBalanceIncrease,BigInteger _fromIndex){        
    }

    @EventLog(indexed=3)
    public void  BurnOnLiquidation(Address _from, BigInteger _value, BigInteger _fromBalanceIncrease,BigInteger _fromIndex){        
    }

    /**
    Returns the name of the token
    **/
    @External(readonly = true)
    public String name() {
        return this._name.get();
    }

    /**
    Returns the symbol of the token
    **/
    @External(readonly = true)
    public String symbol() {
        return this._symbol.get();
    }

    /***
    Returns the number of decimals
    For example, if the decimals = 2, a balance of 25 tokens
    should be displayed to the user as (25 * 10 ** 2)
    Tokens usually opt for value of 18. It is also the IRC2
    uses by default. It can be changed by passing required
    number of decimals during initialization.
    ***/
    @External(readonly = true)
    public BigInteger decimals() {
        return this._decimals.getOrDefault(ZERO);
    }
    

    @External(readonly = true)
    public BigInteger getUserBorrowCumulativeIndex(Address _user) {
        return this._userIndexes.getOrDefault(_user, ZERO);
    }

    public BigInteger _calculateCumulatedBalanceInternal(Address _user, BigInteger _balance) {
       Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
       Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

       BigInteger userIndex = this._userIndexes.getOrDefault(_user, ZERO);
       if (userIndex.equals(ZERO)) {
           return _balance;
       }else {
           BigInteger decimals = this._decimals.getOrDefault(ZERO);

           BigInteger balance = exaDivide(exaMultiply(convertToExa(_balance, decimals),
                   Context.call(BigInteger.class,
                           lendingPoolCoreAddress,
                           "getNormalizedDebt",
                           reserveAddress)),
                   userIndex);
           return convertExaToOther(balance, decimals.intValue());
       }
    }

    /***
     * This shows the state updated balance and includes the accrued interest upto the most recent computation initiated by the user transaction
     ***/
    @External(readonly = true)
    public BigInteger principalBalanceOf(Address _user) {
        return this._balances.getOrDefault(_user, ZERO);
    }
    

    /***
     * This will always include accrued interest as a computed value
     ***/
    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        BigInteger currentPrincipalBalance = principalBalanceOf(_owner);
        BigInteger balance = this._calculateCumulatedBalanceInternal(_owner, currentPrincipalBalance);

        return balance;
    }

    @External(readonly = true)
    public BigInteger principalTotalSupply() {
        return this._totalSupply.getOrDefault(ZERO);
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
        _handleActionEnabled.set(true);
    }

    @External
    public void disableHandleAction() {
        onlyOrElseThrow(Contracts.GOVERNANCE, OMMException.unknown("Only Governance contract can call this method"));
        _handleActionEnabled.set(false);
    }

    /***
     * Returns the total number of tokens in existence 
     ***/
    @External(readonly = true)
    public BigInteger totalSupply() {
        Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

        BigInteger borrowIndex  = Context.call(BigInteger.class,
                lendingPoolCoreAddress,
                "getReserveBorrowCumulativeIndex",
                reserveAddress);
        BigInteger principalTotalSupply = this.principalTotalSupply();

        if (borrowIndex == null || borrowIndex.equals(ZERO)) {
            return this._totalSupply.getOrDefault(ZERO);
        }

        BigInteger decimals = this._decimals.getOrDefault(ZERO);
        BigInteger balance =  exaDivide( exaMultiply( convertToExa(principalTotalSupply,decimals),Context.call(BigInteger.class,
                lendingPoolCoreAddress,
                "getNormalizedDebt",
                reserveAddress)), 
                borrowIndex );
        return  convertExaToOther(balance, decimals.intValue());

    }

    public void _resetDataOnZeroBalanceInternal(Address _user) {
        this._userIndexes.set(_user, ZERO);
    }

    public void _mintInterestAndUpdateIndex(Address _user, BigInteger _balanceIncrease) {
        Address lendingPoolCoreAddress = getAddress(Contracts.LENDING_POOL_CORE.getKey());
        Address reserveAddress = getAddress(Contracts.RESERVE.getKey());

        if (_balanceIncrease!= null && _balanceIncrease.compareTo(ZERO)>0) {
            this._mint(_user, _balanceIncrease, null);
        }
        BigInteger userIndex = Context.call(BigInteger.class,
                lendingPoolCoreAddress,
                "getReserveBorrowCumulativeIndex",
                reserveAddress);
        this._userIndexes.set(_user, userIndex);

    }

    @External
    public void mintOnBorrow(Address _user, BigInteger _amount, BigInteger _balanceIncrease) {
        onlyLendingPoolCore();

        BigInteger beforeTotalSupply = this.principalTotalSupply();
        BigInteger beforeUserSupply = this.principalBalanceOf(_user);
        this._mintInterestAndUpdateIndex(_user, _balanceIncrease);
        this._mint(_user, _amount, null);
        this._handleAction(_user, beforeUserSupply, beforeTotalSupply);
        this.MintOnBorrow(_user, _amount, _balanceIncrease, this._userIndexes.getOrDefault(_user, ZERO));
    }

    public void _handleAction(Address _user,BigInteger _user_balance, BigInteger _total_supply) {
        Context.require(_handleActionEnabled.get(), "Handle Action Disabled.");
        Address rewardsAddress = getAddress(Contracts.REWARDS.getKey());

        UserDetails userDetails = new UserDetails();
        userDetails._user = _user;
        userDetails._userBalance = _user_balance;
        userDetails._totalSupply = _total_supply;
        userDetails._decimals = this.decimals();

        Context.call(rewardsAddress, "handleAction", userDetails);
    }

    @External
    public void burnOnRepay(Address _user, BigInteger _amount, BigInteger _balanceIncrease ) {
        onlyLendingPoolCore();

        BigInteger beforeTotalSupply = this.principalTotalSupply();
        BigInteger beforeUserSupply = this.principalBalanceOf(_user);
        this._mintInterestAndUpdateIndex(_user, _balanceIncrease);
        this._burn(_user, _amount, "loanRepaid".getBytes());

        this._handleAction(_user, beforeUserSupply, beforeTotalSupply);

        if(this.principalBalanceOf(_user).equals(ZERO)) {
            this._resetDataOnZeroBalanceInternal(_user);
        }
        this.BurnOnRepay(_user, _amount, _balanceIncrease, this._userIndexes.getOrDefault(_user, ZERO));

    }

    @External
    public void burnOnLiquidation(Address _user, BigInteger _amount, BigInteger _balanceIncrease) {
        onlyLendingPoolCore();

        BigInteger beforeTotalSupply = this.principalTotalSupply();
        BigInteger beforeUserSupply = this.principalBalanceOf(_user);
        this._mintInterestAndUpdateIndex(_user, _balanceIncrease);
        this._burn(_user, _amount, "userLiquidated".getBytes());

        this._handleAction(_user, beforeUserSupply, beforeTotalSupply);
        if(this.principalBalanceOf(_user).equals(ZERO)) {
            this._resetDataOnZeroBalanceInternal(_user);
        }

        this.BurnOnLiquidation(_user, _amount, _balanceIncrease, this._userIndexes.getOrDefault(_user, ZERO));
    }

    /***
    Transfers certain amount of tokens from sender to the receiver.
    :param _to: The account to which the token is to be transferred.
    :param _value: The no. of tokens to be transferred.
    :param _data: Any information or message
    ***/
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Context.revert(TAG +"Transfer not allowed in debt token");
    }

    /***
    * Creates amount number of tokens, and assigns to account
    * Increases the balance of that account and total supply.
    * This is an internal function.
    * :param account: The account at which token is to be created.
    * :param amount: Number of tokens to be created at the `account`.
    ***/
    public void _mint(Address _account, BigInteger _amount, byte[] _data) {
        if (_data == null || _data.length == 0 ) {
            _data = "mint".getBytes();
        }

        if (_amount == null || _amount.compareTo(ZERO)<0) {
            Context.revert(TAG +": Invalid value:" + _amount +" to mint");
        }

        this._totalSupply.set(this._totalSupply.get().add(_amount));
        this._balances.set(_account, this._balances.getOrDefault(_account, ZERO).add(_amount));
        //Emits an event log Mint
        this.Transfer(AddressConstant.ZERO_ADDRESS, _account, _amount, _data);

    }

    /***
    Destroys `amount` number of tokens from `account`
    Decreases the balance of that `account` and total supply.
    This is an internal function.
    :param account: The account at which token is to be destroyed.
    :param amount: The `amount` of tokens of `account` to be destroyed.
    ***/
    public void _burn(Address _account, BigInteger _amount, byte[] _data) {
        if (_data == null || _data.length == 0 ) {
            _data = "burn".getBytes();
        }

        if(_amount == null || _amount.equals(ZERO)) {
            Context.println("amount must be grather than zero");
            return;
        }

        if (_amount.compareTo(ZERO)<0) {
            Context.revert(TAG +": Invalid value:" + _amount +" to burn");
        }
        BigInteger  totalSupply = this._totalSupply.getOrDefault(ZERO);

        if (_amount.compareTo(totalSupply)>=1) {
            Context.revert(TAG +" "+  _amount  +" is greater than total supply :" + totalSupply);
        }

        BigInteger userBalance = this._balances.getOrDefault(_account, ZERO);
        if(_amount.compareTo(userBalance)>=1) {
            Context.revert(TAG +": Cannot burn more than user balance. Amount to burn: " + _amount +" User Balance: "+userBalance);
        }

        this._totalSupply.set(totalSupply.subtract(_amount));
        this._balances.set(_account, userBalance.subtract(_amount));
        // Emits an event log Burn
        this.Transfer(AddressConstant.ZERO_ADDRESS, _account, _amount, _data);
    }

    /***
    return total supply for reward distribution
    :return: total supply
    ***/   
    @External(readonly = true)
    public TotalStaked getTotalStaked() {
        TotalStaked totalStaked = new TotalStaked();
        totalStaked.decimals = this.decimals();
        totalStaked.totalStaked = this.totalSupply();
        return totalStaked;
    }

    public void onlyLendingPoolCore() {
        onlyOrElseThrow(Contracts.LENDING_POOL_CORE,
                OMMException.unknown(TAG 
                        + ":  SenderNotAuthorized: (sender)" + Context.getCaller() 
                        + " (lendingPool)" + getAddress(Contracts.LENDING_POOL_CORE.getKey()) + "}" ));
    }

}
