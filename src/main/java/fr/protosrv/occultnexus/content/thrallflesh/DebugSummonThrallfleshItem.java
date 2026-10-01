package fr.protosrv.occultnexus.content.thrallflesh;

import fr.protosrv.occultnexus.init.EntityRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Debug-only quick summon, same idea as Occultism's own debug/*Item classes (e.g.
 * SummonFoliotCleanerItem) — instantly creates and binds a Thrallflesh to the clicking player
 * without going through the full ritual, for testing. Not meant to be craftable/obtainable in
 * normal play — creative-inventory only, like Occultism's own equivalents.
 */
public class DebugSummonThrallfleshItem extends Item {

    public DebugSummonThrallfleshItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel) || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        ThrallfleshEntity thrallflesh = EntityRegistrar.THRALLFLESH.get().create(serverLevel);
        if (thrallflesh == null) {
            return InteractionResult.PASS;
        }
        thrallflesh.setFamiliarOwner(context.getPlayer());
        thrallflesh.moveTo(context.getClickedPos().getX() + 0.5, context.getClickedPos().getY() + 1.0, context.getClickedPos().getZ() + 0.5, 0.0f, 0.0f);
        thrallflesh.setCustomName(Component.literal("Debug Thrallflesh"));
        Entity entity = thrallflesh;
        serverLevel.addFreshEntity(entity);
        return InteractionResult.SUCCESS;
    }
}
