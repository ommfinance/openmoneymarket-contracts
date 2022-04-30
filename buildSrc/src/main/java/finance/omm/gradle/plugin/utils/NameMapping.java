package finance.omm.gradle.plugin.utils;

public enum NameMapping {
    rewards("RewardDistribution"),
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
