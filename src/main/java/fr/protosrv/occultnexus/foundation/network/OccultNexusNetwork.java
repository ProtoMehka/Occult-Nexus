package fr.protosrv.occultnexus.foundation.network;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.spiritjar.MessageSpiritSetFilter;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class OccultNexusNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            OccultNexus.prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, MessageSpiritSetFilter.class,
                MessageSpiritSetFilter::encode, MessageSpiritSetFilter::decode, MessageSpiritSetFilter::handle);
    }

    private OccultNexusNetwork() {
    }
}
