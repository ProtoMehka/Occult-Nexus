package fr.protosrv.occultnexus.content.spiritjar;

import com.klikli_dev.occultism.common.container.spirit.SpiritContainer;
import com.klikli_dev.occultism.common.container.spirit.SpiritTransporterContainer;
import com.klikli_dev.occultism.common.entity.job.SpiritJob;
import com.klikli_dev.occultism.common.entity.spirit.SpiritEntity;
import fr.protosrv.occultnexus.init.MenuTypeRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpiritMenuWrapper<T extends MenuProvider> implements MenuProvider {

    private final T menuProvider;
    private final SpiritEntity spirit;

    public SpiritMenuWrapper(T menuProvider, SpiritEntity spirit) {
        this.menuProvider = menuProvider;
        this.spirit = spirit;
    }

    @Override
    public Component getDisplayName() {
        return this.menuProvider.getDisplayName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (this.menuProvider instanceof SpiritJob) {
            return wrappedSpiritTransporter(containerId, playerInventory, this.spirit);
        }
        return wrappedSpirit(containerId, playerInventory, this.spirit);
    }

    public static SpiritContainer wrappedSpirit(int windowId, Inventory inv, SpiritEntity spirit) {
        return new SpiritContainer(windowId, inv, spirit) {
            @Nonnull
            @Override
            public MenuType<?> getType() {
                return MenuTypeRegistrar.SPIRIT_WRAPPER.get();
            }
        };
    }

    public static SpiritTransporterContainer wrappedSpiritTransporter(int windowId, Inventory inv, SpiritEntity spirit) {
        return new SpiritTransporterContainer(windowId, inv, spirit) {
            @Nonnull
            @Override
            public MenuType<?> getType() {
                return MenuTypeRegistrar.SPIRIT_TRANSPORT_WRAPPER.get();
            }
        };
    }
}
