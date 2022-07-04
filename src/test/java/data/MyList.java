package data;

import iskallia.vault.core.data.VDataList;
import iskallia.vault.core.data.type.VType;

import java.util.ArrayList;
import java.util.List;

public class MyList extends VDataList<MyList, Integer> {

	public MyList(VType<Integer> type, List<Integer> delegate) {
		super(type, delegate);
	}

	public MyList() {
		this(VType.ofInt(), new ArrayList<>());
	}

}