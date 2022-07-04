package iskallia.vault.core;

import java.util.function.Predicate;

public enum VVersion {
	v1_0,
	v1_1;

	public static VVersion latest() {
		return values()[0];
	}

	public static VVersion oldest() {
		return values()[values().length - 1];
	}

	public boolean isNewerThan(VVersion v) {
		return this.compareTo(v) < 0;
	}

	public boolean isNewerOrEqualTo(VVersion v) {
		return this.compareTo(v) <= 0;
	}

	public boolean isOlderThan(VVersion v) {
		return this.compareTo(v) > 0;
	}

	public boolean isOlderOrEqualTo(VVersion v) {
		return this.compareTo(v) >= 0;
	}

	public boolean isEqualTo(VVersion v) {
		return this.compareTo(v) == 0;
	}

	public static Predicate<VVersion> newerThan(VVersion version) {
		return v -> v.isNewerThan(version);
	}

	public static Predicate<VVersion> newerOrEqualTo(VVersion version) {
		return v -> v.isNewerOrEqualTo(version);
	}

	public static Predicate<VVersion> olderThan(VVersion version) {
		return v -> v.isOlderThan(version);
	}

	public static Predicate<VVersion> olderOrEqualTo(VVersion version) {
		return v -> v.isOlderOrEqualTo(version);
	}

	public static Predicate<VVersion> equalTo(VVersion version) {
		return v -> v.isEqualTo(version);
	}

}
