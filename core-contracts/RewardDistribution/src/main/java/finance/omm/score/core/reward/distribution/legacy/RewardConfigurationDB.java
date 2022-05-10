package finance.omm.score.core.reward.distribution.legacy;

import static finance.omm.utils.math.MathUtils.ICX;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.score.core.reward.distribution.exception.RewardDistributionException;
import finance.omm.utils.math.MathUtils;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;
import scorex.util.HashMap;

@Deprecated
public class RewardConfigurationDB {

    public static final String TAG = "Reward Configuration DB";
    public static final String EMISSION_PER_SECOND = "EmissionPerSecond";
    public static final String ENTITY_DISTRIBUTION_PERCENTAGE = "EntityDistributionPercentage";
    public static final String SUPPORTED_RECIPIENTS = "SupportedRecipients";
    public static final String ASSET_LEVEL_PERCENTAGE = "AssetLevelPercentage";
    public static final String REWARD_ENTITY_MAPPING = "RewardEntityMapping";
    public static final String POOL_ID_MAPPING = "PoolIDMapping";
    public static final String ASSET_NAME = "AssetName";
    public static final String ASSETS = "Assets";
    public static final String ASSET_INDEXES = "AssetIndexes";

    public static final BigInteger SECONDS_IN_DAY = BigInteger.valueOf(86400L);

    public static final String RESERVE = "reserve";
    public static final String STAKING = "staking";
    public static final String LIQUIDITY = "liquidity";

    public final DictDB<String, BigInteger> _distributionPercentage;
    public final ArrayDB<String> _supportedRecipients;
    public final DictDB<Address, BigInteger> _emissionPerSecond;
    public final DictDB<Address, BigInteger> _assetLevelPercentage;
    public final DictDB<Address, String> _rewardEntityMapping;
    public final DictDB<Address, Integer> _poolIDMapping;
    public final DictDB<Address, String> _assetName;
    public final ArrayDB<Address> _assets;
    public final DictDB<Address, Integer> _indexes;

    public RewardConfigurationDB(String key) {
        _distributionPercentage = Context.newDictDB(key + ENTITY_DISTRIBUTION_PERCENTAGE, BigInteger.class);
        _supportedRecipients = Context.newArrayDB(key + SUPPORTED_RECIPIENTS, String.class);
        _emissionPerSecond = Context.newDictDB(key + EMISSION_PER_SECOND, BigInteger.class);
        _assetLevelPercentage = Context.newDictDB(key + ASSET_LEVEL_PERCENTAGE, BigInteger.class);

        _rewardEntityMapping = Context.newDictDB(key + REWARD_ENTITY_MAPPING, String.class);
        _poolIDMapping = Context.newDictDB(key + POOL_ID_MAPPING, Integer.class);

        _assetName = Context.newDictDB(key + ASSET_NAME, String.class);

        _assets = Context.newArrayDB(key + ASSETS, Address.class);
        _indexes = Context.newDictDB(key + ASSET_INDEXES, Integer.class);
    }

    private int get_index(Address asset) {
        return this._indexes.getOrDefault(asset, 0);
    }

    private int size() {
        return this._assets.size();
    }

    public boolean is_valid_asset(Address asset) {
        return get_index(asset) != 0;
    }

    private void add_asset(Address asset) {
        if (get_index(asset) == 0) {
            this._assets.add(asset);
            this._indexes.set(asset, this._assets.size());
        }
    }

    public void setRecipient(String recipient) {
        if (isRecipient(recipient)) {
            this._supportedRecipients.add(recipient);
        }
    }

