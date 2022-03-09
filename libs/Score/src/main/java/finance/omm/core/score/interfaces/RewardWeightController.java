package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface RewardWeightController {


    @External(readonly = true)
    public String name();

    @External
    public void addType(String key, boolean transferToContract, @Optional Address address);

    @External
    public void setTypeWeight(TypeWeightStruct[] weights, @Optional BigInteger timestamp);

    @External(readonly = true)
    public Map<String, BigInteger> getTypeWeight(String typeId, @Optional BigInteger timestamp);

    @External(readonly = true)
    public Map<String, BigInteger> getALlTypeWeight(@Optional BigInteger timestamp);

    @External
    public void addAsset(String type, Address address, String name);

    @External
    public void setAssetWeight(String typeId, WeightStruct[] weights, @Optional BigInteger timestamp);

    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day);

    @External(readonly = true)
    public BigInteger getDay();

    @External(readonly = true)
    public BigInteger getIntegrateIndex(Address address, BigInteger totalSupply, BigInteger lastUpdatedTimestamp);

    @External(readonly = true)
    public BigInteger getTypeCheckpointCount();

    @External(readonly = true)
    public BigInteger getAssetCheckpointCount(String typeId);

    @External(readonly = true)
    public Map<String, BigInteger> getTypeWeightByTimestamp(BigInteger timestamp);

    @External(readonly = true)
    public Map<String, BigInteger> getAssetWeightByTimestamp(String type, BigInteger timestamp);

    @External(readonly = true)
    public BigInteger getAssetWeight(Address asset, @Optional BigInteger timestamp);

    @External(readonly = true)
    public Map<String, ?> getAllAssetDistributionPercentage(@Optional BigInteger timestamp);

    @External(readonly = true)
    public Map<String, ?> getDailyRewards(@Optional BigInteger _day);

    @External(readonly = true)
    public Map<String, BigInteger> getDistPercentageOfLP(@Optional BigInteger timestamp);

    @External(readonly = true)
    public List<String> getTypes();

    @External(readonly = true)
    public BigInteger getStartTimestamp();


    @External(readonly = true)
    public Map<String, ?> distributionDetails(BigInteger day);


}
