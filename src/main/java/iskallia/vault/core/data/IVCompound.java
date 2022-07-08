package iskallia.vault.core.data;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitPacket;
import iskallia.vault.core.net.IBitSerializable;

public interface IVCompound<D> extends IBitSerializable<D> {

	boolean isDirty(SyncContext context);

	void collectSync(BitPacket packet, SyncContext context);

	void applySync(BitPacket packet, SyncContext context);

	void resetSync();

	boolean isDirtyTree(SyncContext context);

	void collectSyncTree(BitPacket packet, SyncContext context);

	void applySyncTree(BitPacket packet, SyncContext context);

	void resetSyncTree();

}
