package iskallia.vault.core.data.type;

import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.IBitSerializable;

import java.util.UUID;
import java.util.function.Supplier;

public abstract class VType<T> {

	public abstract T validate(T value);

	public abstract void writeValue(BitBuffer buffer, SyncContext context, T value);

	public abstract T readValue(BitBuffer buffer, SyncContext context);

	//========================================================================================//

	public static VVoid ofVoid() {
		return new VVoid();
	}

	public static <T extends IBitSerializable<?>> VCompound<T> ofCompound(Supplier<T> supplier) {
		return new VCompound<>(supplier, false);
	}

	public static VInt ofInt() {
		return new VInt();
	}

	public static VBoundedInt ofBoundedInt(int min, int max) {
		return new VBoundedInt(min, max);
	}

	public static VType<UUID> ofUUID() {
		return new VUUID(false);
	}

}
