package fr.protosrv.occultnexus;

import fr.protosrv.occultnexus.init.Registrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Fork/backport of Ars Ocultas (mystchonky, LGPL-3.0, see CREDITS.txt) to Forge 1.20.1, extended
 * with a hard dependency on Goety. Porting plan in docs/occult_nexus.md. Sacrificial Altar, the
 * FamiliarDragon bridge, and the Spirit Jar / Soul Gem cross-automation are ported. The Soul Gem
 * pickup/place logic is rewritten against 1.20.1's NBT-based ItemStack API — the 1.21.1 source
 * uses Data Components (Minecraft 1.20.5+), absent here.
 *
 * Constructor takes FMLJavaModLoadingContext rather than IEventBus, matching the same fix already
 * applied in CrypticLifeMod: FMLModContainer only reflects a no-arg constructor or one taking
 * exactly FMLJavaModLoadingContext.
 */
@Mod(OccultNexus.MODID)
public class OccultNexus {

    public static final String MODID = "occult_nexus";

    public OccultNexus(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, OccultNexusConfig.SPEC);
        Registrar.init(context.getModEventBus());
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
