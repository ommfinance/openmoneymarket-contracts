package finance.omm.score.core.reward.distribution;


import static finance.omm.utils.constants.TimeConstants.DAYS_PER_YEAR;
import static finance.omm.utils.constants.TimeConstants.DAY_IN_MICRO_SECONDS;
import static finance.omm.utils.math.MathUtils.ICX;
import static finance.omm.utils.math.MathUtils.MILLION;
import static finance.omm.utils.math.MathUtils.convertToExa;
import static finance.omm.utils.math.MathUtils.pow;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.libs.address.AddressProvider;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import finance.omm.libs.structs.UserAssetInput;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.utils.constants.TimeConstants;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

public abstract class AbstractRewardDistribution extends AddressProvider implements RewardDistribution {

    public static final String TAG = "Omm Reward Distribution Manager";
    public static final String REWARD_CONFIG = "rewardConfig";
    public static final String LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String TIMESTAMP_AT_START = "timestampAtStart";
    public static final String ASSET_INDEX = "assetIndex";
    public static final String USER_INDEX = "userIndex";
//    public static final String RESERVE_ASSETS = "reserveAssets";

    public final RewardConfigurationDB _rewardConfig = new RewardConfigurationDB(REWARD_CONFIG);

    public final DictDB<Address, BigInteger> _lastUpdateTimestamp = Context.newDictDB(LAST_UPDATE_TIMESTAMP,
            BigInteger.class);
    public final DictDB<Address, BigInteger> _assetIndex = Context.newDictDB(ASSET_INDEX, BigInteger.class);
    public final BranchDB<Address, DictDB<Address, BigInteger>> _userIndex = Context.newBranchDB(USER_INDEX,
            BigInteger.class);

    //    public final ArrayDB<Address> _reserveAssets = Context.newArrayDB(RESERVE_ASSETS, Address.class);
    public final VarDB<BigInteger> _timestampAtStart = Context.newVarDB(TIMESTAMP_AT_START, BigInteger.class);


    public AbstractRewardDistribution(Address addressProvider) {
        super(addressProvider);
    }


    @EventLog(indexed = 1)
    public void AssetIndexUpdated(Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 2)
    public void UserIndexUpdated(Address _user, Address _asset, BigInteger _oldIndex, BigInteger _newIndex) {
    }


    @EventLog(indexed = 1)
    public void AssetConfigUpdated(Address _asset, BigInteger _emissionPerSecond) {
    }


    @External(readonly = true)
    public Map<String, BigInteger> getAssetEmission() {
        return this._rewardConfig.getAllEmissionPerSecond();
    }

    @External(readonly = true)
    public List<Address> getAssets() {
        return this._rewardConfig.getAssets();
    }

    @External(readonly = true)
    public Map<String, String> getAssetNames() {
        return this._rewardConfig.getAssetNames();
    }

    @External(readonly = true)
    public Map<String, BigInteger> getIndexes(Address _user, Address _asset) {
        return Map.of("userIndex", this._userIndex.at(_user)
                .get(_asset), "assetIndex", this._assetIndex.get(_asset));
    }

    @External
    public void setAssetName(Address _asset, String _name) {
        checkOwner();
        this._rewardConfig.setAssetName(_asset, _name);
    }


    @External
    public void setTimeStamp(BigInteger _timestamp) {
        checkOwner();
        List<Address> _assets = _rewardConfig.getAssets();
        for (Address _asset : _assets) {
            _lastUpdateTimestamp.set(_asset, _timestamp);
        }

    }


    public void _updateDistPercentage(DistPercentage[] _distPercentage) {
        BigInteger totalPercentage = BigInteger.ZERO;
        for (DistPercentage config : _distPercentage) {
            String _recipient = config.recipient;
            BigInteger _percentage = config.percentage;
            totalPercentage = totalPercentage.add(_percentage);
            this._rewardConfig.setDistributionPercentage(_recipient, _percentage);
        }
        if (!totalPercentage.equals(ICX)) {
            throw RewardDistributionException.invalidTotalPercentage(
                    totalPercentage + " :: Percentage doesn't sum upto 100%");
        }

    }

    @External
    public void setDistributionPercentage(DistPercentage[] _distPercentage) {
        checkOwner();
        this._updateDistPercentage(_distPercentage);
        this.updateEmissionPerSecond();
    }

    @External(readonly = true)
    public BigInteger getDistributionPercentage(String _recipient) {
        return this._rewardConfig.getDistributionPercentage(_recipient);
    }

    @External(readonly = true)
    public Map<String, BigInteger> getAllDistributionPercentage() {
        return this._rewardConfig.getAllDistributionPercentage();
    }

    @External(readonly = true)
    public BigInteger assetDistPercentage(Address asset) {
        return this._rewardConfig.getAssetPercentage(asset);
    }

