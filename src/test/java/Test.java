import data.Header;
import data.MyList;
import data.MyMap;
import data.Vault;
import iskallia.vault.core.data.sync.context.SyncContext;
import iskallia.vault.core.net.BitPacket;

import java.util.UUID;

import static data.Header.ID;
import static data.Header.VERSION;
import static data.Vault.*;
import static iskallia.vault.core.VVersion.v1_0;

public class Test {

    public static void main(String[] args) {
        Vault vault1 = new Vault()
            .create(HEADER, new Header()
                .set(VERSION, 0)
                .set(ID, new UUID(0, 0)), context -> true)
            .create(SIZE, 696969, context -> true)
            .create(TYPE, 100000, context -> true)
            .create(LIST, new MyList(), context -> true)
            .create(MAP, new MyMap(), context -> true);

        vault1.get(LIST).add(1);
        vault1.get(LIST).add(2);
        vault1.get(LIST).add(3);
        vault1.get(LIST).add(4);
        vault1.get(LIST).add(5);
        vault1.get(LIST).remove(2);
        vault1.get(MAP).put(15, 15);

        Vault vault2 = new Vault();

        //=====================================================================================================//

        BitPacket packet = new BitPacket();
        vault1.collectSyncTree(packet, new SyncContext(v1_0, REGISTRY));
        vault2.applySyncTree(packet, new SyncContext(v1_0, REGISTRY));

        vault1.resetSyncTree();

        vault1.get(MAP).put(1, 2);
        vault1.get(MAP).put(2, 3);
        vault1.get(MAP).put(3, 4);
        vault1.get(MAP).put(4, 5);
        vault1.get(MAP).put(5, 6);
        vault1.get(MAP).put(3, 12);
        vault1.get(MAP).remove(2);

        packet = new BitPacket();
        vault1.collectSyncTree(packet, new SyncContext(v1_0, REGISTRY));
        vault2.applySyncTree(packet, new SyncContext(v1_0, REGISTRY));

        System.out.println("Server:" + vault1);
        System.out.println("Client:" + vault2);

        System.out.println("Sync packet is " + packet.getSize() + " bytes");
    }

}
