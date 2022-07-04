package iskallia.vault.core.data.type;

import iskallia.vault.core.net.BitBuffer;

public class VInt extends VType<Integer> {

    @Override
    public void writeValue(BitBuffer buffer, Integer value) {
        buffer.writeInt(value);
    }

    @Override
    public Integer readValue(BitBuffer buffer) {
        return buffer.readInt();
    }

}
