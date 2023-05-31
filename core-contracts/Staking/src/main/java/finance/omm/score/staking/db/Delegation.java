package finance.omm.score.staking.db;

import foundation.icon.score.data.ScoreDataObject;
import score.Address;

import java.math.BigInteger;

@ScoreDataObject
public class Delegation {

    private Address address;
    private BigInteger delegationValue;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public BigInteger getDelegationValue() {
        return delegationValue;
    }

    public void setDelegationValue(BigInteger delegationValue) {
        this.delegationValue = delegationValue;
    }

    @Override
    public String toString() {
        return "Delegation{" + "address='" + address + '\'' +
                "delegationValue='" + delegationValue + '\'' +
                '}';
    }
}
