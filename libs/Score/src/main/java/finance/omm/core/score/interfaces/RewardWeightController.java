package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import foundation.icon.score.client.ScoreInterface;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

@ScoreInterface(suffix = "Client")
public interface RewardWeightController {


    String name();

    void addType(String key, boolean transferToContract, @Optional Address address);

    void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp);

    BigInteger getTypeWeight(String type, @Optional BigInteger timestamp);

    Map<String, BigInteger> getAllTypeWeight(@Optional BigInteger timestamp);

    void addAsset(String type, Address address, String name);

    void setAssetWeight(String type, WeightStruct[] weights, @Optional BigInteger timestamp);

    BigInteger tokenDistributionPerDay(BigInteger _day);

    BigInteger getDay();

    BigInteger getIntegrateIndex(Address assetAddr, BigInteger totalSupply, BigInteger lastUpdatedTimestamp);

    BigInteger getTypeCheckpointCount();

    BigInteger getAssetCheckpointCount(String type);

    Map<String, BigInteger> getTypeWeightByTimestamp(BigInteger timestamp);

    Map<String, BigInteger> getAssetWeightByTimestamp(String type, @Optional BigInteger timestamp);

    BigInteger getAssetWeight(Address assetAddr, @Optional BigInteger timestamp);

    Map<String, ?> getAllAssetDistributionPercentage(@Optional BigInteger timestamp);

    Map<String, BigInteger> getAssetDailyRewards(@Optional BigInteger _day);

    Map<String, ?> getDailyRewards(@Optional BigInteger _day);

    Map<String, ?> getDistPercentageOfLP(@Optional BigInteger timestamp);

    List<String> getTypes();

    BigInteger getStartTimestamp();

    Map<String, ?> getDistributionDetails(BigInteger day);

    Map<String, BigInteger> getEmissionRate(@Optional BigInteger timestamp);

}
