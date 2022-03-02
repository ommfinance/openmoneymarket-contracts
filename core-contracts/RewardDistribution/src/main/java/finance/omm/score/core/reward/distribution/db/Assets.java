package finance.omm.score.core.reward.distribution.db;

import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.db.EnumerableDictDB;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;

public class Assets extends EnumerableDictDB<Address, Asset> {


    public static final String LAST_UPDATE_TIMESTAMP = "last-update-timestamp";
    public static final String ASSET_INDEX = "asset-index";
    public static final String USER_INDEX = "user-index";

    // assetId-> last update timestamp in seconds
    public final DictDB<String, BigInteger> lastUpdateTimestamp;
    // assetId-> index
    public final DictDB<String, BigInteger> assetIndex;
    // user-> assetId-> index
    public final BranchDB<Address, DictDB<String, BigInteger>> userIndex;


    public Assets(String key) {
        super(key, Address.class, Asset.class);
        lastUpdateTimestamp = Context.newDictDB(key + LAST_UPDATE_TIMESTAMP, BigInteger.class);
        assetIndex = Context.newDictDB(key + ASSET_INDEX, BigInteger.class);
        userIndex = Context.newBranchDB(key + USER_INDEX, BigInteger.class);
    }

    public BigInteger getUserIndex(String assetId, Address user) {
        return this.userIndex.at(user).getOrDefault(assetId, BigInteger.ZERO);
    }

    public BigInteger getUserIndex(Address assetAddress, Address user) {
        Asset asset = get(assetAddress);
        return getUserIndex(asset.id, user);
    }

    public void setUserIndex(String assetId, Address user, BigInteger newIndex) {
        this.userIndex.at(user).set(assetId, newIndex);
    }

    public BigInteger getAssetIndex(String assetId) {
        return this.assetIndex.getOrDefault(assetId, BigInteger.ZERO);
    }

    public BigInteger getAssetIndex(Address assetAddress) {
        Asset asset = get(assetAddress);
        return getAssetIndex(asset.id);
    }

    public BigInteger getLastUpdateTimestamp(String assetId) {
        return this.lastUpdateTimestamp.get(assetId);
    }

    public void setAssetIndex(String assetId, BigInteger newIndex) {
        this.assetIndex.set(assetId, newIndex);
    }

    public void setLastUpdateTimestamp(String assetId, BigInteger currentTime) {
        this.lastUpdateTimestamp.set(assetId, currentTime);
    }

    public Map<String, String> getAssetName() {
        int size = size();
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Address key = getKey(i);
            result.put(key.toString(), get(key).name);
        }
        return result;
    }

    public Map<String, BigInteger> getLiquidityProviders() {
        int size = size();
        Map<String, BigInteger> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Asset asset = getByIndex(i);
            if (asset != null && BigInteger.ZERO.compareTo(asset.lpID) < 0) {
                result.put(asset.id, asset.lpID);
            }
        }
        return result;
    }

    public BigInteger getPoolIDByAddress(Address asset) {
        return this.get(asset).lpID;
    }
}
