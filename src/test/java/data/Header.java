package data;

import iskallia.vault.core.data.VDataObject;
import iskallia.vault.core.data.key.VKey;
import iskallia.vault.core.data.type.VType;

import java.util.UUID;

import static iskallia.vault.core.VVersion.*;

public class Header extends VDataObject<Header> {
	public static final VKey<Integer> VERSION = VKey.create("version", Integer.class)
		.with(newerOrEqualTo(v1_0), VType.ofBoundedInt(0, 40))
		.build(Vault.REGISTRY);
	public static final VKey<UUID> ID = VKey.create("id", UUID.class)
		.with(newerOrEqualTo(v1_0), VType.ofUUID())
		.build(Vault.REGISTRY);

	public Header() {
		this.create(VERSION, 12, context -> true);
		this.create(ID, UUID.randomUUID(), context -> true);
	}

}