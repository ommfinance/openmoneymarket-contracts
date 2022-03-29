package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.Optional;

public interface RewardWeightController {


    public String name();


    public void addType(String key, boolean transferToContract, @Optional Address address);


    public void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp);


    public BigInteger getTypeWeight(String type, @Optional BigInteger timestamp);


    public Map<String, BigInteger> getALlTypeWeight(@Optional BigInteger timestamp);


    public void addAsset(String type, Address address, String name);


    public void setAssetWeight(String typeId, WeightStruct[] weights, @Optional BigInteger timestamp);

    public BigInteger tokenDistributionPerDay(BigInteger _day);

    public BigInteger getDay();

    public BigInteger getIntegrateIndex(Address address, BigInteger totalSupply, BigInteger lastUpdatedTimestamp);

    public BigInteger getTypeCheckpointCount();

    public BigInteger getAssetCheckpointCount(String typeId);

    public Map<String, BigInteger> getTypeWeightByTimestamp(BigInteger timestamp);

    public Map<String, BigInteger> getAssetWeightByTimestamp(String type, BigInteger timestamp);

    public BigInteger getAssetWeight(Address asset, @Optional BigInteger timestamp);

    public Map<String, ?> getAllAssetDistributionPercentage(@Optional BigInteger timestamp);

    public Map<String, ?> getDailyRewards(@Optional BigInteger _day);

    public Map<String, ?> getDistPercentageOfLP(@Optional BigInteger timestamp);

    public List<String> getTypes();

    public BigInteger getStartTimestamp();

    public Map<String, ?> getDistributionDetails(BigInteger day);


}
