package fr.protosrv.occultnexus.client.renderer;

import fr.protosrv.occultnexus.client.model.WraithModel;
import fr.protosrv.occultnexus.content.wraith.WraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WraithRenderer extends GeoEntityRenderer<WraithEntity> {

    // Ticks (matches WraithEntity's own LIGHT_ATTACK_ANIM_TICKS, not exposed as a public constant).
    private static final float LIGHT_ATTACK_ANIM_TICKS = 10.0f;
    // 3 full oscillations across the 0.5s lunge — a fast "phasing" flicker rather than a slow fade.
    // Time-based and simple to retune (frequency/amplitude below) if it reads too fast/slow in game.
    private static final float FLICKER_CYCLES = 3.0f;
    private static final float FLICKER_BASE_ALPHA = 0.8f;
    private static final float FLICKER_AMPLITUDE = 0.2f; // -> alpha oscillates across ~60%-100%

    public WraithRenderer(EntityRendererProvider.Context context) {
        super(context, new WraithModel());
        // Renders the "glowing" layer baked from reclaimed_wraith_glowmask.png (teal belly glyph
        // + glow dots), auto-detected by GeckoLib as a sibling file next to the base texture.
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.addRenderLayer(new WraithMagicCircleLayer(this));
        this.addRenderLayer(new WraithBeamLayer(this));
    }

    @Override
    public Color getRenderColor(WraithEntity animatable, float partialTick, int packedLight) {
        int ticks = animatable.getLightAttackAnimTicks();
        if (ticks < 0) {
            return Color.WHITE;
        }
        float progress = (ticks + partialTick) / LIGHT_ATTACK_ANIM_TICKS;
        float oscillation = (float) Math.sin(progress * Math.PI * 2.0 * FLICKER_CYCLES);
        float alpha = FLICKER_BASE_ALPHA + FLICKER_AMPLITUDE * oscillation;
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
    }
}
