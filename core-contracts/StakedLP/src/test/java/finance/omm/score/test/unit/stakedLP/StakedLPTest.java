package finance.omm.score.test.unit.stakedLP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.AssetConfig;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class StakedLPTest extends AbstractStakedLPTest {
    @Test
    public void name() {
        String actual = (String) score.call("name");
        String expected = "OMM Staked Lp";
        assertEquals(expected, actual);
    }

    @Test
    public void setMinimumStake(){
        Account notOwner = sm.createAccount();
        Executable notOwnerCall = () -> score.invoke(notOwner,"setMinimumStake",THOUSAND);
        String expectedErrorMessage = "require owner access";
        expectErrorMessage(notOwnerCall, expectedErrorMessage);

        BigInteger _value = THOUSAND.negate();
        Executable negativeStake = () -> score.invoke(owner,"setMinimumStake",_value);
        expectedErrorMessage = "Minimum stake value must be positive, " + _value;
        expectErrorMessage(negativeStake, expectedErrorMessage);

        score.invoke(owner,"setMinimumStake",THOUSAND);
        assertEquals(THOUSAND,score.call("getMinimumStake"));
    }

    @Test
    public void Stake(){

    }

}
