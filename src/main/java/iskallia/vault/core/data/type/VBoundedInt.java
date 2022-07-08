package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;

public class VBoundedInt extends VType<Integer> {

    protected final int min;
    protected final int max;
    protected final int bits;

    public VBoundedInt(int min, int max) {
        this.min = min;
        this.max = max;
        this.bits = 32 - Integer.numberOfLeadingZeros(this.max - this.min);
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public int getBits() {
        return this.bits;
    }

    @Override
    public Integer validate(Integer value) {
        if(value < this.min || value > this.max) {
            throw new UnsupportedOperationException(String.format("Value %d is not between %d and %d", value, this.min, this.max));
        }

        return value;
    }

    @Override
    public void writeValue(BitBuffer buffer, SyncContext context, Integer value) {
        buffer.writeBits(value - this.min, this.bits);
    }

    @Override
    public Integer readValue(BitBuffer buffer, SyncContext context) {
        return this.min + (int)buffer.readBits(this.bits);
    }

}
