package finance.omm.score.token;

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.score.token.enums.Status;
import finance.omm.score.token.exception.OMMTokenException;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.constants.TimeConstants.Timestamp;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.External;
import score.annotation.Optional;

public class OMMTokenImpl extends AbstractOMMToken {


    public OMMTokenImpl(Address addressProvider) {
        super(addressProvider, TOKEN_NAME, SYMBOL_NAME);
    }

    @External(readonly = true)
    public SupplyDetails getPrincipalSupply(Address _user) {
        SupplyDetails details = new SupplyDetails();
        details.principalUserBalance = this.staked_balanceOf(_user);
        details.principalTotalSupply = this.total_staked_balance();
        details.decimals = this.decimals();
        return details;
    }

    @External(readonly = true)
    public TotalStaked getTotalStaked() {
        TotalStaked total = new TotalStaked();
        total.decimals = this.decimals();
        total.totalStaked = this.total_staked_balance();

        return total;
    }

    @External
    public void mint(BigInteger _amount, @Optional byte[] _data) {
        onlyOrElseThrow(Contracts.REWARDS,
                OMMTokenException.unauthorized("Only reward distribution contract can call mint method"));

        if (_data == null) {
            _data = "minted by reward".getBytes();
        }

        if (_amount.compareTo(BigInteger.ZERO) <= 0) {
            throw OMMTokenException.unknown("ZeroValueError: _amount: " + _amount);
        }

        Address to = Context.getCaller();

        BigInteger newTotalSupply = this.totalSupply.getOrDefault(BigInteger.ZERO).add(_amount);
        this.totalSupply.set(newTotalSupply);
        BigInteger newBalance = this.balances.getOrDefault(to, BigInteger.ZERO).add(_amount);
        this.balances.set(to, newBalance);

        this.Transfer(ZERO_SCORE_ADDRESS, to, _amount, _data);
    }

    @External(readonly = true)
    public String name() {return this.name.get();}


    @External(readonly = true)
    public String symbol() {return this.symbol.get();}


    @External(readonly = true)
    public BigInteger decimals() {return this.decimals.get();}

    @External(readonly = true)
    public BigInteger totalSupply() {return this.totalSupply.get();}

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {return this.available_balanceOf(_owner);}

    @External(readonly = true)
    public BigInteger available_balanceOf(Address _owner) {
        Map<String, BigInteger> detail_balance = this.details_balanceOf(_owner);
        return detail_balance.get("availableBalance");
    }

