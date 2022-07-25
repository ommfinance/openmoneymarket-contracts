package finance.omm.score.core.lendingpool;

import static finance.omm.utils.math.MathUtils.exaDivide;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.score.core.lendingpool.exception.LendingPoolException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

public class LendingPoolImpl extends AbstractLendingPool {

    public LendingPoolImpl(Address addressProvider) {
        super(addressProvider, false);
    }

    @External(readonly = true)
    public String name() {
        return "OMM " + TAG;
    }

    @External
    public void setBridgeFeeThreshold(BigInteger _amount) {
        bridgeFeeThreshold.set(_amount);
    }

    @External(readonly = true)
    public BigInteger getBridgeFeeThreshold() {
        return bridgeFeeThreshold.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void setFeeSharingTxnLimit(BigInteger _limit) {
        feeSharingTxnLimit.set(_limit);
    }

    @External(readonly = true)
    public BigInteger getFeeSharingTxnLimit() {
        return feeSharingTxnLimit.getOrDefault(BigInteger.ZERO);
    }

    @External
    public boolean isFeeSharingEnable(Address _user) {
        onlyOrElseThrow(Contracts.OMM_TOKEN, LendingPoolException.unknown(TAG + " Sender not OMM Token"));
        return isFeeSharingEnabled(_user);
    }

    @External
    public List<Address> getDepositWallets(int _index) {
        return getArrayItems(depositWallets, _index);
    }

    @External
    public List<Address> getBorrowWallets(int _index) {
        return getArrayItems(borrowWallets, _index);
    }


    @External
    @Payable
    public void deposit(BigInteger _amount) {
        BigInteger icxValue = Context.getValue();
        if (icxValue.compareTo(_amount) == 0) {
            throw LendingPoolException.unknown(TAG + " : Amount in param " +
                    _amount + "doesnt match with the icx sent " + icxValue + " to the Lending Pool");
        }
        BigInteger rate = call(BigInteger.class, Contracts.STAKING, "getTodayRate");
        BigInteger amount = exaDivide(icxValue, rate);
        Address reserve = getAddress(Contracts.sICX.getKey());
        deposit(reserve, amount, Context.getCaller());
    }

    @External
    public void redeem(Address _oToken, BigInteger _amount, @Optional boolean _waitForUnstaking) {
        checkAndEnableFeeSharing();
        Address caller = Context.getCaller();
        Map<String, Object> redeemParams = call(Map.class, _oToken, "redeem", caller, _amount);
        Address reserve = Address.fromString((String) redeemParams.get("reserve"));
        BigInteger amount = (BigInteger) redeemParams.get("amountToRedeem");
        redeemUnderlying(reserve, caller, _oToken, amount, _waitForUnstaking);
    }

    @External
    public void claimRewards() {
        checkAndEnableFeeSharing();
        call(Contracts.REWARDS, "claimRewards", Context.getCaller());
    }

    @External
    public void stake(BigInteger _value) {
        checkAndEnableFeeSharing();
        call(Contracts.OMM_TOKEN, "stake", _value, Context.getCaller());
    }

    @External
    public void unstake(BigInteger _value) {
        checkAndEnableFeeSharing();
        call(Contracts.OMM_TOKEN, "unstake", _value, Context.getCaller());
    }

    @External
    public void borrow(Address _reserve, BigInteger _amount) {
        Map<String, Object> reserveData = call(Map.class, Contracts.LENDING_POOL_CORE,
                "getReserveData", _reserve);
        BigInteger availableBorrows = (BigInteger) reserveData.get("availableBorrows");

        if (_amount.compareTo(availableBorrows) > 0) {
            throw LendingPoolException.unknown(TAG + "Amount requested " + _amount +
                    " is more then the "+ availableBorrows);
        }

        checkAndEnableFeeSharing();

        boolean isActive = (boolean) reserveData.get("isActive");
        if (! isActive) {
            throw LendingPoolException.unknown("Reserve is not active, borrow unsuccessful");
        }

        boolean isFreezed = (boolean) reserveData.get("isFreezed");
        if (isFreezed) {
            throw LendingPoolException.unknown("Reserve is frozen, borrow unsuccessful");
        }

        boolean isReserveBorrowEnabled = call(Boolean.class,
                Contracts.LENDING_POOL_CORE, "isReserveBorrowingEnabled", _reserve);
        if ( !isReserveBorrowEnabled ) {
            throw LendingPoolException.unknown("Borrow error:borrowing not enabled in the reserve");
        }

        BigInteger availableLiquidity = (BigInteger) reserveData.get("availableLiquidity");
        if (availableLiquidity.compareTo(_amount) < 0) {
            throw LendingPoolException.unknown("Borrow error:Not enough available liquidity in the reserve");
        }

        Address caller = Context.getCaller();

        if (borrowIndex.get(caller) != null) {
            borrowWallets.add(caller);
            borrowIndex.set(caller, BigInteger.valueOf(borrowWallets.size()));
        }

        Map<String, Object> userData = call(Map.class, Contracts.LENDING_POOL_DATA_PROVIDER,
                "getUserAccountData", caller);

        BigInteger userCollateralBalanceUSD = (BigInteger) userData.get(
                "totalCollateralBalanceUSD");
        BigInteger userBorrowBalanceUSD = (BigInteger) userData.get(
                "totalBorrowBalanceUSD");
        BigInteger userTotalFeesUSD = (BigInteger) userData.get("totalFeesUSD");
        BigInteger currentLTV = (BigInteger) userData.get("currentLtv");

        boolean healthFactorBelowThreshold = (boolean) userData.get("healthFactorBelowThreshold");

        if (userCollateralBalanceUSD.compareTo(BigInteger.ZERO) <= 0) {
            throw LendingPoolException.unknown("Borrow error: The user does not have any collateral");
        }

        if (healthFactorBelowThreshold) {
            throw LendingPoolException.unknown("Borrow error: Health factor is below threshold");
        }

        BigInteger borrowFee = call(BigInteger.class, Contracts.FEE_PROVIDER,
                "calculateOriginationFee", _amount);

        if (borrowFee.compareTo(BigInteger.ZERO) <= 0 ) {
            throw LendingPoolException.unknown("Borrow error: borrow amount is very small");
        }

        BigInteger amountOfCollateralNeededUSD = call(BigInteger.class, Contracts.LENDING_POOL_DATA_PROVIDER,
                "calculateCollateralNeededUSD", _reserve, _amount, borrowFee, userBorrowBalanceUSD,
                userTotalFeesUSD, currentLTV);

        if (amountOfCollateralNeededUSD.compareTo(userCollateralBalanceUSD) > 0) {
            throw LendingPoolException.unknown("Borrow error: Insufficient collateral to cover new borrow");
        }

        Map<String, Object> borrowData = call(Map.class, Contracts.LENDING_POOL_CORE, "updateStateOnBorrow",
                _reserve, caller, _amount, borrowFee);

        call(Contracts.LENDING_POOL_CORE, "transferToUser", _reserve, caller, _amount);
        Borrow(_reserve, caller, _amount, (BigInteger) borrowData.get("currentBorrowRate"),
                borrowFee, (BigInteger) borrowData.get("balanceIncrease"));
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        String method;
        JsonObject params;

        try {
            String data = new String(_data);
            JsonObject json = Json.parse(data).asObject();

            method = json.get("method").asString();
            params = json.get("params").asObject();
        } catch (Exception e) {
            throw LendingPoolException.unknown(TAG + " Invalid data: " + _data.toString());
        }

        Address caller = Context.getCaller();

        if (method.equals("deposit")) {
            deposit(caller, _value, _from);
        } else if (method.equals("repay")) {
            repay(caller, _value, _from);
        } else if (method.equals("liquidationCall") && params != null) {
            String collateral = String.valueOf(params.get("_collateral"));
            String reserve = String.valueOf(params.get("_reserve"));
            String user = String.valueOf(params.get("_user"));
            if (collateral == null || reserve == null || user == null) {
                throw LendingPoolException.unknown(TAG + " Invalid data: Collateral" + collateral +
                        " Reserve: "+reserve+ " User: "+ user);
            }
            liquidationCall(Address.fromString(collateral),
                    Address.fromString(user),
                    Address.fromString(reserve),
                    _value, _from);

        } else {
            throw LendingPoolException.unknown(TAG + " No valid method called, data: "+ _data.toString());
        }
    }
}
