package finance.omm.score.core.test;

import finance.omm.libs.address.Contracts;
import org.junit.jupiter.api.Test;
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
        assertEquals("Omm Fee Distribution",score.call("name"));
    }

    @Test
    public void setFeeDistribution(){
        Address[] receiverAddr = new Address[]{testScore.getAddress(),testScore1.getAddress(),
                testScore2.getAddress(),testScore3.getAddress()};
        BigInteger val = (ICX.multiply(BigInteger.valueOf(100))).divide(BigInteger.valueOf(4));

        BigInteger[] weight = new BigInteger[]{val,val,val,val};
        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        assertEquals(val,score.call("getFeeDistributionOf",testScore.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore1.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore2.getAddress()));
        assertEquals(val,score.call("getFeeDistributionOf",testScore3.getAddress()));
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

        BigInteger weight1 = BigInteger.TEN.multiply(ICX);
        BigInteger weight2 = BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN);
        BigInteger weight3 = BigInteger.valueOf(675).multiply(ICX).divide(BigInteger.TEN);

        Address[] receiverAddr = new Address[]{testScore.getAddress(),testScore1.getAddress(),validator};
        BigInteger[] weight = new BigInteger[]{weight1,weight2,weight3};

        doNothing().when(spyScore).call(any(Contracts.class),eq("transfer"),any(),any());

        doReturn(Map.of(
                validator1.getAddress().toString(),BigInteger.valueOf(10).multiply(ICX),
                validator2.getAddress().toString(),BigInteger.valueOf(90).multiply(ICX)
        )).when(spyScore).call(eq(Map.class),any(),eq("getActualUserDelegationPercentage"),any());


        score.invoke(owner,"setFeeDistribution",receiverAddr,weight);

        score.invoke(owner,"tokenFallback",owner.getAddress(),feeAmount,"b".getBytes());


        BigInteger val = BigInteger.valueOf(10).multiply(ICX);
        assertEquals(val,score.call("getFeeDistributed",testScore.getAddress()));
        val = BigInteger.valueOf(225).multiply(ICX).divide(BigInteger.TEN);
        assertEquals(val,score.call("getFeeDistributed",testScore1.getAddress()));


        verify(spyScore).FeeDistributed(testScore.getAddress(),BigInteger.valueOf(10).multiply(ICX));
        verify(spyScore).FeeDistributed(testScore1.getAddress(),val);



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
}
