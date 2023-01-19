package finance.omm.libs.address;

import finance.omm.core.score.interfaces.AddressProvider;
import finance.omm.utils.exceptions.OMMException;
import score.Address;
import score.Context;

public interface Authorization<T extends OMMException> extends AddressProvider {

    default void onlyOwnerOrElseThrow(T exception) {
        if (!Context.getOwner().equals(Context.getCaller())) {
            throw exception;
        }
    }

    default void onlyContractOrElseThrow(Contracts contract, T exception) {
        if (!Context.getCaller().equals(getAddress(contract.getKey()))) {
            throw exception;
        }
    }

    default void onlyOrElseThrow(Address address, T exception) {
        if (address == null || !address.equals(Context.getCaller())) {
            throw exception;
        }
    }
}
