package iskallia.vault.core.data.type;

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

    public int clamp(int value) {
        return value < this.min ? this.min : Math.min(value, this.max);
    }

    @Override
    public void writeValue(BitBuffer buffer, Integer value) {
        buffer.writeBits(this.clamp(value) - this.min, this.bits);
    }

    @Override
    public Integer readValue(BitBuffer buffer) {
        return this.min + (int)buffer.readBits(this.bits);
    }

}
