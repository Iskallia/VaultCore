package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;

public class VInt extends VType<Integer> {

    @Override
    public Integer validate(Integer value) {
        return value;
    }

    @Override
    public void writeValue(BitBuffer buffer, SyncContext context, Integer value) {
        buffer.writeInt(value);
    }

    @Override
    public Integer readValue(BitBuffer buffer, SyncContext context) {
        return buffer.readInt();
    }

}
