package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.WeightStruct;
import java.math.BigInteger;
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

public interface RewardController {


    @External(readonly = true)
    public String name();

    @External
    public void addType(String name);


    @External
    public void setTypeWeight(WeightStruct[] weights, @Optional BigInteger timestamp);

    @External
    public void addAsset(String typeId, String name, @Optional Address address, @Optional BigInteger poolID);

    @External
    public void setAssetWeight(String typeId, WeightStruct[] weights, @Optional BigInteger timestamp);

    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day);

    @External(readonly = true)
    public BigInteger getDay();

    @External(readonly = true)
    public BigInteger getIntegrateIndex(String assetId, BigInteger totalSupply, BigInteger lastUpdatedTimestamp);

}
