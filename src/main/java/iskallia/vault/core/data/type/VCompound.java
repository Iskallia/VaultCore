package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.IBitSerializable;

import java.util.function.Supplier;

public class VCompound<T extends IBitSerializable<?>> extends VType<T> {

	private final Supplier<T> supplier;
	private final boolean nullable;

	public VCompound(Supplier<T> supplier, boolean nullable) {
		this.supplier = supplier;
		this.nullable = nullable;
	}

	public VCompound<T> asNullable() {
		return this.nullable ? this : new VCompound<>(this.supplier, true);
	}

	public Supplier<T> getSupplier() {
		return this.supplier;
	}

	public boolean isNullable() {
		return this.nullable;
	}

	@Override
	public T validate(T value) {
		if(!this.nullable && value == null) {
			throw new UnsupportedOperationException("Value cannot be null");
		}

		return value;
	}

	@Override
	public void writeValue(BitBuffer buffer, SyncContext context, T value) {
		if(this.nullable) {
			buffer.writeBoolean(value == null);
		}

		if(value != null) {
			value.write(buffer, context);
		}
	}

	@Override
	public T readValue(BitBuffer buffer, SyncContext context) {
		T value = this.supplier.get();

		if(this.nullable && buffer.readBoolean()) {
			return null;
		}

		value.read(buffer, context);
		return value;
	}

}
