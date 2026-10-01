package fr.protosrv.occultnexus.init;

import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.klikli_dev.occultism.client.gui.spirit.SpiritGui;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.client.renderer.ThrallfleshRenderer;
import fr.protosrv.occultnexus.client.renderer.WraithRenderer;
import fr.protosrv.occultnexus.content.spiritjar.SpiritScreenWrapper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import javax.annotation.Nonnull;

/**
 * Exposes a jarred SpiritEntity's own item handler capability through its MobJarTile, so hoppers
 * and pipes reaching the jar block can pull from / push into the spirit's inventory. Ars Ocultas
 * 1.21.1 does this via RegisterCapabilitiesEvent (NeoForge's newer unified capability registry,
 * absent in Forge 1.20.1) — the equivalent here is AttachCapabilitiesEvent<BlockEntity>.
 */
@Mod.EventBusSubscriber(modid = OccultNexus.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RegisterEvents {

    @SubscribeEvent
    public static void attachItemCapability(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof MobJarTile jar)) {
            return;
        }
        event.addCapability(OccultNexus.prefix("jarred_spirit_item_handler"), new ICapabilityProvider() {
            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
                if (cap != ForgeCapabilities.ITEM_HANDLER) {
                    return LazyOptional.empty();
                }
                Entity jarEntity = jar.getEntity();
                if (jarEntity instanceof SpiritEntity spirit) {
                    return spirit.getCapability(cap, side);
                }
                return LazyOptional.empty();
            }
        });
    }

    @Mod.EventBusSubscriber(modid = OccultNexus.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientRegistration {
        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(MenuTypeRegistrar.SPIRIT_WRAPPER.get(), SpiritGui<com.klikli_dev.occultism.common.container.spirit.SpiritContainer>::new);
                MenuScreens.register(MenuTypeRegistrar.SPIRIT_TRANSPORT_WRAPPER.get(), SpiritScreenWrapper::wrapTransporterGui);
            });
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EntityRegistrar.THRALLFLESH.get(), ThrallfleshRenderer::new);
            event.registerEntityRenderer(EntityRegistrar.RECLAIMED_WRAITH.get(), WraithRenderer::new);
        }
    }

    private RegisterEvents() {
    }
}
