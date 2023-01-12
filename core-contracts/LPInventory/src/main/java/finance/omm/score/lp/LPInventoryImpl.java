package finance.omm.score.lp;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;

import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.Map;

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
        onlyOwner("Only owner can set new admin");
        Address oldAdmin = getAdmin();
        this.admin.set(newAdmin);
        AdminChanged(oldAdmin, newAdmin);
    }

    @External(readonly = true)
    public Address getAdmin() {
        return this.admin.get();
    }

    @External
    public void stake(BigInteger poolId, BigInteger value) {
        onlyOwner("Only owner can stake LP tokens");
        Address stakedLp = getAddress(Contracts.STAKED_LP.getKey());

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("method", "stake");

        byte[] data = jsonObject.toString().getBytes();
        call(Contracts.DEX, "transfer", stakedLp, value, poolId, data);

    }

    @External
    public void transfer(Address to, BigInteger value, BigInteger poolId, @Optional byte[] _data) {
        onlyOrElseThrow(Contracts.GOVERNANCE,
                OMMException.unknown(
                        TAG + " | SenderNotGovernanceError: sender is not equals to governance"));
        BigInteger availableBalance = call(BigInteger.class, Contracts.DEX, "balanceOf",
                Context.getAddress(),poolId);
        if (value.compareTo(availableBalance) > 0) {
            BigInteger requiredBalance = value.subtract(availableBalance);
            call(Contracts.STAKED_LP, "unstake", poolId.intValue(), requiredBalance);
        }

        call(Contracts.DEX, "transfer", to, value, poolId, _data);
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
