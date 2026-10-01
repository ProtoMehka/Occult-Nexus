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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Optional;

/**
 * The belly-glyph beam cast during attack_heavy. Anchored to the "body" bone (same bone
 * WraithMagicCircleLayer anchors the back circle to) but on the front/lower side, aimed at the
 * current target. Rendered only during the beam-active window of the animation (ticks 25-45 of the
 * 50-tick attack_heavy clip, i.e. t=1.25s-2.25s — see WraithEntity's HEAVY_ATTACK_ACTIVE_* ticks);
 * a small charge glow shows during the 1s telegraph before it (ticks 5-25).
 *
 * Unlike WraithMagicCircleLayer (which only needs to stay glued to the body bone), this layer also
 * needs a *direction*, from the source point to an arbitrary world-space target position — so
 * rather than drawing directly inside the bone-rotated poseStack (which is in the entity's own
 * local/animated frame, not helpful for aiming at a point elsewhere in the world), it first walks
 * the bone chain purely to extract the source anchor's resulting offset from the entity's own
 * render origin (the poseStack's translation column after the bone transforms), then does the
 * actual beam geometry + aiming in a fresh pose built from that offset and the target's
 * entity-relative offset — both expressed in the same "relative to this entity" space the incoming
 * poseStack is already in (see GeoEntityRenderer#actuallyRender/applyRenderLayers: no extra
 * rotation is applied before render layers run, so local offsets here are plain world-aligned
 * offsets from the entity's render position, safe to combine with a target's own world-relative
 * offset).
 */
public class WraithBeamLayer extends GeoRenderLayer<WraithEntity> {

    private static final ResourceLocation TEXTURE = OccultNexus.prefix("textures/entity/reclaimed_wraith_beam.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);

    // Height confirmed against Blockbench: ProtoMehka placed a pivot point at the belly glyph's
    // actual location, Y=30.5 in the body cube's own absolute coordinate space, i.e. 30.5/16 blocks
    // from this frame's origin — matches the previous estimate (1.9) almost exactly.
    private static final float SOURCE_HEIGHT = 30.5f / 16.0f;
    // Confirmed against Blockbench: the entity faces North by default, and North is -Z in
    // Minecraft/Blockbench convention, so the model's front is negative Z. The earlier flip to
    // positive Z was wrong; reverted.
    private static final float FRONT_OFFSET = -0.6f;

    private static final float BEAM_WIDTH = 0.35f;
    private static final int CHARGE_START_TICK = 5;

    public WraithBeamLayer(GeoRenderer<WraithEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, WraithEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // This layer's only purpose is to sell the homemade beam attack — when WraithEntity is
        // configured to deal the attack_heavy damage via a real Goety spell instead, that spell
        // brings its own particles/sound, so drawing this beam on top of it would just be confusing.
        if (WraithEntity.isUsingGoetySpell()) {
            return;
        }
        int animTicks = animatable.getHeavyAttackAnimTicks();
        if (animTicks < 0) {
            return;
        }
        LivingEntity target = animatable.getBeamTarget();
        if (target == null) {
            return;
        }

        Optional<GeoBone> entityBone = bakedModel.getBone("entity");
        Optional<GeoBone> bodyBone = bakedModel.getBone("body");
        if (entityBone.isEmpty() || bodyBone.isEmpty()) {
            return;
        }

        Vec3 sourceOffset = computeSourceOffset(poseStack, entityBone.get(), bodyBone.get(), animatable, partialTick);

        float progress = animTicks + partialTick;
        int activeStart = WraithEntity.getHeavyAttackActiveStartTick();
        int activeEnd = WraithEntity.getHeavyAttackActiveEndTick();

        if (progress < activeStart) {
            float chargeProgress = Mth.clamp((progress - CHARGE_START_TICK) / (activeStart - CHARGE_START_TICK), 0.0f, 1.0f);
            renderChargeGlow(poseStack, sourceOffset, chargeProgress, bufferSource);
            return;
        }
        if (progress > activeEnd) {
            return;
        }

        Vec3 targetOffset = target.getPosition(partialTick)
                .add(0.0, target.getBbHeight() * 0.5, 0.0)
                .subtract(animatable.getPosition(partialTick));
        renderBeam(poseStack, sourceOffset, targetOffset, progress, bufferSource);
    }

    /** Walks entity -> body (incl. the body cube's own static tilt, same as WraithMagicCircleLayer) purely to read off the resulting local offset of the front-torso anchor point. */
    private static Vec3 computeSourceOffset(PoseStack poseStack, GeoBone entityBone, GeoBone bodyBone, WraithEntity animatable, float partialTick) {
        // The incoming poseStack is already translated to the entity's own camera-relative render
        // position (see the class doc) — that base translation has to be subtracted back out below,
        // otherwise the returned "offset" is actually an absolute position and renderBeam ends up
        // adding the entity's own position into itself a second time.
        Matrix4f base = new Matrix4f((Matrix4fc) poseStack.last().pose());

        poseStack.pushPose();
        float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - lerpBodyRot));
        RenderUtils.prepMatrixForBone(poseStack, entityBone);
        RenderUtils.prepMatrixForBone(poseStack, bodyBone);
        for (GeoCube cube : bodyBone.getCubes()) {
            RenderUtils.translateToPivotPoint(poseStack, cube);
            RenderUtils.rotateMatrixAroundCube(poseStack, cube);
            RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
        }
        poseStack.translate(0.0, SOURCE_HEIGHT, FRONT_OFFSET);