    private boolean isRecipient(String recipient) {
        for (int i = 0; i < this._supportedRecipients.size(); i++) {
            if (recipient.equals(this._supportedRecipients.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void setDistributionPercentage(String recipient, BigInteger percentage) {
        if (!isRecipient(recipient)) {
            throw RewardDistributionException.invalidRecipient("unsupported recipient " + recipient);
        }
        this._distributionPercentage.set(recipient, percentage);
    }

    public BigInteger getDistributionPercentage(String recipient) {
        return this._distributionPercentage.getOrDefault(recipient, BigInteger.ZERO);
    }


    public Map<String, BigInteger> getAllDistributionPercentage() {
        int size = this._supportedRecipients.size();
        Map.Entry<String, BigInteger>[] entries = new Map.Entry[size];
        for (int i = 0; i < size; i++) {
            String recipient = this._supportedRecipients.get(i);
            BigInteger value = getDistributionPercentage(recipient);
            entries[i] = Map.entry(recipient, value);
        }
        return Map.ofEntries(entries);
    }

    public String[] getRecipients() {
        int size = this._supportedRecipients.size();
        String[] list = new String[size];
        for (int i = 0; i < size; i++) {
            list[i] = this._supportedRecipients.get(i);
        }
        return list;
    }

    public void removeAssetConfig(Address asset) {
        int index = get_index(asset);
        if (index == 0) {
            throw RewardDistributionException.invalidAsset("Asset not found " + asset);
        }

        this._assetLevelPercentage.set(asset, null);
        this._rewardEntityMapping.set(asset, null);
        this._assetName.set(asset, null);
        this._poolIDMapping.set(asset, null);
        this._emissionPerSecond.set(asset, null);
        int last_index = size();
        //TODO
        Address last_asset = this._assets.pop();
        this._indexes.set(asset, null);
        if (index != last_index) {
            this._assets.set(index - 1, last_asset);
            this._indexes.set(last_asset, index);
        }
    }

    public void setAssetConfig(AssetConfig config) {
        Address asset = config.asset;
        String assetName = config.assetName;
        BigInteger distPercentage = config.distPercentage;
        this._assetLevelPercentage.set(asset, distPercentage);
        this._rewardEntityMapping.set(asset, config.rewardEntity);
        this._poolIDMapping.set(asset, config.poolID);
        this._assetName.set(asset, assetName);
        this.add_asset(asset);

        validateTotalPercentage(config.rewardEntity);
    }

    private void validateTotalPercentage(String rewardEntity) {
        BigInteger total_percentage = BigInteger.ZERO;
        for (int i = 0; i < this._assets.size(); i++) {
            Address asset = this._assets.get(i);
            if (rewardEntity.equals(this._rewardEntityMapping.get(asset))) {
                total_percentage = total_percentage.add(this._assetLevelPercentage.getOrDefault(asset,
                        BigInteger.ZERO));
            }

        }
        if (total_percentage.compareTo(ICX) > 0) {
            throw RewardDistributionException.invalidTotalPercentage(total_percentage.divide(ICX) + " should be less "
                    + "than or equals to 100%");
        }
    }

    public Integer getPoolID(Address asset) {
        return this._poolIDMapping.getOrDefault(asset, 0);
    }


    public BigInteger updateEmissionPerSecond(Address asset, BigInteger distributionPerDay) {
        BigInteger _percentage = this.getAssetPercentage(asset);
        BigInteger _emissionPerSecond = MathUtils.exaMultiply(distributionPerDay.divide(SECONDS_IN_DAY), _percentage);
        this._emissionPerSecond.set(asset, _emissionPerSecond);
        return _emissionPerSecond;
    }

    public BigInteger getAssetPercentage(Address asset) {
        String _entityKey = this._rewardEntityMapping.get(asset);
        BigInteger _entityDistPercentage = this._distributionPercentage.getOrDefault(_entityKey, BigInteger.ZERO);
        BigInteger _assetPercentage = this._assetLevelPercentage.getOrDefault(asset, BigInteger.ZERO);
        return MathUtils.exaMultiply(_entityDistPercentage, _assetPercentage);
    }

    public String getEntity(Address asset) {
        Integer _poolID = this._poolIDMapping.get(asset);
        String _rewardEntity = this._rewardEntityMapping.get(asset);
        if (_poolID > 0 && "liquidityProvider".equals(_rewardEntity)) {
            return LIQUIDITY;
        } else if ("liquidityProvider".equals(_rewardEntity)) {
            return STAKING;
        } else if ("lendingBorrow".equals(_rewardEntity)) {
            return RESERVE;
        }
        return null;
    }


    public Map<String, ?> getAssetConfigs() {
        BigInteger _total_percentage = BigInteger.ZERO;
        int size = this.size();
        Map<String, Object> response = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Address asset = this._assets.get(i);
            String _name = this._assetName.get(asset);
            BigInteger _percentage = this.getAssetPercentage(asset);
            String _entity = this.getEntity(asset);
            if (_entity == null) {
                throw RewardDistributionException.invalidAsset("Unsupported entity :: " + asset);
            }
            Map<String, BigInteger> _entityMap = (Map<String, BigInteger>) response.get(_entity);
            if (_entityMap == null) {
                _entityMap = new HashMap<>() {{
                    put("total", BigInteger.ZERO);
                }};
            }

            BigInteger total = _entityMap.get("total");
            _entityMap.put(_name, _percentage);
            _entityMap.put("total", total.add(_percentage));
            response.put(_entity, _entityMap);
            _total_percentage = _total_percentage.add(_percentage);
        }
        response.put("total", _total_percentage);

        return response;
    }

    public Map<String, Map<String, BigInteger>> assetConfigOfLiquidityProvider() {
        Map<String, BigInteger> configs = new HashMap<>();
        int size = this.size();
        for (int i = 0; i < size; i++) {
            Address asset = this._assets.get(i);
            Integer poolID = this._poolIDMapping.get(asset);
            if (poolID > 0) {
                configs.put(asset.toString(), this.getAssetPercentage(asset));
            }
        }
        return Map.of("liquidity", configs);
    }

    public List<Address> getAssets() {
        int size = this.size();

        List<Address> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            list.add(this._assets.get(i));
        }
        return list;
    }

    public BigInteger getEmissionPerSecond(Address asset) {
        return this._emissionPerSecond.getOrDefault(asset, BigInteger.ZERO);
    }

    public Map<String, BigInteger> getAllEmissionPerSecond() {
        int size = this.size();

        Map<String, BigInteger> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            Address asset = this._assets.get(i);
            if (asset != null) {
                map.put(asset.toString(), this.getEmissionPerSecond(asset));
            }
        }
        return map;
    }

    public Map<String, String> getAssetNames() {
        int size = this.size();

        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            Address asset = this._assets.get(i);
            if (asset != null) {
                map.put(asset.toString(), this._assetName.get(asset));
            }
        }
        return map;
    }

    public void setAssetName(Address asset, String name) {
        if (!is_valid_asset(asset)) {
            throw RewardDistributionException.invalidAsset("Asset not found " + asset);
        }
        this._assetName.set(asset, name);
    }

    public String getAssetName(Address asset) {
        return this._assetName.get(asset);
    }
}
