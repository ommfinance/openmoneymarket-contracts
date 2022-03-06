package finance.omm.score.core.reward.distribution.db;

import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.db.EnumerableDictDB;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import scorex.util.HashMap;

public class Assets extends EnumerableDictDB<Address, Asset> {


    public static final String LAST_UPDATE_TIMESTAMP = "lastUpdateTimestamp";
    public static final String ASSET_INDEX = "assetIndex";
    public static final String USER_INDEX = "userIndex";

    // asset address -> last update timestamp in seconds
    public final DictDB<Address, BigInteger> lastUpdateTimestamp;
    // asset address-> index
    public final DictDB<Address, BigInteger> assetIndex;
    // user address-> asset-> index
    public final BranchDB<Address, DictDB<Address, BigInteger>> userIndex;


    public Assets(String key) {
        super(key, Address.class, Asset.class);
        lastUpdateTimestamp = Context.newDictDB(key + LAST_UPDATE_TIMESTAMP, BigInteger.class);
        assetIndex = Context.newDictDB(key + ASSET_INDEX, BigInteger.class);
        userIndex = Context.newBranchDB(key + USER_INDEX, BigInteger.class);
    }

    public BigInteger getUserIndex(Address assetAddr, Address user) {
        return this.userIndex.at(user).getOrDefault(assetAddr, BigInteger.ZERO);
    }

    public void setUserIndex(Address assetAddr, Address user, BigInteger newIndex) {
        this.userIndex.at(user).set(assetAddr, newIndex);
    }

    public BigInteger getAssetIndex(Address assetAddr) {
        return this.assetIndex.getOrDefault(assetAddr, BigInteger.ZERO);
    }

    public BigInteger getLastUpdateTimestamp(Address assetAddr) {
        return this.lastUpdateTimestamp.get(assetAddr);
    }

    public void setAssetIndex(Address assetAddr, BigInteger newIndex) {
        this.assetIndex.set(assetAddr, newIndex);
    }

    public void setLastUpdateTimestamp(Address assetAddr, BigInteger currentTime) {
        this.lastUpdateTimestamp.set(assetAddr, currentTime);
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
                result.put(asset.address.toString(), asset.lpID);
            }
        }
        return result;
    }

    public BigInteger getPoolIDByAddress(Address asset) {
        return this.get(asset).lpID;
    }
}
