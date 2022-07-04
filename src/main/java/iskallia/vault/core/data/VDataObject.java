package iskallia.vault.core.data;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.key.VKey;
import iskallia.vault.core.data.key.VKeyRegistry;
import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.data.sync.handler.SyncHandler;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.BitPacket;

import java.util.*;

public class VDataObject<D extends VDataObject<D>> implements IVCompound<D> {

	protected Map<VKey<Object>, Entry> values = new IdentityHashMap<>();
	protected Set<VKey<Object>> createdKeys = new HashSet<>();
	protected Set<VKey<Object>> updatedKeys = new HashSet<>();
	protected Set<VKey<Object>> removedKeys = new HashSet<>();

	public VDataObject() {

	}

	public <T> D create(VKey<T> key, T value, SyncHandler handler) {
		if(this.values.containsKey(key)) {
			throw new UnsupportedOperationException("Key already exists");
		}

		Entry entry = new Entry(value, handler);
		this.values.put((VKey<Object>)key, entry);
		this.createdKeys.add((VKey<Object>)key);
		return (D)this;
	}

	public <T> D remove(VKey<T> key) {
		Entry entry = this.values.remove(key);

		if(entry != null) {
			this.updatedKeys.remove(key);

			if(!this.createdKeys.remove(key)) {
				this.removedKeys.add((VKey<Object>)key);
			}
		}

		return (D)this;
	}

	public <T> boolean has(VKey<T> key) {
		return this.values.containsKey(key);
	}

	public <T> D set(VKey<T> key, T value) {
		Entry entry = this.values.get(key);

		if(entry != null) {
			if(!entry.value.equals(value)) {
				entry.value = value;
				this.updatedKeys.add((VKey<Object>)key);
			}
		} else {
			throw new UnsupportedOperationException("Key is not defined");
		}

		return (D)this;
	}

	public <T> T get(VKey<T> type) {
		return (T)this.values.get(type).value;
	}

	public <T> Optional<T> getOpt(VKey<T> type) {
		if(this.values.containsKey(type)) {
			return Optional.of(this.get(type));
		}

		return Optional.empty();
	}

	@Override
	public D write(BitBuffer buffer, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		buffer.writeIntSegmented(this.values.size(), 4);

		this.values.forEach((key, entry) -> {
			if(!entry.handler.canSync(context)) return;
			buffer.writeIntBounded(registry.getIndex(key, version), 0, registry.getSize(version) - 1);
			key.get(version).writeValue(buffer, context, entry.value); //TODO: serialize handler
		});

		return (D)this;
	}

	@Override
	public D read(BitBuffer buffer, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		this.values.clear();
		int size = buffer.readIntSegmented(4);

		for(int i = 0; i < size; i++) {
			VKey<?> key = registry.getKey(buffer.readIntBounded(0, registry.getSize(version) - 1), version);
			Object value = key.get(version).readValue(buffer, context);
			Entry entry = new Entry(value, null); //TODO: serialize handler
			this.values.put((VKey<Object>)key, entry);
		}

		return (D)this;
	}

	@Override
	public boolean isDirty(SyncContext context) {
		if(!this.createdKeys.isEmpty() || !this.updatedKeys.isEmpty() || !this.removedKeys.isEmpty()) {
			return true;
		}

		for(Entry entry : this.values.values()) {
			if(!entry.handler.canSync(context)) continue;

			if(entry.value instanceof VDataObject) {
				if(((VDataObject<?>)entry.value).isDirty(context)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void collectSync(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		for(VKey<Object> removedKey : this.removedKeys) {
			packet.writeBoolean(true);
			packet.writeIntBounded(registry.getIndex(removedKey, version), 0, registry.getSize(version) - 1);
		}

		packet.writeBoolean(false);

		for(VKey<Object> updatedKey : this.updatedKeys) {
			if(this.createdKeys.contains(updatedKey)) continue;
			VDataObject.Entry entry = this.values.get(updatedKey);
			if(!entry.handler.canSync(context)) continue;
			packet.writeBoolean(true);
			packet.writeIntBounded(registry.getIndex(updatedKey, version), 0, registry.getSize(version) - 1);
			updatedKey.get(version).writeValue(packet, context, entry.value);
		}

		packet.writeBoolean(false);

		for(VKey<Object> createdKey : this.createdKeys) {
			VDataObject.Entry entry = this.values.get(createdKey);
			if(!entry.handler.canSync(context)) continue;
			packet.writeBoolean(true);
			packet.writeIntBounded(registry.getIndex(createdKey, version), 0, registry.getSize(version) - 1);
			createdKey.get(version).writeValue(packet, context, entry.value); //TODO: serialize handler
		}

		packet.writeBoolean(false);
	}

	@Override
	public void applySync(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		while(packet.readBoolean()) {
			VKey<?> key = registry.getKey(packet.readIntBounded(0, registry.getSize(version) - 1), version);
			this.values.remove(key);
		}

		while(packet.readBoolean()) {
			VKey<?> key = registry.getKey(packet.readIntBounded(0, registry.getSize(version) - 1), version);
			this.values.get(key).value = key.get(context.getVersion()).readValue(packet, context);
		}

		while(packet.readBoolean()) {
			VKey<?> key = registry.getKey(packet.readIntBounded(0, registry.getSize(version) - 1), version);
			VDataObject.Entry entry = new VDataObject.Entry(key.get(version).readValue(packet, context), null);
			this.values.put((VKey<Object>)key, entry); //TODO: serialize handler
		}
	}

	@Override
	public boolean collectSyncTree(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		this.collectSync(packet, context);
		boolean modified = !this.createdKeys.isEmpty() || !this.updatedKeys.isEmpty() || !this.removedKeys.isEmpty();

		for(Map.Entry<VKey<Object>, Entry> e : this.values.entrySet()) {
			VKey<Object> key = e.getKey();
			Entry entry = e.getValue();

			if(!entry.handler.canSync(context)) continue;

			if(entry.value instanceof IVCompound<?> && !this.createdKeys.contains(key)) {
				IVCompound<?> compound = (IVCompound<?>)entry.value;

				if(compound.isDirty(context)) {
					packet.writeBoolean(true);
					packet.writeIntBounded(registry.getIndex(key, version), 0, registry.getSize(version) - 1);
					compound.collectSyncTree(packet, context);
				}
			}
		}

		packet.writeBoolean(false);
		return modified;
	}

	@Override
	public void applySyncTree(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		this.applySync(packet, context);

		while(packet.readBoolean()) {
			VKey<?> key = registry.getKey(packet.readIntBounded(0, registry.getSize(version) - 1), version);
			((IVCompound<?>)this.values.get(key).value).applySyncTree(packet, context);
		}
	}

	@Override
	public void resetSync() {
		this.createdKeys.clear();
		this.updatedKeys.clear();
		this.removedKeys.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");

		Iterator<Map.Entry<VKey<Object>, Entry>> it = this.values.entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<VKey<Object>, Entry> e = it.next();
			VKey<Object> key = e.getKey();
			Entry entry = e.getValue();

			sb.append("\"").append(key.id).append("\":").append(entry.value.toString());
			if(it.hasNext()) sb.append(",");
		}

		return sb.append("}").toString();
	}

	public static class Entry {
		public Object value;
		public SyncHandler handler;

		public Entry(Object value, SyncHandler handler) {
			this.value = value;
			this.handler = handler;
		}
	}

}
