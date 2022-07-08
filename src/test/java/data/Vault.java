package data;

import iskallia.vault.core.data.VDataObject;
import iskallia.vault.core.data.key.VKey;
import iskallia.vault.core.data.key.VKeyRegistry;
import iskallia.vault.core.data.type.VType;

import static iskallia.vault.core.VVersion.newerOrEqualTo;
import static iskallia.vault.core.VVersion.v1_0;

public class Vault extends VDataObject<Vault> {

	public static VKeyRegistry REGISTRY = new VKeyRegistry();

	public Vault() {

	}

	public static final VKey<Header> HEADER = VKey.create("header", Header.class)
		.with(newerOrEqualTo(v1_0), VType.ofCompound(Header::new))
		.build(REGISTRY);
	public static final VKey<Integer> SIZE = VKey.create("size", Integer.class)
		.with(newerOrEqualTo(v1_0), VType.ofInt())
		.build(Vault.REGISTRY);
	public static final VKey<Integer> TYPE = VKey.create("type", Integer.class)
		.with(newerOrEqualTo(v1_0), VType.ofInt())
		.build(Vault.REGISTRY);
	public static final VKey<MyList> LIST = VKey.create("list", MyList.class)
		.with(newerOrEqualTo(v1_0), VType.ofCompound(MyList::new))
		.build(Vault.REGISTRY);
	public static final VKey<MyMap> MAP = VKey.create("map", MyMap.class)
		.with(newerOrEqualTo(v1_0), VType.ofCompound(MyMap::new))
		.build(Vault.REGISTRY);

}
