package fr.protosrv.occultnexus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.protosrv.occultnexus.OccultNexus;
import fr.protosrv.occultnexus.content.wraith.WraithEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Optional;

/**
 * The floating rune circle behind the Wraith's back, spinning continuously in its own plane.
 * A flat quad drawn directly rather than modeled in Blockbench, per ProtoMehka's preference.
 */
public class WraithMagicCircleLayer extends GeoRenderLayer<WraithEntity> {

    private static final ResourceLocation TEXTURE = OccultNexus.prefix("textures/entity/reclaimed_wraith_circle.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final float RADIUS = 0.9f;
    // Chest-height and behind-the-back offset in the "body" bone's own local frame (block units,
    // same convention RenderUtils uses — see prepMatrixForBone). Approximate: no in-game preview
    // was possible while writing this, nudge if it still clips or floats too far off the back.
    private static final float CHEST_HEIGHT = 2.2f;
    private static final float BACK_OFFSET = 0.75f;
    private static final float SPIN_DEGREES_PER_TICK = 3.0f;

    public WraithMagicCircleLayer(GeoRenderer<WraithEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, WraithEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        Optional<GeoBone> entityBone = bakedModel.getBone("entity");
        Optional<GeoBone> bodyBone = bakedModel.getBone("body");
        if (entityBone.isEmpty() || bodyBone.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        // GeoEntityRenderer#actuallyRender applies the body-yaw rotation and pops it again before
        // whole-model render layers (this one) run, so it has to be reapplied here to track the
        // entity's facing — see GeoEntityRenderer#applyRotations for the equivalent vanilla logic.
        float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - lerpBodyRot));
        // Walk the same bone chain renderRecursively uses (entity -> body) so the circle inherits
        // the body bone's own animated rotation too — e.g. the forward tilt during attack_heavy.
        RenderUtils.prepMatrixForBone(poseStack, entityBone.get());
        RenderUtils.prepMatrixForBone(poseStack, bodyBone.get());
        // The torso's resting tilt (12.5 degrees) is baked into the body cube's own rotation, not
        // the bone's — GeoBone#getRotX() alone misses it, so replicate GeoRenderer#renderCube's
        // pivot/rotation handling for that cube too.
        for (GeoCube cube : bodyBone.get().getCubes()) {
            RenderUtils.translateToPivotPoint(poseStack, cube);
            RenderUtils.rotateMatrixAroundCube(poseStack, cube);
            RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
        }

        poseStack.translate(0.0, CHEST_HEIGHT, BACK_OFFSET);
        float spin = (animatable.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

        VertexConsumer circleBuffer = bufferSource.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        vertex(circleBuffer, positionMatrix, normalMatrix, -RADIUS, -RADIUS, 0.0f, 1.0f);
        vertex(circleBuffer, positionMatrix, normalMatrix, RADIUS, -RADIUS, 1.0f, 1.0f);
        vertex(circleBuffer, positionMatrix, normalMatrix, RADIUS, RADIUS, 1.0f, 0.0f);
        vertex(circleBuffer, positionMatrix, normalMatrix, -RADIUS, RADIUS, 0.0f, 0.0f);

        poseStack.popPose();
    }

    // Full-bright (uv2 below) already makes this glow regardless of ambient light; dimming the
    // vertex color tones down how blown-out that reads instead of fighting the light level.
    private static final int DIMMED_COLOR = 150;

    private static void vertex(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix, float x, float y, float u, float v) {
        buffer.vertex(positionMatrix, x, y, 0.0f)
                .color(DIMMED_COLOR, DIMMED_COLOR, DIMMED_COLOR, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(normalMatrix, 0.0f, 0.0f, 1.0f)
                .endVertex();
    }
}
