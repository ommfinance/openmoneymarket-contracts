package finance.omm.libs.structs;

import java.math.BigInteger;
import score.Address;

public class AssetConfig {

    public int poolID;
    public Address asset;
    public BigInteger distPercentage;
    public String assetName;
    public String rewardEntity;
}
