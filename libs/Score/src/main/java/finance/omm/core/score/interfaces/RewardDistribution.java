package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import finance.omm.libs.structs.UserDetails;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface RewardDistribution {

    @External(readonly = true)
    public Map<String, BigInteger> getAssetEmission();

    @External(readonly = true)
    public List<Address> getAssets();

    @External(readonly = true)
    public Map<String, String> getAssetNames();

    @External(readonly = true)
    public Map<String, BigInteger> getIndexes(Address _user, Address _asset);

    @External
    public void setAssetName(Address _asset, String _name);

    @External
    public void setDistributionPercentage(DistPercentage[] _distPercentage);

    @External(readonly = true)
    public BigInteger getDistributionPercentage(String _recipient);

    @External(readonly = true)
    public Map<String, BigInteger> getAllDistributionPercentage();

    @External(readonly = true)
    public BigInteger assetDistPercentage(Address asset);

    @External(readonly = true)
    public Map<String, ?> allAssetDistPercentage();

    @External(readonly = true)
    public Map<String, Map<String, BigInteger>> distPercentageOfAllLP();

    @External
    public void configureAssetConfigs(AssetConfig[] _assetConfig);


    @External
    public void removeAssetConfig(Address _asset);

    @External
    public void updateEmissionPerSecond();

    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day);

    @External(readonly = true)
    public BigInteger getDay();

    @External(readonly = true)
    public BigInteger getStartTimestamp();


    @External(readonly = true)
    public BigInteger getPoolIDByAsset(Address _asset);


    @External(readonly = true)
    public String[] getRecipients();

    @External()
    public void handleAction(UserDetails _userAssetDetails);

    @External()
    public void disableRewardClaim();

    @External()
    public void enableRewardClaim();

    @External(readonly = true)
    public Boolean isRewardClaimEnabled();

    @External
    public void handleLPAction(Address _asset, UserDetails _userDetails);

    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day);


    @External(readonly = true)
    public Map<String, ?> getRewards(Address _user);

    @External
    public void startDistribution();

    @External
    public void claimRewards(Address _user);

    @External
    public void distribute();

    @External(readonly = true)
    public BigInteger getDistributedDay();

    @External
    public void transferOmmToDaoFund(BigInteger _value);

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data);

}
