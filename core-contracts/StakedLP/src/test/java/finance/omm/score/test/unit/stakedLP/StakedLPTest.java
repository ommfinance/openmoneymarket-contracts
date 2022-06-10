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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;
import score.ArrayDB;
import scorex.util.ArrayList;

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

    private void _addPool(Account account,int id, Address poolAddress){
        score.invoke(account,"addPool",id,poolAddress);
    }
    @Test
    public void addPool(){
        int id = 1;
        Address pool = addresses[0];
        Executable unauthorized = () -> _addPool(notGovernanceScore,id,pool);
        String expectedErrorMessage = "Sender not score governance error: (sender) " +
                notGovernanceScore.getAddress() + " governance " + GOVERNANCE_TOKEN_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

//        score.invoke(GOVERNANCE_TOKEN_ACCOUNT,"addPool",id,pool);
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,pool);
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

    private void _stake(Account account, Address operator, Address from, int id,
                        BigInteger amount,byte[] data ){
        doReturn(Map.of(
                "quote_decimals", BigInteger.valueOf(2),
                "base_decimals",BigInteger.valueOf(3) // OMM/USDS -> base/quote
        )).when(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),
                any(BigInteger.class));

        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());
        score.invoke(account,"onIRC31Received", operator,from,id,amount,data);
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
        Executable unauthorized = () -> _stake(notDEXScore,operator.getAddress(),from.getAddress(),id,value,data);
        String expectedErrorMessage = "Sender not score dex error: (sender) " +
                notDEXScore.getAddress() + " dex " + DEX_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

        // invalidMethod Name
        String invalidName = "notStake";
        byte[] invalidData = createByteArray(invalidName);
        Executable invalidCall = () -> _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id,value,invalidData);
        expectedErrorMessage = "No valid method called :: ";
        expectErrorMessageIn(invalidCall,expectedErrorMessage);

        // invalid pool Id
        Executable invalidPoolId = () -> _stake(DEX_ACCOUNT,
                operator.getAddress(),from.getAddress(),id,value,data);
        expectedErrorMessage = "pool with id: " + id + " is not supported";
        expectErrorMessage(invalidPoolId,expectedErrorMessage);

        // invalid stake amount
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        BigInteger invalidAmount = BigInteger.valueOf(10).negate();
        Executable invalidStakeAmount = () -> _stake(DEX_ACCOUNT,
                operator.getAddress(),from.getAddress(),id,invalidAmount,data);
        expectedErrorMessage = "Cannot stake less than zero ,value to stake "  + invalidAmount ;
        expectErrorMessage(invalidStakeAmount,expectedErrorMessage);

        // stake amount less than minimum stake
        // minimum stake is set to 1
        setMinimumStake();
        Executable lessStakeAmount = () -> _stake(DEX_ACCOUNT,
                operator.getAddress(),from.getAddress(),id,ZERO,data);
        expectedErrorMessage= "Amount to stake: " +ZERO + " is smaller the minimum stake: 1";
        expectErrorMessage(lessStakeAmount,expectedErrorMessage);

        doReturn(Map.of(
                "quote_decimals", BigInteger.valueOf(2),
                "base_decimals",BigInteger.valueOf(3)
        )).when(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),
                any(BigInteger.class));

        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());

        _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id,value,data);

        verify(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));
        verify(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());

        // balanceOF
    }

    @Test
    public void getTotalStaked(){ // check
        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        int id = 1;
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id,value,data);

        TotalStaked totalStaked = (TotalStaked) score.call("getTotalStaked", 1);

        BigInteger addedDecimals = BigInteger.TWO.add(BigInteger.valueOf(3));
        BigInteger expectedDecimals = (addedDecimals).divide(BigInteger.valueOf(2));

        assertEquals(BigInteger.valueOf(10),totalStaked.totalStaked);
        assertEquals(expectedDecimals,totalStaked.decimals);
    }

    @Test
    public void getPoolBalanceByUser(){
        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
//        int id = 1;
//        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        int[] id = {1,2,3};
        BigInteger[] value = {BigInteger.valueOf(10),BigInteger.valueOf(20),ZERO};

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[0],addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id[0],value[0],data);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[1],addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id[1],value[1],data);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[2],addresses[0]);

        doReturn(BigInteger.valueOf(90)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(Integer.class));
        List<Map<String, BigInteger>> expected = (List<Map<String, BigInteger>>) score.call("getPoolBalanceByUser", owner.getAddress());

        System.out.println(expected.get(0).get("poolID"));

        for (int i = 0; i < 3; i++) {

            BigInteger totalStakeBalance = (BigInteger) score.call("totalStaked", id[i]);

            assertEquals(expected.get(i).get("poolID"),BigInteger.valueOf(id[i]));
            assertEquals(expected.get(i).get("userTotalBalance"),BigInteger.valueOf(90).add(value[i]));
            assertEquals(expected.get(i).get("userAvailableBalance"),BigInteger.valueOf(90));
            assertEquals(expected.get(i).get("totalStakedBalance"),totalStakeBalance);
            assertEquals(expected.get(i).get("userStakedBalance"),value[i]);

        }
    }

    @Test
    public void balanceOf(){
        int id = 1;
        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(Integer.class));
        Map<String,BigInteger> expected = (Map<String, BigInteger>) score.call("balanceOf",owner.getAddress(),id);
        BigInteger totalStakeBalance = (BigInteger) score.call("totalStaked", id);

        assertEquals(expected.get("poolID"),BigInteger.valueOf(id));
        assertEquals(expected.get("userTotalBalance"),BigInteger.valueOf(100).add(ZERO));
        assertEquals(expected.get("userAvailableBalance"),BigInteger.valueOf(100));
        assertEquals(expected.get("userStakedBalance"),ZERO);
        assertEquals(expected.get("totalStakedBalance"),totalStakeBalance);

        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);


        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id,value,data);

        doReturn(BigInteger.valueOf(90)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(Integer.class));

        expected = (Map<String, BigInteger>) score.call("balanceOf",owner.getAddress(),id);

