package fr.protosrv.occultnexus.content.familiar;

import com.hollingsworth.arsnouveau.api.familiar.IFamiliar;
import com.klikli_dev.occultism.common.entity.familiar.DragonFamiliarEntity;
import com.klikli_dev.occultism.registry.OccultismEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class FamiliarDragon extends DragonFamiliarEntity implements IFamiliar {

    public ResourceLocation holderID;

    public FamiliarDragon(EntityType<? extends DragonFamiliarEntity> ent, Level world) {
        super(ent, world);
    }

    @Override
    public EntityType<?> getType() {
        return OccultismEntities.DRAGON_FAMILIAR.get();
    }

    @Override
    public ResourceLocation getHolderID() {
        return this.holderID;
    }

    @Override
    public void setHolderID(ResourceLocation id) {
        this.holderID = id;
    }

    @Override
    public UUID getOwnerID() {
        return this.getOwnerId();
    }

    @Override
    public void setOwnerID(UUID uuid) {
        this.setFamiliarOwner((LivingEntity) ((ServerLevel) this.level()).getEntity(uuid));
    }
}
