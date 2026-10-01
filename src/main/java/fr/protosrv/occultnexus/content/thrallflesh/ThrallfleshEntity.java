package fr.protosrv.occultnexus.content.thrallflesh;

import com.klikli_dev.occultism.common.entity.familiar.FamiliarEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.List;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Occult Nexus signature creature #2, docs/occult_nexus.md Partie C + docs/thrallflesh_brief.md.
 * Occultism -> Goety bridge, melee/tank counterpart to Reclaimed Wraith: a spirit forced into a
 * physical vessel of flesh and cursed metal instead of staying disembodied.
 *
 * Extends Occultism's own FamiliarEntity (not vanilla Monster) — this is load-bearing, not
 * cosmetic: FamiliarRitual#finish() (verified by decompiling Occultism) only calls
 * setFamiliarOwner(...) and actually spawns the entity in the world if it's an `instanceof
 * FamiliarEntity`. A plain Monster would silently fail to bind to the caster or appear at all.
 */
public class ThrallfleshEntity extends FamiliarEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_LIGHT = RawAnimation.begin().thenPlay("attack_light");
    private static final RawAnimation ATTACK_HEAVY = RawAnimation.begin().thenPlay("attack_heavy");

    /**
     * Vanilla EvokerFangs (used for the visual, per request — Goety's own fangs didn't read well)
     * deal a hardcoded 6.0 damage with no way to configure it, unlike Goety's Fangs. To keep the
     * agreed ~16 total for the heavy attack, damage is dealt directly to the current target once,
     * decoupled from however many of the spawned fangs actually end up overlapping it.
     */
    private static final float HEAVY_ATTACK_DIRECT_DAMAGE = 16.0f;
    private static final int HEAVY_ATTACK_COOLDOWN_TICKS = 140;
    private static final int HEAVY_ATTACK_FANG_COUNT = 6;

    // Passive out-of-combat regen (also on WraithEntity, same values) — ProtoMehka asked for both
    // summon creatures to slowly heal back up once they've gone a while without taking a hit,
    // rather than staying wherever a fight left them until the owner heals them some other way.
    private static final int PASSIVE_REGEN_DELAY_TICKS = 200; // 10s without damage before it starts
    private static final int PASSIVE_REGEN_INTERVAL_TICKS = 40; // then +1 HP every 2s
    private static final float PASSIVE_REGEN_AMOUNT = 1.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int heavyAttackCooldown;
    private int noDamageTicks;

    public ThrallfleshEntity(EntityType<? extends ThrallfleshEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FamiliarEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThrallfleshHeavyAttackGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        // Both provided by FamiliarEntity itself (shift-right-click toggles sitting; teleports to
        // the owner if left too far behind) — same pattern DragonFamiliarEntity uses.
        // SitGoal is a non-static inner class of FamiliarEntity (needs an enclosing-instance
        // qualifier); ThrallfleshEntity extends FamiliarEntity, so `this` doubles as both.
        this.goalSelector.addGoal(3, this.new SitGoal(this));
        this.goalSelector.addGoal(4, new FamiliarEntity.FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // Bound familiar, not a roaming hostile: it only fights back or joins its owner's fights —
        // it must never target players (including its own owner) the way a plain Monster would.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new ThrallfleshDefendOwnerGoal());
    }

    @Override
    public Iterable<MobEffectInstance> getFamiliarEffects() {
        return List.of();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            this.triggerAnim("attack_controller", "attack_light");
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.heavyAttackCooldown > 0) {
            this.heavyAttackCooldown--;
        }
        if (!this.level().isClientSide) {
            this.noDamageTicks++;
            if (this.noDamageTicks >= PASSIVE_REGEN_DELAY_TICKS
                    && this.noDamageTicks % PASSIVE_REGEN_INTERVAL_TICKS == 0
                    && this.isAlive() && this.getHealth() < this.getMaxHealth()) {
                this.heal(PASSIVE_REGEN_AMOUNT);
            }
        }
    }

    @Override
    protected void actuallyHurt(DamageSource damageSource, float amount) {
        super.actuallyHurt(damageSource, amount);
        this.noDamageTicks = 0;
    }

    private void spawnFangsTowardTarget() {
        LivingEntity target = this.getTarget();
        if (!(this.level() instanceof ServerLevel serverLevel) || target == null) {
            return;
        }
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        // Same convention as Goety's own FangSpell/WandUtil.spawnFangs (verified via
        // decompilation): yRot in radians from atan2(dz, dx), stepped position via cos/sin of it.
        // Vanilla EvokerFangs' constructor takes the same (Level, x, y, z, yRot, warmup, owner)
        // shape, just without a damage parameter.
        float yRot = (float) Math.atan2(dz, dx);
        for (int i = 0; i < HEAVY_ATTACK_FANG_COUNT; i++) {
            double distance = 1.25 * (i + 1);
            double x = this.getX() + Math.cos(yRot) * distance;
            double z = this.getZ() + Math.sin(yRot) * distance;
            EvokerFangs fangs = new EvokerFangs(serverLevel, x, this.getY(), z, yRot, i, this);
            serverLevel.addFreshEntity(fangs);
        }
        target.hurt(this.damageSources().mobAttack(this), HEAVY_ATTACK_DIRECT_DAMAGE);
    }

    /** Triggered from the "spawn_fangs" custom instruction keyframe on the attack_heavy clip — see ThrallfleshModel/RegisterEvents wiring. */
    public void onSpawnFangsKeyframe() {
        this.spawnFangsTowardTarget();
    }

    private class ThrallfleshHeavyAttackGoal extends Goal {

        @Override
        public boolean canUse() {
            LivingEntity target = ThrallfleshEntity.this.getTarget();
            return target != null && target.isAlive() && ThrallfleshEntity.this.heavyAttackCooldown <= 0
                    && ThrallfleshEntity.this.distanceToSqr(target) > 9.0;
        }

        @Override
        public void start() {
            ThrallfleshEntity.this.heavyAttackCooldown = HEAVY_ATTACK_COOLDOWN_TICKS;
            ThrallfleshEntity.this.triggerAnim("attack_controller", "attack_heavy");
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }
    }

    /** Vanilla's OwnerHurtByTargetGoal/OwnerHurtTargetGoal are typed to TamableAnimal specifically — FamiliarEntity uses its own separate ownership system (getFamiliarOwner()), so this is a small equivalent for that system instead. */
    private class ThrallfleshDefendOwnerGoal extends Goal {

        @Override
        public boolean canUse() {
            LivingEntity owner = ThrallfleshEntity.this.getFamiliarOwner();
            if (owner == null) {
                return false;
            }
            LivingEntity ownerTarget = owner.getLastHurtByMob();
            LivingEntity ownerAttacking = owner.getLastHurtMob();
            return (ownerTarget != null && ownerTarget.isAlive()) || (ownerAttacking != null && ownerAttacking.isAlive());
        }

        @Override
        public void start() {
            LivingEntity owner = ThrallfleshEntity.this.getFamiliarOwner();
            if (owner == null) {
                return;
            }
            LivingEntity ownerTarget = owner.getLastHurtByMob();
            if (ownerTarget != null && ownerTarget.isAlive()) {
                ThrallfleshEntity.this.setTarget(ownerTarget);
                return;
            }
            LivingEntity ownerAttacking = owner.getLastHurtMob();
            if (ownerAttacking != null && ownerAttacking.isAlive()) {
                ThrallfleshEntity.this.setTarget(ownerAttacking);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attack_controller", 0, state -> PlayState.STOP)
                .triggerableAnim("attack_light", ATTACK_LIGHT)
                .triggerableAnim("attack_heavy", ATTACK_HEAVY)
                .setCustomInstructionKeyframeHandler(event -> {
                    if ("spawn_fangs".equals(event.getKeyframeData().getInstructions())) {
                        ThrallfleshEntity.this.onSpawnFangsKeyframe();
                    }
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
