package fr.protosrv.occultnexus.content.wraith;

import com.Polarice3.Goety.common.magic.spells.necromancy.LeechingSpell;
import com.klikli_dev.occultism.common.entity.familiar.FamiliarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.EnumSet;
import java.util.List;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Occult Nexus signature creature #1, docs/occult_nexus.md Partie C — ranged spell-caster
 * counterpart to Thrallflesh, sourced from a Goety Servant's soul instead of an Occultism familiar.
 *
 * Extends Occultism's FamiliarEntity for the same load-bearing reason as ThrallfleshEntity: the
 * summon ritual for this creature is also ritual_type "occultism:familiar" (see
 * thrallflesh_summon.json and docs/occult_nexus.md Partie C, "un rituel occultism:familiar" for
 * the Wraith's own trigger) — FamiliarRitual#finish() only spawns the entity if it is an
 * `instanceof FamiliarEntity`. Goety's own caster mob base (BoundStormCaster, decompiled from
 * goety-2.5.55.4) extends Goety's unrelated AbstractBoundIllager/Owned ownership hierarchy, which
 * is incompatible with the Occultism familiar ritual this creature actually needs to spawn through
 * — so despite the "ranged spell caster" framing, FamiliarEntity is still the correct base class,
 * not a Goety class or vanilla Monster/PathfinderMob.
 *
 * Real Goety ISpell casting (NECROMANCY/ABYSS/VOID per docs) is prototyped alongside the original
 * bespoke beam, not replacing it — see USE_GOETY_SPELL and castGoetySpell() below. Both paths are
 * wired into the same attack_heavy damage ticks so ProtoMehka can flip the flag and compare.
 */
