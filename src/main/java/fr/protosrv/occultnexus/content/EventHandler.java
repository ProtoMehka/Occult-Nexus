package fr.protosrv.occultnexus.content;

import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.hollingsworth.arsnouveau.common.items.MobJarItem;
import com.hollingsworth.arsnouveau.common.lib.EntityTags;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import com.klikli_dev.occultism.common.item.tool.SoulGemItem;
import com.klikli_dev.occultism.registry.OccultismItems;
import com.klikli_dev.occultism.registry.OccultismTags;
import com.klikli_dev.occultism.util.EntityUtil;
import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.OccultNexusConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Occultism Soul Gem <-> Ars Nouveau Mob Jar interaction. Ars Ocultas 1.21.1 stores the captured
 * entity via Minecraft's Data Components API (DataComponents.ENTITY_DATA / CustomData), which
 * doesn't exist before 1.20.5 — rewritten here against plain NBT, using the exact "entityData" tag
 * key Occultism's own SoulGemItem already uses natively for its Containment Jar interaction, so a
 * gem captured through either path stays interchangeable.
 *
 * Trinity Gem handling from the 1.21.1 source is dropped entirely: OccultismItems.TRINITY_GEM_ITEM
 * and OccultismTags.Entities.TRINITY_GEM_DENY_LIST don't exist in this Occultism build (1.158.0) —
 * that item was added in a later Occultism version than the one installed in this pack.
 */
@Mod.EventBusSubscriber(modid = OccultNexus.MODID)
public class EventHandler {

    private static final String TRANSLATION_KEY_BASE = "gui.occultism.spirit";

    @SubscribeEvent
    public static void itemTooltips(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        Level level = player.level();
        Entity fromItem = MobJarItem.fromItem(stack, level);
        if (!(fromItem instanceof SpiritEntity spirit)) {
            return;
        }
        String job = spirit.getJobID();
        if (StringUtils.isBlank(job)) {
            return;
        }
        job = job.replace(":", ".");
        List<Component> tooltips = event.getToolTip();
        String jobText = I18n.get(TRANSLATION_KEY_BASE + ".job", I18n.get("job." + job));
        tooltips.add(Component.translatable(jobText).withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void playerRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SoulGemItem)) {
            return;
        }
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (!(be instanceof MobJarTile jar)) {
            return;
        }
        CompoundTag stackTag = stack.getTag();
        boolean hasGemData = stackTag != null && stackTag.contains("entityData");
        Entity jarEntity = jar.getEntity();

        if (!hasGemData && jarEntity != null) {
            if (!OccultNexusConfig.CONTAINMENT_JARS_SOUL_GEM_PICKUP.get()) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            EntityType<?> type = jarEntity.getType();
            if (type.is(EntityTags.JAR_RELEASE_BLACKLIST) || (!type.is(EntityTags.JAR_WHITELIST) && type.is(EntityTags.JAR_BLACKLIST))) {
                player.sendSystemMessage(Component.translatable(stack.getDescriptionId() + ".message.entity_type_denied"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            if (type.is(OccultismTags.Entities.SOUL_GEM_DENY_LIST) && stack.getItem() == OccultismItems.SOUL_GEM_ITEM.get()) {
                player.sendSystemMessage(Component.translatable(stack.getDescriptionId() + ".message.entity_type_denied"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            CompoundTag entityData = jarEntity.serializeNBT();
            stack.getOrCreateTag().put("entityData", entityData);
            jar.removeEntity();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (hasGemData && jarEntity == null) {
            if (!OccultNexusConfig.CONTAINMENT_JARS_SOUL_GEM_PLACE.get()) {
                return;
            }
            CompoundTag entityData = stackTag.getCompound("entityData");
            stackTag.remove("entityData");
            EntityType<?> type = EntityUtil.entityTypeFromNbt(entityData);
            if (!type.is(EntityTags.JAR_WHITELIST) && type.is(EntityTags.JAR_BLACKLIST)) {
                player.sendSystemMessage(Component.translatable(stack.getDescriptionId() + ".message.entity_type_denied"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
            if (type.is(OccultismTags.Entities.SOUL_GEM_DENY_LIST) && stack.getItem() == OccultismItems.SOUL_GEM_ITEM.get()) {
                player.sendSystemMessage(Component.translatable(stack.getDescriptionId() + ".message.entity_type_denied"));
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            Entity entity = type.create(level);
            if (entity == null) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
            entity.load(entityData);
            jar.setEntityData(entity);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
