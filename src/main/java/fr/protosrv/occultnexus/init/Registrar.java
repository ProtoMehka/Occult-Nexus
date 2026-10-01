package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.foundation.network.OccultNexusNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class Registrar {

    public static void init(IEventBus modBus) {
        BlockRegistrar.register(modBus);
        BlockEntityRegistrar.register(modBus);
        MenuTypeRegistrar.register(modBus);
        EntityRegistrar.register(modBus);
        ItemRegistrar.register(modBus);
        ModCreativeTab.register(modBus);
        modBus.addListener(Registrar::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        OccultNexusNetwork.register();
        event.enqueueWork(IntegrationRegistrar::postInit);
    }
}