public class WraithEntity extends FamiliarEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("stand");
    // Legless hovering creature — walk reuses the same seamless "stand" bob loop, per the model
    // brief (no separate walk clip was authored).
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("stand");
    private static final RawAnimation ATTACK_LIGHT = RawAnimation.begin().thenPlay("attack_light");
    private static final RawAnimation ATTACK_HEAVY = RawAnimation.begin().thenPlay("attack_heavy");

    // Below this distance, the melee lunge (attack_light) is preferred over channeling the beam —
    // same threshold value ThrallfleshEntity uses for its own melee-vs-reach split.
    private static final float MELEE_TRIGGER_RANGE = 3.0f;
    private static final float MELEE_TRIGGER_RANGE_SQR = MELEE_TRIGGER_RANGE * MELEE_TRIGGER_RANGE;
    // "Meaningfully longer than melee range" per the design brief; well inside FOLLOW_RANGE (40).
    private static final float HEAVY_ATTACK_RANGE = 14.0f;
    private static final float HEAVY_ATTACK_RANGE_SQR = HEAVY_ATTACK_RANGE * HEAVY_ATTACK_RANGE;
    // Mirrors ThrallfleshEntity.HEAVY_ATTACK_COOLDOWN_TICKS — no reason found to diverge, the two
    // signature creatures' heavy attacks are meant to be comparable in overall pace.
    private static final int HEAVY_ATTACK_COOLDOWN_TICKS = 140;

    // attack_heavy is 2.5s / 50 ticks at 20 tps (see reclaimed_wraith.animation.json): 0-5 windup,
    // 5-25 charge telegraph (no damage), 25-45 beam active (damage + full beam render), 45-50 return.
    private static final int HEAVY_ATTACK_ANIM_TICKS = 50;
    private static final int HEAVY_ATTACK_ACTIVE_START_TICK = 25;
    private static final int HEAVY_ATTACK_ACTIVE_END_TICK = 45;
    private static final int HEAVY_ATTACK_DAMAGE_TICK_1 = HEAVY_ATTACK_ACTIVE_START_TICK;
    private static final int HEAVY_ATTACK_DAMAGE_TICK_2 = 35; // midpoint of the beam-active window
    // Two ticks of 9.0 = 18.0 total, deliberately a bit above Thrallflesh's single 16.0-damage
    // heavy hit: this attack has a full 2.5s tell and can be dodged/interrupted by breaking line of
    // sight, so it pays out more when it actually connects. Both are anchored off their own base
    // ATTACK_DAMAGE (Wraith 6.0, Thrallflesh 12.0) rather than picked arbitrarily.
    private static final float HEAVY_ATTACK_TICK_DAMAGE = 9.0f;

    // attack_light is 0.5s / 10 ticks.
    private static final int LIGHT_ATTACK_ANIM_TICKS = 10;

    // Melee is a blink-strike: teleport adjacent to the target, hit, hold through the lunge
    // animation, then teleport back to wherever the wraith started — a spectral caster shouldn't
    // physically walk/fly across the room to throw a punch. Landing distance keeps it just inside
    // doHurtTarget's own reach check (Mob's default MeleeAttackGoal used ~entity-size + 1 range).
    private static final double MELEE_LANDING_DISTANCE = 1.3;
    // Upper bound so it doesn't blink across half the map for a melee hit — same order of magnitude
    // as HEAVY_ATTACK_RANGE, a judgment call, not a value from the design docs.
    private static final double MELEE_MAX_RANGE = 20.0;
    private static final double MELEE_MAX_RANGE_SQR = MELEE_MAX_RANGE * MELEE_MAX_RANGE;
    // Cooldown between blink-strikes; independent from HEAVY_ATTACK_COOLDOWN_TICKS.
    private static final int MELEE_ATTACK_COOLDOWN_TICKS = 30;

    // Experimental prototype (docs/occult_nexus.md Partie C, "Constat technique confirmant la
    // faisabilite"): flip to false to fall back to the original dealBeamDamage()/WraithBeamLayer
    // path. Both are kept intact side by side so ProtoMehka can compare them in-game rather than
    // this being a one-way replacement.
    private static final boolean USE_GOETY_SPELL = true;

    /** Read by WraithBeamLayer so the homemade beam/charge-glow only render when it's actually the one dealing damage. */
    public static boolean isUsingGoetySpell() {
        return USE_GOETY_SPELL;
    }

    // Same instance reused across casts — LeechingSpell carries no per-cast mutable state (unlike
    // e.g. KillingSpell's lockedTarget field), and mobSpellResult() reads everything it needs from
    // the caster/target/spellStat arguments each call.
    private static final LeechingSpell GOETY_SPELL = new LeechingSpell();

    // Passive out-of-combat regen (also on ThrallfleshEntity, same values) — ProtoMehka asked for
    // both summon creatures to slowly heal back up once they've gone a while without taking a hit.
    private static final int PASSIVE_REGEN_DELAY_TICKS = 200; // 10s without damage before it starts
    private static final int PASSIVE_REGEN_INTERVAL_TICKS = 40; // then +1 HP every 2s
    private static final float PASSIVE_REGEN_AMOUNT = 1.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Server-authoritative elapsed-tick counters for the currently playing attack animation, or -1
    // when idle. Synced to the client via entityData so the beam render layer and the attack_light
    // flicker (see WraithModel/WraithRenderer) can read animation progress without depending on the
    // separate GeckoLib trigger-packet's own timing — see tickAttackAnimTracking().
    private static final EntityDataAccessor<Integer> DATA_LIGHT_ATTACK_TICKS =
            SynchedEntityData.defineId(WraithEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HEAVY_ATTACK_TICKS =
            SynchedEntityData.defineId(WraithEntity.class, EntityDataSerializers.INT);
    // Mob#getTarget() is server-only AI state, never networked — WraithBeamLayer runs client-side
    // and needs to know who to aim at, same problem vanilla Guardian solves with its own synced
    // attack-target-id field. -1 = no beam target.
    private static final EntityDataAccessor<Integer> DATA_BEAM_TARGET_ID =
            SynchedEntityData.defineId(WraithEntity.class, EntityDataSerializers.INT);

    private int heavyAttackCooldown;
    private int meleeAttackCooldown;
    private int noDamageTicks;

    public WraithEntity(EntityType<? extends WraithEntity> type, Level level) {
        super(type, level);
        // FamiliarEntity extends PathfinderMob, which defaults to ground-based movement — a legless
        // hovering caster needs the same flight setup vanilla's own FlyingMob subclasses use
        // (Phantom, Bee): a move control that flies straight toward a 3D point instead of pathing
        // along the ground, and gravity off so it doesn't sink when idle.
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FamiliarEntity.createAttributes()
                // Bumped from 70/6 — ProtoMehka found it too fragile in testing; still meant to stay
                // squishier than Thrallflesh's 100/14 (glass-cannon ranged caster vs. melee tank).
                .add(Attributes.MAX_HEALTH, 85.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    // Copied from vanilla FlyingMob#travel — can't extend that class (FamiliarEntity is the
    // required base, see the class doc), so its airborne movement physics are replicated here:
    // no ground friction, and gravity is already off via setNoGravity(true).
    @Override
    public void travel(Vec3 relativeMovement) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02f, relativeMovement);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            } else if (this.isInLava()) {
                this.moveRelative(0.02f, relativeMovement);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            } else {
                BlockPos ground = this.getBlockPosBelowThatAffectsMyMovement();
                float friction = 0.91f;
                if (this.onGround()) {
                    friction = this.level().getBlockState(ground).getFriction((LevelReader) this.level(), ground, this) * 0.91f;
                }
                float inertia = 0.16277137f / (friction * friction * friction);
                this.moveRelative(this.onGround() ? 0.1f * inertia : 0.02f, relativeMovement);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale((double) friction));
            }
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // No fall damage for a creature that flies — same reason FlyingMob overrides this.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_LIGHT_ATTACK_TICKS, -1);
        this.entityData.define(DATA_HEAVY_ATTACK_TICKS, -1);
        this.entityData.define(DATA_BEAM_TARGET_ID, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Same shape as ThrallfleshEntity's goal list: a dedicated goal for the signature heavy
        // attack at higher priority than the generic melee goal, so it's preferred whenever the
        // target is in range for it.
        this.goalSelector.addGoal(1, new WraithHeavyAttackGoal());
        this.goalSelector.addGoal(2, new WraithMeleeAttackGoal());
        this.goalSelector.addGoal(3, this.new SitGoal(this));
        this.goalSelector.addGoal(4, new WraithFollowOwnerGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new WraithDefendOwnerGoal());
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
            this.entityData.set(DATA_LIGHT_ATTACK_TICKS, 0);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.heavyAttackCooldown > 0) {
            this.heavyAttackCooldown--;
        }
        if (this.meleeAttackCooldown > 0) {
            this.meleeAttackCooldown--;
        }
        if (!this.level().isClientSide) {
            this.tickAttackAnimTracking();
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

    /**
     * Advances the two synced tick counters while their animation is playing, and fires the beam's
     * damage at the two fixed offsets within the beam-active window. This is the code-side
     * equivalent of ThrallfleshEntity's "spawn_fangs" custom instruction keyframe — same idea (a
     * timed gameplay callback during an attack animation), but keyed off a plain tick count instead
     * of an authored keyframe, since attack_heavy's charge/active split is a code-side decision
     * layered on top of a single static held pose, not something baked into the animation file.
     */
    private void tickAttackAnimTracking() {
        int lightTicks = this.entityData.get(DATA_LIGHT_ATTACK_TICKS);
        if (lightTicks >= 0) {
            lightTicks++;
            this.entityData.set(DATA_LIGHT_ATTACK_TICKS, lightTicks > LIGHT_ATTACK_ANIM_TICKS ? -1 : lightTicks);
        }

        int heavyTicks = this.entityData.get(DATA_HEAVY_ATTACK_TICKS);
        if (heavyTicks >= 0) {
            heavyTicks++;
            if (heavyTicks == HEAVY_ATTACK_DAMAGE_TICK_1 || heavyTicks == HEAVY_ATTACK_DAMAGE_TICK_2) {
                if (USE_GOETY_SPELL) {
                    this.castGoetySpell();
                } else {
                    this.dealBeamDamage();
                }
            }
            this.entityData.set(DATA_HEAVY_ATTACK_TICKS, heavyTicks > HEAVY_ATTACK_ANIM_TICKS ? -1 : heavyTicks);
        }
    }

    private void dealBeamDamage() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // indirectMagic rather than mobAttack (what ThrallfleshEntity uses for its melee hit): this
        // is a cast spell, not a physical strike, so it shouldn't interact with shield-blocking the
        // way a melee attack does.
        target.hurt(this.damageSources().indirectMagic(this, this), HEAVY_ATTACK_TICK_DAMAGE);
    }

    /**
     * Experimental alternative to dealBeamDamage(): casts Goety's own LeechingSpell (NECROMANCY
     * school) directly via Spell#mobSpellResult(), the same entry point Goety's own AbstractNecromancer
     * uses for its non-player ranged attack (decompiled from goety-2.5.55.4). LeechingSpell was
     * picked over other NECROMANCY/ABYSS/VOID candidates because it is a plain single-target ranged
     * damage spell with no summon/AoE/self-harm side effects (unlike e.g. KillingSpell, which
     * feeds back a share of the damage onto the caster).
     *
     * Deliberately calls the 3-arg mobSpellResult(caster, staff, spellStat) overload with a plain
     * spell.defaultStats() rather than the 1-arg overload: that one internally computes stats via
     * WandUtil.getStats(), which reads Goety's own ModAttributes.SPELL_* attributes off the caster —
     * attributes this entity's AttributeSupplier (createAttributes() above) never registers, so that
     * path would throw. Passing ItemStack.EMPTY as the "staff" is safe for the same reason: it is
     * only consulted for optional staff-type bonuses (typeStaff()/enchantedFocus()), which degrade
     * gracefully to "no bonus" for an empty stack.
     *
     * LeechingSpell.SpellResult's actual damage is `SpellConfig.LeechingDamage * WandUtil
     * .damageMultiply() + spellStat.getPotency()` — with this pack's Goety config unmodified
     * (leechingDamage=1.0, spellDamageMultiplier=1), that's just 1.0 base per cast, called twice
     * per attack_heavy (see tickAttackAnimTracking()) = 2.0 total, far below the old beam's 18.0.
     * Rather than editing Goety's own global config (which would also affect any other caster of
     * this spell in the pack), the extra damage is added per-cast via SpellStat#setPotency, which
     * SpellResult adds directly on top of the config-driven base — 8 potency per hit here reproduces
     * the old (1.0 + 8) * 2 = 18.0 total exactly.
     */
    private static final int GOETY_SPELL_POTENCY = 8;

    private void castGoetySpell() {
        LivingEntity target = this.getTarget();
        // Only called from the server-only branch of tick() (see tickAttackAnimTracking()), but
        // guard explicitly anyway since Spell#mobSpellResult ultimately requires a ServerLevel.
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel)) {
            return;
        }
        GOETY_SPELL.mobSpellResult(this, ItemStack.EMPTY, GOETY_SPELL.defaultStats().setPotency(GOETY_SPELL_POTENCY));
    }

    /** -1 when idle, otherwise elapsed ticks (0..LIGHT_ATTACK_ANIM_TICKS) into the current attack_light playback. Read by WraithModel for the transparency flicker. */
    public int getLightAttackAnimTicks() {
        return this.entityData.get(DATA_LIGHT_ATTACK_TICKS);
    }

    /** -1 when idle, otherwise elapsed ticks (0..HEAVY_ATTACK_ANIM_TICKS) into the current attack_heavy playback. Read by WraithBeamLayer. */
    public int getHeavyAttackAnimTicks() {
        return this.entityData.get(DATA_HEAVY_ATTACK_TICKS);
    }

    /**
     * The beam's aim point, resolved from the synced id (see DATA_BEAM_TARGET_ID) rather than
     * getTarget() directly — same reason vanilla Guardian#getActiveAttackTarget() does its own
     * client-side id lookup instead of reading Mob#target: that field is server-only AI state and
     * is null on the client entity instance a render layer runs against.
     */
    public LivingEntity getBeamTarget() {
        if (!this.level().isClientSide) {
            return this.getTarget();
        }
        int id = this.entityData.get(DATA_BEAM_TARGET_ID);
        if (id < 0) {
            return null;
        }
        Entity entity = this.level().getEntity(id);
        return entity instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    /** Snaps yaw/body-yaw directly toward the target, bypassing the usual gradual head-then-body turn. */
    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
    }

    public static int getHeavyAttackActiveStartTick() {
        return HEAVY_ATTACK_ACTIVE_START_TICK;
    }

    public static int getHeavyAttackActiveEndTick() {
        return HEAVY_ATTACK_ACTIVE_END_TICK;
    }

    /** Same equivalent-of-vanilla-OwnerHurtByTargetGoal pattern as ThrallfleshDefendOwnerGoal — FamiliarEntity uses getFamiliarOwner(), not TamableAnimal's owner system. */
    private class WraithDefendOwnerGoal extends Goal {

        @Override
        public boolean canUse() {
            LivingEntity owner = WraithEntity.this.getFamiliarOwner();
            if (owner == null) {
                return false;
            }
            LivingEntity ownerTarget = owner.getLastHurtByMob();
            LivingEntity ownerAttacking = owner.getLastHurtMob();
            return (ownerTarget != null && ownerTarget.isAlive()) || (ownerAttacking != null && ownerAttacking.isAlive());
        }

        @Override
        public void start() {
            LivingEntity owner = WraithEntity.this.getFamiliarOwner();
            if (owner == null) {
                return;
            }
            LivingEntity ownerTarget = owner.getLastHurtByMob();
            if (ownerTarget != null && ownerTarget.isAlive()) {
                WraithEntity.this.setTarget(ownerTarget);
                return;
            }
            LivingEntity ownerAttacking = owner.getLastHurtMob();
            if (ownerAttacking != null && ownerAttacking.isAlive()) {
                WraithEntity.this.setTarget(ownerAttacking);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return false;
        }
    }

    /**
     * Occultism's own FamiliarEntity.FollowOwnerGoal already has a catch-up teleport, but its
     * private tryTeleport() requires BlockPathTypes.WALKABLE ground under the landing spot — for a
     * legless hovering creature (and in terrain with gaps, like the floating-island biome this was
     * tested in), that check routinely fails, so the built-in catch-up never fires and the wraith
     * just falls further behind when the owner outpaces its normal path-following speed. Same
     * follow/stop distances as before, but the teleport only needs empty air, not solid footing.
     */
    private class WraithFollowOwnerGoal extends Goal {

        private static final float START_FOLLOW_DISTANCE = 10.0f;
        private static final float STOP_FOLLOW_DISTANCE = 2.0f;
        private static final double CATCH_UP_DISTANCE_SQR = 144.0; // teleport once >12 blocks behind
        private static final int TELEPORT_ATTEMPTS = 10;

        private LivingEntity owner;
        private int cooldown;

        WraithFollowOwnerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean shouldFollow(double thresholdSqr) {
            return this.owner != null && !this.owner.isSpectator()
                    && WraithEntity.this.distanceToSqr(this.owner) > thresholdSqr;
        }

        @Override
        public boolean canUse() {
            this.owner = WraithEntity.this.getFamiliarOwner();
            return this.shouldFollow((double) START_FOLLOW_DISTANCE * START_FOLLOW_DISTANCE);
        }

        @Override
        public boolean canContinueToUse() {
            return this.shouldFollow((double) STOP_FOLLOW_DISTANCE * STOP_FOLLOW_DISTANCE);
        }

        @Override
        public void start() {
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            WraithEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            WraithEntity.this.getLookControl().setLookAt(this.owner, 10.0f, WraithEntity.this.getMaxHeadXRot());
            if (--this.cooldown >= 0) {
                return;
            }
            this.cooldown = 10;
            if (WraithEntity.this.isLeashed() || WraithEntity.this.isPassenger()) {
                return;
            }
            if (WraithEntity.this.distanceToSqr(this.owner) >= CATCH_UP_DISTANCE_SQR) {
                this.tryTeleportNear();
            } else {
                WraithEntity.this.getNavigation().moveTo(this.owner, 1.0);
            }
        }

        private void tryTeleportNear() {
            RandomSource random = WraithEntity.this.getRandom();
            Vec3 ownerPos = this.owner.position();
            for (int i = 0; i < TELEPORT_ATTEMPTS; i++) {
                double x = ownerPos.x + (random.nextDouble() - 0.5) * 6.0;
                double y = ownerPos.y + (random.nextDouble() - 0.5) * 4.0;
                double z = ownerPos.z + (random.nextDouble() - 0.5) * 6.0;
                AABB landingBox = WraithEntity.this.getBoundingBox()
                        .move(x - WraithEntity.this.getX(), y - WraithEntity.this.getY(), z - WraithEntity.this.getZ());
                if (WraithEntity.this.level().noCollision(WraithEntity.this, landingBox)) {
                    WraithEntity.this.teleportTo(x, y, z);
                    WraithEntity.this.getNavigation().stop();
                    return;
                }
            }
        }
    }

    /** Goal for the ranged beam (attack_heavy) — same shape as ThrallfleshEntity.ThrallfleshHeavyAttackGoal, but gated on a range window instead of "too far for melee". */
    private class WraithHeavyAttackGoal extends Goal {

        WraithHeavyAttackGoal() {
            // MeleeAttackGoal (vanilla) reserves MOVE+LOOK; without claiming the same flags here,
            // it runs concurrently with this goal despite the lower priority and immediately closes
            // to melee range every time, so the heavy beam almost never gets a chance to fire.
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = WraithEntity.this.getTarget();
            if (target == null || !target.isAlive() || WraithEntity.this.heavyAttackCooldown > 0) {
                return false;
            }
            double distanceSqr = WraithEntity.this.distanceToSqr(target);
            return distanceSqr > MELEE_TRIGGER_RANGE_SQR && distanceSqr <= HEAVY_ATTACK_RANGE_SQR
                    && WraithEntity.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            // Let the full 2.5s cast play out once triggered rather than canceling mid-beam if the
            // target briefly steps out of range/LoS — dealBeamDamage() re-checks the target is still
            // alive at each damage tick regardless.
            return WraithEntity.this.entityData.get(DATA_HEAVY_ATTACK_TICKS) >= 0;
        }

        @Override
        public void start() {
            WraithEntity.this.heavyAttackCooldown = HEAVY_ATTACK_COOLDOWN_TICKS;
            WraithEntity.this.triggerAnim("attack_controller", "attack_heavy");
            WraithEntity.this.entityData.set(DATA_HEAVY_ATTACK_TICKS, 0);
            LivingEntity target = WraithEntity.this.getTarget();
            WraithEntity.this.entityData.set(DATA_BEAM_TARGET_ID, target != null ? target.getId() : -1);
            if (target != null) {
                // Snap the torso to face the target immediately rather than waiting on the natural
                // head-then-body catch-up turn (LivingEntity eases yBodyRot toward yHeadRot ~30%/tick)
                // — WraithBeamLayer anchors the beam to the body bone's own facing, so if the torso
                // hasn't actually turned to face the target yet, the belly-glyph source point ends up
                // pointed the wrong way even though the beam itself still aims correctly at the target.
                WraithEntity.this.faceTarget(target);
            }
        }

        @Override
        public void stop() {
            WraithEntity.this.entityData.set(DATA_BEAM_TARGET_ID, -1);
        }

        @Override
        public void tick() {
            LivingEntity target = WraithEntity.this.getTarget();
            if (target != null) {
                WraithEntity.this.getLookControl().setLookAt(target, 30.0f, 30.0f);
                WraithEntity.this.faceTarget(target);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /**
     * Blink-strike melee: teleport in adjacent to the target, hit immediately, hold position through
     * the 0.5s attack_light lunge, then teleport back to wherever the wraith started. Reserves the
     * same MOVE+LOOK flags as WraithHeavyAttackGoal so the two stay mutually exclusive.
     */
    private class WraithMeleeAttackGoal extends Goal {

        // Minecraft still visually lerps a teleport over a few ticks on the client (the position
        // delta is too large for a normal relative-move packet, but the client doesn't just snap to
        // it) — hiding the model for a couple of ticks around each teleport is simpler and more
        // reliable than fighting that interpolation, and it reads as a "blink" rather than a slide.
        private static final int TELEPORT_HIDE_TICKS = 3;

        private Vec3 returnPosition;
        private int returnDelay = -1;
        private int hideTicks;

        WraithMeleeAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.returnDelay >= 0) {
                return true;
            }
            LivingEntity target = WraithEntity.this.getTarget();
            if (target == null || !target.isAlive() || WraithEntity.this.meleeAttackCooldown > 0) {
                return false;
            }
            double distanceSqr = WraithEntity.this.distanceToSqr(target);
            return distanceSqr <= MELEE_MAX_RANGE_SQR && WraithEntity.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return this.returnDelay >= 0 || this.hideTicks > 0;
        }

        @Override
        public void start() {
            LivingEntity target = WraithEntity.this.getTarget();
            if (target == null) {
                return;
            }
            WraithEntity.this.meleeAttackCooldown = MELEE_ATTACK_COOLDOWN_TICKS;
            this.returnPosition = WraithEntity.this.position();

            // Land just short of the target, approaching from wherever the wraith started (so it
            // doesn't end up clipped inside the target's own hitbox).
            Vec3 awayFromTarget = this.returnPosition.subtract(target.position());
            awayFromTarget = awayFromTarget.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : awayFromTarget.normalize();
            Vec3 landing = target.position().add(awayFromTarget.scale(MELEE_LANDING_DISTANCE));
            WraithEntity.this.setInvisible(true);
            WraithEntity.this.teleportTo(landing.x, landing.y, landing.z);
            this.hideTicks = TELEPORT_HIDE_TICKS;
            WraithEntity.this.getLookControl().setLookAt(target, 180.0f, 180.0f);
            WraithEntity.this.doHurtTarget(target);

            this.returnDelay = LIGHT_ATTACK_ANIM_TICKS;
        }

        @Override
        public void tick() {
            LivingEntity target = WraithEntity.this.getTarget();
            if (target != null) {
                WraithEntity.this.getLookControl().setLookAt(target, 180.0f, 180.0f);
            }
            if (this.hideTicks > 0) {
                this.hideTicks--;
                if (this.hideTicks == 0) {
                    WraithEntity.this.setInvisible(false);
                }
            }
            if (this.returnDelay > 0) {
                this.returnDelay--;
                return;
            }
            if (this.returnDelay == 0 && this.returnPosition != null) {
                WraithEntity.this.setInvisible(true);
                WraithEntity.this.teleportTo(this.returnPosition.x, this.returnPosition.y, this.returnPosition.z);
                this.hideTicks = TELEPORT_HIDE_TICKS;
            }
            this.returnDelay = -1;
        }

        @Override
        public void stop() {
            WraithEntity.this.setInvisible(false);
            this.returnDelay = -1;
            this.hideTicks = 0;
            this.returnPosition = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
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
                .triggerableAnim("attack_heavy", ATTACK_HEAVY));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
