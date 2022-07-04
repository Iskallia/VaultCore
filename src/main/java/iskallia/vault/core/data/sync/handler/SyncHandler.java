package iskallia.vault.core.data.sync.handler;

import iskallia.vault.core.data.sync.context.SyncContext;

@FunctionalInterface
public interface SyncHandler {

	boolean canSync(SyncContext context);

}
