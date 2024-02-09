package finance.omm.score.core.test;

import com.iconloop.score.test.Account;
import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import score.Address;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

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

        Address[] receiverAddr = new Address[]{testScore.getAddress(),daoFund,validator};
        BigInteger[] weight = new BigInteger[]{weight1,weight2,weight3};

        doNothing().when(spyScore).call(any(Contracts.class),eq("transfer"),any(),any());

        doReturn(Map.of(
                validator1.getAddress().toString(),BigInteger.valueOf(10).multiply(ICX),
                validator2.getAddress().toString(),BigInteger.valueOf(90).multiply(ICX)
        )).when(spyScore).call(eq(Map.class),any(),eq("getActualUserDelegationPercentage"),any());



        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        Address sicx = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        contextMock.when(mockCaller()).thenReturn(sicx);
        System.out.println(sicx);
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
        // user has accumulated rewards
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
    void distributeFee_withClaimRewards(){
        tokenFallback();

        Account claimAddress = testScore;

        assertEquals(BigInteger.ZERO,score.call("getCollectedFee",claimAddress.getAddress()));
        assertEquals(BigInteger.ZERO,score.call("getAccumulatedFee",claimAddress.getAddress()));

        assertEquals(BigInteger.ZERO,score.call("getCollectedFee",validator2.getAddress()));
        assertEquals(BigInteger.ZERO,score.call("getAccumulatedFee",validator2.getAddress()));

        score.invoke(validator2,"claimRewards",validator2.getAddress());

        assertEquals(BigInteger.ZERO,score.call("getCollectedFee",claimAddress.getAddress()));
        assertEquals(BigInteger.valueOf(10).multiply(ICX),score.call("getAccumulatedFee",claimAddress.getAddress()));

        verify(spyScore).FeeDisbursed(BigInteger.valueOf(100).multiply(ICX));
        verify(spyScore,never()).FeeClaimed(validator2.getAddress(),validator2.getAddress(),BigInteger.ZERO);
    }

    @Test
    void check_claimableFee(){
        BigInteger feeAmount = BigInteger.valueOf(1100).multiply(ICX);

//        1100 ICX ->
        // 40% -> validator
//        10% to sICX wallet,
        // 25% -> daoFund,
        // 25% -> test wallet,

        // validator -> 440 ICX
        // sicx wallet -> 110 ICX
        // test wallet -> 275 ICX
        // daoFund -> 275 ICX

        Address validator = MOCK_CONTRACT_ADDRESS.get(Contracts.LENDING_POOL_CORE).getAddress();
        Address sicxWallet = testScore.getAddress();
        Address test = testScore1.getAddress();
        Address daoFund = MOCK_CONTRACT_ADDRESS.get(Contracts.DAO_FUND).getAddress();

        BigInteger weight1 = (BigInteger.valueOf(40).multiply(ICX)).divide(HUNDRED);
        BigInteger weight2 = BigInteger.TEN.multiply(ICX).divide(HUNDRED);
        BigInteger weight3 = (BigInteger.valueOf(25).multiply(ICX)).divide(HUNDRED);
        BigInteger weight4 = (BigInteger.valueOf(25).multiply(ICX)).divide(HUNDRED);

        Address[] receiverAddr = new Address[]{validator,sicxWallet,test,daoFund};
        BigInteger[] weight = new BigInteger[]{weight1,weight2,weight3,weight4};
        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        doNothing().when(spyScore).call(any(Contracts.class),eq("transfer"),any(),any());

        doReturn(Map.of(
                validator1.getAddress().toString(),BigInteger.valueOf(10).multiply(ICX),
                validator2.getAddress().toString(),BigInteger.valueOf(90).multiply(ICX)
        )).when(spyScore).call(eq(Map.class),any(),eq("getActualUserDelegationPercentage"),any());




        Address sicx = MOCK_CONTRACT_ADDRESS.get(Contracts.sICX).getAddress();
        contextMock.when(mockCaller()).thenReturn(sicx);
        score.invoke(owner,"tokenFallback",owner.getAddress(),feeAmount,"b".getBytes());

        verify(spyScore).FeeDistributed(BigInteger.valueOf(1100).multiply(ICX));

        assertEquals(BigInteger.valueOf(44).multiply(ICX),score.call("getClaimableFee",validator1.getAddress()));
        assertEquals(BigInteger.valueOf(110).multiply(ICX),score.call("getClaimableFee",sicxWallet));
        assertEquals(BigInteger.valueOf(275).multiply(ICX),score.call("getClaimableFee",test));

    }

}