    @External(readonly = true)
    public Map<String, ?> allAssetDistPercentage() {
        return this._rewardConfig.getAssetConfigs();
    }

    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> distPercentageOfAllLP() {
        return this._rewardConfig.assetConfigOfLiquidityProvider();
    }

    public void _configureAsset(BigInteger distributionPerDay, AssetConfig _assetConfig) {
        Address asset = _assetConfig.asset;
        this._rewardConfig.setAssetConfig(_assetConfig);
        BigInteger _totalBalance = this._getTotalBalance(asset);
        this._updateAssetStateInternal(asset, _totalBalance);
        BigInteger _emissionPerSecond = this._rewardConfig.updateEmissionPerSecond(asset, distributionPerDay);
        this.AssetConfigUpdated(asset, _emissionPerSecond);
    }

    @External
    public void configureAssetConfigs(AssetConfig[] _assetConfig) {
        checkGovernance();
        BigInteger distributionPerDay = this.tokenDistributionPerDay(this.getDay());
        for (AssetConfig config : _assetConfig) {
            this._configureAsset(distributionPerDay, config);
        }
    }

    @External
    public void configureAssetConfig(AssetConfig _assetConfig) {
        checkGovernance();
        BigInteger distributionPerDay = this.tokenDistributionPerDay(this.getDay());
        this._configureAsset(distributionPerDay, _assetConfig);
    }


    @External
    public void removeAssetConfig(Address _asset) {
        checkGovernance();
        BigInteger _totalBalance = this._getTotalBalance(_asset);
        this._updateAssetStateInternal(_asset, _totalBalance);

        this._rewardConfig.removeAssetConfig(_asset);
    }


    @External
    public void updateEmissionPerSecond() {
        BigInteger distributionPerDay = this.tokenDistributionPerDay(this.getDay());
        List<Address> _assets = this._rewardConfig.getAssets();
        for (Address asset : _assets) {
            BigInteger _totalBalance = this._getTotalBalance(asset);
            this._updateAssetStateInternal(asset, _totalBalance);
            this._rewardConfig.updateEmissionPerSecond(asset, distributionPerDay);
        }
    }

    public BigInteger _updateAssetStateInternal(Address _asset, BigInteger _totalBalance) {
        BigInteger oldIndex = this._assetIndex.getOrDefault(_asset, BigInteger.ZERO);
        BigInteger lastUpdateTimestamp = this._lastUpdateTimestamp.get(_asset);

        BigInteger currentTime = TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);

        if (currentTime.equals(lastUpdateTimestamp)) {
            return oldIndex;
        }
        BigInteger _emissionPerSecond = this._rewardConfig.getEmissionPerSecond(_asset);

        BigInteger newIndex = this._getAssetIndex(oldIndex, _emissionPerSecond, lastUpdateTimestamp, _totalBalance);
        if (!newIndex.equals(oldIndex)) {
            this._assetIndex.set(_asset, newIndex);
            this.AssetIndexUpdated(_asset, oldIndex, newIndex);
        }

