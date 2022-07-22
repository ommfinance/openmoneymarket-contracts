package finance.omm.score.test.unit.stakedLP;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.SupplyDetails;
import finance.omm.libs.structs.TotalStaked;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
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

        score.invoke(owner,"setMinimumStake",BigInteger.valueOf(2));
        assertEquals(BigInteger.valueOf(2),score.call("getMinimumStake"));
    }

    @Test
    public void getPoolById(){
        int id =1;
        Address poolAddress = addresses[0];
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,poolAddress);
        Address poolAd = (Address) score.call("getPoolById",id);

        assertEquals(poolAd,poolAddress);
    }

    @Test
    public void addPool(){
        int id = 1;
        Address pool = addresses[0];
        Executable unauthorized = () -> _addPool(notGovernanceScore,id,pool);
        String expectedErrorMessage = "Sender not score governance error: (sender) " +
                notGovernanceScore.getAddress() + " governance " + GOVERNANCE_TOKEN_ACCOUNT.getAddress();
        expectErrorMessage(unauthorized,expectedErrorMessage);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,pool);
        assertEquals(pool,score.call("getPoolById",id));

        Map<String,Address> expected = (Map<String, Address>) score.call("getSupportedPools");
        assertEquals(expected.get(String.valueOf(id)),pool);

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
    public void getSupportedPools(){
        int id = 1;
        Address poolAddress = addresses[0];
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,poolAddress);
        Map<String,Address> expected = (Map<String, Address>) score.call("getSupportedPools");
        assertEquals(expected.get(String.valueOf(id)),poolAddress);

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
        setMinimumStake();
        Executable lessStakeAmount = () -> _stake(DEX_ACCOUNT,
                operator.getAddress(),from.getAddress(),id,ONE,data);
        expectedErrorMessage= "Amount to stake: " +ONE + " is smaller the minimum stake: 2";
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

    }

    @Test
    public void getTotalStaked(){
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

        verify(scoreSpy).call(eq(Contracts.REWARDS), eq("handleLPAction"),any(),any());

    }

    @Test
    public void getBalanceByPool(){

        Account from = sm.createAccount(100);
        Account operator = sm.createAccount(100);
        BigInteger[] id = {BigInteger.ONE,BigInteger.TWO,BigInteger.valueOf(3)};

        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class),eq(Contracts.DEX), eq("balanceOf"),any(),eq(id[0]));
        doReturn(BigInteger.valueOf(50)).when(scoreSpy).call(eq(BigInteger.class),eq(Contracts.DEX), eq("balanceOf"),any(),eq(id[1]));
        doReturn(BigInteger.valueOf(25)).when(scoreSpy).call(eq(BigInteger.class),eq(Contracts.DEX), eq("balanceOf"),any(),eq(id[2]));

        BigInteger[] amount = {BigInteger.valueOf(100),BigInteger.valueOf(50),BigInteger.valueOf(25)};

        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[0].intValue(),addresses[0]);
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[1].intValue(),addresses[1]);
        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id[2].intValue(),addresses[2]);

        _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id[0].intValue(),amount[0],data);
        _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id[1].intValue(),amount[1],data);
        _stake(DEX_ACCOUNT,operator.getAddress(),from.getAddress(),id[2].intValue(),amount[2],data);

        List<Map<String,BigInteger>> expected = (List<Map<String, BigInteger>>) score.call("getBalanceByPool");
        System.out.println(expected);

        for (int i = 0; i < 3; i++) {
            assertEquals(expected.get(i).get("poolID"),id[i]);
            assertEquals(expected.get(i).get("totalStakedBalance"),amount[i]);
        }

        verify(scoreSpy,times(3)).call(eq(Contracts.REWARDS), eq("handleLPAction"),any(),any());

        verify(scoreSpy,times(3)).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));
    }


    @Test
    public void getPoolBalanceByUser(){
        Account operator = sm.createAccount(100);
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
                eq("balanceOf"),any(Address.class),any(BigInteger.class));
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
        verify(scoreSpy,times(3)).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(),any());
        verify(scoreSpy,times(2)).call(eq(Contracts.REWARDS), eq("handleLPAction"),any(),any());
        verify(scoreSpy,times(2)).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));

    }

    @Test
    public void balanceOf(){
        int id = 1;
        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(BigInteger.class));
        Map<String,BigInteger> expected = (Map<String, BigInteger>) score.call("balanceOf",owner.getAddress(),id);
        BigInteger totalStakeBalance = (BigInteger) score.call("totalStaked", id);

        assertEquals(expected.get("poolID"),BigInteger.valueOf(id));
        assertEquals(expected.get("userTotalBalance"),BigInteger.valueOf(100).add(ZERO));
        assertEquals(expected.get("userAvailableBalance"),BigInteger.valueOf(100));
        assertEquals(expected.get("userStakedBalance"),ZERO);
        assertEquals(expected.get("totalStakedBalance"),totalStakeBalance);

        Account operator = sm.createAccount(100);
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);


        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id,value,data);

        doReturn(BigInteger.valueOf(90)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(BigInteger.class));

        expected = (Map<String, BigInteger>) score.call("balanceOf",owner.getAddress(),id);

        assertEquals(expected.get("poolID"),BigInteger.valueOf(id));
        assertEquals(expected.get("userTotalBalance"),BigInteger.valueOf(90).add(TEN));
        assertEquals(expected.get("userAvailableBalance"),BigInteger.valueOf(90));
        assertEquals(expected.get("userStakedBalance"),value);
        assertEquals(expected.get("totalStakedBalance"),totalStakeBalance.add(value));

        verify(scoreSpy,times(2)).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(),any());
        verify(scoreSpy).call(eq(Contracts.REWARDS), eq("handleLPAction"),any(),any());
        verify(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));


    }

    @Test
    public void unstake(){
        int falseId = 6;
        int id = 1;
        BigInteger highvalue = BigInteger.valueOf(11);

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

        //unstake negative value
        BigInteger negativevalue = NEGATIVE;
        Executable lessthanZero = () ->score.call("unstake",id,negativevalue);
        expectedErrorMessage= "Cannot unstake less than zero value to stake" + negativevalue;
        expectErrorMessage(lessthanZero,expectedErrorMessage);

        //unstake more than staked value
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
        verify(scoreSpy,times(2)).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());
        verify(scoreSpy).call(eq(Contracts.DEX),eq("transfer"),any(),any(),any(),any());

    }

    @Test
    public void getLPStakedSupply(){
        int id = 1;
        Account operator = sm.createAccount(100);
        BigInteger value = BigInteger.valueOf(10);
        String methodName = "stake";
        byte[] data = createByteArray(methodName);

        _addPool(GOVERNANCE_TOKEN_ACCOUNT,id,addresses[0]);
        _stake(DEX_ACCOUNT,operator.getAddress(),owner.getAddress(),id,value,data);

        doReturn(BigInteger.valueOf(100)).when(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(Address.class),any(BigInteger.class));
        SupplyDetails supplyDetails = (SupplyDetails) score.call("getLPStakedSupply",id,owner.getAddress());

        BigInteger totalStakeBalance = (BigInteger) score.call("totalStaked", id);

        BigInteger addedDecimals = BigInteger.TWO.add(BigInteger.valueOf(3));
        BigInteger expectedDecimals = (addedDecimals).divide(BigInteger.valueOf(2));

        assertEquals(supplyDetails.decimals,expectedDecimals);
        assertEquals(supplyDetails.principalTotalSupply,totalStakeBalance);
        assertEquals(supplyDetails.principalUserBalance,value);

        verify(scoreSpy).call(eq(BigInteger.class), eq(Contracts.DEX),
                eq("balanceOf"),any(),any());
        verify(scoreSpy).call(eq(Contracts.REWARDS), eq("handleLPAction"),any(),any());
        verify(scoreSpy,times(2)).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),any(BigInteger.class));

    }

    private void _addPool(Account account,int id, Address poolAddress){
        score.invoke(account,"addPool",id,poolAddress);
    }

    private void _stake(Account account, Address operator, Address from, int id,
                        BigInteger amount,byte[] data ){
        doReturn(Map.of(
                "quote_decimals", BigInteger.valueOf(2),
                "base_decimals",BigInteger.valueOf(3)
        )).when(scoreSpy).call(eq(Map.class),eq(Contracts.DEX),eq("getPoolStats"),
                any(BigInteger.class));

        doNothing().when(scoreSpy).call(eq(Contracts.REWARDS),eq("handleLPAction"),any(),any());
        score.invoke(account,"onIRC31Received", operator,from,BigInteger.valueOf(id),amount,data);
    }


}