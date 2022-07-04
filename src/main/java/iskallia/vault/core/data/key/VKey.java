package iskallia.vault.core.data.key;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.type.VType;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

public class VKey<T> {

	public String id;
	public List<Entry<T>> entries = new ArrayList<>();
	public BitSet supportedVersions = new BitSet(VVersion.values().length);

	private VKey() {

	}

	public static <T> Builder<T> create(String id, Class<T> type) {
		return new Builder<>(id);
	}

	public VType<T> get(VVersion version) {
		for(Entry<T> entry : this.entries) {
			if(entry.predicate.test(version)) {
				return entry.type;
			}
		}

		return null;
	}

	public boolean supports(VVersion version) {
		return this.supportedVersions.get(version.ordinal());
	}

	public static class Builder<T> {
		private final String id;
		private final List<Entry<T>> entries = new ArrayList<>();

		private Builder(String id) {
			this.id = id;
		}

		public Builder<T> with(Predicate<VVersion> predicate, VType<T> type) {
			this.entries.add(new Entry<>(predicate, type));
			return this;
		}

		public VKey<T> build(VKeyRegistry registry) {
			VKey<T> key = new VKey<>();
			key.id = this.id;
			key.entries = this.entries;

			for(VVersion version : VVersion.values()) {
				boolean supports = false;

				for(Entry<T> entry : this.entries) {
					if(entry.predicate.test(version)) {
						if(supports) {
							System.err.println(this.id + " is defined multiple times for " + version);
						}

						supports = true;
					}
				}

				key.supportedVersions.set(version.ordinal(), supports);
			}

			if(registry != null) {
				registry.register(key);
			}

			return key;
		}
	}

	private static class Entry<T> {
		public Predicate<VVersion> predicate;
		public VType<T> type;

		public Entry(Predicate<VVersion> predicate, VType<T> type) {
			this.predicate = predicate;
			this.type = type;
		}
	}

}
