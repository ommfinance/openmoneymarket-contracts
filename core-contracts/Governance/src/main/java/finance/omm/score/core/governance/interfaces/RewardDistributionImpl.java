package finance.omm.score.core.governance.interfaces;

import finance.omm.core.score.interfaces.RewardDistribution;
import finance.omm.libs.structs.AssetConfig;
import foundation.icon.score.client.ScoreInterface;

@ScoreInterface(suffix = "Client")
public interface RewardDistributionImpl extends RewardDistribution {

    void configureAssetConfig(AssetConfig _assetConfig);
}
