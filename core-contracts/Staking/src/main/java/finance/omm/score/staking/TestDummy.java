package finance.omm.score.staking;

import score.Address;
import score.Context;
import score.annotation.External;

import java.util.Map;

import static finance.omm.score.staking.utils.Constant.SYSTEM_SCORE_ADDRESS;

public class TestDummy {

    @External(readonly = true)
    public String name(){
        return "DUMMY";
    }

    @External(readonly = true)
    public void checkValidPrep(Address prepAddr){
        try {
           Context.call(SYSTEM_SCORE_ADDRESS, "getPRep", prepAddr);
        }catch (Exception e){
            Context.revert("not a valid prep");
        }
    }

    @External(readonly = true)
    public boolean checkValidPrep2(Address prepAddr){
        Map<String, Object> prepDict = (Map<String, Object>) Context.call(SYSTEM_SCORE_ADDRESS, "getPRep", prepAddr);
        if (!prepDict.isEmpty()){
            return true;
        }
        return false;
    }
}
