package finance.omm.core.score.interfaces;

import finance.omm.libs.structs.UserDetails;
import finance.omm.libs.structs.WeightStruct;
import java.math.BigInteger;
import java.util.Map;
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
