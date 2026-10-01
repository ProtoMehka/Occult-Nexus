package fr.protosrv.occultnexus.content.spiritjar;

import com.klikli_dev.occultism.client.gui.spirit.SpiritTransporterGui;
import com.klikli_dev.occultism.common.container.spirit.SpiritTransporterContainer;
import fr.protosrv.occultnexus.foundation.network.OccultNexusNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpiritScreenWrapper {

    public static SpiritTransporterGui wrapTransporterGui(SpiritTransporterContainer container, Inventory playerInventory, Component titleIn) {
        return new SpiritTransporterGui(container, playerInventory, titleIn) {
            @Override
            public void setIsBlacklist(boolean isBlacklist) {
                super.setIsBlacklist(isBlacklist);
                OccultNexusNetwork.CHANNEL.sendToServer(new MessageSpiritSetFilter(isBlacklist, this.spirit.blockPosition()));
            }
        };
    }
}
