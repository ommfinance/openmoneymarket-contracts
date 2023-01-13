package finance.omm.score.lp;

import finance.omm.libs.address.Contracts;
import finance.omm.utils.exceptions.OMMException;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

public class LPInventoryImpl extends AbstractLPInventory {

    public LPInventoryImpl(Address addressProvider) {
        super(addressProvider);
    }


    @External(readonly = true)
    public String name() {
        return "Omm " + TAG;
    }

    @External
    public void setAdmin(Address newAdmin) {
        onlyAdmin(TAG + " | Only current admin can set new admin");
        candidate.set(newAdmin);
        AdminCandidatePushed(Context.getCaller(),newAdmin);
    }

    @External(readonly = true)
    public Address getAdmin() {
        return this.admin.get();
    }

    @External(readonly = true)
    public Address getCandidate() {
        return this.candidate.get();
    }

    @External
    public void claimAdminRole() {
        Address candidate = this.candidate.get();
        if (candidate == null) {
            throw OMMException.unknown(TAG + " | Candidate address is null");
        }
        if (!candidate.equals(Context.getCaller())) {
            throw OMMException.unknown(TAG + " | The candidate's address and the caller do not match.");
        }
        this.candidate.set(null);
        Address oldAdmin = this.admin.get();
        this.admin.set(candidate);
        AdminRoleClaimed(oldAdmin, candidate);
    }


    @External
    public void stake(BigInteger poolId, BigInteger value) {
        onlyAdmin(TAG + " | Only admin can stake LP tokens");
        Address stakedLp = getAddress(Contracts.STAKED_LP.getKey());

        byte[] data = "{\"method\":\"stake\"}".getBytes();
        call(Contracts.DEX, "transfer", stakedLp, value, poolId, data);

    }

    @External
    public void transfer(Address to, BigInteger value, BigInteger poolId, @Optional byte[] _data) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(TAG + " | SenderNotGovernanceError: sender is not equals to governance"));

        BigInteger availableBalance = call(BigInteger.class, Contracts.DEX, "balanceOf", Context.getAddress(), poolId);

        if (value.compareTo(availableBalance) > 0) {
            BigInteger requiredBalance = value.subtract(availableBalance);
            call(Contracts.STAKED_LP, "unstake", poolId.intValue(), requiredBalance);
        }

        call(Contracts.DEX, "transfer", to, value, poolId, _data);
    }

    @External(readonly = true)
    public Map<String, BigInteger> balanceOf(Address owner, BigInteger poolId) {
        return call(Map.class, Contracts.STAKED_LP, "balanceOf", owner, poolId);
    }


    @External
    public void unstake(BigInteger poolId, BigInteger value) {
        onlyAdmin(TAG + " | Only admin can unstake LP token");
        call(Contracts.STAKED_LP, "unstake", poolId.intValue(), value);
    }

}
