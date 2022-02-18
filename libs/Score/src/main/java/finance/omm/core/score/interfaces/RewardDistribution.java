package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import score.Address;
import score.annotation.External;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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


}
