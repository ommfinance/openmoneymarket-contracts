package finance.omm.score.staking.db;

import foundation.icon.score.data.ScoreDataObject;
import score.Address;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@ScoreDataObject
public class DelegationListDB {
    private List<Delegation> delegationList;

    public List<Delegation> getDelegationList() {
        return delegationList;
    }

    public void setDelegationList(List<Delegation> delegationList) {
        this.delegationList = delegationList;
    }

    @Override
    public String toString() {
        return "DelegationListDB{" + "delegationList=" + delegationList + "}";
    }

    public Map<String, BigInteger> toMap() {
        if (delegationList.size() == 0) {
            return Map.of();
        }

        Map<String, BigInteger> delegationMap = new HashMap<>();
        for (Delegation delegation : delegationList) {
            delegationMap.put(delegation.getAddress().toString(), delegation.getDelegationValue());
        }
        return delegationMap;
    }

    public static DelegationListDBSdo fromMap(Map<String, BigInteger> delegationMap) {
        List<Delegation> delegationList = new ArrayList<>();
        if (!delegationMap.isEmpty()) {
            for (Map.Entry<String, BigInteger> delegationEntry : delegationMap.entrySet()) {
                Delegation delegation = new Delegation();
                delegation.setAddress(Address.fromString(delegationEntry.getKey()));
                delegation.setDelegationValue(delegationEntry.getValue());
                delegationList.add(delegation);
            }
        }

        DelegationListDBSdo delegationListDB = new DelegationListDBSdo();
        delegationListDB.setDelegationList(delegationList);
        return delegationListDB;
    }
}
