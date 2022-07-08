package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;

import java.util.UUID;

public class VUUID extends VType<UUID> {

    private final boolean nullable;

    public VUUID(boolean nullable) {
        this.nullable = nullable;
    }

    public VUUID asNullable() {
        return this.nullable ? this : new VUUID(true);
    }

    public boolean isNullable() {
        return this.nullable;
    }

    @Override
    public UUID validate(UUID value) {
        if(!this.nullable && value == null) {
            throw new UnsupportedOperationException("Value cannot be null");
        }

        return value;
    }

    @Override
    public void writeValue(BitBuffer buffer, SyncContext context, UUID value) {
        if(this.nullable) {
            buffer.writeBoolean(value == null);
        }

        if(value != null) {
            buffer.writeLong(value.getMostSignificantBits());
            buffer.writeLong(value.getLeastSignificantBits());
        }
    }

    @Override
    public UUID readValue(BitBuffer buffer, SyncContext context) {
        if(this.nullable && buffer.readBoolean()) {
            return null;
        }

        return new UUID(buffer.readLong(), buffer.readLong());
    }

}
