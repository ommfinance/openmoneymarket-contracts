package finance.omm.score.core.delegation.db;

import static finance.omm.score.core.delegation.DelegationImpl.TAG;
import static finance.omm.utils.constants.AddressConstant.ZERO_SCORE_ADDRESS;

import finance.omm.score.core.delegation.exception.DelegationException;
import finance.omm.utils.db.EnumerableSet;
import java.math.BigInteger;
import java.util.Map;
import score.Address;
import score.Context;

public class PREPEnumerableSet extends EnumerableSet<Address> {

    public PREPEnumerableSet(String varKey, Class<Address> valueClass) {
        super(varKey, valueClass);
    }

    public boolean contains(Address value) {
        boolean isExists = super.contains(value);
        if (!isExists) {
            validatePrep(value);
        }
        return isExists;
    }


    private void validatePrep(Address _address) {
        Map<String, ?> prepDetails = Context.call(Map.class, ZERO_SCORE_ADDRESS, "getPRep", _address);
        boolean isActive = prepDetails.get("status").equals(BigInteger.ZERO);
        if (!isActive) {
            throw DelegationException.unknown(TAG + ": Invalid prep: " + _address);
        }
    }
}
