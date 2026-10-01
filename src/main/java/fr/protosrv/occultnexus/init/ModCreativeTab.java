package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.OccultNexus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Dedicated tab for Occult Nexus's own content, exclusive (not also shown in Ars Nouveau's or
 * Occultism's own tabs) — mirrors the pattern already used by Cryptic Life Mod's own tab.
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OccultNexus.MODID);

    public static final RegistryObject<CreativeModeTab> OCCULT_NEXUS_TAB = TABS.register("occult_nexus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.occult_nexus"))
                    .icon(() -> new ItemStack(BlockRegistrar.ALTAR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(BlockRegistrar.ALTAR.get());
                        output.accept(ItemRegistrar.THRALLFLESH_SPAWN_EGG.get());
                        output.accept(ItemRegistrar.DEBUG_SUMMON_THRALLFLESH.get());
                        output.accept(ItemRegistrar.RECLAIMED_WRAITH_SPAWN_EGG.get());
                        output.accept(ItemRegistrar.DEBUG_SUMMON_RECLAIMED_WRAITH.get());
                    })
                    .build());

    private ModCreativeTab() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
