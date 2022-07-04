package iskallia.vault.core.data.sync.context;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.key.VKeyRegistry;

public class ClientSyncContext extends SyncContext {

	public ClientSyncContext(VVersion version, VKeyRegistry registry) {
		super(version, registry);
	}

}