        Matrix4f anchor = poseStack.last().pose();
        Vec3 offset = new Vec3(anchor.m30() - base.m30(), anchor.m31() - base.m31(), anchor.m32() - base.m32());
        poseStack.popPose();
        return offset;
    }

    private void renderBeam(PoseStack poseStack, Vec3 source, Vec3 target, float progress, MultiBufferSource bufferSource) {
        Vec3 direction = target.subtract(source);
        double length = direction.length();
        if (length < 1.0E-3) {
            return;
        }
        direction = direction.normalize();

        poseStack.pushPose();
        poseStack.translate(source.x, source.y, source.z);
        // Same yaw/pitch-from-direction technique as vanilla GuardianRenderer's laser beam: aligns
        // local +Y with the source->target direction, so the beam quads below (drawn along Y) point
        // straight at the target regardless of the entity's own facing.
        float pitch = (float) Math.acos(direction.y);
        float yaw = (float) Math.atan2(direction.z, direction.x);
        poseStack.mulPose(Axis.YP.rotationDegrees((1.5707964f - yaw) * 57.295776f));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 57.295776f));

        VertexConsumer beamBuffer = bufferSource.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        float len = (float) length;
        float half = BEAM_WIDTH * 0.5f;
        // Scrolls the texture along the beam's length over time for a simple "flowing energy" look.
        float scroll = (progress * 0.2f) % 1.0f;
        float v0 = scroll;
        float v1 = len + scroll;

        // Two crossed quads (X-facing and Z-facing) rather than Guardian's full octagonal prism —
        // simpler geometry, ProtoMehka's stated preference over a library-accurate reproduction.
        quad(beamBuffer, positionMatrix, normalMatrix, -half, half, 0, 0, len, v0, v1, 0.0f, 0.0f, 1.0f);
        quad(beamBuffer, positionMatrix, normalMatrix, 0, 0, -half, half, len, v0, v1, 1.0f, 0.0f, 0.0f);

        poseStack.popPose();
    }

    private void renderChargeGlow(PoseStack poseStack, Vec3 source, float chargeProgress, MultiBufferSource bufferSource) {
        float size = 0.12f + 0.28f * chargeProgress;
        poseStack.pushPose();
        poseStack.translate(source.x, source.y, source.z);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f positionMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        vertex(buffer, positionMatrix, normalMatrix, -size, -size, 0, 0.0f, 1.0f, 0, 0, 1);
        vertex(buffer, positionMatrix, normalMatrix, size, -size, 0, 1.0f, 1.0f, 0, 0, 1);
        vertex(buffer, positionMatrix, normalMatrix, size, size, 0, 1.0f, 0.0f, 0, 0, 1);
        vertex(buffer, positionMatrix, normalMatrix, -size, size, 0, 0.0f, 0.0f, 0, 0, 1);

        poseStack.popPose();
    }

    /**
     * Draws one quad spanning local Y in [0, length] ("along the beam", from the direction rotation
     * already applied by the caller), with its perpendicular width going from (widthA0, widthA1) on
     * whichever single axis (X or Z) the caller passes as the fixed one — the other stays at 0.
     */
    private static void quad(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix, float widthA0, float widthA1, float widthB0, float widthB1, float length, float v0, float v1, float nx, float ny, float nz) {
        vertex(buffer, positionMatrix, normalMatrix, widthA0, 0, widthB0, 0.0f, v0, nx, ny, nz);
        vertex(buffer, positionMatrix, normalMatrix, widthA1, 0, widthB1, 1.0f, v0, nx, ny, nz);
        vertex(buffer, positionMatrix, normalMatrix, widthA1, length, widthB1, 1.0f, v1, nx, ny, nz);
        vertex(buffer, positionMatrix, normalMatrix, widthA0, length, widthB0, 0.0f, v1, nx, ny, nz);
    }

    // Full-bright, same dimming convention as WraithMagicCircleLayer.
    private static final int DIMMED_COLOR = 180;

    private static void vertex(VertexConsumer buffer, Matrix4f positionMatrix, Matrix3f normalMatrix, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.vertex(positionMatrix, x, y, z)
                .color(DIMMED_COLOR, DIMMED_COLOR, DIMMED_COLOR, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }
}
