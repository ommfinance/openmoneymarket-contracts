package finance.omm.score;

import java.math.BigInteger;
import java.util.Map;

import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.structs.AddressDetails;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import static finance.omm.utils.math.MathUtils.*;
import finance.omm.libs.address.Contracts;


/**
Implementation of IRC2
**/
public class DToken extends AddressProvider{

    private static final String NAME = "token_name";
    private static final String SYMBOL = "token_symbol";
    private static final String DECIMALS = "decimals";
    private static final String TOTAL_SUPPLY = "total_supply";
    private static final String BALANCES = "balances";
    private static final String USER_INDEXES = "user_indexes";
    
    /**
    Variable Definition
    **/
    private final VarDB<String> _name = Context.newVarDB(NAME, String.class);
    private final VarDB<String> _symbol = Context.newVarDB(SYMBOL, String.class);
    private final VarDB<BigInteger> _decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    private final VarDB<BigInteger> _totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    private final DictDB<Address, BigInteger> _balances = Context.newDictDB(BALANCES, BigInteger.class);
    private final DictDB<Address, BigInteger> _userIndexes = Context.newDictDB(USER_INDEXES, BigInteger.class); 
    
    /***
    Variable Initialization.
    :param _addressProvider: the address of addressProvider
    :param _name: The name of the token.
    :param _symbol: The symbol of the token.
    :param _decimals: The number of decimals. Set to 18 by default.
    ***/
    public DToken(Address _addressProvider, String _name, String _symbol, BigInteger _decimals, boolean _update) {
        super(_addressProvider, _update);

        if (_symbol.isEmpty()) {
            Context.revert("Invalid Symbol name");
        }

        if (_name.isEmpty()) {
            Context.revert("Invalid Token Name");
        }

        if (_decimals.compareTo(BigInteger.ZERO) < 0) {
            Context.revert("Decimals cannot be less than zero");
        }

        this._name.set(_name);
        this._symbol.set(_symbol);
        this._decimals.set(_decimals);
        this._totalSupply.set(BigInteger.ZERO);
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
        return this._decimals.getOrDefault(BigInteger.ZERO);
    }
    

    @External(readonly = true)
    public BigInteger getUserBorrowCumulativeIndex(Address _user) {
        return this._userIndexes.getOrDefault(_user, BigInteger.ZERO);
    }
      
    
    public BigInteger _calculateCumulatedBalanceInternal(Address _user, BigInteger _balance) {
       Address lendingPoolCoreAddress = this._addresses.get(Contracts.LENDING_POOL_CORE.getKey());
       Address reserveAddress = this._addresses.get(Contracts.RESERVE.getKey());

       
       BigInteger userIndex = this._userIndexes.getOrDefault(_user, BigInteger.ZERO);
       if (userIndex.equals(BigInteger.ZERO)) {
           return _balance;
       }else {
           BigInteger decimals = this._decimals.getOrDefault(BigInteger.ZERO);

           BigInteger balance = exaDivide(exaMultiply(convertToExa(_balance,decimals),  Context.call(BigInteger.class,
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
        return this._balances.getOrDefault(_user, BigInteger.ZERO);
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
        return this._totalSupply.getOrDefault(BigInteger.ZERO);
    }
    
    
    @External(readonly = true)
    public SupplyDetails getPrincipalSupply(Address _user) {
        SupplyDetails supplyDetails = new SupplyDetails();
        supplyDetails.decimals = this.decimals();
        supplyDetails.principalUserBalance = this.principalBalanceOf(_user);
        supplyDetails.principalTotalSupply = this.principalTotalSupply();
        return supplyDetails;
    }   

   
    public void _resetDataOnZeroBalanceInternal(Address _user) {
        this._userIndexes.set(_user, BigInteger.ZERO);
    }
    
}

        
                
                
}
