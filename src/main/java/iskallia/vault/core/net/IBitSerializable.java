package iskallia.vault.core.net;

import iskallia.vault.core.data.sync.context.SyncContext;

public interface IBitSerializable<T> {

	T write(BitBuffer buffer, SyncContext context);

	T read(BitBuffer buffer, SyncContext context);

}
