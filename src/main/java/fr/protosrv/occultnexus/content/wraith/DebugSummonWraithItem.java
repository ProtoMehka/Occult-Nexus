package fr.protosrv.occultnexus.content.wraith;

import fr.protosrv.occultnexus.init.EntityRegistrar;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Debug-only quick summon, mirrors DebugSummonThrallfleshItem — the vanilla spawn egg alone
 * would leave getFamiliarOwner() null, which FamiliarEntity AI doesn't expect.
 */
public class DebugSummonWraithItem extends Item {

    public DebugSummonWraithItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel) || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        WraithEntity wraith = EntityRegistrar.RECLAIMED_WRAITH.get().create(serverLevel);
        if (wraith == null) {
            return InteractionResult.PASS;
        }
        wraith.setFamiliarOwner(context.getPlayer());
        wraith.moveTo(context.getClickedPos().getX() + 0.5, context.getClickedPos().getY() + 1.0, context.getClickedPos().getZ() + 0.5, 0.0f, 0.0f);
        Entity entity = wraith;
        serverLevel.addFreshEntity(entity);
        return InteractionResult.SUCCESS;
    }
}
