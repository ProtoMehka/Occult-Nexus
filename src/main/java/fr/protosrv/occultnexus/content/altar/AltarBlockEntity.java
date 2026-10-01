package fr.protosrv.occultnexus.content.altar;

import com.hollingsworth.arsnouveau.api.util.SourceUtil;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.klikli_dev.occultism.common.blockentity.GoldenSacrificialBowlBlockEntity;
import com.klikli_dev.occultism.crafting.recipe.RitualRecipe;
import fr.protosrv.occultnexus.foundation.TickingBlockEntity;
import fr.protosrv.occultnexus.init.BlockEntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AltarBlockEntity extends BlockEntity implements TickingBlockEntity {

    private static final int COST = 5000;

    public AltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistrar.ALTAR.get(), pos, blockState);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 300L != 0L) {
            return;
        }
        BlockEntity above = level.getBlockEntity(pos.above());
        if (!(above instanceof GoldenSacrificialBowlBlockEntity bowl)) {
            return;
        }
        // Ars Ocultas 1.21.1 unwraps this via RecipeHolder#value() (Minecraft 1.21+ concept,
        // absent in 1.20.1) — this mod's getCurrentRitualRecipe() already returns RitualRecipe directly.
        RitualRecipe recipe = bowl.getCurrentRitualRecipe();
        if (recipe == null || bowl.sacrificeFulfilled()) {
            return;
        }
        for (BlockPos b : BlockPos.withinManhattan(this.getBlockPos().above(3), 10, 10, 6)) {
            BlockEntity candidate = level.getBlockEntity(b);
            if (!(candidate instanceof MobJarTile mobJarTile)) {
                continue;
            }
            Entity jarEntity = mobJarTile.getEntity();
            if (!(jarEntity instanceof LivingEntity entity)) {
                continue;
            }
            if (!recipe.getRitual().isValidSacrifice(entity)) {
                continue;
            }
            if (SourceUtil.hasSourceNearby(this.getBlockPos(), level, 10, COST)) {
                SourceUtil.takeSourceWithParticles(this.getBlockPos(), level, 10, COST);
                bowl.notifySacrifice(null);
            }
            return;
        }
    }
}
