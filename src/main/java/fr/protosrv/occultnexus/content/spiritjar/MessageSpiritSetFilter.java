package fr.protosrv.occultnexus.content.spiritjar;

import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MessageSpiritSetFilter(boolean isBlacklistFilter, BlockPos blockPos) {

    public static void encode(MessageSpiritSetFilter message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.isBlacklistFilter());
        buf.writeBlockPos(message.blockPos());
    }

    public static MessageSpiritSetFilter decode(FriendlyByteBuf buf) {
        return new MessageSpiritSetFilter(buf.readBoolean(), buf.readBlockPos());
    }

    public static void handle(MessageSpiritSetFilter message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(message.blockPos());
            if (!(blockEntity instanceof MobJarTile jar)) {
                return;
            }
            Entity jarEntity = jar.getEntity();
            if (jarEntity instanceof SpiritEntity spirit) {
                spirit.setFilterBlacklist(message.isBlacklistFilter());
            }
        });
        context.setPacketHandled(true);
    }
}
