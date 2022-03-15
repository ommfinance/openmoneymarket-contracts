package finance.omm.commons;

import java.util.Map;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;

public abstract class BaseScore {

	public abstract String getTag();
	public abstract Address getAddress(String _name);
	public abstract Address getAddressProvider();

	public void onlyOwner() {
		Address sender = Context.getCaller();
		Address owner = Context.getOwner();
		if (!sender.equals(owner)){
			Context.revert(getTag() + ": SenderNotScoreOwnerError:  (sender)"+ sender + " (owner)"+owner);
		}
	}

	public void onlyAddressProvider() {
		Address addressProvider = getAddressProvider();
		Address sender = Context.getCaller();
		if (!sender.equals(addressProvider)){
			Context.revert(
					getTag() + ": SenderNotAddressProviderError:  (sender)"+ sender + " (address provider)"+addressProvider);
		}
	}

	public void onlyGovernance() {
		Address sender = Context.getCaller();
		Address governance = getAddress("governance");
		if (!sender.equals(governance)) {
			Context.revert(
					getTag() + ": SenderNotGovernanceError: (sender)"+sender+" (governance)"+governance);
		}
	}

	public <T> Boolean containsInArrayDb(T value, ArrayDB<T> arraydb) {
		boolean found = false;
		if(arraydb == null || value == null) {
			return found;
		}

		for(int i = 0; i< arraydb.size(); i++) {
			if(arraydb.get(i) != null
					&& arraydb.get(i).equals(value)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public <T> T getFromArrayDb(T value, ArrayDB<T> arraydb) {
		if(arraydb == null || value == null) {
			return null;
		}

		for(int i = 0; i< arraydb.size(); i++) {
			T item = arraydb.get(i);
			if(item != null
					&& item.equals(value)) {
				return item;
			}
		}
		return null;
	}

	public <K,V> Map<K,V> arrayAndDictDbToMap(ArrayDB<K> keys, DictDB<K, V> dictDb){
		int size = keys.size();

		@SuppressWarnings("unchecked")
		Map.Entry<K,V>[] addresses = new Map.Entry[size];

		for (int i= 0; i< size; i++ ) {
			K item = keys.get(i);
			V address = dictDb.get(item);
			addresses[i] = Map.entry(item, address);
		}
		return Map.<K,V>ofEntries(addresses);
	}
}