    @External(readonly = true)
    public BigInteger staked_balanceOf(Address _owner) {
        return this.stakedBalances.at(_owner).getOrDefault(Status.STAKED.getKey(), BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger unstaked_balanceOf(Address _owner) {
        Map<String, BigInteger> detail_balance = this.details_balanceOf(_owner);
        return detail_balance.get("unstakingBalance");
    }


    /**
     * Set the minimum staking period
     *
     * @param _timeInSeconds - Staking time period in seconds.
     */
    @External
    public void setUnstakingPeriod(BigInteger _timeInSeconds) {
        onlyOwnerOrElseThrow(OMMTokenException.notOwner());
        if (_timeInSeconds.compareTo(BigInteger.ZERO) < 0) {
            throw OMMTokenException.unknown(TAG + " : Time cannot be negative.");
        }

        TimeConstants.checkIsValidTimestamp(_timeInSeconds, Timestamp.SECONDS,
                OMMTokenException.unknown("Invalid time "));

        BigInteger totalTime = _timeInSeconds.multiply(TimeConstants.SECOND);

        this.unstakingPeriod.set(totalTime);

    }

    @External(readonly = true)
    public BigInteger getUnstakingPeriod() {
        return this.unstakingPeriod.getOrDefault(BigInteger.ZERO);
    }

    @External
    public void addStakers(Address[] stakers) {
        for (Address staker : stakers) {
            this.addStaker(staker);
        }
    }

    @External
    public void removeStakers(Address[] stakers) {
        for (Address staker : stakers) {
            this.removeStaker(staker);
        }
    }

    @External(readonly = true)
    public List<Address> getStakersList(int _start, int _end) {
        if (_end <= _start) {
            throw OMMTokenException.unknown("StakerList :: start index cannot be greater than end index");
        }

        if (_end - _start > 100) {
            throw OMMTokenException.unknown("StakerList :: range cannot be greater than 100");
        }
        return this.stakers.range(_start, _end);
    }

    @External(readonly = true)
    public int totalStakers() {return this.stakers.length();}

    @External(readonly = true)
    public boolean inStakerList(Address _staker) {return this.stakers.indexOf(_staker) != null;}

    @External(readonly = true)
    public Map<String, BigInteger> details_balanceOf(Address _owner) {
        BigInteger userBalance = this.balances.getOrDefault(_owner, BigInteger.ZERO);
        DictDB<Integer, BigInteger> staked = this.stakedBalances.at(_owner);

        BigInteger currentTime = TimeConstants.getBlockTimestamp();

        BigInteger stakedBalance = staked.getOrDefault(Status.STAKED.getKey(), BigInteger.ZERO);
        BigInteger unstakingAmount = staked.getOrDefault(Status.UNSTAKING.getKey(), BigInteger.ZERO);
        BigInteger unstakingTime = staked.getOrDefault(Status.UNSTAKING_PERIOD.getKey(), BigInteger.ZERO);

        if (unstakingTime.compareTo(currentTime) < 0) {
            unstakingAmount = BigInteger.ZERO;
        }

        if (unstakingAmount.equals(BigInteger.ZERO)) {
            unstakingTime = BigInteger.ZERO;
        }

        return Map.of(
                "totalBalance", userBalance,
                "availableBalance", userBalance.subtract(stakedBalance).subtract(unstakingAmount),
                "stakedBalance", stakedBalance,
                "unstakingBalance", unstakingAmount,
                "unstakingTimeInMicro", unstakingTime
        );
    }

    @External(readonly = true)
    public BigInteger total_staked_balance() {return this.totalStakedBalance.getOrDefault(BigInteger.ZERO);}


    @External
    public void setMinimumStake(BigInteger _min) {
        onlyOwnerOrElseThrow(OMMTokenException.notOwner());
        this.minimumStake.set(_min);
    }

    @External(readonly = true)
    public BigInteger getMinimumStake() {return this.minimumStake.getOrDefault(BigInteger.ZERO);}


    @External
    public void addToLockList(Address user) {
        onlyOwnerOrElseThrow(OMMTokenException.notOwner());

        this.lockList.add(user);

        DictDB<Integer, BigInteger> stakedInfo = this.stakedBalances.at(user);

        BigInteger stakedBalance = stakedInfo.getOrDefault(Status.STAKED.getKey(), BigInteger.ZERO);

        if (stakedBalance.compareTo(BigInteger.ZERO) > 0) {
            this.makeAvailable(user);

            stakedInfo.set(Status.STAKED.getKey(), BigInteger.ZERO);
            BigInteger unstakingAmount = stakedInfo.getOrDefault(Status.UNSTAKING.getKey(), BigInteger.ZERO);

            stakedInfo.set(Status.STAKED.getKey(), BigInteger.ZERO);
            stakedInfo.set(Status.UNSTAKING.getKey(), unstakingAmount.add(stakedBalance));
            stakedInfo.set(Status.UNSTAKING_PERIOD.getKey(),
                    TimeConstants.getBlockTimestamp().add(getUnstakingPeriod()));

            BigInteger totalStakedBalance = this.totalStakedBalance.getOrDefault(BigInteger.ZERO);
            this.totalStakedBalance.set(totalStakedBalance.subtract(unstakingAmount));
        }
    }


    @External
    public void removeFromLockList(Address user) {
        onlyOwnerOrElseThrow(OMMTokenException.notOwner());
        this.lockList.remove(user);
    }

    @External(readonly = true)
    public List<Address> get_locklist_addresses(int _start, int _end) {
        if (_end <= _start) {
            throw OMMTokenException.unknown("Locklist :: start index cannot be greater than end index");
        }

        if (_end - _start > 100) {
            throw OMMTokenException.unknown("Locklist :: range cannot be greater than 100");
        }
        return this.lockList.range(_start, _end);
    }


    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        Address from = Context.getCaller();
        if (this.lockList.contains(from)) {
            throw OMMTokenException.notPermitted("Cannot transfer, the sender " + from + " is locked");
        }

        if (this.lockList.contains(_to)) {
            throw OMMTokenException.notPermitted("Cannot transfer, the receiver " + _to + " is locked");
        }

        if (_data == null) {
            _data = new byte[0];
        }

        this._transfer(from, _to, _value, _data);
    }

    @External
    public void stake(BigInteger _value, Address _user) {
        onlyOrElseThrow(Contracts.LENDING_POOL,
                OMMTokenException.unauthorized("Only lending pool contract can call stake method"));
        throw OMMTokenException.notSupported("Staking of OMM token no longer supported.");
    }

    @External
    public void cancelUnstake(BigInteger _value) {
        if (_value.compareTo(BigInteger.ZERO) < 0) {
            throw OMMTokenException.unknown("Cannot cancel negative unstake");
        }
        Address user = Context.getCaller();

        if (this.lockList.contains(user)) {
            throw OMMTokenException.notPermitted("Cannot cancel unstake,the address " + user + " is locked'");
        }

        Map<String, BigInteger> userBalances = this.details_balanceOf(user);
        BigInteger unstakingBalance = userBalances.get("unstakingBalance");
        BigInteger userOldStake = userBalances.get("'stakedBalance'");

        if (unstakingBalance.compareTo(_value) > 0) {
            throw OMMTokenException.insufficientBalance(
                    "Cannot cancel unstake,cancel value is more than the actual unstaking amount");
        }
        BigInteger oldTotalSupply = this.totalStakedBalance.getOrDefault(BigInteger.ZERO);

        Boolean isFeeSharingEnabled = call(Boolean.class, Contracts.LENDING_POOL, "isFeeSharingEnable", user);

        if (isFeeSharingEnabled) {
            Context.setFeeSharingProportion(100);
        }

        BigInteger newStakedBalance = userOldStake.add(_value);

        DictDB<Integer, BigInteger> statedInfo = this.stakedBalances.at(user);

        statedInfo.set(Status.STAKED.getKey(), newStakedBalance);
        statedInfo.set(Status.UNSTAKING.getKey(), unstakingBalance.subtract(_value));
        this.addStaker(user);

        BigInteger newTotalStakedBalance = oldTotalSupply.add(_value);
    }


    @External
    public void unstake(BigInteger _value, Address _user) {
        onlyOrElseThrow(Contracts.LENDING_POOL,
                OMMTokenException.unauthorized("Only lending pool contract can call unstake method"));
        if (_value.compareTo(BigInteger.ZERO) < 0) {
            throw OMMTokenException.unknown("Cannot unstake less than zero value to stake " + _value);
        }
        if (lockList.contains(_user)) {
            throw OMMTokenException.notPermitted("Cannot unstake,the address " + _user + " is locked");
        }
        this.makeAvailable(_user);

        BigInteger stakedBalance = this.staked_balanceOf(_user);

        if (stakedBalance.compareTo(_value) > 0) {
            throw OMMTokenException.insufficientBalance(
                    "Cannot unstake,user dont have enough staked balance amount to unstake " + _value
                            + " staked balance of user: " + _user + " is " + stakedBalance);
        }
        BigInteger oldTotalSupply = this.totalStakedBalance.getOrDefault(BigInteger.ZERO);

        DictDB<Integer, BigInteger> stakeInfo = this.stakedBalances.at(_user);
        BigInteger currentUnstakingAmount = stakeInfo.getOrDefault(Status.UNSTAKING.getKey(), BigInteger.ZERO);

        if (currentUnstakingAmount.compareTo(BigInteger.ZERO) > 0) {
            throw OMMTokenException.unknown("you already have a unstaking order,try after the amount is unstaked");
        }

        BigInteger newStakedBalance = stakedBalance.subtract(_value);

        stakeInfo.set(Status.STAKED.getKey(), newStakedBalance);
        stakeInfo.set(Status.UNSTAKING.getKey(), _value);
        stakeInfo.set(Status.UNSTAKING_PERIOD.getKey(),
                TimeConstants.getBlockTimestamp().add(this.getUnstakingPeriod()));

        BigInteger newTotalStakedBalance = oldTotalSupply.subtract(_value);

        if (newStakedBalance.equals(BigInteger.ZERO)) {
            this.removeStaker(_user);
        }
    }


    @Override
    public void migrateStakedOMM(BigInteger _amount, BigInteger _lockPeriod) {
        Address user = Context.getCaller();
        BigInteger stakedBalance = this.staked_balanceOf(user);

        if (stakedBalance.compareTo(_amount) < 0) {
            throw OMMTokenException.insufficientBalance("Cannot lock more than staked.");
        }

        Map<String, BigInteger> lockedBalance = call(Map.class, Contracts.BOOSTED_OMM, "getLocked", user);
        JsonObject data = new JsonObject();
        JsonObject params = new JsonObject().add("unlockTime", _lockPeriod.toString());
        if (lockedBalance.get("amount").compareTo(BigInteger.ZERO) > 0) {
            data.add("method", "increaseAmount");
        } else {
            data.add("method", "createLock");
        }
        data.add("params", params);

        DictDB<Integer, BigInteger> stakeInfo = this.stakedBalances.at(user);
        BigInteger oldStaked = stakeInfo.getOrDefault(Status.STAKED.getKey(), BigInteger.ZERO);
        BigInteger newStaked = oldStaked.subtract(_amount);
        stakeInfo.set(Status.STAKED.getKey(), newStaked);

        BigInteger oldTotalStaked = this.totalSupply.getOrDefault(BigInteger.ZERO);

        this.totalSupply.set(oldTotalStaked.subtract(_amount));

        if (newStaked.equals(BigInteger.ZERO)) {
            removeStaker(user);
        }
        this._transfer(user, getAddress(Contracts.BOOSTED_OMM.getKey()), _amount, data.toString().getBytes());
    }
}
