package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;

public class VVoid extends VType<Void> {

	@Override
	public void writeValue(BitBuffer buffer, SyncContext context, Void value) {

	}

	@Override
	public Void readValue(BitBuffer buffer, SyncContext context) {
		return null;
	}

}
