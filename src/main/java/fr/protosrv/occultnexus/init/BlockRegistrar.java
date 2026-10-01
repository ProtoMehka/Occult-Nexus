package fr.protosrv.occultnexus.init;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.altar.AltarBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistrar {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OccultNexus.MODID);
    private static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OccultNexus.MODID);

    public static final RegistryObject<AltarBlock> ALTAR = BLOCKS.register("altar", () -> new AltarBlock(BlockBehaviour.Properties.of()));
    private static final RegistryObject<BlockItem> ALTAR_ITEM = BLOCK_ITEMS.register("altar", () -> new BlockItem(ALTAR.get(), new Item.Properties()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }
}
