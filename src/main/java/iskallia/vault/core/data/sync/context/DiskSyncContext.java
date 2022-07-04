package iskallia.vault.core.data.sync.context;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.key.VKeyRegistry;

public class DiskSyncContext extends SyncContext {

	public DiskSyncContext(VVersion version, VKeyRegistry registry) {
		super(version, registry);
	}

}