        this._lastUpdateTimestamp.set(_asset, currentTime);
        return newIndex;
    }

    public BigInteger _updateUserReserveInternal(Address _user, Address _asset, BigInteger _userBalance,
            BigInteger _totalBalance) {
        BigInteger userIndex = this._userIndex.at(_user).getOrDefault(_asset, BigInteger.ZERO);
        BigInteger accruedRewards = BigInteger.ZERO;

        BigInteger newIndex = this._updateAssetStateInternal(_asset, _totalBalance);

        if (!userIndex.equals(newIndex)) {
            if (!BigInteger.ZERO.equals(_userBalance)) {
                accruedRewards = AbstractRewardDistribution._getRewards(_userBalance, newIndex, userIndex);
            }

            this._userIndex.at(_user).set(_asset, newIndex);
            this.UserIndexUpdated(_user, _asset, userIndex, newIndex);
        }
        return accruedRewards;
    }

    private static BigInteger _getRewards(BigInteger _userBalance, BigInteger _assetIndex, BigInteger _userIndex) {
        return MathUtils.exaMultiply(_userBalance, _assetIndex.subtract(_userIndex));
    }

    public BigInteger _getAssetIndex(BigInteger _currentIndex, BigInteger _emissionPerSecond,
            BigInteger _lastUpdateTimestamp, BigInteger _totalBalance) {
        BigInteger currentTime = TimeConstants.getBlockTimestamp().divide(TimeConstants.SECOND);
        if (_emissionPerSecond.equals(BigInteger.ZERO) || _totalBalance.equals(BigInteger.ZERO)
                || _lastUpdateTimestamp.equals(currentTime)) {
            return _currentIndex;
        }
        BigInteger timeDelta = currentTime.subtract(_lastUpdateTimestamp);
        return MathUtils.exaDivide(_emissionPerSecond.multiply(timeDelta), _totalBalance).add(_currentIndex);
    }

    public BigInteger _getUnclaimedRewards(Address _user, UserAssetInput _assetInput) {
        Address asset = _assetInput.asset;
        BigInteger _emissionPerSecond = this._rewardConfig.getEmissionPerSecond(asset);
        BigInteger assetIndex = this._getAssetIndex(this._assetIndex.getOrDefault(asset, BigInteger.ZERO),
                _emissionPerSecond,
                this._lastUpdateTimestamp.getOrDefault(asset, BigInteger.ZERO), _assetInput.totalBalance);
        return AbstractRewardDistribution._getRewards(_assetInput.userBalance, assetIndex, this._userIndex.at(_user)
                .getOrDefault(asset, BigInteger.ZERO));
    }

    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day) {

        if (MathUtils.isLessThan(_day, BigInteger.ZERO)) {
            return BigInteger.ZERO;
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(30L))) {
            return MILLION;
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR)) {
            return BigInteger.valueOf(4L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, DAYS_PER_YEAR.multiply(BigInteger.TWO))) {
            return BigInteger.valueOf(3L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(3L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.valueOf(2L).multiply(MILLION).divide(BigInteger.TEN);
        } else if (MathUtils.isLessThan(_day, BigInteger.valueOf(4L).multiply(DAYS_PER_YEAR))) {
            return BigInteger.ONE.multiply(MILLION).divide(BigInteger.TEN);
        } else {
            BigInteger index = _day.divide(DAYS_PER_YEAR).subtract(BigInteger.valueOf(4L));
            return pow(BigInteger.valueOf(103L), (index.intValue()))
                    .multiply(BigInteger.valueOf(3L))
                    .multiply(BigInteger.valueOf(383L).multiply(MILLION))
                    .divide(DAYS_PER_YEAR)
                    .divide(pow(BigInteger.valueOf(100L),
                            (index.intValue() + 1)));
        }
    }

    @External(readonly = true)
    public BigInteger getDay() {
        BigInteger timestamp = TimeConstants.getBlockTimestamp();
        return timestamp.subtract(_timestampAtStart.get())
                .divide(DAY_IN_MICRO_SECONDS);
    }

    @External(readonly = true)
    public BigInteger getStartTimestamp() {
        return this._timestampAtStart.get();
    }

    public BigInteger _getTotalBalance(Address asset) {
        Integer poolId = this._rewardConfig.getPoolID(asset);
        Map<String, ?> map = null;
        if (poolId > 0) {
            map = (Map<String, ?>) Context.call(getAddress(Contracts.STAKED_LP.getKey()),
                    "getTotalStaked", poolId);
        } else {
            map = (Map<String, ?>) Context.call(asset, "getTotalStaked");
        }
        if (map == null) {
            throw RewardDistributionException.unknown("total staked is null");
        }
        TotalStaked totalStaked = TotalStaked.fromMap(map);
        return MathUtils.convertToExa(totalStaked.totalStaked, totalStaked.decimals);
    }

    public UserAssetInput _getUserAssetDetails(Address asset, Address user) {
        Integer poolId = this._rewardConfig.getPoolID(asset);
        UserAssetInput result = new UserAssetInput();

        Map<String, ?> map = null;
        if (poolId > 0) {
            map = (Map<String, ?>) Context.call(getAddress(Contracts.STAKED_LP.getKey()),
                    "getLPStakedSupply", poolId, user);
        } else {
            map = (Map<String, ?>) Context.call(asset, "getPrincipalSupply", user);
        }

        result.asset = asset;
        if (map == null) {
            throw RewardDistributionException.unknown("supply is null");
        }
        SupplyDetails supplyDetails = SupplyDetails.fromMap(map);

        BigInteger _decimals = supplyDetails.decimals;
        result.userBalance = convertToExa(supplyDetails.principalUserBalance, _decimals);
        result.totalBalance = convertToExa(supplyDetails.principalTotalSupply, _decimals);

        return result;


    }

    @External(readonly = true)
    public BigInteger getPoolIDByAsset(Address _asset) {
        return BigInteger.valueOf(this._rewardConfig.getPoolID(_asset));
    }


    protected void checkOwner() {
        if (!Context.getOwner()
                .equals(Context.getCaller())) {
            throw RewardDistributionException.notOwner();
        }
    }

    protected void checkGovernance() {
        if (!Context.getCaller()
                .equals(this.getAddress(Contracts.GOVERNANCE.getKey()))) {
            throw RewardDistributionException.notGovernanceContract();
        }
    }

}
