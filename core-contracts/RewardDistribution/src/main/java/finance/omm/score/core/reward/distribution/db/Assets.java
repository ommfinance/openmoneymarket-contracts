package finance.omm.score.core.reward.distribution.db;

import finance.omm.score.core.reward.distribution.model.Asset;
import finance.omm.utils.db.EnumerableDictDB;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.BranchDB;
import score.Context;
import score.DictDB;
import scorex.util.HashMap;

public class Assets extends EnumerableDictDB<Address, Asset> {


    public static final String LAST_UPDATE_TIMESTAMP = "indexUpdatedTimestamp";
    public static final String ASSET_INDEX = "assetIndex";
    public static final String USER_INDEX = "userIndex";
    public static final String USERS_ACCRUED_REWARDS = "user-accrued-rewards";

    // asset address -> last update timestamp in seconds
    private final DictDB<Address, BigInteger> indexUpdatedTimestamp;
    // asset address-> index
    public final DictDB<Address, BigInteger> assetIndex;
    // user address-> asset-> index
    public final BranchDB<Address, DictDB<Address, BigInteger>> userIndex;

    // user address-> asset-> rewards
    private final BranchDB<Address, DictDB<Address, BigInteger>> userAccruedRewards;


    public Assets(String key) {
        super(key, Address.class, Asset.class);
        indexUpdatedTimestamp = Context.newDictDB(key + LAST_UPDATE_TIMESTAMP, BigInteger.class);
        assetIndex = Context.newDictDB(key + ASSET_INDEX, BigInteger.class);
        userIndex = Context.newBranchDB(key + USER_INDEX, BigInteger.class);
        userAccruedRewards = Context.newBranchDB(USERS_ACCRUED_REWARDS, BigInteger.class);
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

    public BigInteger getIndexUpdateTimestamp(Address assetAddr) {
        return this.indexUpdatedTimestamp.get(assetAddr);
    }

    public void setAssetIndex(Address assetAddr, BigInteger newIndex) {
        this.assetIndex.set(assetAddr, newIndex);
    }

    public void setIndexUpdatedTimestamp(Address assetAddr, BigInteger currentTime) {
        this.indexUpdatedTimestamp.set(assetAddr, currentTime);
    }


    public Map<String, BigInteger> getLiquidityProviders() {
        int size = size();
        Map<String, BigInteger> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Asset asset = getByIndex(i);
            if (asset != null && asset.lpID != null && asset.lpID.compareTo(BigInteger.ZERO) > 0) {
                result.put(asset.address.toString(), asset.lpID);
            }
        }
        return result;
    }

    public BigInteger getPoolIDByAddress(Address asset) {
        return this.get(asset).lpID;
    }

    public Map<String, String> getAssetName() {
        return getAssetName(List.of());
    }

    public Map<String, String> getAssetName(List<Address> excludes) {
        int size = size();
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Address key = getKey(i);
            if (!excludes.contains(key)) {
                result.put(key.toString(), get(key).name);
            }
        }
        return result;
    }

    public BigInteger getAccruedRewards(Address userAddr, Address assetAddr) {
        return this.userAccruedRewards.at(userAddr).getOrDefault(assetAddr, BigInteger.ZERO);
    }

    public void setAccruedRewards(Address userAddr, Address assetAddr, BigInteger value) {
        this.userAccruedRewards.at(userAddr).set(assetAddr, value);
    }
}
