package fr.protosrv.occultnexus.content.spiritjar;

import com.hollingsworth.arsnouveau.api.item.inv.InventoryManager;
import com.hollingsworth.arsnouveau.api.item.inv.MultiInsertReference;
import com.hollingsworth.arsnouveau.api.mob_jar.JarBehavior;
import com.hollingsworth.arsnouveau.api.util.InvUtil;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.klikli_dev.occultism.api.OccultismAPI;
import com.klikli_dev.occultism.common.entity.job.CleanerJob;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Lets a SpiritEntity (Occultism) live inside a Mob Jar (Ars Nouveau) and keep doing its job from
 * there. Ars Ocultas 1.21.1 additionally redirects CrusherJob/SmelterJob/TraderJob output into
 * nearby inventories via Forge sub-events on those job classes — SmelterJob/CrystallizerJob don't
 * exist at all in this Occultism build (1.20.1-1.158.0), and CrusherJob/TraderJob have no such
 * nested event class either, so that redirect isn't portable here. Only the Cleaner-job redirect
 * (handled directly in tick() below, no external event needed) is kept.
 */
public class SpiritJarBehaviour<T extends SpiritEntity> extends JarBehavior<T> {

    private static final String TRANSLATION_KEY_BASE = "gui.occultism.spirit";

    @Override
    public void use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, MobJarTile tile) {
        if (world.isClientSide) {
            return;
        }
        ItemStack heldStack = player.getItemInHand(handIn);
        SpiritEntity spirit = this.entityFromJar(tile);
        if (player.isSecondaryUseActive() && heldStack.isEmpty()) {
            this.openScreen(player, spirit, tile);
            return;
        }
        if (canAcceptItemStack(spirit, heldStack)) {
            ItemStack duplicate = heldStack.copy();
            if (ItemHandlerHelper.insertItemStacked(spirit.inventory, duplicate, true).getCount() < duplicate.getCount()) {
                ItemStack remaining = ItemHandlerHelper.insertItemStacked(spirit.inventory, duplicate, false);
                heldStack.setCount(remaining.getCount());
            }
        }
    }

    @Override
    public void tick(MobJarTile tile) {
        Level level = tile.getLevel();
        if (level.isClientSide) {
            return;
        }
        SpiritEntity spirit = this.entityFromJar(tile);
        if (!spirit.isInitialized()) {
            spirit.init();
        }
        if (level.getGameTime() % 40L == 0L) {
            List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, new AABB(tile.getBlockPos()).inflate(3.0), Entity::isAlive);
            if (!itemEntities.isEmpty()) {
                ItemEntity itemEntity = itemEntities.stream()
                        .filter(item -> OccultismAPI.get().canPickupItem(spirit, item).orElse(false))
                        .findFirst()
                        .orElse(null);
                if (itemEntity != null) {
                    ItemStack duplicate = itemEntity.getItem().copy();
                    if (ItemHandlerHelper.insertItemStacked(spirit.inventory, duplicate, true).getCount() < duplicate.getCount()) {
                        ItemStack remaining = ItemHandlerHelper.insertItemStacked(spirit.inventory, duplicate, false);
                        itemEntity.getItem().setCount(remaining.getCount());
                    }
                }
            }
        }
        spirit.getJob().ifPresent(spiritJob -> {
            if (spiritJob instanceof CleanerJob) {
                ItemStack input = spirit.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack remainder = this.tryItemNearbyTransfer(tile, level, input.copy());
                spirit.setItemInHand(InteractionHand.MAIN_HAND, remainder);
            }
            spiritJob.update();
        });
    }

    private boolean canAcceptItemStack(SpiritEntity spirit, ItemStack stack) {
        OccultismAPI api = OccultismAPI.get();
        return api.getItemsToPickUp(spirit).map(ingredients -> ingredients.stream().anyMatch(item -> item.test(stack))).orElse(false);
    }

    @Override
    public void getTooltip(MobJarTile tile, List<Component> tooltips) {
        super.getTooltip(tile, tooltips);
        SpiritEntity spirit = this.entityFromJar(tile);
        String job = spirit.getJobID();
        if (!StringUtils.isBlank(job)) {
            job = job.replace(":", ".");
            String jobText = I18n.get(TRANSLATION_KEY_BASE + ".job", I18n.get("job." + job));
            tooltips.add(Component.translatable(jobText).withStyle(ChatFormatting.GOLD));
        }
        tooltips.add(spirit.getDisplayName());
        if (spirit.hasCustomName()) {
            MutableComponent name = spirit.getType().getDescription().copy();
            name.withStyle(ChatFormatting.GRAY);
            tooltips.add(name);
        }
    }

    public void openScreen(Player playerEntity, SpiritEntity spirit, MobJarTile tile) {
        Object currentJob = spirit.getJob().orElse(null);
        SpiritMenuWrapper<?> menuProvider = currentJob instanceof MenuProvider jobMenuProvider
                ? new SpiritMenuWrapper<>(jobMenuProvider, spirit)
                : new SpiritMenuWrapper<>(spirit, spirit);
        if (playerEntity instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, menuProvider, tile.getBlockPos());
        }
    }

    public ItemStack tryItemNearbyTransfer(MobJarTile tile, Level level, ItemStack input) {
        List<com.hollingsworth.arsnouveau.api.item.inv.FilterableItemHandler> inventories = InvUtil.adjacentInventories(level, tile.getBlockPos());
        if (inventories.isEmpty()) {
            return input;
        }
        InventoryManager manager = new InventoryManager(inventories);
        MultiInsertReference reference = manager.insertStackWithReference(input);
        if (!reference.isEmpty()) {
            return reference.getRemainder();
        }
        return input;
    }
}
