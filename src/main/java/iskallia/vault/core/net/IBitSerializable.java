package iskallia.vault.core.net;

public interface IBitSerializable<T> {

	T write(BitBuffer buffer);

	T read(BitBuffer buffer);

}
