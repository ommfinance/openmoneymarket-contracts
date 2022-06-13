package finance.omm.score.tokens;
/*
 * Copyright (c) 2022 omm.finance.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static finance.omm.utils.constants.AddressConstant.ZERO_ADDRESS;
import static finance.omm.utils.math.MathUtils.convertToNumber;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.score.tokens.exception.BoostedOMMException;
import finance.omm.score.tokens.model.LockedBalance;
import finance.omm.score.tokens.model.Point;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.UnsignedBigInteger;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.HashMap;

public class BoostedOMM extends AbstractBoostedOMM {


    public BoostedOMM(Address addressProvider, Address tokenAddress, String name, String symbol) {
        super(addressProvider, tokenAddress, name, symbol);
    }

    @External
    public void setMinimumLockingAmount(BigInteger value) {
        ownerRequired();
        if (value.signum() < 0) {
            throw BoostedOMMException.unknown("invalid value for minimum locking amount");
        }

        this.minimumLockingAmount.set(value);
    }

    @External(readonly = true)
    public BigInteger getMinimumLockingAmount() {
        return this.minimumLockingAmount.get();
    }


    @External
    public void commitTransferOwnership(Address address) {
        ownerRequired();
        futureAdmin.set(address);
        CommitOwnership(address);
    }

    @External
    public void applyTransferOwnership() {
        ownerRequired();
        Address futureAdmin = this.futureAdmin.get();
        Context.require(futureAdmin != null, "Apply transfer ownership: Admin not set");
        this.admin.set(futureAdmin);
        this.futureAdmin.set(null);
        ApplyOwnership(futureAdmin);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getLocked(Address _owner) {
        LockedBalance balance = getLockedBalance(_owner);
        return Map.of("amount", balance.amount, "end", balance.getEnd());
    }

    @External(readonly = true)
    public BigInteger getTotalLocked() {
        return (BigInteger) callToken("balanceOf", Context.getAddress());
    }

    @External(readonly = true)
    public List<Address> getUsers(int start, int end) {
        Context.require(end - start <= 100, "Get users :Fetch only 100 users at a time");
        return users.range(start, end);
    }

    @External(readonly = true)
    public int activeUsersCount() {
        return users.length();
    }

    @External(readonly = true)
    public boolean hasLocked(Address _owner) {
        return users.contains(_owner);
    }


    @External(readonly = true)
    public BigInteger getLastUserSlope(Address address) {
        BigInteger userPointEpoch = this.userPointEpoch.get(address);
        return getUserPointHistory(address, userPointEpoch).slope;
    }

    @External(readonly = true)
    public BigInteger userPointHistoryTimestamp(Address address, BigInteger index) {
        return getUserPointHistory(address, index).getTimestamp();
    }

    @External(readonly = true)
    public BigInteger lockedEnd(Address address) {
        return getLockedBalance(address).getEnd();
    }

    private void checkpoint(Address address, LockedBalance oldLocked, LockedBalance newLocked) {
        Point uOld = new Point();
        Point uNew = new Point();
        BigInteger oldDSlope = BigInteger.ZERO;
        BigInteger newDSlope = BigInteger.ZERO;
        BigInteger epoch = this.epoch.get();

        UnsignedBigInteger blockTimestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
        UnsignedBigInteger blockHeight = UnsignedBigInteger.valueOf(Context.getBlockHeight());

        if (!address.equals(ZERO_ADDRESS)) {
            //            Calculate slopes and biases
            //            Kept at zero when they have to
            if (oldLocked.end.compareTo(blockTimestamp) > 0 && oldLocked.amount.compareTo(BigInteger.ZERO) > 0) {
                uOld.slope = oldLocked.amount.divide(MAX_TIME);
                UnsignedBigInteger delta = oldLocked.end.subtract(blockTimestamp);
                uOld.bias = uOld.slope.multiply(delta.toBigInteger());
            }

            if (newLocked.end.compareTo(blockTimestamp) > 0 && newLocked.amount.compareTo(BigInteger.ZERO) > 0) {
                uNew.slope = newLocked.amount.divide(MAX_TIME);
                UnsignedBigInteger delta = newLocked.end.subtract(blockTimestamp);
                uNew.bias = uNew.slope.multiply(delta.toBigInteger());
            }

            //          Read values of scheduled changes in the slope
            //          oldLocked.end can be in the past and in the future
            //          newLocked.end can ONLY be in the FUTURE unless everything expired: than zeros
            oldDSlope = this.slopeChanges.getOrDefault(oldLocked.getEnd(), BigInteger.ZERO);
            if (!newLocked.end.equals(UnsignedBigInteger.ZERO)) {
                if (newLocked.end.equals(oldLocked.end)) {
                    newDSlope = oldDSlope;
                } else {
                    newDSlope = this.slopeChanges.getOrDefault(newLocked.getEnd(), BigInteger.ZERO);
                }
            }
        }

        Point lastPoint = new Point(BigInteger.ZERO, BigInteger.ZERO, blockTimestamp.toBigInteger(),
                blockHeight.toBigInteger());
        if (epoch.compareTo(BigInteger.ZERO) > 0) {
            lastPoint = this.pointHistory.getOrDefault(epoch, new Point());
        }
        UnsignedBigInteger lastCheckPoint = lastPoint.timestamp;

        //      initialLastPoint is used for extrapolation to calculate block number
        //      (approximately, for *At methods) and save them
        //      as we cannot figure that out exactly from inside the contract
        Point initialLastPoint = lastPoint.newPoint();
        UnsignedBigInteger blockSlope = UnsignedBigInteger.ZERO;
        if (blockTimestamp.compareTo(lastPoint.timestamp) > 0) {
            blockSlope = MULTIPLIER.multiply(blockHeight.subtract(lastPoint.block))
                    .divide(blockTimestamp.subtract(lastPoint.timestamp));
            //          If last point is already recorded in this block, slope = 0
            //          But that's ok because we know the block in such case
        }

        //      Go over week's to fill history and calculate what the current point is
        UnsignedBigInteger timeIterator = lastCheckPoint.divide(TimeConstants.U_WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.U_WEEK_IN_MICRO_SECONDS);

        for (int index = 0; index < 255; ++index) {
            timeIterator = timeIterator.add(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
            BigInteger dSlope = BigInteger.ZERO;
            if (timeIterator.compareTo(blockTimestamp) > 0) {
                timeIterator = blockTimestamp;
            } else {
                dSlope = this.slopeChanges.getOrDefault(timeIterator.toBigInteger(), BigInteger.ZERO);
            }

            lastPoint.bias = lastPoint.bias.subtract(lastPoint.slope.multiply(timeIterator.subtract(lastCheckPoint)
                    .toBigInteger()));
            lastPoint.slope = lastPoint.slope.add(dSlope);

            if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.bias = BigInteger.ZERO;
            }

            if (lastPoint.slope.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.slope = BigInteger.ZERO;
            }

            lastCheckPoint = timeIterator;
            lastPoint.timestamp = timeIterator;
            UnsignedBigInteger dtime = timeIterator.subtract(initialLastPoint.timestamp);
            lastPoint.block = initialLastPoint.block.add(blockSlope.multiply(dtime).divide(MULTIPLIER));
            epoch = epoch.add(BigInteger.ONE);

            if (timeIterator.equals(blockTimestamp)) {
                lastPoint.block = blockHeight;
                break;
            } else {
                pointHistory.set(epoch, lastPoint);
            }
        }

        this.epoch.set(epoch);
        if (!address.equals(ZERO_ADDRESS)) {
            lastPoint.slope = lastPoint.slope.add(uNew.slope.subtract(uOld.slope));
            lastPoint.bias = lastPoint.bias.add(uNew.bias.subtract(uOld.bias));

            if (lastPoint.slope.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.slope = BigInteger.ZERO;
            }
            if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.bias = BigInteger.ZERO;
            }
        }

        this.pointHistory.set(epoch, lastPoint);

        if (!address.equals(ZERO_ADDRESS)) {
            if (oldLocked.end.compareTo(blockTimestamp) > 0) {
                oldDSlope = oldDSlope.add(uOld.slope);
                if (newLocked.end.equals(oldLocked.end)) {
                    oldDSlope = oldDSlope.subtract(uNew.slope);
                }
                this.slopeChanges.set(oldLocked.getEnd(), oldDSlope);
            }

            if (newLocked.end.compareTo(blockTimestamp) > 0 && newLocked.end.compareTo(oldLocked.end) > 0) {
                newDSlope = newDSlope.subtract(uNew.slope);
                this.slopeChanges.set(newLocked.getEnd(), newDSlope);
            }

            BigInteger userEpoch = this.userPointEpoch.getOrDefault(address, BigInteger.ZERO).add(BigInteger.ONE);
            this.userPointEpoch.set(address, userEpoch);
            uNew.timestamp = blockTimestamp;
            uNew.block = blockHeight;
            this.userPointHistory.at(address).set(userEpoch, uNew);
        }
    }

    private void depositFor(Address address, BigInteger value, BigInteger unlockTime, LockedBalance lockedBalance,
            int type) {
        LockedBalance locked = lockedBalance.newLockedBalance();
        BigInteger supplyBefore = this.supply.get();
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());

        this.supply.set(supplyBefore.add(value));
        LockedBalance oldLocked = locked.newLockedBalance();

        locked.amount = locked.amount.add(value);
        if (!unlockTime.equals(BigInteger.ZERO)) {
            locked.end = new UnsignedBigInteger(unlockTime);
        }

        this.locked.set(address, locked);
        this.checkpoint(address, oldLocked, locked);

        Deposit(address, value, locked.getEnd(), type, blockTimestamp);
        Supply(supplyBefore, supplyBefore.add(value));

        updateDelegationAndHandleAction(address);
    }

    @External
    public void checkpoint() {
        this.checkpoint(ZERO_ADDRESS, new LockedBalance(), new LockedBalance());
    }

    private void depositFor(Address address, BigInteger value) {
        this.nonReentrant.updateLock(true);
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        LockedBalance locked = getLockedBalance(address);

        Context.require(value.compareTo(BigInteger.ZERO) > 0, "Deposit for: Need non zero value");
        Context.require(locked.amount.compareTo(BigInteger.ZERO) > 0, "Deposit for: No existing lock found");
        Context.require(locked.getEnd()
                .compareTo(blockTimestamp) > 0, "Deposit for: Cannot add to expired lock. Withdraw");

        this.depositFor(address, value, BigInteger.ZERO, locked, DEPOSIT_FOR_TYPE);
        this.nonReentrant.updateLock(false);
    }

    private void assertNotContract(Address address) {
        Context.require(!address.isContract(), "Assert Not contract: Smart contract depositors not allowed");
    }

    private void createLock(Address sender, BigInteger value, BigInteger unlockTime) {
        this.nonReentrant.updateLock(true);
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        this.assertNotContract(sender);

        unlockTime = unlockTime.divide(TimeConstants.WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.WEEK_IN_MICRO_SECONDS);
        LockedBalance locked = getLockedBalance(sender);

        Context.require(value.compareTo(BigInteger.ZERO) > 0, "Create Lock: Need non zero value");
        Context.require(locked.amount.equals(BigInteger.ZERO), "Create Lock: Withdraw old tokens first");
        Context.require(unlockTime.compareTo(blockTimestamp) > 0, "Create Lock: Can only lock until time in the " +
                "future");
        Context.require(unlockTime.compareTo(blockTimestamp.add(MAX_TIME)) <= 0,
                "Create Lock: Voting Lock can be 4 years max");

        users.add(sender);
        this.depositFor(sender, value, unlockTime, locked, CREATE_LOCK_TYPE);
        this.nonReentrant.updateLock(false);
    }

    private void increaseAmount(Address sender, BigInteger value, BigInteger unlockTime) {
        this.nonReentrant.updateLock(true);
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        this.assertNotContract(sender);
        LockedBalance locked = getLockedBalance(sender);

        if (!unlockTime.equals(BigInteger.ZERO)) {
            unlockTime = unlockTime.divide(TimeConstants.WEEK_IN_MICRO_SECONDS)
                    .multiply(TimeConstants.WEEK_IN_MICRO_SECONDS);
            Context.require(unlockTime.compareTo(locked.end.toBigInteger()) >= 0,
                    "Increase unlock time: Can only increase lock duration");
            Context.require(unlockTime.compareTo(blockTimestamp.add(MAX_TIME)) <= 0,
                    "Increase unlock time: Voting lock can be 4 years max");
        }

        Context.require(value.compareTo(BigInteger.ZERO) > 0, "Increase amount: Need non zero value");
        Context.require(locked.amount.compareTo(BigInteger.ZERO) > 0, "Increase amount: No existing lock found");
        Context.require(locked.getEnd()
                .compareTo(blockTimestamp) > 0, "Increase amount: Cannot add to expired lock.");

        this.depositFor(sender, value, unlockTime, locked, INCREASE_LOCK_AMOUNT);
        this.nonReentrant.updateLock(false);
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
        Address token = Context.getCaller();
        Context.require(token.equals(this.tokenAddress.get()), "Token Fallback: Only Omm deposits are allowed");

        Context.require(_value.signum() > 0, "Token Fallback: Token value should be a positive number");
        String unpackedData = new String(_data);
        Context.require(!unpackedData.equals(""), "Token Fallback: Data can't be empty");

        JsonObject json = Json.parse(unpackedData).asObject();

        String method = json.get("method").asString();
        JsonObject params = json.get("params").asObject();
        BigInteger unlockTime = convertToNumber(params.get("unlockTime"), BigInteger.ZERO);

        switch (method) {
            case "increaseAmount":
                this.increaseAmount(_from, _value, unlockTime);
                break;
            case "createLock":
                BigInteger minimumLockingAmount = this.minimumLockingAmount.get();
                if (minimumLockingAmount.compareTo(_value) > 0) {
                    throw BoostedOMMException.invalidMinimumLockingAmount(minimumLockingAmount);
                }
                this.createLock(_from, _value, unlockTime);
                break;
            case "depositFor":
                Address sender = Address.fromString(params.get("address").asString());
                this.depositFor(sender, _value);
                break;
            default:
                Context.revert("Token fallback: Unimplemented token fallback action");
                break;
        }
    }

    @External
    public void increaseUnlockTime(BigInteger unlockTime) {
        this.nonReentrant.updateLock(true);
        Address sender = Context.getCaller();
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());

        this.assertNotContract(sender);
        LockedBalance locked = getLockedBalance(sender);
        unlockTime = unlockTime.divide(TimeConstants.WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.WEEK_IN_MICRO_SECONDS);

        Context.require(locked.amount.compareTo(BigInteger.ZERO) > 0, "Increase unlock time: Nothing is locked");
        Context.require(locked.getEnd().compareTo(blockTimestamp) > 0, "Increase unlock time: Lock expired");
        Context.require(unlockTime.compareTo(locked.end.toBigInteger()) > 0,
                "Increase unlock time: Can only increase lock duration");
        Context.require(unlockTime.compareTo(blockTimestamp.add(MAX_TIME)) <= 0,
                "Increase unlock time: Voting lock " + "can be 4 years max");

        this.depositFor(sender, BigInteger.ZERO, unlockTime, locked, INCREASE_UNLOCK_TIME);
        this.nonReentrant.updateLock(false);
    }

    @External
    public void withdraw() {
        this.nonReentrant.updateLock(true);
        Address sender = Context.getCaller();
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());

        LockedBalance locked = getLockedBalance(sender);
        Context.require(blockTimestamp.compareTo(locked.getEnd()) >= 0, "Withdraw: The lock didn't expire");
        BigInteger value = locked.amount;

        LockedBalance oldLocked = locked.newLockedBalance();
        locked.end = UnsignedBigInteger.ZERO;
        locked.amount = BigInteger.ZERO;
        this.locked.set(sender, locked);
        BigInteger supplyBefore = this.supply.get();
        this.supply.set(supplyBefore.subtract(value));

        this.checkpoint(sender, oldLocked, locked);
        callToken("transfer", sender, value, "withdraw".getBytes());
        users.remove(sender);
        Withdraw(sender, value, blockTimestamp);
        Supply(supplyBefore, supplyBefore.subtract(value));

        updateDelegationAndHandleAction(sender);
        this.nonReentrant.updateLock(false);
    }

    private void updateDelegationAndHandleAction(Address sender) {
        //calling update delegation
        call(Contracts.DELEGATION, "updateDelegations", null, sender);
        // calling handle action for rewards
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("_user", sender);
        userDetails.put("_userBalance", balanceOf(sender, BigInteger.ZERO));
        userDetails.put("_totalSupply", totalSupply(BigInteger.ZERO));
        userDetails.put("_decimals", decimals());

        call(Contracts.REWARDS, "handleAction", userDetails);
    }

    private BigInteger findBlockEpoch(BigInteger block, BigInteger maxEpoch) {
        BigInteger min = BigInteger.ZERO;
        BigInteger max = maxEpoch;

        for (int index = 0; index < 256 && min.compareTo(max) < 0; ++index) {
            BigInteger mid = min.add(max).add(BigInteger.ONE).divide(BigInteger.TWO);
            Point point = this.pointHistory.getOrDefault(mid, new Point());
            if (point.block.compareTo(block) <= 0) {
                min = mid;
            } else {
                max = mid.subtract(BigInteger.ONE);
            }
        }

        return min;
    }

    private BigInteger findUserPointHistory(Address address, BigInteger block) {
        BigInteger min = BigInteger.ZERO;
        BigInteger max = this.userPointEpoch.getOrDefault(address, BigInteger.ZERO);

        for (int index = 0; index < 256 && min.compareTo(max) < 0; ++index) {
            BigInteger mid = min.add(max).add(BigInteger.ONE).divide(BigInteger.TWO);
            if (getUserPointHistory(address, mid).block.compareTo(block) <= 0) {
                min = mid;
            } else {
                max = mid.subtract(BigInteger.ONE);
            }
        }
        return min;
    }

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner, @Optional BigInteger timestamp) {
        UnsignedBigInteger uTimestamp;
        if (timestamp.equals(BigInteger.ZERO)) {
            uTimestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
        } else {
            uTimestamp = new UnsignedBigInteger(timestamp);
        }

        BigInteger epoch = this.userPointEpoch.getOrDefault(_owner, BigInteger.ZERO);
        if (epoch.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else {
            Point lastPoint = getUserPointHistory(_owner, epoch);
            UnsignedBigInteger _delta = uTimestamp.subtract(lastPoint.timestamp);
            lastPoint.bias = lastPoint.bias.subtract(lastPoint.slope.multiply(_delta.toBigInteger()));
            if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
                lastPoint.bias = BigInteger.ZERO;
            }

            return lastPoint.bias;
        }
    }

    @External(readonly = true)
    public BigInteger balanceOfAt(Address _owner, BigInteger block) {
        UnsignedBigInteger blockHeight = UnsignedBigInteger.valueOf(Context.getBlockHeight());
        UnsignedBigInteger blockTimestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());

        Context.require(block.compareTo(blockHeight.toBigInteger()) <= 0,
                "BalanceOfAt: Invalid given block height");
        BigInteger userEpoch = this.findUserPointHistory(_owner, block);
        Point uPoint = this.userPointHistory.at(_owner).getOrDefault(userEpoch, new Point());

        BigInteger maxEpoch = this.epoch.get();
        BigInteger epoch = this.findBlockEpoch(block, maxEpoch);
        Point point0 = this.pointHistory.getOrDefault(epoch, new Point());
        UnsignedBigInteger dBlock;
        UnsignedBigInteger dTime;

        if (epoch.compareTo(maxEpoch) < 0) {
            Point point1 = this.pointHistory.getOrDefault(epoch.add(BigInteger.ONE), new Point());
            dBlock = point1.block.subtract(point0.block);
            dTime = point1.timestamp.subtract(point0.timestamp);
        } else {
            dBlock = blockHeight.subtract(point0.block);
            dTime = blockTimestamp.subtract(point0.timestamp);
        }

        UnsignedBigInteger blockTime = point0.timestamp;
        if (!dBlock.equals(UnsignedBigInteger.ZERO)) {
            blockTime = blockTime.add(dTime.multiply(new UnsignedBigInteger(block).subtract(point0.block))
                    .divide(dBlock));
        }
        UnsignedBigInteger delta = blockTime.subtract(uPoint.timestamp);
        uPoint.bias = uPoint.bias.subtract(uPoint.slope.multiply(delta.toBigInteger()));
        return uPoint.bias.compareTo(BigInteger.ZERO) >= 0 ? uPoint.bias : BigInteger.ZERO;
    }

    private BigInteger supplyAt(Point point, BigInteger time) {
        Point lastPoint = point.newPoint();
        UnsignedBigInteger timestampIterator = lastPoint.timestamp.divide(TimeConstants.U_WEEK_IN_MICRO_SECONDS)
                .multiply(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
        UnsignedBigInteger uTime = new UnsignedBigInteger(time);
        for (int index = 0; index < 255; ++index) {
            timestampIterator = timestampIterator.add(TimeConstants.U_WEEK_IN_MICRO_SECONDS);
            BigInteger dSlope = BigInteger.ZERO;
            if (timestampIterator.compareTo(time) > 0) {
                timestampIterator = uTime;
            } else {
                dSlope = this.slopeChanges.getOrDefault(timestampIterator.toBigInteger(), BigInteger.ZERO);
            }
            UnsignedBigInteger delta = timestampIterator.subtract(lastPoint.timestamp);
            lastPoint.bias = lastPoint.bias.subtract(lastPoint.slope.multiply(delta.toBigInteger()));
            if (timestampIterator.equals(uTime)) {
                break;
            }

            lastPoint.slope = lastPoint.slope.add(dSlope);
            lastPoint.timestamp = timestampIterator;
        }

        if (lastPoint.bias.compareTo(BigInteger.ZERO) < 0) {
            lastPoint.bias = BigInteger.ZERO;
        }
        return lastPoint.bias;
    }

    @External(readonly = true)
    public BigInteger totalSupply(@Optional BigInteger time) {
        BigInteger blockTimestamp;
        if (time.equals(BigInteger.ZERO)) {
            blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
            time = blockTimestamp;
        }

        BigInteger epoch = this.epoch.get();
        Point lastPoint = this.pointHistory.getOrDefault(epoch, new Point());
        return this.supplyAt(lastPoint, time);
    }

    @External(readonly = true)
    public BigInteger totalSupplyAt(BigInteger block) {
        UnsignedBigInteger blockHeight = UnsignedBigInteger.valueOf(Context.getBlockHeight());
        UnsignedBigInteger blockTimestamp = UnsignedBigInteger.valueOf(Context.getBlockTimestamp());
        UnsignedBigInteger uBlock = new UnsignedBigInteger(block);
        Context.require(uBlock.compareTo(blockHeight) <= 0, "TotalSupplyAt: Invalid given block height");
        BigInteger epoch = this.epoch.get();
        BigInteger targetEpoch = findBlockEpoch(block, epoch);

        Point point = this.pointHistory.getOrDefault(targetEpoch, new Point());
        UnsignedBigInteger dTime = UnsignedBigInteger.ZERO;
        if (targetEpoch.compareTo(epoch) < 0) {
            Point pointNext = this.pointHistory.getOrDefault(targetEpoch.add(BigInteger.ONE), new Point());
            if (!point.block.equals(pointNext.block)) {
                dTime = uBlock.subtract(point.block).multiply(pointNext.timestamp.subtract(point.timestamp))
                        .divide(pointNext.block.subtract(point.block));
            }
        } else {
            if (!point.block.equals(blockHeight)) {
                dTime = uBlock.subtract(point.block).multiply(blockTimestamp.subtract(point.timestamp))
                        .divide(blockHeight.subtract(point.block));
            }
        }

        return this.supplyAt(point, point.timestamp.add(dTime).toBigInteger());
    }

    @External(readonly = true)
    public SupplyDetails getPrincipalSupply(Address _user) {
        SupplyDetails response = new SupplyDetails();
        response.decimals = BigInteger.valueOf(decimals());
        response.principalUserBalance = balanceOf(_user, BigInteger.ZERO);
        response.principalTotalSupply = totalSupply(BigInteger.ZERO);

        return response;
    }

    @External
    public void kick(Address _user) {
        call(Contracts.DELEGATION, "kick", _user);
        call(Contracts.REWARDS, "kick", _user);
    }

    @External(readonly = true)
    public Address admin() {
        return this.admin.get();
    }

    @External(readonly = true)
    public Address futureAdmin() {
        return this.futureAdmin.get();
    }


    @External(readonly = true)
    public BigInteger userPointEpoch(Address _owner) {
        return this.userPointEpoch.getOrDefault(_owner, BigInteger.ZERO);
    }

    private void ownerRequired() {
        if (!Context.getCaller().equals(this.admin.get())) {
            throw BoostedOMMException.notOwner();
        }
    }

    private LockedBalance getLockedBalance(Address user) {
        return locked.getOrDefault(user, new LockedBalance());
    }

    private Point getUserPointHistory(Address user, BigInteger epoch) {
        return this.userPointHistory.at(user).getOrDefault(epoch, new Point());
    }

}
