package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import finance.omm.libs.structs.UserDetails;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface RewardDistribution {

    @External(readonly = true)
    public Map<String, BigInteger> getAssetEmission();

    @External(readonly = true)
    public Address[] getAssets();

    @External(readonly = true)
    public Map<String, String> getAssetNames();

    @External(readonly = true)
    public Map<String, BigInteger> getIndexes(Address _user, Address _asset);

    @External(readonly = true)
    public BigInteger getAssetIndex(Address _asset);

    @External(readonly = true)
    public BigInteger getLastUpdatedTimestamp(Address _asset);

    @External
    public void setAssetName(Address _asset, String _name);

    @Deprecated
    @External
    public void setDistributionPercentage(DistPercentage[] _distPercentage);

    @Deprecated
    @External(readonly = true)
    public BigInteger getDistributionPercentage(String _recipient);

    @External(readonly = true)
    @Deprecated
    public Map<String, BigInteger> getAllDistributionPercentage();

    @Deprecated
    @External(readonly = true)
    public BigInteger assetDistPercentage(Address asset);

    @Deprecated
    @External(readonly = true)
    public Map<String, ?> allAssetDistPercentage();

    @Deprecated
    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> distPercentageOfAllLP();

    @External(readonly = true)
    public Map<String, BigInteger> getLiquidityProviders();

    @Deprecated
    @External
    public void configureAssetConfigs(AssetConfig[] _assetConfig);

    @Deprecated
    @External
    public void removeAssetConfig(Address _asset);

    @Deprecated
    @External
    public void updateEmissionPerSecond();


    @Deprecated
    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day);

    @Deprecated
    @External(readonly = true)
    public BigInteger getDay();

    @Deprecated
    @External(readonly = true)
    public BigInteger getStartTimestamp();

    @External(readonly = true)
    public BigInteger getPoolIDByAsset(Address _asset);

    @Deprecated
    @External(readonly = true)
    public String[] getRecipients();


    @External()
    public void disableRewardClaim();

    @External()
    public void enableRewardClaim();

    @External(readonly = true)
    public boolean isRewardClaimEnabled();

    @Deprecated
    @External(readonly = true)
    public Map<String, ?> getDailyRewards(BigInteger _day);

    @Deprecated
    @External
    public void startDistribution();

    @External
    public void distribute();

    @External(readonly = true)
    public BigInteger getDistributedDay();

    @External
    public void transferOmmToDaoFund(BigInteger _value);

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data);


    @External()
    public void handleAction(UserDetails _userAssetDetails);

    @External
    public void handleLPAction(Address _asset, UserDetails _userDetails);

    @External
    public void setWeight(BigInteger _weight);

    @External
    public void addType(String key, boolean transferToContract);


    @External
    public void addAsset(String type, String name, Address address, @Optional BigInteger poolID);

    @External(readonly = true)
    public Map<String, ?> getRewards(Address user);

    @External
    public void claimRewards(Address user);

    @External(readonly = true)
    public BigInteger getClaimedReward(Address user);

}
