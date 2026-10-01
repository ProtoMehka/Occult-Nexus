package fr.protosrv.occultnexus.foundation;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

public class BlockHelper {

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> typeA, BlockEntityType<E> typeB, BlockEntityTicker<? super E> ticker) {
        return typeB == typeA ? (BlockEntityTicker<A>) ticker : null;
    }

    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> getTicker(
            BlockEntityType<A> typeA, BlockEntityType<E> typeB) {
        return createTicker(typeA, typeB, (level, pos, state, blockEntity) -> ((TickingBlockEntity) blockEntity).tick(level, pos, state));
    }
}
