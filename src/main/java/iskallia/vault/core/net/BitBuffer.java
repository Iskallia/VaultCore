package iskallia.vault.core.net;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class BitBuffer {

	protected IntSupplier reader;
	protected IntConsumer writer;

	protected int rBuffer = 0, wBuffer = 0;
	protected int rPosition = 8, wPosition = 0;

	protected BitBuffer(IntSupplier reader, IntConsumer writer) {
		this.reader = reader;
		this.writer = writer;
	}

	//==========================================================================================================//

	public static BitBuffer ofReader(IntSupplier reader) {
		return new BitBuffer(reader, b -> {});
	}

	public static BitBuffer ofWriter(IntConsumer writer) {
		return new BitBuffer(() -> 0, writer);
	}

	public static BitBuffer ofReaderWriter(IntSupplier reader, IntConsumer writer) {
		return new BitBuffer(reader, writer);
	}

	//==========================================================================================================//

	public int getReadBuffer() {
		return this.rBuffer;
	}

	public int getWriteBuffer() {
		return this.wBuffer;
	}

	public int getReadPosition() {
		return this.rPosition;
	}

	public int getWritePosition() {
		return this.wPosition;
	}

	//==========================================================================================================//

	public void readEnd() {
		this.rPosition = 8;
	}

	public BitBuffer writeEnd() {
		this.wPosition = 0;
		this.wBuffer = 0;
		return this;
	}

	//==========================================================================================================//

	public long readBits(int bits) {
		if(this.rPosition == 8) {
			this.rBuffer = this.reader.getAsInt();
			this.rPosition = 0;
		}

		if(bits + this.rPosition < 8) {
			long result = (this.rBuffer >> this.rPosition) & (1L << bits) - 1;
			this.rPosition += bits;
			return result;
		}

		long result = this.rBuffer >> this.rPosition;
		int offset;

		for(offset = 8 - this.rPosition; offset + 8 <= bits; offset += 8) {
			result |= (long)this.reader.getAsInt() << offset;
		}

		this.rPosition = bits - offset;
		return result | ((this.rBuffer = this.reader.getAsInt()) & (1L << bits - offset) - 1) << offset;
	}

	public BitBuffer writeBits(long value, int bits) {
		value &= (1L << bits) - 1;

		if(bits + this.wPosition < 8) {
			this.wBuffer |= value << this.wPosition;
			this.wPosition += bits;
			return this;
		}

		this.writer.accept(this.wBuffer | (int)(value << this.wPosition));

		int offset;

		for(offset = 8 - this.wPosition; offset + 8 <= bits; offset += 8) {
			this.writer.accept((int)(value >>> offset));
		}

		this.wBuffer = (int)(value >>> offset);
		this.wPosition = bits - offset;
		return this;
	}

	//==========================================================================================================//

	public boolean readBoolean() {
		if(this.rPosition == 8) {
			this.rBuffer = this.reader.getAsInt();
			this.rPosition = 0;
		}

		return (this.rBuffer >> this.rPosition++ & 1) != 0;
	}

	public BitBuffer writeBoolean(boolean value) {
		if(this.wPosition != 7) {
			this.wBuffer |= (value ? 1 : 0) << this.wPosition++;
			return this;
		}

		this.writer.accept(this.wBuffer | (value ? 0b10000000 : 0));
		this.wBuffer = 0;
		this.wPosition = 0;
		return this;
	}

	//==========================================================================================================//

	public byte readByte() {
		if(this.rPosition == 8) {
			return (byte)this.reader.getAsInt();
		}

		return (byte)(this.rBuffer >> this.rPosition
			| (this.rBuffer = this.reader.getAsInt()) << 8 - this.rPosition);
	}

	public BitBuffer writeByte(byte value) {
		this.writer.accept(this.wBuffer | value << this.wPosition);
		this.wBuffer = (value & 0xFF) >> 8 - this.wPosition;
		return this;
	}

	//==========================================================================================================//

	public short readShort() {
		if(this.rPosition == 8) {
			return (short)(this.reader.getAsInt()
				| this.reader.getAsInt() << 8);
		}

		return (short)(this.rBuffer >> this.rPosition
			| this.reader.getAsInt() << 8 - this.rPosition
			| (this.rBuffer = this.reader.getAsInt()) << 8 - this.rPosition + 8);
	}

	public BitBuffer writeShort(short value) {
		this.writer.accept(this.wBuffer | value << this.wPosition);
		this.writer.accept(value >>> 8 - this.wPosition);
		this.wBuffer = (value & 0xFFFF) >> 8 >> 8 - this.wPosition;
		return this;
	}

	//==========================================================================================================//

	public int readInt() {
		if(this.rPosition == 8) {
			return this.reader.getAsInt()
				| this.reader.getAsInt() << 8
				| this.reader.getAsInt() << 16
				| this.reader.getAsInt() << 24;
		}

		int value = this.rBuffer >> this.rPosition
			| this.reader.getAsInt() << 8 - this.rPosition
			| this.reader.getAsInt() << 8 - this.rPosition + 8
			| this.reader.getAsInt() << 8 - this.rPosition + 16;

		this.rBuffer = this.reader.getAsInt();

		if(this.rPosition != 0) {
			value |= this.rBuffer << 8 - this.rPosition + 24;
		}

		return value;
	}

	public int readIntBounded(int min, int max) {
		return (int)this.readBits(32 - Integer.numberOfLeadingZeros(max - min)) + min;
	}

	public int readIntSegmented(int segment) {
		int mask = 1 << segment;
		int value = 0;

		for(int shift = 0; ; shift += segment) {
			long bits = this.readBits(segment + 1);

			if((bits & mask) != 0) {
				value |= (bits - mask) << shift;
				return value;
			}

			value |= bits << shift;
		}
	}

	public BitBuffer writeInt(int value) {
		this.writer.accept(this.wBuffer | value << this.wPosition);
		this.writer.accept(value >>> 8 - this.wPosition);
		this.writer.accept(value >>> 8 - this.wPosition + 8);
		this.writer.accept(value >>> 8 - this.wPosition + 16);
		this.wBuffer = value >>> 24 >>> 8 - this.wPosition;
		return this;
	}


	public BitBuffer writeIntBounded(int value, int min, int max) {
		return this.writeBits(value - min, 32 - Integer.numberOfLeadingZeros(max - min));
	}

	public BitBuffer writeIntSegmented(int value, int segment) {
		int mask = (1 << segment) - 1;

		while(true) {
			long bits = value & mask;
			value >>>= segment;

			if(value == 0) {
				this.writeBits((1L << segment) | bits, segment + 1);
				break;
			}

			this.writeBits(bits, segment + 1);
		}

		return this;
	}

	//==========================================================================================================//

	public float readFloat() {
		return Float.intBitsToFloat(this.readInt());
	}

	public BitBuffer writeFloat(float value) {
		return this.writeInt(Float.floatToIntBits(value));
	}

	//==========================================================================================================//

	public long readLong() {
		if(this.rPosition == 8) {
			return (long)this.reader.getAsInt()
				| (long)this.reader.getAsInt() << 8
				| (long)this.reader.getAsInt() << 16
				| (long)this.reader.getAsInt() << 24
				| (long)this.reader.getAsInt() << 32
				| (long)this.reader.getAsInt() << 40
				| (long)this.reader.getAsInt() << 48
				| (long)this.reader.getAsInt() << 56;
		}

		long value = this.rBuffer >> this.rPosition
			| (long)this.reader.getAsInt() << 8 - this.rPosition
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 8
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 16
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 24
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 32
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 40
			| (long)this.reader.getAsInt() << 8 - this.rPosition + 48;

		this.rBuffer = this.reader.getAsInt();

		if(this.rPosition != 0) {
			value |= (long)this.rBuffer << 8 - this.rPosition + 56;
		}

		return value;
	}

	public long readLongBounded(long min, long max) {
		return this.readBits(64 - Long.numberOfLeadingZeros(max - min)) + min;
	}

	public long readLongSegmented(int segment) {
		long mask = 1L << segment;
		long value = 0L;

		for(int shift = 0; ; shift += segment) {
			long bits = this.readBits(segment + 1);

			if((bits & mask) != 0) {
				value |= (bits - mask) << shift;
				return value;
			}

			value |= bits << shift;
		}
	}

	public BitBuffer writeLong(long value) {
		this.writer.accept((int)(this.wBuffer | value << this.wPosition));
		this.writer.accept((int)(value >>> 8 - this.wPosition));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 8));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 16));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 24));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 32));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 40));
		this.writer.accept((int)(value >>> 8 - this.wPosition + 48));
		this.wBuffer = (int)(value >>> 56 >>> 8 - this.wPosition);
		return this;
	}

	public BitBuffer writeLongBounded(long value, long min, long max) {
		return this.writeBits(value - min, 64 - Long.numberOfLeadingZeros(max - min));
	}

	public BitBuffer writeLongSegmented(long value, int segment) {
		long mask = (1L << segment) - 1;

		while(true) {
			long bits = value & mask;
			value >>>= segment;

			if(value == 0) {
				this.writeBits((1L << segment) | bits, segment + 1);
				break;
			}

			this.writeBits(bits, segment + 1);
		}

		return this;
	}

	//==========================================================================================================//

	public double readDouble() {
		return Double.longBitsToDouble(this.readLong());
	}

	public BitBuffer writeDouble(double value) {
		return this.writeLong(Double.doubleToLongBits(value));
	}

	//==========================================================================================================//

}
