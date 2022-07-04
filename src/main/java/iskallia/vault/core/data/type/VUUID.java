package iskallia.vault.core.data.type;

import iskallia.vault.core.net.BitBuffer;

import java.util.UUID;

public class VUUID extends VType<UUID> {

    @Override
    public void writeValue(BitBuffer buffer, UUID value) {
        buffer.writeLong(value.getMostSignificantBits());
        buffer.writeLong(value.getLeastSignificantBits());
    }

    @Override
    public UUID readValue(BitBuffer buffer) {
        return new UUID(buffer.readLong(), buffer.readLong());
    }

}
