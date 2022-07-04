package iskallia.vault.core.data;

import iskallia.vault.core.VVersion;
import iskallia.vault.core.data.key.VKeyRegistry;
import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.data.type.VType;
import iskallia.vault.core.net.BitBuffer;
import iskallia.vault.core.net.BitPacket;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VDataList<D extends VDataList<D, E>, E> extends AbstractList<E> implements IVCompound<D> {

	protected VType<E> type;
	protected List<E> values;

	protected List<Action> actions = new ArrayList<>();

	public VDataList(VType<E> type, List<E> delegate) {
		this.type = type;
		this.values = delegate;
	}

	@Override
	public int size() {
		return this.values.size();
	}

	@Override
	public boolean add(E element) {
		this.addAction(Action.ofAppend(this.values.size(), element));
		this.values.add(element);
		return true;
	}

	@Override
	public E get(int index) {
		return this.values.get(index);
	}

	@Override
	public E set(int index, E element) {
		this.addAction(Action.ofSet(index, element, this.values.size()));
		return this.values.set(index, element);
	}

	@Override
	public void add(int index, E element) {
		this.addAction(Action.ofAdd(index, element, this.values.size()));
		this.values.add(index, element);
	}

	@Override
	public E remove(int index) {
		this.addAction(Action.ofRemove(index, this.values.size()));
		return this.values.remove(index);
	}

	@Override
	public void clear() {
		this.addAction(Action.ofClear());
		this.values.clear();
	}


	public void addAction(Action action) {
		if(action.type == ActionType.CLEAR) {
			this.actions.clear();
			this.actions.add(action);
		} else if(action.type == ActionType.SET) {
			IndexTracker tracker = new IndexTracker(action.index);

			for(int i = this.actions.size() - 1; i >= 0; i--) {
				Action target = this.actions.get(i);

				if(target.index != tracker.index) {
					tracker.previous(target);
					continue;
				}

				if(target.type == ActionType.SET || target.type == ActionType.ADD || target.type == ActionType.APPEND) {
					target.value = action.value;
					return;
				}

				tracker.previous(target);
			}

			this.actions.add(action);
		} else if(action.type == ActionType.REMOVE) {
			IndexTracker tracker = new IndexTracker(action.index);

			for(int i = this.actions.size() - 1; i >= 0; i--) {
				Action target = this.actions.get(i);

				if(target.index != tracker.index) {
					tracker.previous(target);
					continue;
				}

				if(target.type == ActionType.ADD || target.type == ActionType.APPEND) {
					IndexTracker anchor = new IndexTracker(target.index);

					for(int j = i + 1; j < this.actions.size(); j++) {
						Action other = this.actions.get(j);
						other.size--;

						if(other.index > anchor.index) {
							other.index--;
						} else if(other.index == anchor.index && other.type == ActionType.SET) {
							this.actions.remove(j--);
						}

						anchor.next(other);
					}

					this.actions.remove(i);
					return;
				}

				tracker.previous(target);
			}

			this.actions.add(action);
		} else {
			this.actions.add(action);
		}
	}

	@Override
	public D write(BitBuffer buffer) {
		return (D)this;
	}

	@Override
	public D read(BitBuffer buffer) {
		return (D)this;
	}

	@Override
	public boolean isDirty(SyncContext context) {
		if(!this.actions.isEmpty()) {
			return true;
		}

		for(Object value : this.values) {
			if(value instanceof IVCompound<?>) {
				if(((IVCompound<?>)value).isDirty(context)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void collectSync(BitPacket packet, SyncContext context) {
		packet.writeBoolean(this.actions.isEmpty());
		if(this.actions.isEmpty()) return;

		boolean doClear = this.actions.get(0).type == ActionType.CLEAR;
		packet.writeBoolean(doClear);
		packet.writeIntSegmented(this.actions.size() - (doClear ? 1 : 0), 4);

		for(int i = doClear ? 1 : 0; i < this.actions.size(); i++) {
			Action action = this.actions.get(i);
			packet.writeBits(action.type.ordinal(), 2);

			if(action.type != ActionType.APPEND) {
				packet.writeIntBounded(action.index, 0, action.size - 1);
			}

			if(action.type == ActionType.APPEND || action.type == ActionType.ADD) {
				this.type.writeValue(packet, (E)action.value);
			}
		}
	}

	@Override
	public void applySync(BitPacket packet, SyncContext context) {
		if(packet.readBoolean()) return;

		if(packet.readBoolean()) {
			this.values.clear();
		}

		int size = packet.readIntSegmented(4);

		for(int i = 0; i < size; i++) {
			Action action = new Action(-1, null, ActionType.values()[(int)packet.readBits(2)], -1);

			if(action.type != ActionType.APPEND) {
				action.index = packet.readIntBounded(0, this.values.size() - 1);
			}

			if(action.type == ActionType.APPEND || action.type == ActionType.ADD) {
				action.value = this.type.readValue(packet);
			}

			action.apply(this);
		}
	}

	@Override
	public boolean collectSyncTree(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		this.collectSync(packet, context);
		boolean modified = !this.actions.isEmpty();

		for(E value : this.values) {
			if(value instanceof IVCompound<?>) {
				IVCompound<?> compound = (IVCompound<?>)value;
				packet.writeBoolean(compound.isDirty(context));
				compound.collectSyncTree(packet, context);
			}
		}

		return modified;
	}

	@Override
	public void applySyncTree(BitPacket packet, SyncContext context) {
		VVersion version = context.getVersion();
		VKeyRegistry registry = context.getRegistry();

		this.applySync(packet, context);

		for(E value : this.values) {
			if(value instanceof IVCompound<?>) {
				IVCompound<?> compound = (IVCompound<?>)value;

				if(packet.readBoolean()) {
					compound.applySyncTree(packet, context);
				}
			}
		}
	}

	public void resetSync() {
		this.actions.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		Iterator<E> it = this.values.iterator();

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
				case APPEND -> list.add(this.value);
				case ADD -> list.add(this.index, this.value);
				case SET -> list.set(this.index, this.value);
				case REMOVE -> list.remove(this.index);
				case CLEAR -> list.clear();
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
