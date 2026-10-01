package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.altar.AltarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OccultNexus.MODID);

    public static final RegistryObject<BlockEntityType<AltarBlockEntity>> ALTAR = BLOCK_ENTITY_TYPES.register("altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, BlockRegistrar.ALTAR.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCK_ENTITY_TYPES.register(modBus);
    }
}
