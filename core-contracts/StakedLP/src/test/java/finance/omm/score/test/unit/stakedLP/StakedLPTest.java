package finance.omm.score.test.unit.stakedLP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.libs.structs.TotalStaked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

public class StakedLPTest extends AbstractStakedLPTest {

    Account GOVERNANCE_TOKEN_ACCOUNT = MOCK_CONTRACT_ADDRESS.get(Contracts.GOVERNANCE);
    Account DEX_ACCOUNT = MOCK_CONTRACT_ADDRESS.get(Contracts.DEX);

    Account notGovernanceScore = sm.createAccount(100);
    Account notDEXScore = sm.createAccount(100);
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

        score.invoke(owner,"setMinimumStake",ONE);
        assertEquals(ONE,score.call("getMinimumStake"));
    }
    @Test
    public void addPool(){
        int id = 1;
        Address pool = addresses[0];
        Executable unauthorized = () -> score.invoke(notGovernanceScore,"addPool",id,pool);
        String expectedErrorMessage = "Sender not score governance error: (sender) " +
                notGovernanceScore.getAddress() + " governance " + GOVERNANCE_TOKEN_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"addPool",id,pool);
        assertEquals(pool,score.call("getPoolById",id));

        // asserting HASH map
//        Map<String,Address> pools = new HashMap<>();
//        pools.put(String.valueOf(id),pool);
//        assertEquals(pools,score.call("getSupportedPools"));
//        System.out.println(score.call("getSupportedPools"));
    }

    @Test
    public void removePool(){
        int falseId = 1;

        int poolId = 5;
        int poolId2 = 6;

        Executable unauthorized = () -> score.invoke(notGovernanceScore,"removePool",falseId);
        String expectedErrorMessage = "Sender not score governance error: (sender) " +
                notGovernanceScore.getAddress() + " governance " + GOVERNANCE_TOKEN_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

        Executable noPoolId = () -> score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"removePool",falseId);
        expectedErrorMessage = "Staked Lp: " + falseId + " is not in address map";
        expectErrorMessage(noPoolId,expectedErrorMessage);

        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"addPool",poolId,addresses[0]);
        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"addPool",poolId2,addresses[1]);

        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"removePool",poolId);
        assertNull(score.call("getPoolById", poolId));
        assertEquals(addresses[1],score.call("getPoolById",poolId2));

        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"removePool",poolId2);
        assertNull(score.call("getPoolById", poolId2));
    }

    @Test
    public void stake(){
        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        int id = 1;
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";

        // not called by DEX
        byte[] data = createByteArray(methodName);
        Executable unauthorized = () -> score.invoke(notDEXScore,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,value,data);
        String expectedErrorMessage = "Sender not score dex error: (sender) " +
                notDEXScore.getAddress() + " dex " + DEX_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

        // invalidMethod Name
        String invalidName = "notStake";
        byte[] invalidData = createByteArray(invalidName);
        Executable invalidCall = () -> score.invoke(DEX_ACCOUNT,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,value,invalidData);
        expectedErrorMessage = "No valid method called :: ";
        expectErrorMessageIn(invalidCall,expectedErrorMessage);

        // invalid pool Id
        Executable invalidPoolId = () -> score.invoke(DEX_ACCOUNT,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,value,data);
        expectedErrorMessage = "pool with id: " + id + " is not supported";
        expectErrorMessage(invalidPoolId,expectedErrorMessage);

        addPool();
        // invalid stake amount
        BigInteger invalidAmount = BigInteger.valueOf(10).negate();
        Executable invalidStakeAmount = () -> score.invoke(DEX_ACCOUNT,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,invalidAmount,data);
        expectedErrorMessage = "Cannot stake less than zero ,value to stake "  + invalidAmount ;
        expectErrorMessage(invalidStakeAmount,expectedErrorMessage);

        // stake amount less than minimum stake
        // minimum stake is set to 1
        setMinimumStake();
        Executable lessStakeAmount = () -> score.invoke(DEX_ACCOUNT,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,ZERO,data);
        expectedErrorMessage= "Amount to stake: " +ZERO + " is smaller the minimum stake: 1";
        expectErrorMessage(lessStakeAmount,expectedErrorMessage);

        doReturn(Map.of(
                "quote_decimals", BigInteger.valueOf(2),
                "base_decimals",BigInteger.valueOf(3)
        )).when(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),
                any(BigInteger.class));

        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());

        score.invoke(DEX_ACCOUNT,"onIRC31Received",
                operator.getAddress(),from.getAddress(),id,value,data);

        verify(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));
        verify(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());

    }

    @Test
    public void getTotalStaked(){ // check
        stake();
        TotalStaked totalStaked = (TotalStaked) score.call("getTotalStaked", 1);

        BigInteger addedDecimals = BigInteger.TWO.add(BigInteger.valueOf(3));
        BigInteger expectedDecimals = (addedDecimals).divide(BigInteger.valueOf(2));

        assertEquals(BigInteger.valueOf(10),totalStaked.totalStaked);
        assertEquals(expectedDecimals,totalStaked.decimals);
    }

    @Test
    public void getPoolBalanceByUser(){
        stake();
        List<Map<String, BigInteger>> totalStaked = (List<Map<String, BigInteger>>) score.call("getPoolBalanceByUser", owner.getAddress());




    }

    @Test
    public void balanceOf(){
        stake();
        int  _id = 1;
        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(BigInteger.class,Contracts.DEX,"balanceOf",owner.getAddress(), _id);
        Map<String, BigInteger> balance = (Map<String, BigInteger>) score.call("balanceOf",owner.getAddress(), 1);
//        assertEquals(balance.get("poolID"),1);

    }

    @Test
    public void getBalanceByPool(){
        addPool();
        List result = (List) score.call("getBalanceByPool");



    }

    @Test
    public void getPoolById(){
        addPool();
        Address poolAd = (Address) score.call("getPoolById",1);
        Address expected = addresses[0];
        assertEquals(poolAd,expected);
    }

    @Test
    public void unstake(){
        int falseId = 6;
        int id = 1;
        BigInteger lessvalue = ONE;
        BigInteger value = TEN;
        BigInteger highvalue = BigInteger.valueOf(11);
        stake();

        Executable notSupportedCall = () ->score.call("unstake",falseId,value);
        String expectedErrorMessage= "pool with id: " + falseId + "is not supported";
        expectErrorMessage(notSupportedCall,expectedErrorMessage);

        BigInteger negativevalue = NEGATIVE;
        Executable lessthanZero = () ->score.call("unstake",id,negativevalue);
        expectedErrorMessage= "Cannot unstake less than zero value to stake" + negativevalue;
        expectErrorMessage(lessthanZero,expectedErrorMessage);


        Executable moreThanStaked = () ->score.invoke(owner,"unstake",id,highvalue);
        expectedErrorMessage= "Cannot unstake,user dont have enough staked balance" +
                "amount to unstake " + highvalue +
                "staked balance of user:" + owner.getAddress()  + "is" + ONE;
        expectErrorMessage(moreThanStaked,expectedErrorMessage);

        score.invoke(owner,"unstake",id,ONE);



    }

    @Test
    public void getLPStakedSupply(){
        stake();



    }

}