package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.AssetConfig;
import finance.omm.libs.structs.DistPercentage;
import finance.omm.libs.structs.UserDetails;
import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface RewardDistribution extends AddressProvider {

    @External(readonly = true)
    String name();

    @External(readonly = true)
    Map<String, BigInteger> getAssetEmission();

    @External(readonly = true)
    List<Address> getAssets();

    @External(readonly = true)
    Map<String, String> getAssetNames();

    @External(readonly = true)
    Map<String, BigInteger> getIndexes(Address _user, Address _asset);

    @External(readonly = true)
    BigInteger getAssetIndex(Address _asset);

    @External(readonly = true)
    BigInteger getLastUpdatedTimestamp(Address _asset);

    @External
    void setAssetName(Address _asset, String _name);

    @Deprecated
    @External
    void setDistributionPercentage(DistPercentage[] _distPercentage);

    @Deprecated
    @External(readonly = true)
    BigInteger getDistributionPercentage(String _recipient);

    @External(readonly = true)
    @Deprecated
    Map<String, BigInteger> getAllDistributionPercentage();

    @Deprecated
    @External(readonly = true)
    BigInteger assetDistPercentage(Address asset);

    @Deprecated
    @External(readonly = true)
    Map<String, ?> allAssetDistPercentage();

    @Deprecated
    @External(readonly = true)
    Map<String, Map<String, BigInteger>> distPercentageOfAllLP();

    @External(readonly = true)
    Map<String, BigInteger> getLiquidityProviders();

    @Deprecated
    @External
    void configureAssetConfigs(AssetConfig[] _assetConfig);

    @Deprecated
    @External
    void removeAssetConfig(Address _asset);

    @Deprecated
    @External
    void updateEmissionPerSecond();


    @Deprecated
    @External(readonly = true)
    BigInteger tokenDistributionPerDay(BigInteger _day);

    @Deprecated
    @External(readonly = true)
    BigInteger getDay();

    @Deprecated
    @External(readonly = true)
    BigInteger getStartTimestamp();

    @External(readonly = true)
    BigInteger getPoolIDByAsset(Address _asset);

    @Deprecated
    @External(readonly = true)
    String[] getRecipients();


    @External()
    void disableRewardClaim();

    @External()
    void enableRewardClaim();

    @External(readonly = true)
    boolean isRewardClaimEnabled();

    @Deprecated
    @External(readonly = true)
    Map<String, ?> getDailyRewards(BigInteger _day);

    @Deprecated
    @External
    void startDistribution();

    @External
    void distribute();

    @External(readonly = true)
    BigInteger getDistributedDay();

    @External
    void transferOmmToDaoFund(BigInteger _value);

    @External
    void tokenFallback(Address _from, BigInteger _value, byte[] _data);


    @External()
    void handleAction(UserDetails _userAssetDetails);

    @External
    void handleLPAction(Address _asset, UserDetails _userDetails);

    @External
    void addType(String key, boolean transferToContract);


    @External
    void addAsset(String type, String name, Address address, @Optional BigInteger poolID);

    @External(readonly = true)
    Map<String, ?> getRewards(Address user);

    @External
    void claimRewards(Address user);

    @External(readonly = true)
    BigInteger getClaimedReward(Address user);


    @External(readonly = true)
    Map<String, BigInteger> getWorkingBalances(Address user);

    @External(readonly = true)
    Map<String, BigInteger> getWorkingTotal();

    void disableHandleActions();

    void enableHandleActions();

    boolean isHandleActionEnabled();

    Map<String, ?> getAllAssetLegacyIndexes();

    Map<String, Map<String, BigInteger>> getUserAllLegacyIndexes(Address _user);

    void kick(Address userAddr);

    void updateAssetIndexes();

    void migrateUserRewards(Address[] userAddresses);

    Map<String, BigInteger> getUserDailyReward(Address user);
}
