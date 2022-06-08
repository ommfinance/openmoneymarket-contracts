package finance.omm.score.test.unit.OMMToken;

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.utils.constants.TimeConstants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class OMMTokenTest extends AbstractOMMTokenTest {

    BigInteger FIVE_MINUTES = BigInteger.valueOf(5L).multiply(SIXTY).multiply(BigInteger.valueOf(1_000_000L));
    Account notOwner = sm.createAccount(10);
    Account REWARDS_TOKEN_ACCOUNT = MOCK_CONTRACT_ADDRESS.get(Contracts.REWARDS);
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
        Executable locklistAddresses = () -> score.call("get_locklist_addresses", 0, 0);
        expectErrorMessage(locklistAddresses, "Locklist :: start index cannot be greater than end index");

        Executable locklistAddressesOver100 = () -> score.call("get_locklist_addresses", 0, 110);
        expectErrorMessage(locklistAddressesOver100, "Locklist :: range cannot be greater than 100");

        assertEquals(score.call("get_locklist_addresses", 0, 10), new ArrayList<>());

        // add address to stakers list
//        Address user1 = sm.createAccount().getAddress();
//        Address user2 = sm.createAccount().getAddress();
//        Address user3 = sm.createAccount().getAddress();
//        Address[] stakerList = new Address[]{user1, user2, user3};
//        Object[] params = new Object[]{stakerList};

//        Executable addStakers = () -> score.invoke(notOwner, "addStakers",params);
//        expectErrorMessage(addStakers, "require owner access");
//
//        Executable removeStakers = () -> score.invoke(notOwner, "removeStakers", params);
//        expectErrorMessage(removeStakers, "require owner access");
//
//        score.invoke(owner, "addStakers", params);

        // inStakerList check
//        assertEquals(score.call("inStakerList",user1), true);
//        assertEquals(score.call("inStakerList",user2), true);
//        assertEquals(score.call("inStakerList",user3), true);

        // 0,100 range, has 3 in list
//        List<Address> actualStakers = (List<Address>) score.call("getStakersList",0,100);
//        assertEquals(actualStakers.get(0), stakerList[0]);
//        assertEquals(actualStakers.get(1), stakerList[1]);
//        assertEquals(actualStakers.get(2), stakerList[2]);
//
//        // 0,2 range has 3 in list
//        actualStakers = (List<Address>) score.call("getStakersList",0,2);
//        assertEquals(actualStakers.get(0), stakerList[0]);
//        assertEquals(actualStakers.get(1), stakerList[1]);
//        assertEquals(actualStakers.get(2), stakerList[2]);
//
//        // 0,1 range has 3 in list
//        actualStakers = (List<Address>) score.call("getStakersList",0,1);
//        assertEquals(actualStakers.get(0), stakerList[0]);
//        assertEquals(actualStakers.get(1), stakerList[1]);
    }

    public void initialize() {
        score.invoke(owner, "setUnstakingPeriod", FIVE_MINUTES);
        score.invoke(owner, "setMinimumStake", ICX);
    }
    @Test
    public void transfer(){
        Account bob = sm.createAccount(100);
        Account alice = sm.createAccount(100);

        BigInteger value = BigInteger.valueOf(50);
        BigInteger amountToMint = BigInteger.valueOf(100);
        BigInteger senderAvailableBalance = BigInteger.valueOf(10);
        byte[] data = "transfer".getBytes();

         //add bob to lockList
        score.invoke(owner, "addToLockList", bob.getAddress());
        Executable transferSenderLock = () -> score.invoke(bob,"transfer",alice.getAddress(),value,data);
        expectErrorMessage(transferSenderLock, "Cannot transfer, the sender " +
                bob.getAddress() + " is locked");

        //remove bob from lockList and add Alice
        score.invoke(owner, "removeFromLockList",bob.getAddress() );
        score.invoke(owner, "addToLockList", alice.getAddress());
        Executable transferReceiverLock = () -> score.invoke(bob,"transfer",
                alice.getAddress(),value,data);
        expectErrorMessage(transferReceiverLock, "Cannot transfer, the receiver " +
                alice.getAddress() + " is locked");

        // remove alice from lockList
        score.invoke(owner,"removeFromLockList",alice.getAddress());
        Executable transferLessThanZero = () -> score.invoke(bob,"transfer",
                alice.getAddress(),BigInteger.valueOf(-1),data);
        expectErrorMessage(transferLessThanZero,"Transferring value cannot be less than 0.");

        Executable insufficientBalance = () -> score.invoke(bob,"transfer",alice.getAddress(),value,data);
        expectErrorMessage(insufficientBalance,"Insufficient balance");

        // mint token by Rewards Contracts
        score.invoke(REWARDS_TOKEN_ACCOUNT,"mint",amountToMint,data);

        doReturn( true).when(scoreSpy).call(eq(Boolean.class),eq(
                Contracts.LENDING_POOL),eq("isFeeSharingEnable"),any());

        // transfer token to bob's account
        score.invoke(REWARDS_TOKEN_ACCOUNT,"transfer",bob.getAddress(),value,data);

        doReturn(senderAvailableBalance).when(scoreSpy).available_balanceOf(REWARDS_TOKEN_ACCOUNT.getAddress());
        Executable senderAvialableBalance = () -> score.invoke(REWARDS_TOKEN_ACCOUNT,"transfer",
                bob.getAddress(),value,data);
        expectErrorMessage(senderAvialableBalance,"available balance of user " +
                senderAvailableBalance + "balance to transfer " + value);

        // transfer token from bob to alice
        BigInteger beforeBalanceBob = (BigInteger) score.call("balanceOf",bob.getAddress());
        BigInteger beforeBalanceAlice = (BigInteger) score.call("balanceOf",alice.getAddress());

        score.invoke(bob,"transfer",alice.getAddress(),BigInteger.valueOf(10),data);

        BigInteger afterBalanceBob = beforeBalanceBob.subtract(BigInteger.valueOf(10));
        BigInteger afterBalanceAlice = beforeBalanceAlice.add(BigInteger.valueOf(10));

        assertEquals(afterBalanceBob,score.call("balanceOf",bob.getAddress()));
        assertEquals(afterBalanceAlice,score.call("balanceOf",alice.getAddress()));

        verify(scoreSpy,times(3)).call(eq(Boolean.class),
                eq(Contracts.LENDING_POOL), eq("isFeeSharingEnable"), any());
        verify(scoreSpy).Transfer(bob.getAddress(),alice.getAddress(),BigInteger.valueOf(10),data);
        verify(scoreSpy).Transfer(REWARDS_TOKEN_ACCOUNT.getAddress(),bob.getAddress(),value,data);
        verify(scoreSpy).Transfer(ZERO_SCORE_ADDRESS, REWARDS_TOKEN_ACCOUNT.getAddress(),amountToMint,data);
    }

    @Test
    public void mint(){
        Account notRewardScore = sm.createAccount(100);
        BigInteger amountToMint = BigInteger.valueOf(1000);
        byte[] data = "mint".getBytes();
        Executable unauthorized = () -> score.invoke(notRewardScore,"mint", amountToMint,data);
        expectErrorMessage(unauthorized,"Only reward distribution contract can call mint method");

        Executable zeroValueError = () -> score.invoke(REWARDS_TOKEN_ACCOUNT,"mint", ZERO,data);
        expectErrorMessage(zeroValueError,"ZeroValueError: _amount: " + ZERO);

        BigInteger beforeTotalSupply = (BigInteger) score.call("totalSupply");

        score.invoke(REWARDS_TOKEN_ACCOUNT,"mint",amountToMint,data);

        BigInteger afterTotalSupply = beforeTotalSupply.add(BigInteger.valueOf(1000));
        assertEquals(afterTotalSupply,score.call("totalSupply"));

        // userBalance details
        Map<String,BigInteger >details = (Map<String, BigInteger>) score.
                call("details_balanceOf",REWARDS_TOKEN_ACCOUNT.getAddress());
        assertEquals(details.get("totalBalance"),afterTotalSupply);

        // minting again
        // totalSupply and userBalance should increase by 100.
        score.invoke(REWARDS_TOKEN_ACCOUNT,"mint",BigInteger.valueOf(100),data);

        BigInteger newAfterTotalSupply = afterTotalSupply.add(BigInteger.valueOf(100)) ;
        assertEquals(newAfterTotalSupply,score.call("totalSupply"));

        // userBalance details after minting again.
        Map<String,BigInteger >details1 = (Map<String, BigInteger>) score.call
                ("details_balanceOf",REWARDS_TOKEN_ACCOUNT.getAddress());
        assertEquals(details1.get("totalBalance"),newAfterTotalSupply);

        verify(scoreSpy).Transfer(ZERO_SCORE_ADDRESS, REWARDS_TOKEN_ACCOUNT.getAddress(),amountToMint,data);
        verify(scoreSpy).Transfer(ZERO_SCORE_ADDRESS, REWARDS_TOKEN_ACCOUNT.getAddress(),BigInteger.valueOf(100),data);
    }

    @Test
    public void stake() {
        Account LENDING_POOL = MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL);
        Executable notLendingPool = () -> score.invoke(notOwner, "stake", THOUSAND_ICX, notOwner.getAddress());
        expectErrorMessage(notLendingPool, "Only lending pool contract can call stake method");

        Executable notSupported = () -> score.invoke(LENDING_POOL, "stake", THOUSAND_ICX, notOwner.getAddress());
        expectErrorMessage(notSupported, "Staking of OMM token no longer supported.");
    }



    @Test
    public void stakedBalanceTransfer() {
        Account userWallet = sm.createAccount(10);
        Address user = userWallet.getAddress();
        byte[] data = new byte[0];

        mockStakedBalance(ZERO,ZERO,ZERO);
        score.invoke(REWARDS_TOKEN_ACCOUNT,"mint",THOUSAND_ICX, "data".getBytes());

        doReturn( true).when(scoreSpy).call(eq(Boolean.class),eq(
                Contracts.LENDING_POOL),eq("isFeeSharingEnable"),any());

        score.invoke(REWARDS_TOKEN_ACCOUNT,"transfer",user,THOUSAND_ICX, data);

        Map<?,?> value = (Map<?,?>) score.call("details_balanceOf", user);
        assertEquals(value.get("totalBalance"), THOUSAND_ICX);
        assertEquals(value.get("availableBalance"), THOUSAND_ICX);
        assertEquals(value.get("unstakingBalance"), ZERO);
        assertEquals(value.get("stakedBalance"), ZERO);
        assertEquals(value.get("unstakingTimeInMicro"), ZERO);

        mockStakedBalance(ICX, ZERO, ZERO);
        Address user2 = sm.createAccount(10).getAddress();
        Executable insufficient = () -> score.invoke(userWallet,"transfer", user2, THOUSAND_ICX, data);
        BigInteger senderAvailableBalance = THOUSAND_ICX.subtract(ICX);
        expectErrorMessage(insufficient, "available balance of user " +
                senderAvailableBalance + "balance to transfer " + THOUSAND_ICX);


        score.invoke(userWallet,"transfer", user2, senderAvailableBalance, data);

        Map<?,?> senderDetails = (Map<?,?>) score.call("details_balanceOf", user);
        System.out.println(senderDetails);
        assertEquals(senderDetails.get("totalBalance"), ICX);
        assertEquals(senderDetails.get("availableBalance"), ZERO);
        assertEquals(senderDetails.get("unstakingBalance"), ZERO);
        assertEquals(senderDetails.get("stakedBalance"), ICX);
        assertEquals(senderDetails.get("unstakingTimeInMicro"), ZERO);

        Map<?,?> recieverDetails = (Map<?,?>) score.call("details_balanceOf", user2);
        assertEquals(recieverDetails.get("totalBalance"), senderAvailableBalance);
        assertEquals(recieverDetails.get("availableBalance"), senderAvailableBalance.subtract(ICX));
        assertEquals(recieverDetails.get("unstakingBalance"), ZERO);
        assertEquals(recieverDetails.get("stakedBalance"), ICX);
        assertEquals(recieverDetails.get("unstakingTimeInMicro"), ZERO);

    }
}
