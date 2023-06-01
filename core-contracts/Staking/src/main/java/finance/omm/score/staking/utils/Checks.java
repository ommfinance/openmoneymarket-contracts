package finance.omm.score.staking.utils;

import finance.omm.score.staking.StakingImpl;
import score.Address;
import score.Context;

public class Checks {

    public static Address defaultAddress = new Address(new byte[Address.LENGTH]);

    public static void onlyOwner() {
        Address caller = Context.getCaller();
        Address owner = Context.getOwner();
        Context.require(caller.equals(owner), "SenderNotScoreOwner: Sender=" + caller + "Owner=" + owner);
    }

    public static void stakingOn() {
        if (!StakingImpl.stakingOn.get()) {
            Context.revert(Constant.TAG + ": ICX Staking SCORE is not active.");
        }
    }
}
