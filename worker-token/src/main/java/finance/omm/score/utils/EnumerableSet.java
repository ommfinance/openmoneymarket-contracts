package finance.omm.score.utils;

import score.ArrayDB;
import score.Context;
import score.DictDB;

public class EnumerableSet<V> {
	private final ArrayDB<V> entries;
	private final DictDB<V, Integer> indexes;

	public EnumerableSet(String id, Class<V> valueClass) {
		// array of valueClass
		this.entries = Context.newArrayDB(id+"_es_entries", valueClass);
		// value => array index
		this.indexes = Context.newDictDB(id+"_es_indexes", Integer.class);
	}

	public int length() {
		return entries.size();
	}

	public V at(int index) {
		return entries.get(index);
	}

	public boolean contains(V value) {
		return indexes.get(value) != null;
	}

	public Integer indexOf(V value) {
		// returns null if value doesn't exist
		Integer result = indexes.get(value);
		if (result != null) {
			return result - 1;
		}
		return null;
	}

	public void add(V value) {
		if (!contains(value)) {
			// add new value
			entries.add(value);
			indexes.set(value, entries.size());
		}
	}

	public void remove(V value) {
		var valueIndex = indexes.get(value);
		if (valueIndex != null) {
			// pop and swap with the last entry
			int lastIndex = entries.size();
			V lastValue = entries.pop();
			indexes.set(value, null);
			if (lastIndex != valueIndex) {
				entries.set(valueIndex - 1, lastValue);
				indexes.set(lastValue, valueIndex);
			}
		}
	}

	public ArrayDB<V> getEntries(){
		return this.entries;
	}
}
