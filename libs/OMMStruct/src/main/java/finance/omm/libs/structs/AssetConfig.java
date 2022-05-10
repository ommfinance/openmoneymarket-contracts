package finance.omm.libs.structs;

import score.Address;

import java.math.BigInteger;

public class AssetConfig {
    public Integer poolID;
    public Address asset;
    public BigInteger distPercentage;
    public String assetName;
    public String rewardEntity;
}
