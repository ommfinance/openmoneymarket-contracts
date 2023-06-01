package finance.omm.score.staking.utils;

import score.Address;

import java.math.BigInteger;

public class UnstakeDetails {
    public BigInteger nodeId;
    public BigInteger unstakeAmount;
    public Address key;
    public BigInteger unstakeBlockHeight;
    public Address receiverAddress;

    public UnstakeDetails(BigInteger nodeId, BigInteger unstakeAmount, Address key, BigInteger unstakeBlockHeight,
                          Address receiverAddress) {
        this.nodeId = nodeId;
        this.unstakeAmount = unstakeAmount;
        this.key = key;
        this.unstakeBlockHeight = unstakeBlockHeight;
        this.receiverAddress = receiverAddress;
    }
}
