package finance.omm.score.staking.utils;

import finance.omm.score.staking.StakingImpl;
import score.Address;
import score.Context;
import score.VarDB;

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


    public static void checkStatus(VarDB<Address> address) {
        Address handler = address.get();
        if (handler == null) {
            return;
        }

        checkStatus(handler);
    }

    public static void checkStatus(Address handler) {
        String caller = Context.getCaller().toString();
        Context.call(handler, "checkStatus", caller);
    }
}
