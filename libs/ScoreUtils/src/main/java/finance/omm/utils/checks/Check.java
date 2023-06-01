package finance.omm.utils.checks;

import score.Address;
import score.Context;
import score.VarDB;

public class Check {
    public static void onlyOwner() {
        Address caller = Context.getCaller();
        Address owner = Context.getOwner();
        Context.require(caller.equals(owner), "SenderNotScoreOwner: Sender=" + caller + "Owner=" + owner);
    }

    public static void only(VarDB<Address> authorizedCaller) {
        only(authorizedCaller.get());
    }

    public static void only(Address authorizedCallerAddress) {
        Address caller = Context.getCaller();
        Context.require(authorizedCallerAddress != null, "Authorization Check: Address not set");
        Context.require(caller.equals(authorizedCallerAddress),
                "Authorization Check: Authorization failed. Caller: " + caller + " Authorized Caller: " + authorizedCallerAddress);
    }

}
