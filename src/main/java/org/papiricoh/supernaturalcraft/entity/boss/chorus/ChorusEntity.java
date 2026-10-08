package org.papiricoh.supernaturalcraft.entity.boss.chorus;

import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.Holy;
import org.papiricoh.supernaturalcraft.weather.StormLock;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Broken Chorus: ophanim, seraph and cherub fused into one colossal angel.
 *
 * <pre>EMERGING → P1 Faces → T → P2 Wings → T → P3 Eyes → T → Final (the core) → DYING</pre>
 *
 * <p><b>Parts.</b> 24 {@link ChorusPart}s (the Ender Dragon's pattern): four FACEs, six WINGs,
 * twelve EYEs riding its wheels, the CORE and the SHELL that turns blows away from it. Only the
 * parts of the current phase can be struck (eyes only while open). Its health is always the sum
 * of what its parts have left ({@link ChorusBalance}), so the bar never lies and breaking the
 * last part of a phase is what moves the fight on.
 *
 * <p><b>Where things are.</b> Every hit box comes from {@link ChorusGeometry}, which the renderer
 * uses too, driven by game time and a few synced values: its heading, the wheels' spin, the
 * wings' pose and which eyes are open.
 */
public class ChorusEntity extends Monster implements GeoEntity, AttackScheduler.Host<ChorusEntity>, CappedBoss {

    public static final byte EMERGING = 0, IDLE = 1, WINDUP = 2, ACTIVE = 3, RECOVER = 4, TRANSITION = 5, DYING = 6,
            KNEELING = 7, RESTING = 8;
    public static final int EMERGE_TICKS = 180, DEATH_TICKS = 400, KNEEL_TICKS = 100, REST_TICKS = 90;
    public static final int[] TRANSITION_TICKS = {0, 0, 160, 200, 240};
    /** The platform's radius at the start, and the floor left after each phase change. */
    public static final int[] FLOOR_RADIUS = {22, 22, 18, 14, 10};

    public static final int FIRST_FACE = 0, FIRST_WING = 4, FIRST_EYE = 10, CORE = 22, SHELL = 23, PART_COUNT = 24;
    /** Wing poses: in flight, laid down on the floor, and the top pair folded forward as a veil. */
    public static final byte FLIGHT = 0, LANDED = 1, VEIL = 2;
    private static final int POSE_BLEND = 20;
    /** Wheel spin speeds (radians per tick) per wheel: calm, out of step (P3), and dying. */
    private static final float[][] WHEEL_SPEEDS = {{0.02f, -0.025f, 0.03f}, {0.034f, -0.032f, 0.042f}, {0.004f, -0.003f, 0.005f}};

    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PARTS_ALIVE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CRACKED = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EYES_FORCED = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEADING = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> WHEEL_ANCHOR = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> WHEEL_BASE_0 = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_BASE_1 = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WHEEL_BASE_2 = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> WHEEL_SPEED = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> WING_POSE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> POSE_SINCE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> HYMN_NOTE = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> HARMONY = SynchedEntityData.defineId(ChorusEntity.class, EntityDataSerializers.BYTE);
    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Float>[] WHEEL_BASE = new EntityDataAccessor[]{WHEEL_BASE_0, WHEEL_BASE_1, WHEEL_BASE_2};

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final AttackScheduler<ChorusEntity> scheduler = new AttackScheduler<>(this, this);
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final ChorusPart[] parts = new ChorusPart[PART_COUNT];
    private final float[] partHealth = new float[PART_COUNT];
    private final float[] partMax = new float[PART_COUNT];
    private final List<BlockPos> bells = new ArrayList<>();
    private @Nullable BlockPos altar;
    private @Nullable UUID arenaId;
    private int stateTimer, finalTicks;
    /** Everything its parts held when the fight began; vanilla caps max health at 1024, so its
     *  own health is kept here and shown as a fraction of this. */
    private float totalPool = ChorusBalance.REFERENCE_HEALTH;
    private final BossHealthGuard guard = new BossHealthGuard();
    private float headingO;
    private @Nullable UUID lastPlayerAttacker;
    /** Ticks until the next Hymn, the chant only the bells can break. */
    private int hymnTimer;
    private boolean pendingKneel, veiled;
    /** A dive across the summit in progress: where from, where to, how long it takes and has left. */
    private @Nullable Vec3 swoopFrom, swoopTo;
    private int swoopTicks, swoopLength;
    private int lightningTimer = 80;
    /** After this long in its final phase, it stops holding back. */
    public static final int ENRAGE_TICKS = 3600;

    public ChorusEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        for (int i = 0; i < ChorusGeometry.FACES; i++) parts[FIRST_FACE + i] = new ChorusPart(this, ChorusPart.Kind.FACE, FIRST_FACE + i, 2.4f, 2.4f);
        for (int i = 0; i < ChorusGeometry.WINGS; i++) parts[FIRST_WING + i] = new ChorusPart(this, ChorusPart.Kind.WING, FIRST_WING + i, 3.6f, 3.4f);
        for (int i = 0; i < ChorusGeometry.EYES; i++) parts[FIRST_EYE + i] = new ChorusPart(this, ChorusPart.Kind.EYE, FIRST_EYE + i, 1.4f, 1.4f);
        parts[CORE] = new ChorusPart(this, ChorusPart.Kind.CORE, CORE, 2.6f, 2.6f);
        parts[SHELL] = new ChorusPart(this, ChorusPart.Kind.SHELL, SHELL, 4.5f, 6.0f);
        // Part ids must follow the boss's: claim a block of them (as the Ender Dragon does).
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        xpReward = 700;
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
        bossBar.setVisible(false);
        resetPartHealth(1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) parts[i].setId(id + i + 1);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, (byte) 1);
        builder.define(STATE, IDLE);
        builder.define(PARTS_ALIVE, maskFor(1));
        builder.define(CRACKED, 0);
        builder.define(EYES_FORCED, 0);
        builder.define(HEADING, 0f);
        builder.define(WHEEL_ANCHOR, 0L);
        builder.define(WHEEL_BASE_0, 0f);
        builder.define(WHEEL_BASE_1, 2.1f);
        builder.define(WHEEL_BASE_2, 4.2f);
        builder.define(WHEEL_SPEED, (byte) 0);
        builder.define(WING_POSE, FLIGHT);
        builder.define(POSE_SINCE, 0L);
        builder.define(HYMN_NOTE, (byte) -1);
        builder.define(HARMONY, (byte) 0);
    }

    // --- state ----------------------------------------------------------------------------

    public int phase() {
        return entityData.get(PHASE);
    }

    public byte state() {
        return entityData.get(STATE);
    }

    void setState(byte s) {
        entityData.set(STATE, s);
    }

    public boolean isInvulnerablePhase() {
        byte s = state();
        return s == EMERGING || s == TRANSITION || s == DYING;
    }

    public boolean kneeling() {
        return state() == KNEELING;
    }

    public boolean isEnraged() {
        return phase() == 4 && finalTicks > ENRAGE_TICKS;
    }

    public AttackScheduler<ChorusEntity> scheduler() {
        return scheduler;
    }

    public List<BlockPos> bells() {
        return bells;
    }

    public @Nullable BlockPos altar() {
        return altar;
    }

    /** The note it is singing in a Hymn (0-6), or -1. */
    public int hymnNote() {
        return entityData.get(HYMN_NOTE);
    }

    public void setHymnNote(int note) {
        entityData.set(HYMN_NOTE, (byte) note);
    }

    public int harmony() {
        return entityData.get(HARMONY);
    }

    public void setHarmony(int echoes) {
        entityData.set(HARMONY, (byte) Mth.clamp(echoes, 0, 127));
    }

    // --- time, heading, wheels, wings and eyes (shared with the renderer) ------------------

    public double time(float partial) {
        return level().getGameTime() + (double) partial;
    }

    public float heading(float partial) {
        return Mth.rotLerp(partial, headingO, entityData.get(HEADING));
    }

    public float heading() {
        return entityData.get(HEADING);
    }

    public void setHeading(float yaw) {
        entityData.set(HEADING, Mth.wrapDegrees(yaw));
    }

    public double wheelSpin(int wheel, double time) {
        float speed = WHEEL_SPEEDS[entityData.get(WHEEL_SPEED)][wheel];
        return entityData.get(WHEEL_BASE[wheel]) + speed * (time - entityData.get(WHEEL_ANCHOR));
    }

    public Vector3f wheelRot(int wheel, double time) {
        return ChorusGeometry.wheelRot(wheel, time, wheelSpin(wheel, time));
    }

    /** Changes how fast the wheels turn without making them jump: the spin carries on from where it is. */
    public void setWheelSpeed(int set) {
        double now = time(0);
        for (int w = 0; w < 3; w++) entityData.set(WHEEL_BASE[w], (float) Mth.wrapDegrees(Math.toDegrees(wheelSpin(w, now))) * Mth.DEG_TO_RAD);
        entityData.set(WHEEL_ANCHOR, (long) now);
        entityData.set(WHEEL_SPEED, (byte) set);
    }

    public byte wingPose() {
        return (byte) (entityData.get(WING_POSE) & 0xF);
    }

    public void setWingPose(byte pose) {
        if (pose == wingPose()) return;
        entityData.set(WING_POSE, (byte) (pose | (wingPose() << 4)));
        entityData.set(POSE_SINCE, level().getGameTime());
    }

    /** How far the wings are toward {@code pose}, blending from the previous pose over a second. */
    private float poseWeight(byte pose, double time) {
        byte packed = entityData.get(WING_POSE);
        float k = (float) Mth.clamp((time - entityData.get(POSE_SINCE)) / POSE_BLEND, 0, 1);
        float now = (packed & 0xF) == pose ? 1 : 0, before = ((packed >> 4) & 0xF) == pose ? 1 : 0;
        return before + (now - before) * k;
    }

    public Vector3f wingRot(int wing, double time) {
        return ChorusGeometry.wingRot(wing, time, poseWeight(LANDED, time), poseWeight(VEIL, time));
    }

    /** Eyes open and close in a slow staggered cycle in the third phase; attacks can force them open. */
    public boolean eyeOpen(int eye, double time) {
        if ((entityData.get(EYES_FORCED) & (1 << eye)) != 0) return true;
        if (phase() != 3 || isInvulnerablePhase()) return false;
        return ((long) time + eye * 37L) % 160 < 100;
    }

    public void forceEyes(int mask) {
        entityData.set(EYES_FORCED, mask);
    }

    public int forcedEyes() {
        return entityData.get(EYES_FORCED);
    }

    // --- parts ----------------------------------------------------------------------------

    /** Parts that live in a phase; the shell guards the core until the end. */
    private static int maskFor(int phase) {
        int m = 0;
        if (phase <= 1) m |= 0b1111 << FIRST_FACE;
        if (phase <= 2) m |= 0b111111 << FIRST_WING;
        if (phase <= 3) m |= 0xFFF << FIRST_EYE;
        m |= 1 << CORE;
        if (phase <= 3) m |= 1 << SHELL;
        return m;
    }

    public boolean partAlive(int i) {
        return (entityData.get(PARTS_ALIVE) & (1 << i)) != 0;
    }

    private void setPartAlive(int i, boolean alive) {
        int m = entityData.get(PARTS_ALIVE);
        entityData.set(PARTS_ALIVE, alive ? m | (1 << i) : m & ~(1 << i));
    }

    /** Faces and wings show cracks once they are below half their strength. */
    public boolean cracked(int i) {
        return i < FIRST_EYE && (entityData.get(CRACKED) & (1 << i)) != 0;
    }

    public static ChorusPart.Kind kindOf(int i) {
        if (i < FIRST_WING) return ChorusPart.Kind.FACE;
        if (i < FIRST_EYE) return ChorusPart.Kind.WING;
        if (i < CORE) return ChorusPart.Kind.EYE;
        return i == CORE ? ChorusPart.Kind.CORE : ChorusPart.Kind.SHELL;
    }

    /** The kind of part that must be broken in a phase. */
    public static ChorusPart.Kind targetKind(int phase) {
        return switch (phase) {
            case 1 -> ChorusPart.Kind.FACE;
            case 2 -> ChorusPart.Kind.WING;
            case 3 -> ChorusPart.Kind.EYE;
            default -> ChorusPart.Kind.CORE;
        };
    }

    /** Whether a part can be struck right now. */
    public boolean partPickable(int i) {
        if (!partAlive(i) || isInvulnerablePhase()) return false;
        ChorusPart.Kind kind = kindOf(i);
        if (kind == ChorusPart.Kind.SHELL) return phase() < 4;
        if (kind != targetKind(phase())) return false;
        return kind != ChorusPart.Kind.EYE || eyeOpen(i - FIRST_EYE, time(0));
    }

    public ChorusPart part(int i) {
        return parts[i];
    }

    public float partHealth(int i) {
        return partHealth[i];
    }

    public int aliveOf(ChorusPart.Kind kind) {
        int n = 0;
        for (int i = 0; i < PART_COUNT; i++) if (kindOf(i) == kind && partAlive(i)) n++;
        return n;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    /** Where part {@code i} stands (its feet), relative to the boss's own position. */
    public Vec3 partOffset(int i, float partial) {
        double time = time(partial);
        ChorusPart p = parts[i];
        // What is broken is gone: its box rests inside the core, out of everyone's way.
        if (!partAlive(i) && i < CORE) return ChorusGeometry.coreOffset().subtract(0, p.getBbHeight() / 2, 0);
        Vec3 centre = switch (kindOf(i)) {
            case FACE -> ChorusGeometry.faceOffset(i - FIRST_FACE, heading(partial));
            case WING -> ChorusGeometry.wingOffset(i - FIRST_WING, heading(partial), wingRot(i - FIRST_WING, time));
            case EYE -> {
                int eye = i - FIRST_EYE;
                yield ChorusGeometry.eyeOffset(eye, wheelRot(ChorusGeometry.wheelOf(eye), time));
            }
            case CORE, SHELL -> ChorusGeometry.coreOffset();
        };
        return centre.subtract(0, p.getBbHeight() / 2, 0);
    }

    /** Centre of a part in the world, now. */
    public Vec3 partCentre(int i) {
        return parts[i].getBoundingBox().getCenter();
    }

    private void placeParts() {
        for (int i = 0; i < parts.length; i++) {
            ChorusPart p = parts[i];
            Vec3 at = position().add(partOffset(i, 0));
            p.xo = p.xOld = p.getX();
            p.yo = p.yOld = p.getY();
            p.zo = p.zOld = p.getZ();
            p.setPos(at);
        }
    }

    /** Fills every part's pool for a fight of this many challengers (the power curve's true health, shared out). */
    private void resetPartHealth(int challengers) {
        double base = SNConfig.SPEC.isLoaded() ? Balance.bossHealth(BossProgression.Boss.BROKEN_CHORUS)
                : ProgressionScale.of(BossProgression.Boss.BROKEN_CHORUS).trueHealth();
        double per = SNConfig.SPEC.isLoaded() ? SNConfig.CHORUS_HEALTH_PER_PLAYER.get() : 0.5;
        float scale = ChorusBalance.scaled(1, per, challengers);
        float parts = ChorusBalance.partScale(base) * scale;
        for (int i = 0; i < PART_COUNT; i++) {
            partMax[i] = switch (kindOf(i)) {
                case FACE -> ChorusBalance.FACE_POOL * parts;
                case WING -> ChorusBalance.WING_POOL * parts;
                case EYE -> ChorusBalance.EYE_POOL * parts;
                case CORE -> 0;
                case SHELL -> 1;
            };
        }
        float sum = 0;
        for (int i = 0; i < CORE; i++) sum += partMax[i];
        partMax[CORE] = ChorusBalance.corePool(base) * scale;
        for (int i = 0; i < PART_COUNT; i++) partHealth[i] = partAlive(i) ? partMax[i] : 0;
        totalPool = sum + partMax[CORE];
    }

    /** Its true health: whatever its living parts still hold. */
    public float poolSum() {
        float sum = 0;
        for (int i = 0; i <= CORE; i++) sum += partAlive(i) ? partHealth[i] : 0;
        return sum;
    }

    /** Test and command hook: shrinks (never grows) what every living part holds so the total is that share of the whole. */
    public void scaleHealth(float fraction) {
        float now = poolSum(), want = totalPool * fraction;
        if (now <= 0 || want >= now) return;
        float k = want / now;
        for (int i = 0; i <= CORE; i++) if (partAlive(i)) partHealth[i] = Math.max(0.01f, partHealth[i] * k);
        syncHealth();
    }

    public float totalPool() {
        return totalPool;
    }

    /** Vanilla health mirrors the pools as a fraction (it cannot go above 1024). */
    private void syncHealth() {
        setHealth(Math.max(0.5f, getMaxHealth() * poolSum() / Math.max(1, totalPool)));
        guard.accept(this);
    }

    // --- the cap (v0.15) ------------------------------------------------------------------

    @Override
    public float trueMaxHealth() {
        return totalPool;
    }

    @Override
    public float healthScale() {
        return totalPool / getMaxHealth();
    }

    /** Its pools, not its vanilla health, decide its phases: only while it is untouchable is nothing to be taken. */
    @Override
    public float vanillaFloor() {
        return isInvulnerablePhase() ? getMaxHealth() : 0f;
    }

    @Override
    public void acceptHealth() {
        guard.accept(this);
    }

    /** What every attack of its is multiplied by: the power curve's, and its config factor. */
    public float attackDamageMultiplier() {
        return Balance.bossDamage(BossProgression.Boss.BROKEN_CHORUS) * SNConfig.CHORUS_DAMAGE_FACTOR.get().floatValue();
    }

    /**
     * Health lost outside its own pipeline ({@link BossHealthGuard}, {@code taken} true health after the soft cap) is a
     * blow on the first part that can be struck now; the bar is then put back in step with the pools.
     */
    private void guardedBlow(float taken) {
        for (int i = 0; i <= CORE; i++) {
            if (!partPickable(i)) continue;
            float dealt = Math.min(taken, partHealth[i]);
            partHealth[i] -= dealt;
            if (partHealth[i] <= 0.01f && level() instanceof ServerLevel level) {
                partHealth[i] = 0;
                breakPart(level, i, partCentre(i));
            }
            break;
        }
        syncHealth();
    }

    // --- arena ----------------------------------------------------------------------------

    public @Nullable ArenaController arena() {
        return level() instanceof ServerLevel server && arenaId != null ? ArenaSavedData.get(server).get(arenaId) : null;
    }

    public void bindArena(ArenaController arena, @Nullable BlockPos altar, List<BlockPos> bellSites) {
        this.arenaId = arena.id();
        this.altar = altar;
        arena.setBoss(getUUID());
        bells.clear();
        bells.addAll(bellSites);
    }

    public List<ServerPlayer> challengers() {
        if (!(level() instanceof ServerLevel server)) return List.of();
        ArenaController arena = arena();
        List<ServerPlayer> out = new ArrayList<>();
        if (arena != null) {
            for (ServerPlayer p : arena.livingParticipants(server)) if (!p.isCreative()) out.add(p);
        } else {
            for (ServerPlayer p : server.players()) {
                if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < 48 * 48) out.add(p);
            }
        }
        return out;
    }

    // --- spawning -------------------------------------------------------------------------

    public void beginEmergence() {
        setState(EMERGING);
        stateTimer = EMERGE_TICKS;
        setWheelSpeed(0);
        triggerAnim("action", "emerge");
        playSound(AllSounds.CHORUS_EMERGE.get(), 6.0f, 1.0f);
        ChorusCinematics.emergence(this);
    }

    private void finishEmergence() {
        setState(IDLE);
        int n = Math.max(1, challengers().size());
        entityData.set(PARTS_ALIVE, maskFor(1));
        resetPartHealth(n);
        totalPool = poolSum();
        syncHealth();
        bossBar.setVisible(true);
        updateBossBar();
        scheduler.delay(40);
        hymnTimer = SNConfig.CHORUS_HYMN_INTERVAL.get() / 2;
        playSound(AllSounds.CHORUS_HYMN.get(), 5.0f, 0.8f);
    }

    /** Test and preview hook: its death throes end on the next tick. */
    public void skipDying() {
        if (state() == DYING) stateTimer = 1;
    }

    /** Test and preview hook: skip whatever it is doing (emerging, transforming) and fight. */
    public void skipToFight() {
        if (state() == EMERGING) finishEmergence();
        else if (state() == TRANSITION) finishTransition();
    }

    // --- ticking --------------------------------------------------------------------------

    @Override
    public void tick() {
        headingO = entityData.get(HEADING);
        if (!level().isClientSide && !dead) {
            float taken = guard.tick(this, this);
            if (taken > 0 && !isInvulnerablePhase()) guardedBlow(taken);
            else if (taken > 0) syncHealth();
        }
        super.tick();
        placeParts();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) clientEffects();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        ArenaController arena = arena();
        if (arenaId == null || arena == null || !arena.isActive()) {
            if (arenaId == null) {
                ArenaController made = ChorusSummoning.openArena(level, blockPosition());
                if (made == null) {
                    discard();
                    return;
                }
                bindArena(made, null, List.of());
            } else {
                retreat(level, "message.supernaturalcraft.chorus.arena_gone");
                return;
            }
            arena = arena();
        }
        bossBar.setProgress(Mth.clamp(poolSum() / Math.max(1, totalPool), 0, 1));
        if (pendingKneel) {
            pendingKneel = false;
            if (!isInvulnerablePhase()) kneel(KNEEL_TICKS);
        }
        if (tickCount % 20 == 0) refreshBossBarViewers(level, arena);
        fly(arena);

        switch (state()) {
            case EMERGING -> {
                if (--stateTimer <= 0) finishEmergence();
            }
            case TRANSITION -> tickTransition(level, arena);
            case DYING -> tickDying(level, arena);
            case KNEELING, RESTING -> {
                if (--stateTimer <= 0) rise();
            }
            default -> {
                if (arena.checkAbandoned(level, SNConfig.FAILURE_SECONDS.get() * 20)) {
                    retreat(level, "message.supernaturalcraft.chorus.victorious");
                    return;
                }
                tickCombat();
            }
        }
    }

    /** How high its feet hang over the platform. */
    private double hover() {
        if (state() == KNEELING || state() == RESTING) return -2.0;
        if (state() == DYING) return -5.0 - 4.0 * (1 - stateTimer / (double) DEATH_TICKS);
        return switch (phase()) {
            case 2 -> 2.0;
            case 4 -> -5.0;
            default -> 1.0;
        };
    }

    /** Glides over the heart of the summit; it is never pushed about. */
    private void fly(ArenaController arena) {
        Vec3 c = arena.centerVec();
        double hover = hover();
        if (state() == EMERGING) hover = 40 - 39 * Math.min(1, (EMERGE_TICKS - stateTimer) / (EMERGE_TICKS * 0.7));
        double sway = phase() == 2 && state() != RESTING && state() != KNEELING ? 2.5 : 0.8;
        Vec3 want = new Vec3(c.x + Math.cos(tickCount * 0.012) * sway, c.y + hover + Math.sin(tickCount * 0.05) * 0.3,
                c.z + Math.sin(tickCount * 0.012) * sway);
        double ease = state() == EMERGING ? 0.2 : 0.06;
        if (swoopTicks > 0 && swoopFrom != null && swoopTo != null) {
            // A dive: low and fast along its line, whatever it was doing.
            double k = 1 - (swoopTicks - 1) / (double) swoopLength;
            Vec3 p = swoopFrom.lerp(swoopTo, k);
            want = new Vec3(p.x, c.y - 1.0, p.z);
            ease = 0.5;
            swoopTicks--;
        }
        setPos(position().add(want.subtract(position()).scale(ease)));
        setDeltaMovement(Vec3.ZERO);
        setYRot(0);
        setYBodyRot(0);
        setYHeadRot(0);
        LivingEntity t = attackTarget();
        if (t != null && state() != DYING && state() != KNEELING) turnToward(t, phase() == 4 ? 4 : 2);
    }

    /** Turns its faces toward {@code t}, a few degrees a tick. */
    public void turnToward(Entity t, float maxStep) {
        float want = (float) Math.toDegrees(Math.atan2(-(t.getX() - getX()), t.getZ() - getZ()));
        float now = heading();
        setHeading(now + Mth.clamp(Mth.wrapDegrees(want - now), -maxStep, maxStep));
    }

    /** Dives along a line across the summit over {@code ticks}. */
    public void swoop(Vec3 from, Vec3 to, int ticks) {
        swoopFrom = from;
        swoopTo = to;
        swoopTicks = swoopLength = Math.max(1, ticks);
    }

    public boolean veiled() {
        return veiled;
    }

    public void setVeiled(boolean veiled) {
        this.veiled = veiled;
    }

    private void tickCombat() {
        if (phase() == 4) finalTicks++;
        if (--lightningTimer <= 0) {
            lightningTimer = 60 + random.nextInt(60);
            ChorusAttacks.ambientLightning(this);
        }
        if (tickCount % 20 == 0) setHarmony(ChorusAttacks.echoes(this).size());
        LivingEntity target = attackTarget();
        setTarget(target);
        if (hymnTimer > 0) hymnTimer--;
        scheduler.tick();
        if (tickCount % 120 == 0 && random.nextFloat() < 0.5f) {
            playSound(AllSounds.CHORUS_AMBIENT.get(), 4.0f, 1.0f + random.nextFloat() * 0.5f);
        }
    }

    /** The Hymn is due: the scheduler will force it next. */
    public boolean hymnDue() {
        return hymnTimer <= 0;
    }

    public void hymnSung() {
        hymnTimer = SNConfig.CHORUS_HYMN_INTERVAL.get();
    }

    private void refreshBossBarViewers(ServerLevel level, ArenaController arena) {
        for (ServerPlayer p : List.copyOf(bossBar.getPlayers())) {
            if (!arena.isParticipant(p) || arena.horizontalDistance(p.position()) > arena.radius() + 24) bossBar.removePlayer(p);
        }
        for (ServerPlayer p : level.players()) {
            if (arena.isParticipant(p) && arena.horizontalDistance(p.position()) <= arena.radius() + 24) bossBar.addPlayer(p);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    // --- kneeling and resting -------------------------------------------------------------

    /** Brought low (a broken Hymn): it sinks to the floor, wings down, and takes more from every blow. */
    public void kneel(int ticks) {
        // The state first: cancelling the attack must see it already kneeling.
        setState(KNEELING);
        stateTimer = ticks;
        swoopTicks = 0;
        scheduler.cancel();
        setWingPose(LANDED);
        setHymnNote(-1);
        triggerAnim("action", "kneel");
        playSound(AllSounds.CHORUS_KNEEL.get(), 4.0f, 1.0f);
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.chorus.kneels").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Kneels at the start of the next tick (safe to call from inside an attack). */
    public void kneelSoon() {
        pendingKneel = true;
    }

    /** Settles on the platform for a while (after a dive in the second phase): wings within reach. */
    public void rest(int ticks) {
        setState(RESTING);
        stateTimer = ticks;
        setWingPose(LANDED);
    }

    private void rise() {
        setState(IDLE);
        setWingPose(FLIGHT);
        scheduler.delay(30);
    }

    // --- AttackScheduler.Host -------------------------------------------------------------

    @Override
    public List<AttackScheduler.Option<ChorusEntity>> attackPool() {
        return ChorusAttacks.pool(this);
    }

    @Override
    public @Nullable java.util.function.Supplier<BossAttack<ChorusEntity>> forcedAttack(LivingEntity target) {
        return ChorusAttacks.forced(this);
    }

    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    public int attackGap() {
        int gap = switch (phase()) {
            case 1 -> 40;
            case 2 -> 34;
            case 3 -> 30;
            default -> 24;
        };
        return isEnraged() ? Math.round(gap * 0.6f) : gap;
    }

    @Override
    public float windupScale() {
        return (isEnraged() ? 0.8f : 1.0f) * (1 - 0.1f * Math.min(3, harmony()));
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<ChorusEntity> attack) {
        if (state() == KNEELING || state() == RESTING || isInvulnerablePhase()) return;
        switch (stage) {
            case WINDUP -> {
                setState(WINDUP);
                if (attack != null && !attack.animation.isEmpty()) triggerAnim("action", attack.animation);
            }
            case ACTIVE -> setState(ACTIVE);
            case RECOVER -> setState(RECOVER);
            case IDLE -> setState(IDLE);
        }
    }

    // --- damage ---------------------------------------------------------------------------

    private static boolean isChorus(DamageSource source) {
        Entity e = source.getEntity();
        return e instanceof ChorusEntity || e instanceof ChoirEchoEntity;
    }

    /** A hit on one of its parts. */
    public boolean hurtPart(ChorusPart part, DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (!partPickable(part.index) || isChorus(source)) {
            deflect();
            return false;
        }
        if (part.kind == ChorusPart.Kind.SHELL) {
            deflect();
            return false;
        }
        if (veiled && part.kind == ChorusPart.Kind.WING) {
            // Under the veil arrows glance off, the folded top wings are proof, the low wings bare.
            int wing = part.index - FIRST_WING;
            if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile || wing < 2) {
                deflect();
                return false;
            }
            if (!BossDamage.isExact(source)) amount *= 2;
        }
        if (source.getEntity() instanceof ServerPlayer p) {
            lastPlayerAttacker = p.getUUID();
            setLastHurtByPlayer(p);
        }
        return damagePart(part.index, source, amount);
    }

    /** Takes a blow off a part and off the boss together; breaks the part when it runs out. */
    boolean damagePart(int i, DamageSource source, float amount) {
        float capped = BossDamage.softCap(source, amount, ChorusBalance.boost(Holy.isHoly(source), kneeling()), trueMaxHealth());
        float dealt = Math.max(0, Math.min(capped, partHealth[i]));
        if (dealt <= 0) return false;
        partHealth[i] -= dealt;
        syncHealth();
        ServerLevel level = (ServerLevel) level();
        Vec3 at = partCentre(i);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.4, 0.4, 0.4, 0.03);
        if (i < FIRST_EYE && partHealth[i] < partMax[i] / 2) entityData.set(CRACKED, entityData.get(CRACKED) | (1 << i));
        if (partHealth[i] > 0.01f) {
            playSound(AllSounds.CHORUS_HURT.get(), 2.0f, 1.0f + random.nextFloat() * 0.4f);
            return true;
        }
        partHealth[i] = 0;
        breakPart(level, i, at);
        return true;
    }

    private void breakPart(ServerLevel level, int i, Vec3 at) {
        setPartAlive(i, false);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(AllParticles.GRACE.get(), at.x, at.y, at.z, 40, 0.8, 0.8, 0.8, 0.15);
        playSound(AllSounds.CHORUS_PART_BREAK.get(), 4.0f, 1.0f);
        playSound(SoundEvents.BELL_RESONATE, 4.0f, 0.6f + 0.1f * (i % 7));
        if (kindOf(i) == ChorusPart.Kind.FACE) ChorusAttacks.faceBroken(this, i - FIRST_FACE, at);
        if (kindOf(i) == ChorusPart.Kind.CORE) {
            beginDying();
            return;
        }
        if (aliveOf(targetKind(phase())) == 0 && phase() < 4) beginTransition(phase() + 1);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (BossDamage.passesThrough(source)) {
            boolean killed = super.hurt(source, amount);
            guard.accept(this);
            return killed;
        }
        if (isInvulnerablePhase() || isChorus(source)) {
            if (source.getEntity() instanceof Player) deflect();
            return false;
        }
        // A blow on its body (a spell's blast, an arrow in the cluster): it lands, at half
        // strength, on whichever breakable part is nearest to where it came from.
        Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : position();
        int best = -1;
        double bestD = 8 * 8;
        for (int i = 0; i <= CORE; i++) {
            if (!partPickable(i)) continue;
            double d = partCentre(i).distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        if (best < 0) {
            if (source.getEntity() instanceof Player) deflect();
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer p) {
            lastPlayerAttacker = p.getUUID();
            setLastHurtByPlayer(p);
        }
        return damagePart(best, source, amount * 0.5f);
    }

    private void deflect() {
        if (tickCount % 8 == 0) playSound(AllSounds.CHORUS_DEFLECT.get(), 0.8f, 1.0f);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(11, 4, 11).expandTowards(0, 10, 0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // --- phases, death --------------------------------------------------------------------

    public void beginTransition(int to) {
        scheduler.cancel();
        entityData.set(PHASE, (byte) to);
        setState(TRANSITION);
        stateTimer = TRANSITION_TICKS[to];
        forceEyes(0);
        setHymnNote(-1);
        setWingPose(to == 4 ? LANDED : FLIGHT);
        triggerAnim("action", "transform_" + to);
        playSound(AllSounds.CHORUS_TRANSFORM.get(), 5.0f, 1.0f);
        ChorusCinematics.transition(this, to);
        updateBossBar();
        for (ServerPlayer p : challengers()) {
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            if (away.length() < 10) {
                away = away.normalize();
                p.setDeltaMovement(away.x * 1.0, 0.4, away.z * 1.0);
                p.hurtMarked = true;
            }
        }
    }

    private void tickTransition(ServerLevel level, ArenaController arena) {
        int to = phase();
        if (stateTimer % 4 == 0) {
            Vec3 c = position().add(ChorusGeometry.coreOffset());
            level.sendParticles(AllParticles.GRACE.get(), c.x, c.y, c.z, 20, 2.0, 2.0, 2.0, 0.1);
        }
        if (stateTimer == TRANSITION_TICKS[to] / 2) {
            // The summit gives way under it: the outer ring of the platform falls into the clouds.
            int c = arena.center().getY();
            ArenaTerrain.collapseRing(level, arena, FLOOR_RADIUS[to], Math.max(arena.radius(), FLOOR_RADIUS[1]) + 1, c + 14, c - 6);
            if (to == 3) setWheelSpeed(1);
            if (to == 4) setWheelSpeed(2);
            arena.setPhase(to);
            ArenaEvents.broadcast(level, arena, true);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 5.0f, 0.5f);
        }
        if (--stateTimer <= 0) finishTransition();
    }

    private void finishTransition() {
        setState(IDLE);
        if (phase() == 3) setWheelSpeed(1);
        if (phase() == 4) {
            setWheelSpeed(2);
            setWingPose(LANDED);
        }
        if (arena() != null && level() instanceof ServerLevel level) {
            arena().setPhase(phase());
            ArenaEvents.broadcast(level, arena(), true);
        }
        scheduler.delay(30);
    }

    private void beginDying() {
        scheduler.cancel();
        setState(DYING);
        stateTimer = DEATH_TICKS;
        forceEyes(0);
        setHymnNote(-1);
        triggerAnim("action", "death");
        playSound(AllSounds.CHORUS_DEATH.get(), 5.0f, 1.0f);
        ChorusCinematics.death(this);
    }

    private void tickDying(ServerLevel level, ArenaController arena) {
        int elapsed = DEATH_TICKS - stateTimer;
        Vec3 c = position().add(ChorusGeometry.coreOffset());
        if (elapsed % 3 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a) * 1.5, c.y, c.z + Math.sin(a) * 1.5,
                    0, Math.cos(a) * 0.5, 0.3, Math.sin(a) * 0.5, 1.0);
        }
        if (elapsed % 40 == 0) playSound(SoundEvents.BELL_BLOCK, 5.0f, 0.5f + elapsed / (float) DEATH_TICKS);
        if (--stateTimer <= 0) {
            ServerPlayer killer = lastPlayerAttacker == null ? null : (ServerPlayer) level.getPlayerByUUID(lastPlayerAttacker);
            if (killer != null) setLastHurtByPlayer(killer);
            level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 4, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), c.x, c.y, c.z, 200, 2.0, 2.5, 2.0, 0.3);
            ChorusAttacks.unmakeEchoes(this);
            arena.beginRestore(true);
            StormLock.release(level, arena);
            ChorusCinematics.victory(this);
            setHealth(0);
            guard.accept(this);
            die(killer != null ? damageSources().playerAttack(killer) : damageSources().magic());
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    /** Everyone fell or fled: the choir rises back into the storm, and the summit mends. */
    private void retreat(ServerLevel level, String messageKey) {
        ArenaController arena = arena();
        Vec3 c = position().add(ChorusGeometry.coreOffset());
        level.sendParticles(AllParticles.GRACE.get(), c.x, c.y, c.z, 120, 2, 2, 2, 0.1);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
        }
        ChorusAttacks.unmakeEchoes(this);
        if (arena != null) {
            arena.beginRestore(false);
            StormLock.release(level, arena);
        }
        bossBar.removeAllPlayers();
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    // --- presentation ---------------------------------------------------------------------

    private void updateBossBar() {
        bossBar.setName(Component.translatable("entity.supernaturalcraft.broken_chorus.phase" + phase()).withStyle(ChatFormatting.GOLD));
        bossBar.setColor(phase() == 4 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.YELLOW);
    }

    private void clientEffects() {
        if (random.nextFloat() < 0.5f) {
            Vec3 c = position().add(ChorusGeometry.coreOffset());
            level().addParticle(AllParticles.GRACE.get(), c.x + random.nextGaussian() * 1.5, c.y + random.nextGaussian() * 1.5,
                    c.z + random.nextGaussian() * 1.5, 0, 0.02, 0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192 * 192;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEADING.equals(key) && tickCount < 2) headingO = entityData.get(HEADING);
    }

    // --- GeckoLib -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.broken_chorus.idle");
        RawAnimation last = RawAnimation.begin().thenLoop("animation.broken_chorus.final_idle");
        controllers.add(new AnimationController<>(this, "base", 10, state -> {
            if (state() == EMERGING || state() == DYING) return PlayState.STOP;
            return state.setAndContinue(phase() == 4 ? last : idle);
        }));
        AnimationController<ChorusEntity> action = new AnimationController<>(this, "action", 4, state -> PlayState.STOP);
        for (String name : ChorusAnimations.TRIGGERED) {
            boolean hold = name.equals("emerge") || name.equals("death");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold("animation.broken_chorus." + name)
                    : RawAnimation.begin().thenPlay("animation.broken_chorus." + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- persistence ----------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arenaId != null) tag.putUUID("Arena", arenaId);
        if (altar != null) tag.putLong("Altar", altar.asLong());
        tag.putByte("Phase", (byte) phase());
        tag.putByte("State", state());
        tag.putInt("StateTimer", stateTimer);
        tag.putInt("Parts", entityData.get(PARTS_ALIVE));
        tag.putInt("Cracked", entityData.get(CRACKED));
        tag.putInt("FinalTicks", finalTicks);
        tag.putFloat("TotalPool", totalPool);
        float[] hp = partHealth.clone();
        ListTag hs = new ListTag();
        for (float h : hp) hs.add(net.minecraft.nbt.FloatTag.valueOf(h));
        tag.put("PartHealth", hs);
        ListTag bs = new ListTag();
        bells.forEach(b -> bs.add(LongTag.valueOf(b.asLong())));
        tag.put("Bells", bs);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Arena")) arenaId = tag.getUUID("Arena");
        if (tag.contains("Altar")) altar = BlockPos.of(tag.getLong("Altar"));
        entityData.set(PHASE, (byte) Mth.clamp(tag.getByte("Phase"), 1, 4));
        byte s = tag.getByte("State");
        setState(s == EMERGING || s == DYING || s == TRANSITION ? s : IDLE);
        stateTimer = tag.getInt("StateTimer");
        if (tag.contains("Parts")) entityData.set(PARTS_ALIVE, tag.getInt("Parts"));
        entityData.set(CRACKED, tag.getInt("Cracked"));
        finalTicks = tag.getInt("FinalTicks");
        resetPartHealth(1);
        if (tag.contains("TotalPool")) totalPool = tag.getFloat("TotalPool");
        ListTag hs = tag.getList("PartHealth", Tag.TAG_FLOAT);
        for (int i = 0; i < Math.min(PART_COUNT, hs.size()); i++) partHealth[i] = hs.getFloat(i);
        bells.clear();
        for (Tag t : tag.getList("Bells", Tag.TAG_LONG)) bells.add(BlockPos.of(((LongTag) t).getAsLong()));
        if (phase() >= 3) setWheelSpeed(phase() == 3 ? 1 : 2);
        updateBossBar();
        bossBar.setVisible(s != EMERGING);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        updateBossBar();
    }
}
