package finance.omm.score.core.test;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class FeeDistributionTest extends AbstractFeeDistributionTest {

    @Test
    public void name(){
        assertEquals("OMM Fee Distribution",score.call("name"));
    }

    @Test
    public void setFeeDistribution(){
        Address[] receiverAddr = new Address[]{testScore.getAddress(),testScore1.getAddress(),
                testScore2.getAddress(),testScore3.getAddress()};
        BigInteger val = (ICX.divide(BigInteger.valueOf(4)));

        BigInteger[] weight = new BigInteger[]{val,val,val,val};
        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        assertEquals(val,score.call("getFeeDistributionOf",testScore.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore1.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore2.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore3.getAddress()));
    }

    @Test
    public void tokenFallBackFailCase(){
        BigInteger feeAmount = BigInteger.valueOf(100).multiply(ICX);
        Executable call = () -> score.invoke(owner,"tokenFallback",owner.getAddress(),feeAmount,"b".getBytes());
        expectErrorMessage(call,"Token Fallback: Only sicx contract is allowed to call");

    }

    @Test
    public void setFeeDistributionFailCase(){
        Executable call = () -> score.invoke(testScore,"setFeeDistribution",new Address[]{},new BigInteger[]{});
        expectErrorMessage(call,"require owner access");

        Address[] receiverAddr = new Address[]{testScore.getAddress(),testScore1.getAddress()};
        BigInteger val = (ICX.divide(BigInteger.valueOf(4)));

        BigInteger[] weight = new BigInteger[]{val,val,val,val};
        call = () -> score.invoke(owner,"setFeeDistribution",receiverAddr,weight);
        expectErrorMessage(call,"Fee Distribution :: Invalid pair length of arrays");

        Address[] addresses = new Address[]{testScore.getAddress(),testScore1.getAddress()};
        val = (ICX.divide(BigInteger.valueOf(2)));
        BigInteger[] percentages = new BigInteger[]{val,val.divide(BigInteger.TEN)};
        call = () -> score.invoke(owner,"setFeeDistribution",addresses,percentages);
        expectErrorMessage(call,"Fee Distribution :: Sum of percentages not equal to 100 550000000000000000");

        Address[] duplicateAddress = new Address[]{testScore.getAddress(),testScore.getAddress()};
        BigInteger[] weight2 = new BigInteger[]{val,val};
        call = () -> score.invoke(owner,"setFeeDistribution",duplicateAddress,weight2);
        expectErrorMessage(call,"Fee Distribution :: Array has duplicate addresses");

    }

    @Test
    public void tokenFallback(){
        BigInteger feeAmount = BigInteger.valueOf(100).multiply(ICX);

//        100 ICX -> 10% to test1,
        // 22.5% -> test2,
        // 67.5% -> validator

        // test 1 -> 10 ICX
        // test 2 -> 22.5 ICX
        // test 3 -> 67.5 ICX (validator)

        Address validator = MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress();
        Address daoFund = MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress();

        BigInteger weight1 = BigInteger.TEN.multiply(ICX).divide(HUNDRED);
        BigInteger weight2 = (BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN)).divide(HUNDRED);
        BigInteger weight3 = (BigInteger.valueOf(675).multiply(ICX).divide(BigInteger.TEN)).divide(HUNDRED);

        Address[] receiverAddr = new Address[]{testScore.getAddress(),validator,daoFund};
        BigInteger[] weight = new BigInteger[]{weight1,weight3,weight2};

        doNothing().when(spyScore).call(any(Contracts.class),eq("transfer"),any(),any());

        doReturn(Map.of(
                validator1.getAddress().toString(),BigInteger.valueOf(10).multiply(ICX),
                validator2.getAddress().toString(),BigInteger.valueOf(90).multiply(ICX)
        )).when(spyScore).call(eq(Map.class),any(Contracts.class),eq("getActualUserDelegationPercentage"),any());



        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        Address sicx = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        contextMock.when(mockCaller()).thenReturn(sicx);
        score.invoke(owner,"tokenFallback",owner.getAddress(),feeAmount,"b".getBytes());

        verify(spyScore).FeeDistributed(BigInteger.valueOf(100).multiply(ICX));
    }

    @Test
    public void claimRewards(){
        // from tokenFallback test method
        // validator = 67.5 ICX
        // validator1 = 10% of 67.5 = 6.75 ICX
        // validator 2 = 90% of 67.5 = 60.75 ICX
        tokenFallback();

        doReturn(Map.of()).when(spyScore).call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", validator1.getAddress());
        doReturn(Map.of("jailFlags",BigInteger.ZERO)).when(spyScore).call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", validator2.getAddress());

        BigInteger calimAmountValidator1 = BigInteger.valueOf(675).multiply(ICX).divide(BigInteger.valueOf(100));

        Address validator1_claim_address = sm.createAccount().getAddress();

        contextMock.when(mockCaller()).thenReturn(validator1.getAddress());
        // fee will be disbursed here
        score.invoke(validator1,"claimRewards",validator1_claim_address);

        assertEquals(BigInteger.valueOf(6075).multiply(ICX).divide(HUNDRED),
                score.call("getAccumulatedFee",validator2.getAddress()));


        Address daoFund = MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress();
        BigInteger val = BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN);
        assertEquals(val,score.call("getCollectedFee",daoFund));


        assertEquals(calimAmountValidator1,score.call("getCollectedFee",validator1_claim_address));


        verify(spyScore).FeeDisbursed(BigInteger.valueOf(100).multiply(ICX));
        verify(spyScore).FeeClaimed(validator1.getAddress(),validator1_claim_address,calimAmountValidator1);

    }

    @Test
    void claimRewards_without_fee_disburse(){
        claimRewards();
        BigInteger calimSicx = BigInteger.valueOf(10).multiply(ICX);
        Account sicx_calim_address = testScore;

        BigInteger calimAmountValidator2 = BigInteger.valueOf(6075).multiply(ICX).divide(BigInteger.valueOf(100));


        contextMock.when(mockCaller()).thenReturn(validator2.getAddress());
        score.invoke(validator2,"claimRewards",validator2.getAddress());

        contextMock.when(mockCaller()).thenReturn(sicx_calim_address.getAddress());
        score.invoke(sicx_calim_address,"claimRewards",sicx_calim_address.getAddress());

        assertEquals(calimAmountValidator2,score.call("getCollectedFee",validator2.getAddress()));
        assertEquals(calimSicx,score.call("getCollectedFee",sicx_calim_address.getAddress()));

        verify(spyScore).FeeClaimed(validator2.getAddress(),validator2.getAddress(),calimAmountValidator2);
        verify(spyScore).FeeClaimed(sicx_calim_address.getAddress(),sicx_calim_address.getAddress(),calimSicx);

    }

    @Test
    void distributeFeeToValidator_withJailedPreps(){
        tokenFallback();
        // validator = 67.5 ICX
        // validator1 = 30% of 67.5 = 20.25 ICX
        // validator 2 = 50% of 67.5 = 33.75 ICX
        // validator 3 = 20% of 67.5 = 13.5 ICX


        doReturn(Map.of()).when(spyScore).call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", validator1.getAddress());
        doReturn(Map.of("jailFlags",BigInteger.ZERO)).when(spyScore).call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", validator2.getAddress());
        doReturn(Map.of("jailFlags",BigInteger.TEN)).when(spyScore).call(Map.class,ZERO_SCORE_ADDRESS, "getPRep", validator3.getAddress());


        // only 2 validators of omm are in valid Preps of staking
        doReturn(Map.of(
                validator1.getAddress().toString(),BigInteger.valueOf(30).multiply(ICX),
                validator2.getAddress().toString(),BigInteger.valueOf(50).multiply(ICX),
                validator3.getAddress().toString(),BigInteger.valueOf(20).multiply(ICX)
        )).when(spyScore).call(eq(Map.class),any(Contracts.class),eq("getActualUserDelegationPercentage"),any());

        contextMock.when(mockCaller()).thenReturn(validator1.getAddress());

        // fee will be disbursed here
        score.invoke(validator1,"claimRewards",validator1.getAddress());

        assertEquals(BigInteger.valueOf(3375).multiply(ICX).divide(HUNDRED),
                score.call("getAccumulatedFee",validator2.getAddress()));
        assertEquals(BigInteger.ZERO,
                score.call("getAccumulatedFee",validator3.getAddress()));


        Address daoFund = MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress();
        BigInteger val = BigInteger.valueOf(36).multiply(ICX);
        assertEquals(val,score.call("getCollectedFee",daoFund));


        assertEquals(BigInteger.valueOf(2025).multiply(ICX).divide(HUNDRED),
                score.call("getCollectedFee",validator1.getAddress()));

        verify(spyScore).FeeDisbursed(BigInteger.valueOf(100).multiply(ICX));
        verify(spyScore).FeeClaimed(validator1.getAddress(),validator1.getAddress(),
                BigInteger.valueOf(2025).multiply(ICX).divide(HUNDRED));



    }

    @Test
    public void claimZeroReward(){
        Executable call = () -> score.invoke(validator2,"claimRewards",validator2.getAddress());
        expectErrorMessage(call,"Fee Distribution :: Caller has no reward to claim");
    }
}