//
        assertEquals(expected.get("poolID"),BigInteger.valueOf(id));
        assertEquals(expected.get("userTotalBalance"),BigInteger.valueOf(90).add(TEN));
        assertEquals(expected.get("userAvailableBalance"),BigInteger.valueOf(90));
        assertEquals(expected.get("userStakedBalance"),value);
        assertEquals(expected.get("totalStakedBalance"),totalStakeBalance.add(value));

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
        BigInteger highvalue = BigInteger.valueOf(11);

        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id,value,data);

        //false Id
        Executable notSupportedCall = () ->score.call("unstake",falseId,value);
        String expectedErrorMessage= "pool with id: " + falseId + "is not supported";
        expectErrorMessage(notSupportedCall,expectedErrorMessage);

        //Tried to unstake negative value
        BigInteger negativevalue = NEGATIVE;
        Executable lessthanZero = () ->score.call("unstake",id,negativevalue);
        expectedErrorMessage= "Cannot unstake less than zero value to stake" + negativevalue;
        expectErrorMessage(lessthanZero,expectedErrorMessage);

        //Tried to unstake more than staked value
        Executable moreThanStaked = () ->score.invoke(owner,"unstake",id,highvalue);
        expectedErrorMessage= "Cannot unstake,user dont have enough staked balance" +
                "amount to unstake " + highvalue +
                "staked balance of user:" + owner.getAddress()  + "is" + value;
        expectErrorMessage(moreThanStaked,expectedErrorMessage);

        BigInteger unstakedValue = BigInteger.valueOf(7);

        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());
        doNothing().when(scoreSpy).call(eq(Contracts.DEX),eq("transfer"),any(),any(),any(),any());

        score.invoke(owner,"unstake",id,unstakedValue);

        TotalStaked totalStaked = (TotalStaked) score.call("getTotalStaked", 1);

        BigInteger addedDecimals = BigInteger.TWO.add(BigInteger.valueOf(3));
        BigInteger expectedDecimals = (addedDecimals).divide(BigInteger.valueOf(2));

        assertEquals(BigInteger.valueOf(3),totalStaked.totalStaked);
        assertEquals(expectedDecimals,totalStaked.decimals);
    }

    @Test
    public void getLPStakedSupply(){
        int id = 1;
        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id,value,data);

        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(Integer.class));
        SupplyDetails supplyDetails = (SupplyDetails) score.call("getLPStakedSupply",id,owner.getAddress());

        BigInteger totalStakeBalance = (BigInteger) score.call("totalStaked", id);

        BigInteger addedDecimals = BigInteger.TWO.add(BigInteger.valueOf(3));
        BigInteger expectedDecimals = (addedDecimals).divide(BigInteger.valueOf(2));

        assertEquals(supplyDetails.decimals,expectedDecimals);
        assertEquals(supplyDetails.principalTotalSupply,value);
        assertEquals(supplyDetails.principalUserBalance,totalStakeBalance);

    }

}