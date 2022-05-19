package finance.omm.score.test.unit.OMMToken;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.iconloop.score.test.Account;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class OMMTokenTest extends AbstractOMMTokenTest {

    BigInteger FIVE_MINUTES = BigInteger.valueOf(5L).multiply(SIXTY).multiply(BigInteger.valueOf(1_000_000L));
    Account notOwner = sm.createAccount(10);

    @Test
    public void tokenBasicInfo() {
        String actual = (String) score.call("name");
        String expected = "Omm Token";
        assertEquals(expected, actual);

        actual = (String) score.call("symbol");
        expected = "OMM";
        assertEquals(expected, actual);

        BigInteger actualDecimals = (BigInteger) score.call("decimals");
        BigInteger expectedDecimals = BigInteger.valueOf(18L);
        assertEquals(actualDecimals, expectedDecimals);

        BigInteger actualTotal = (BigInteger) score.call("totalSupply");
        BigInteger expectedTotal = BigInteger.ZERO;
        assertEquals(actualTotal, expectedTotal);

        BigInteger actualBalance = (BigInteger) score.call("balanceOf", owner.getAddress());
        BigInteger expectedBalance = BigInteger.ZERO;
        assertEquals(actualBalance, expectedBalance);

        Map<String, BigInteger> actualDetailBalance = (Map<String, BigInteger>) score.call
                ("details_balanceOf", owner.getAddress());

        Map<String, BigInteger> expectedDetailBalance = Map.of(
                "totalBalance", ZERO,
                "availableBalance", ZERO,
                "stakedBalance", ZERO,
                "unstakingBalance", ZERO,
                "unstakingTimeInMicro", ZERO
        );
        assertEquals(actualDetailBalance, expectedDetailBalance);
    }

    @Test
    public void setterGetter() {
        // owner
        BigInteger currentTime = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        score.invoke(owner, "setUnstakingPeriod", currentTime);
        BigInteger actual = (BigInteger) score.call("getUnstakingPeriod");
        assertEquals(actual, currentTime.multiply(TimeConstants.SECOND));

        BigInteger expectedMin = THOUSAND;
        score.invoke(owner, "setMinimumStake", expectedMin);
        BigInteger actualMin = (BigInteger) score.call("getMinimumStake");
        assertEquals(actualMin, expectedMin);

        // not owner
        Executable unstakingPeriod = () -> score.invoke(notOwner, "setUnstakingPeriod", currentTime);
        expectErrorMessage(unstakingPeriod, "require owner access");

        Executable minimumStake = () -> score.invoke(notOwner, "setMinimumStake", currentTime);
        expectErrorMessage(minimumStake, "require owner access");
    }

    @Test
    public void addRemoveFromLocklist() {
        Executable addToLocklist = () -> score.invoke(notOwner, "addToLockList", owner.getAddress());
        expectErrorMessage(addToLocklist, "require owner access");

        Executable removeFromLocklist = () -> score.invoke(notOwner, "removeFromLockList", owner.getAddress());
        expectErrorMessage(removeFromLocklist, "require owner access");

        // no address in locklist yet
        Executable locklistAddresses = () ->  score.call("get_locklist_addresses", 0, 0);
        expectErrorMessage(locklistAddresses, "Locklist :: start index cannot be greater than end index");

        Executable locklistAddressesOver100 = () ->  score.call("get_locklist_addresses", 0, 110);
        expectErrorMessage(locklistAddressesOver100, "Locklist :: range cannot be greater than 100");

        assertEquals(score.call("get_locklist_addresses", 0,10), new ArrayList<>());

        // add address to stakers list
        Address user1 = sm.createAccount().getAddress();
        Address user2 = sm.createAccount().getAddress();
        Address user3 = sm.createAccount().getAddress();
        Address[] stakerList = new Address[]{user1, user2, user3};
        Object[] params = new Object[]{stakerList};

        Executable addStakers = () -> score.invoke(notOwner, "addStakers",params);
        expectErrorMessage(addStakers, "require owner access");

        Executable removeStakers = () -> score.invoke(notOwner, "removeStakers", params);
        expectErrorMessage(removeStakers, "require owner access");

        score.invoke(owner, "addStakers", params);

        // inStakerList check
        assertEquals(score.call("inStakerList",user1), true);
        assertEquals(score.call("inStakerList",user2), true);
        assertEquals(score.call("inStakerList",user3), true);

        // 0,100 range, has 3 in list
        List<Address> actualStakers = (List<Address>) score.call("getStakersList",0,100);
        assertEquals(actualStakers.get(0), stakerList[0]);
        assertEquals(actualStakers.get(1), stakerList[1]);
        assertEquals(actualStakers.get(2), stakerList[2]);

        // 0,2 range has 3 in list
        actualStakers = (List<Address>) score.call("getStakersList",0,2);
        assertEquals(actualStakers.get(0), stakerList[0]);
        assertEquals(actualStakers.get(1), stakerList[1]);
        assertEquals(actualStakers.get(2), stakerList[2]);

        // 0,1 range has 3 in list
        actualStakers = (List<Address>) score.call("getStakersList",0,1);
        assertEquals(actualStakers.get(0), stakerList[0]);
        assertEquals(actualStakers.get(1), stakerList[1]);
    }

    public void initialize() {
        score.invoke(owner, "setUnstakingPeriod", FIVE_MINUTES);
        score.invoke(owner, "setMinimumStake", ICX);
    }
}
