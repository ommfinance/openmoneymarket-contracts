package finance.omm.score.lp;

import finance.omm.libs.address.Contracts;

import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

public class LPStakerImpl extends AbstractLPStaker {
    public LPStakerImpl(Address addressProvider) {
        super(addressProvider);
    }


    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void stake(BigInteger poolId, BigInteger value) {
        onlyOwner("Only owner can stake LP tokens");
        Address stakedLp = getAddress(Contracts.STAKED_LP.getKey());

        byte[] data = "{\"method\":\"stake\"}".getBytes();
        call(Contracts.DEX, "transfer", stakedLp, value, poolId, data);

    }

    @External
    public void transfer(Address to, BigInteger value, BigInteger poolId, @Optional byte[] _data) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(
                        TAG + " | SenderNotGovernanceError: sender is not equals to governance"));
        call(Contracts.DEX, "transfer", to, value, poolId,_data);
    }

    @External(readonly = true)
    public Map<String, BigInteger> balanceOfLp(Address owner, BigInteger poolId) {

        return call(Map.class, Contracts.STAKED_LP, "balanceOf", owner, poolId);
    }


    @External
    public void unstake(BigInteger poolId, BigInteger value) {
        onlyOwner("Only owner can unstake LP token");
        call(Contracts.STAKED_LP, "unstake", poolId.intValue(), value);
    }

}
