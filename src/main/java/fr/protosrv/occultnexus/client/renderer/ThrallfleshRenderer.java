package fr.protosrv.occultnexus.client.renderer;

import fr.protosrv.occultnexus.client.model.ThrallfleshModel;
import fr.protosrv.occultnexus.content.thrallflesh.ThrallfleshEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ThrallfleshRenderer extends GeoEntityRenderer<ThrallfleshEntity> {

    public ThrallfleshRenderer(EntityRendererProvider.Context context) {
        super(context, new ThrallfleshModel());
        // EntityType#sized() only affects the hitbox, not the GeckoLib model's rendered size —
        // this is the actual visual scale-up.
        this.withScale(1.8f);
    }
}
