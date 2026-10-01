package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.thrallflesh.DebugSummonThrallfleshItem;
import fr.protosrv.occultnexus.content.wraith.DebugSummonWraithItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OccultNexus.MODID);

    public static final RegistryObject<Item> THRALLFLESH_SPAWN_EGG = ITEMS.register("thrallflesh_spawn_egg",
            () -> new ForgeSpawnEggItem(EntityRegistrar.THRALLFLESH, 0x1D1D1D, 0x800000, new Item.Properties()));

    public static final RegistryObject<Item> DEBUG_SUMMON_THRALLFLESH = ITEMS.register("debug_summon_thrallflesh",
            () -> new DebugSummonThrallfleshItem(new Item.Properties()));

    public static final RegistryObject<Item> RECLAIMED_WRAITH_SPAWN_EGG = ITEMS.register("reclaimed_wraith_spawn_egg",
            () -> new ForgeSpawnEggItem(EntityRegistrar.RECLAIMED_WRAITH, 0x0d3b36, 0x62ded1, new Item.Properties()));

    public static final RegistryObject<Item> DEBUG_SUMMON_RECLAIMED_WRAITH = ITEMS.register("debug_summon_reclaimed_wraith",
            () -> new DebugSummonWraithItem(new Item.Properties()));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
