package iskallia.vault.core.data.type;

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
	public void writeValue(BitBuffer buffer, T value) {
		value.write(buffer);
	}

	@Override
	public T readValue(BitBuffer buffer) {
		T value = this.supplier.get();
		value.read(buffer);
		return value;
	}

}
