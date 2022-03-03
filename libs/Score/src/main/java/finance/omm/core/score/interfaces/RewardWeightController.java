package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.TypeWeightStruct;
import finance.omm.libs.structs.WeightStruct;
import java.math.BigInteger;
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

    @External
    public void addAsset(String type, Address address, String name);

    @External
    public void setAssetWeight(String typeId, WeightStruct[] weights, @Optional BigInteger timestamp);

    @External(readonly = true)
    public BigInteger tokenDistributionPerDay(BigInteger _day);

    @External(readonly = true)
    public BigInteger getDay();

    @External(readonly = true)
    public BigInteger getIntegrateIndex(Address asset, BigInteger totalSupply, BigInteger lastUpdatedTimestamp);

}
