package iskallia.vault.core.data.sync.handler;

import iskallia.vault.core.data.sync.context.ClientSyncContext;
import iskallia.vault.core.data.sync.context.SyncContext;

import java.util.UUID;

public class PlayerSyncHandler implements SyncHandler {

	private final UUID playerId;

	public PlayerSyncHandler(UUID playerId) {
		this.playerId = playerId;
	}

	@Override
	public boolean canSync(SyncContext context) {
		if(context instanceof ClientSyncContext) {
			return true;
		}

		return false;
	}

	public static class Factory {
		public PlayerSyncHandler of(UUID playerId) {
			return new PlayerSyncHandler(playerId);
		}

		//TODO: add player entity
	}

}
