package iskallia.vault.core.data.type;

import iskallia.vault.core.net.BitBuffer;

public class VVoid extends VType<Void> {

	@Override
	public void writeValue(BitBuffer buffer, Void value) {

	}

	@Override
	public Void readValue(BitBuffer buffer) {
		return null;
	}

}
