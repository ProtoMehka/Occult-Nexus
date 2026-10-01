package fr.protosrv.occultnexus.content.familiar;

import com.hollingsworth.arsnouveau.api.familiar.AbstractFamiliarHolder;
import com.hollingsworth.arsnouveau.api.familiar.IFamiliar;
import com.klikli_dev.occultism.common.entity.familiar.DragonFamiliarEntity;
import com.klikli_dev.occultism.registry.OccultismEntities;
import fr.protosrv.occultnexus.OccultNexus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class FamiliarDragonHolder extends AbstractFamiliarHolder {

    public FamiliarDragonHolder() {
        super(OccultNexus.prefix(FamiliarLibrary.FAMILIAR_DRAGON), e -> e instanceof DragonFamiliarEntity);
    }

    @Override
    public IFamiliar getSummonEntity(Level world, CompoundTag tag) {
        return new FamiliarDragon(OccultismEntities.DRAGON_FAMILIAR_TYPE.get(), world);
    }

    @Override
    public String getBookName() {
        return "Dragon";
    }

    @Override
    public String getBookDescription() {
        return "Small dragon dude";
    }
}
