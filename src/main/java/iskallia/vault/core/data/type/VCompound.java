package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.IBitSerializable;

import java.util.function.Supplier;

public class VCompound<T extends IBitSerializable<?>> extends VType<T> {

	private final Supplier<T> supplier;

	public VCompound(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public Supplier<T> getSupplier() {
		return this.supplier;
	}

	@Override
	public void writeValue(BitBuffer buffer, SyncContext context, T value) {
		value.write(buffer, context);
	}

	@Override
	public T readValue(BitBuffer buffer, SyncContext context) {
		T value = this.supplier.get();
		value.read(buffer, context);
		return value;
	}

}
