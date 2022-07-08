package iskallia.vault.core.data;

import iskallia.vault.core.data.action.ListAction;
import iskallia.vault.core.data.action.ListTracker;
import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.data.type.VType;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.BitPacket;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import static iskallia.vault.core.data.action.ListAction.Type.*;

public class VDataList<D extends VDataList<D, E>, E> extends AbstractList<E> implements IVCompound<D> {

	private final List<E> delegate;
	private final VType<E> type;

	protected final ListTracker tracker = new ListTracker();

	public VDataList(List<E> delegate, VType<E> type) {
		this.delegate = delegate;
		this.type = type;
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public boolean add(E element) {
		this.tracker.addAction(ListAction.ofAppend(this.delegate.size(), element));
		this.delegate.add(element);
		return true;
	}

	@Override
	public E get(int index) {
		return this.delegate.get(index);
	}

	@Override
	public E set(int index, E element) {
		this.tracker.addAction(ListAction.ofSet(index, element, this.delegate.size()));
		return this.delegate.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		this.tracker.addAction(ListAction.ofAdd(index, element, this.delegate.size()));
		this.delegate.add(index, element);
	}

	@Override
	public E remove(int index) {
		this.tracker.addAction(ListAction.ofRemove(index, this.delegate.size()));
		return this.delegate.remove(index);
	}

	@Override
	public void clear() {
		this.tracker.addAction(ListAction.ofClear());
		this.delegate.clear();
	}

	@Override
	public D write(BitBuffer buffer, SyncContext context) {
		buffer.writeIntSegmented(this.delegate.size(), 4);

		for(E value : this.delegate) {
			this.type.writeValue(buffer, context, this.type.validate(value));
		}

		return (D)this;
	}

	@Override
	public D read(BitBuffer buffer, SyncContext context) {
		int size = buffer.readIntSegmented(4);
		this.delegate.clear();

		for(int i = 0; i < size; i++) {
			this.add(this.type.readValue(buffer, context));
		}

		return (D)this;
	}

	@Override
	public boolean isDirty(SyncContext context) {
		return !this.tracker.getActions().isEmpty();
	}

	@Override
	public void collectSync(BitPacket packet, SyncContext context) {
		packet.writeBoolean(this.tracker.getActions().isEmpty());
		if(this.tracker.getActions().isEmpty()) return;

		boolean doClear = this.tracker.getActions().get(0).type == CLEAR;
		packet.writeBoolean(doClear);
		packet.writeIntSegmented(this.tracker.getActions().size() - (doClear ? 1 : 0), 4);

		for(int i = doClear ? 1 : 0; i < this.tracker.getActions().size(); i++) {
			ListAction action = this.tracker.getActions().get(i);
			packet.writeBits(action.type.ordinal(), 2);

			if(action.type != APPEND) {
				packet.writeIntBounded(action.index, 0, action.size - 1);
			}

			if(action.type != REMOVE) {
				this.type.writeValue(packet, context, this.type.validate((E)action.value));
			}
		}
	}

	@Override
	public void applySync(BitPacket packet, SyncContext context) {
		if(packet.readBoolean()) return;

		if(packet.readBoolean()) {
			this.delegate.clear();
		}

		int size = packet.readIntSegmented(4);

		for(int i = 0; i < size; i++) {
			Action action = new Action(-1, null, ActionType.values()[(int)packet.readBits(2)], -1);

			if(action.type != ActionType.APPEND) {
				action.index = packet.readIntBounded(0, this.delegate.size() - 1);
			}

			if(action.type != ActionType.REMOVE) {
				action.value = this.type.readValue(packet, context);
			}

			action.apply(this);
		}
	}

	@Override
	public void resetSync() {
		this.tracker.getActions().clear();
	}

	@Override
	public boolean isDirtyTree(SyncContext context) {
		if(this.isDirty(context)) {
			return true;
		}

		for(Object value : this.delegate) {
			if(value instanceof IVCompound<?>) {
				if(((IVCompound<?>)value).isDirty(context)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void collectSyncTree(BitPacket packet, SyncContext context) {
		this.collectSync(packet, context);

		for(E value : this.delegate) {
			if(value instanceof IVCompound<?>) {
				IVCompound<?> compound = (IVCompound<?>)value;

				if(compound.isDirtyTree(context)) {
					packet.writeBoolean(true);
					compound.collectSyncTree(packet, context);
				} else {
					packet.writeBoolean(false);
				}
			}
		}
	}

	@Override
	public void applySyncTree(BitPacket packet, SyncContext context) {
		this.applySync(packet, context);

		for(E value : this.delegate) {
			if(value instanceof IVCompound<?>) {
				IVCompound<?> compound = (IVCompound<?>)value;

				if(packet.readBoolean()) {
					compound.applySyncTree(packet, context);
				}
			}
		}
	}

	@Override
	public void resetSyncTree() {
		this.resetSync();

		for(E value : this.delegate) {
			if(value instanceof IVCompound<?>) {
				((IVCompound<?>)value).resetSyncTree();
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		Iterator<E> it = this.delegate.iterator();

		while(it.hasNext()) {
			sb.append(it.next());
			if(it.hasNext()) sb.append(",");
		}

		return sb.append("]").toString();
	}

	private static class Action {
		public int index;
		public Object value;
		public ActionType type;
		public int size;

		public Action(int index, Object value, ActionType type, int size) {
			this.index = index;
			this.value = value;
			this.type = type;
			this.size = size;
		}

		public static Action ofAppend(int size, Object value) {
			return new Action(size, value, ActionType.APPEND, -1);
		}

		public static Action ofAdd(int index, Object value, int size) {
			return new Action(index, value, ActionType.ADD, size);
		}

		public static Action ofSet(int index, Object value, int size) {
			return new Action(index, value, ActionType.SET, size);
		}

		public static Action ofRemove(int index, int size) {
			return new Action(index, null, ActionType.REMOVE, size);
		}

		public static Action ofClear() {
			return new Action(-1, null, ActionType.CLEAR, -1);
		}

		public void apply(List list) {
			switch(this.type) {
				case APPEND:
					list.add(this.value);
					break;
				case ADD:
					list.add(this.index, this.value);
					break;
				case SET:
					list.set(this.index, this.value);
					break;
				case REMOVE:
					list.remove(this.index);
					break;
				case CLEAR:
					list.clear();
					break;
			}
		}
	}

	private enum ActionType {
		APPEND, ADD, SET, REMOVE, CLEAR
	}

	private static class IndexTracker {
		private int index;

		public IndexTracker(int index) {
			this.index = index;
		}

		public void next(Action action) {
			if(this.index < 0) return;

			if(action.type == ActionType.ADD) {
				if(this.index >= action.index) {
					this.index++;
				}
			} else if(action.type == ActionType.REMOVE) {
				if(this.index == action.index) {
					this.index = -1;
				}
			}
		}

		public void previous(Action action) {
			if(this.index < 0) return;

			if(action.type == ActionType.ADD) {
				if(this.index > action.index) {
					this.index--;
				}
			} else if(action.type == ActionType.REMOVE) {
				if(this.index > action.index) {
					this.index--;
				}
			}
		}
	}

}
