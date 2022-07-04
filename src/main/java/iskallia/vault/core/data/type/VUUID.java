package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;

import java.util.UUID;

public class VUUID extends VType<UUID> {

    @Override
    public void writeValue(BitBuffer buffer, SyncContext context, UUID value) {
        buffer.writeLong(value.getMostSignificantBits());
        buffer.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public UUID readValue(BitBuffer buffer, SyncContext context) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

}
