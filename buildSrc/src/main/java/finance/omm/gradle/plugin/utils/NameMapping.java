package finance.omm.gradle.plugin.utils;

public enum NameMapping {
    rewardDistribution("RewardDistribution"),
    rewardWeightController("RewardWeightController"),
    bOMM("VotingEscrowToken");

    private final String module;

    NameMapping(String module) {
        this.module = module;
    }

    public String toString() {
        return this.module;
    }
}
