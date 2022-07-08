package iskallia.vault.core.net;

public class BitPacket extends BitBuffer {

	protected int[] packet = new int[8];
	protected int readPos;
	protected int writePos;

	public BitPacket() {
		super(() -> 0, b -> {});
		this.reader = this::read;
		this.writer = this::write;
	}

	public int getSize() {
		return this.writePos + (this.wPosition != 0 ? 1 : 0);
	}

	protected int read() {
		if(this.readPos == this.writePos) {
			this.readPos++;
			return this.wBuffer;
		}

		int i = this.readPos / 4;
		int j = this.readPos % 4;
		this.readPos++;
		return this.packet[i] >>> j * 8 & 255;
	}

	protected void write(int b) {
		int i = this.writePos / 4;
		int j = this.writePos % 4;
		this.ensureCapacity(i);
		this.packet[i] |= (b & 255) << j * 8;
		this.writePos++;
	}

	protected void ensureCapacity(int size) {
		if(size < this.packet.length) return;
		int[] newPacket = new int[this.packet.length << 1];
		System.arraycopy(this.packet, 0, newPacket, 0, this.packet.length);
		this.packet = newPacket;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(int pos = 0; pos < this.writePos; pos++) {
			int b = this.packet[pos / 4] >>> (pos % 4) * 8 & 255;

			for(int i = 0; i < 8; i++) {
				sb.append(((b << i) & (1 << 7)) == 0 ? "0" : "1");
			}

			if(this.wPosition != 0) {
				sb.append("-");
			}
		}

		for(int i = 0; i < this.wPosition; i++) {
			sb.append(((this.wBuffer << i) & (1 << this.wPosition - 1)) == 0 ? "0" : "1");
		}

		return sb.toString();
	}

}
