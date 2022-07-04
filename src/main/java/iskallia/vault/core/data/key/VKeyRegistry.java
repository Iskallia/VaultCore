package iskallia.vault.core.data.key;

import iskallia.vault.core.VVersion;

import java.util.*;

public class VKeyRegistry {

	public List<VKey<?>> keys = new ArrayList<>();

	public Map<VVersion, List<VKey<?>>> keyCache = new HashMap<>();
	public Map<VVersion, Map<VKey<?>, Integer>> indexCache = new HashMap<>();

	//TODO: maybe lock the registry at some point
	public void register(VKey<?> key) {
		this.keys.add(key);
		this.keys.sort(Comparator.comparing(k -> k.id));
	}

	public int getIndex(VKey<?> key, VVersion version) {
		this.ensureCacheIsPresent(version);
		return this.indexCache.get(version).get(key);
	}

	public VKey<?> getKey(int index, VVersion version) {
		this.ensureCacheIsPresent(version);
		return this.keyCache.get(version).get(index);
	}

	public int getSize(VVersion version) {
		this.ensureCacheIsPresent(version);
		return this.keyCache.get(version).size();
	}

	private void ensureCacheIsPresent(VVersion version) {
		if(this.keyCache.containsKey(version)) return;

		List<VKey<?>> keys = new ArrayList<>();
		Map<VKey<?>, Integer> indices = new IdentityHashMap<>();

		for(int i = 0; i < this.keys.size(); i++) {
			VKey<?> key = this.keys.get(i);
			if(key.supports(version)) {
				keys.add(key);
				indices.put(key, i);
			}
		}

		this.keyCache.put(version, keys);
		this.indexCache.put(version, indices);
	}

}
