package fr.protosrv.occultnexus.client.model;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.thrallflesh.ThrallfleshEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ThrallfleshModel extends GeoModel<ThrallfleshEntity> {

    @Override
    public ResourceLocation getModelResource(ThrallfleshEntity animatable) {
        return OccultNexus.prefix("geo/thrallflesh.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ThrallfleshEntity animatable) {
        return OccultNexus.prefix("textures/entity/thrallflesh.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ThrallfleshEntity animatable) {
        return OccultNexus.prefix("animations/thrallflesh.animation.json");
    }
}
