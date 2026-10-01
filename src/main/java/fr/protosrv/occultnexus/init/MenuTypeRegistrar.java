package fr.protosrv.occultnexus.init;

import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.klikli_dev.occultism.common.container.spirit.SpiritContainer;
import com.klikli_dev.occultism.common.container.spirit.SpiritTransporterContainer;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.spiritjar.SpiritMenuWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class MenuTypeRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, OccultNexus.MODID);

    public static final RegistryObject<MenuType<SpiritContainer>> SPIRIT_WRAPPER = MENU_TYPES.register("spirit_wrapper",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                MobJarTile tile = (MobJarTile) Minecraft.getInstance().level.getBlockEntity(pos);
                Entity entity = tile.getEntity();
                return SpiritMenuWrapper.wrappedSpirit(windowId, inv, (SpiritEntity) entity);
            }));

    public static final RegistryObject<MenuType<SpiritTransporterContainer>> SPIRIT_TRANSPORT_WRAPPER = MENU_TYPES.register("spirit_transport_wrapper",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                MobJarTile tile = (MobJarTile) Minecraft.getInstance().level.getBlockEntity(pos);
                Entity entity = tile.getEntity();
                return SpiritMenuWrapper.wrappedSpiritTransporter(windowId, inv, (SpiritEntity) entity);
            }));

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
