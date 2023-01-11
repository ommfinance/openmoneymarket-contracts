package finance.omm.score.lp;

import finance.omm.libs.address.Contracts;

import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.annotation.External;

import java.math.BigInteger;

public class LPStakerImpl extends AbstractLPStaker {
    public LPStakerImpl(Address addressProvider) {
        super(addressProvider);
    }


    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void stakeLP(BigInteger poolId, BigInteger value) {
        Address stakedLp = getAddress(Contracts.STAKED_LP.getKey());

        byte[] data = "{\"method\":\"stake\"}".getBytes();
        call(Contracts.DEX, "transfer", stakedLp, value, poolId, data);

    }

    @External
    public void transferLp(Address to, BigInteger value, BigInteger poolId,byte[] _data) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(
                        TAG + " | SenderNotGovernanceError: sender is not equals to governance"));
        call(Contracts.DEX, "transfer", to, value, poolId,_data);
    }

    @External(readonly = true)
    public BigInteger balanceOfLp(BigInteger poolId) {
        return call(BigInteger.class, Contracts.DEX, "balanceOf", Context.getAddress(), poolId);
    }

    @External(readonly = true)
    public void transferFunds(Address _to, BigInteger _value, byte[] _data) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(
                        TAG + " | SenderNotGovernanceError: sender is not equals to governance"));

        Address ommAddress = getAddress(Contracts.OMM_TOKEN.getKey());

        if (ommAddress == null) {
            Context.revert(TAG + "| omm address was not set");
        }
        call(ommAddress, "transfer", _to, _value, _data);
    }

    @External
    public void unstakeLP(BigInteger poolId, BigInteger value) {
        onlyAdmin();
        call(Contracts.STAKED_LP, "unstake", poolId.intValue(), value);
    }

    @External
    public void claimRewards() {
        call(Contracts.LENDING_POOL, "claimRewards", Context.getAddress());
    }
}
