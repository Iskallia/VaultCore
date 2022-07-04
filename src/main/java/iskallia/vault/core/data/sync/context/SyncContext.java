package iskallia.vault.core.data.sync.context;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.key.VKeyRegistry;

public class SyncContext {

	private final VVersion version;
	private final VKeyRegistry registry;

	public SyncContext(VVersion version, VKeyRegistry registry) {
		this.version = version;
		this.registry = registry;
	}

	public VVersion getVersion() {
		return this.version;
	}

	public VKeyRegistry getRegistry() {
		return this.registry;
	}

}
