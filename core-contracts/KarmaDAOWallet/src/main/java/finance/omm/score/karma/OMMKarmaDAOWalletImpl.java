package finance.omm.score.karma;

import finance.omm.libs.address.Contracts;
import finance.omm.score.karma.exception.KarmaDAOWalletException;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.Context;
import score.annotation.External;

public class OMMKarmaDAOWalletImpl extends AbstractKarmaDAOWallet {

    public OMMKarmaDAOWalletImpl(Address addressProvider) {
        super(addressProvider);
    }


    @External(readonly = true)
    public String name() {
        return "OMM Karma wallet";
    }

    @External(readonly = true)
    public Map<String, Address> getPoolDetails(BigInteger poolId) {
        Address treasuryContract = treasuryContracts.get(poolId);
        if (treasuryContract == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        Address bondContract = bondContracts.get(poolId);
        return Map.of("treasuryContract", treasuryContract, "bondContract", bondContract);
    }

    @External
    public void setAdmin(Address newAdmin) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        candidate.set(newAdmin);
        AdminCandidatePushed(newAdmin);
    }

    @External(readonly = true)
    public Address getAdmin() {
        return this.admin.get();
    }

    @External(readonly = true)
    public Address getCandidate() {
        return this.candidate.get();
    }

    @External(readonly = true)
    public Map<String, BigInteger> balanceOf(BigInteger poolId) {
        Address treasury = treasuryContracts.get(poolId);
        return Map.of("wallet", call(BigInteger.class, Contracts.OMM_TOKEN, "balanceOf", Context.getAddress()),
                "treasury", call(BigInteger.class, Contracts.OMM_TOKEN, "balanceOf", treasury));
    }


    @External(readonly = true)
    public Map<String, BigInteger> lpBalanceOf(BigInteger poolId) {
        Address treasury = treasuryContracts.get(poolId);
        return Map.of("wallet", call(BigInteger.class, Contracts.DEX, "balanceOf", Context.getAddress(), poolId),
                "treasury", call(BigInteger.class, Contracts.DEX, "balanceOf", treasury, poolId));
    }

    @External(readonly = true)
    public boolean bondContract(BigInteger poolId) {
        Address treasury = treasuryContracts.get(poolId);

        KarmaTreasury t = new KarmaTreasuryClient(treasury);
        Address bondContract = bondContracts.get(poolId);
        return t.bondContract(bondContract);
    }

    @External
    public void claimAdminStatus() {
        Address candidate = this.candidate.get();
        if (candidate == null) {
            throw KarmaDAOWalletException.unknown("candidate address is null");
        }
        if (!candidate.equals(Context.getCaller())) {
            throw KarmaDAOWalletException.unknown("The candidate's address and the caller do not match.");
        }
        this.candidate.set(null);
        this.admin.set(candidate);
        AdminStatusClaimed(candidate);
    }

    @External
    public void addBondContract(BigInteger poolId, Address bondContract, Address treasuryContract) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        bondContracts.set(poolId, bondContract);
        treasuryContracts.set(poolId, treasuryContract);
    }


    @External
    public void pushManagement(BigInteger poolId, Address newOwner) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        KarmaTreasury t = new KarmaTreasuryClient(treasury);
        t.pushManagement(newOwner);
//        this.OwnershipPushed(newOwner);
    }


    @External
    public void pullManagement(BigInteger poolId) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        KarmaTreasury t = new KarmaTreasuryClient(treasury);
        t.pullManagement();
//        this.OwnershipPulled();
    }

    @External
    public void transferOMMToTreasury(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        byte[] data = "{\"method\":\"funding\"}".getBytes();

        call(Contracts.OMM_TOKEN, "transfer", treasury, value, data);
        this.FundTransferred(poolId, value);
    }


    @External
    public void withdrawOMMFromTreasury(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        KarmaTreasury t = new KarmaTreasuryClient(treasury);

        t.withdraw(getAddress(Contracts.OMM_TOKEN.getKey()), Context.getAddress(), value);

        this.FundWithdrawn(poolId, value);
    }


    @External
    public void withdrawLPFromTreasury(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        KarmaTreasury t = new KarmaTreasuryClient(treasury);

        t.withdrawLp(getAddress(Contracts.DEX.getKey()), Context.getAddress(), value, poolId);

        this.LPWithdrawn(poolId, value);
    }

    @External
    public void toggleBondContract(BigInteger poolId) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address treasury = treasuryContracts.get(poolId);
        if (treasury == null) {
            throw KarmaDAOWalletException.unknown("invalid treasury address");
        }
        KarmaTreasury t = new KarmaTreasuryClient(treasury);
        t.toggleBondContract(bondContracts.get(poolId));
    }

    @External
    public void removeLP(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        call(Contracts.DEX, "remove", poolId, value, true);
        this.LPTokenRemoved(poolId, value);
    }

    @External
    public void transferToken(Address token, Address to, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        call(token, "transfer", to, value);
        this.TokenTransferred(token, to, value);
    }

    @External
    public void stakeLP(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        Address stakedLp = getAddress(Contracts.STAKED_LP.getKey());
        byte[] data = "{\"method\":\"stake\"}".getBytes();
        call(Contracts.DEX, "transfer", stakedLp, value, poolId, data);
    }

    @External
    public void unstakeLP(BigInteger poolId, BigInteger value) {
        onlyOrElseThrow(admin.get(), KarmaDAOWalletException.notAdmin());
        call(Contracts.STAKED_LP, "unstake", poolId.intValue(), value);
    }

}
