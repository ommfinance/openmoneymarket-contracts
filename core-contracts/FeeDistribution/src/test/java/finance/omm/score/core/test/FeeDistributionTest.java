package finance.omm.score.core.test;

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
        BigInteger HUNDRED = BigInteger.valueOf(100);
        BigInteger weight1 = BigInteger.TEN.multiply(ICX).divide(HUNDRED);
        BigInteger weight2 = (BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN)).divide(HUNDRED);
        BigInteger weight3 = (BigInteger.valueOf(675).multiply(ICX).divide(BigInteger.TEN)).divide(HUNDRED);

        Address[] receiverAddr = new Address[]{testScore.getAddress(),testScore1.getAddress(),validator};
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


        BigInteger val = BigInteger.valueOf(10).multiply(ICX);
        assertEquals(val,score.call("getFeeDistributed",testScore.getAddress()));
        val = BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN);
        assertEquals(val,score.call("getFeeDistributed",testScore1.getAddress()));


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
        BigInteger calimAmountValidator2 = BigInteger.valueOf(6075).multiply(ICX).divide(BigInteger.valueOf(100));

        Address validator1_claim_address = sm.createAccount().getAddress();

        contextMock.when(mockCaller()).thenReturn(validator1.getAddress());
        score.invoke(validator1,"claimValidatorsRewards",validator1_claim_address);

        contextMock.when(mockCaller()).thenReturn(validator2.getAddress());
        score.invoke(validator2,"claimValidatorsRewards",validator2.getAddress());

        assertEquals(calimAmountValidator1,score.call("getFeeDistributed",validator1_claim_address));
        assertEquals(calimAmountValidator2,score.call("getFeeDistributed",validator2.getAddress()));


        verify(spyScore).FeeClaimed(validator1.getAddress(),validator1_claim_address,calimAmountValidator1);
        verify(spyScore).FeeClaimed(validator2.getAddress(),validator2.getAddress(),calimAmountValidator2);



    }

    @Test
    public void claimZeroReward(){
        Executable call = () -> score.invoke(validator2,"claimValidatorsRewards",validator2.getAddress());
        expectErrorMessage(call,"Fee Distribution :: Caller has no reward to claim");
    }
}
