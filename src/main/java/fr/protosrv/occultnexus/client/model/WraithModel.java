package fr.protosrv.occultnexus.client.model;

import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.wraith.WraithEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WraithModel extends GeoModel<WraithEntity> {

    @Override
    public ResourceLocation getModelResource(WraithEntity animatable) {
        return OccultNexus.prefix("geo/reclaimed_wraith.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WraithEntity animatable) {
        return OccultNexus.prefix("textures/entity/reclaimed_wraith.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WraithEntity animatable) {
        return OccultNexus.prefix("animations/reclaimed_wraith.animation.json");
    }

    @Override
    public RenderType getRenderType(WraithEntity animatable, ResourceLocation texture) {
        // GeoModel's own default (entityCutoutNoCull) renders with NO_TRANSPARENCY — alpha values
        // modulated via WraithRenderer#getRenderColor would have no visible effect on it (the
        // fragment is either fully opaque or discarded past the cutout threshold, no blending in
        // between). Real translucency for the attack_light flicker needs an actual blending render
        // type, so this only switches to it during the flicker window and falls back otherwise.
        if (animatable.getLightAttackAnimTicks() >= 0) {
            return RenderType.entityTranslucent(texture);
        }
        return RenderType.entityCutoutNoCull(texture);
    }
}
