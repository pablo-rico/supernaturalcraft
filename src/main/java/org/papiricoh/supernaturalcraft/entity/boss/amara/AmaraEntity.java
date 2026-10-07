package org.papiricoh.supernaturalcraft.entity.boss.amara;

import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.eclipse.Eclipses;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.light.LightWellBlock;
import org.papiricoh.supernaturalcraft.light.TempLights;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.weapon.Holy;
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
 * Amara, the Darkness. A floating mass of night that only light can wound.
 *
 * <pre>EMERGING → P1 Mass (rings) → T → P2 Tentacles → T → P3 Eclipse (core open) → T → Final form → DYING</pre>
 *
 * <p><b>Parts.</b> Twelve {@link AmaraPart}s: four orbiting ANCHORs (P1), six TENTACLE cysts (P2),
 * the CORE and the SHELL that hides it. Her own body cannot be struck until the final form. Break
 * every anchor (P1) or three cysts (P2) and she sinks, EXPOSED, core open; in P3 the core stays open.
 *
 * <p><b>Damage to the core</b> follows {@link AmaraBalance#damageMultiplier}: the four Light Wells,
 * holy strikes and light on the core all count. Hits are capped, and none carries her past a
 * phase threshold.
 */
public class AmaraEntity extends Monster implements GeoEntity, AttackScheduler.Host<AmaraEntity> {

    public static final byte EMERGING = 0, IDLE = 1, WINDUP = 2, ACTIVE = 3, RECOVER = 4, TRANSITION = 5, DYING = 6, EXPOSED = 7;
    public static final int EMERGE_TICKS = 360, DEATH_TICKS = 400;
    public static final int[] TRANSITION_TICKS = {0, 0, 200, 240, 300};
    public static final int P1_EXPOSE_TICKS = 240, P2_EXPOSE_TICKS = 200, CYST_REGROW_TICKS = 600, CYSTS_TO_EXPOSE = 3;
    public static final float ANCHOR_HEALTH = 80, CYST_HEALTH = 60;
    private static final float[] THRESHOLDS = {0.7f, 0.4f, 0.1f};

    public static final int ANCHORS = 4, TENTACLES = 6;
    public static final int FIRST_ANCHOR = 0, FIRST_TENTACLE = 4, CORE = 10, SHELL = 11, PART_COUNT = 12;
    /** How much larger than its pixels her model is drawn: the mass, and the final form. */
    public static final float MODEL_SCALE = 2.2f, FORM_SCALE = 1.1f;
    /** World sizes, in blocks: her mass centre above her feet, the anchors' orbit, the cysts' ring. */
    public static final float MASS_CENTER = 5.28f, ANCHOR_ORBIT = 7.5f, CYST_RING = 6.5f;

    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PARTS_ALIVE = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> LIT_WELLS = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final AttackScheduler<AmaraEntity> scheduler = new AttackScheduler<>(this, this);
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final AmaraPart[] parts = new AmaraPart[PART_COUNT];
    private final float[] partHealth = new float[PART_COUNT];
    private final List<BlockPos> wells = new ArrayList<>();
    private @Nullable UUID arenaId;
    private int stateTimer, regrowTimer, finalFormTicks;
    /** After this long in her final form she stops holding back. */
    public static final int ENRAGE_TICKS = 3600;
    private @Nullable UUID lastPlayerAttacker;
    /** Set while a hit on a part is passed on to her own health. */
    private boolean routing;

    public AmaraEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        for (int i = 0; i < ANCHORS; i++) parts[FIRST_ANCHOR + i] = new AmaraPart(this, AmaraPart.Kind.ANCHOR, FIRST_ANCHOR + i, 3.0f, 3.0f);
        for (int i = 0; i < TENTACLES; i++) {
            parts[FIRST_TENTACLE + i] = new AmaraPart(this, AmaraPart.Kind.TENTACLE, FIRST_TENTACLE + i, 1.6f, 1.4f);
        }
        parts[CORE] = new AmaraPart(this, AmaraPart.Kind.CORE, CORE, 1.9f, 1.9f);
        parts[SHELL] = new AmaraPart(this, AmaraPart.Kind.SHELL, SHELL, 6.4f, 6.2f);
        // Part ids must follow the boss's: claim a block of them (as the Ender Dragon does).
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        xpReward = 800;
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
        bossBar.setDarkenScreen(true);
        bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1400.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
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
        builder.define(LIT_WELLS, (byte) AmaraBalance.WELLS);
    }

    // --- state ----------------------------------------------------------------------------

    public int phase() {
        return entityData.get(PHASE);
    }

    public byte state() {
        return entityData.get(STATE);
    }

    private void setState(byte s) {
        entityData.set(STATE, s);
    }

    public boolean isInvulnerablePhase() {
        byte s = state();
        return s == EMERGING || s == TRANSITION || s == DYING;
    }

    public boolean exposed() {
        return state() == EXPOSED;
    }

    public boolean coreOpen() {
        return phase() == 3 || exposed();
    }

    public boolean isEnraged() {
        return phase() == 4 && (finalFormTicks > ENRAGE_TICKS || getHealth() < getMaxHealth() * 0.05f);
    }

    public int litWells() {
        return entityData.get(LIT_WELLS);
    }

    public AttackScheduler<AmaraEntity> scheduler() {
        return scheduler;
    }

    public List<BlockPos> wells() {
        return wells;
    }

    // --- parts ----------------------------------------------------------------------------

    /** Parts that live in a phase: anchors in P1, cysts in P2, core and shell until the final form. */
    private static int maskFor(int phase) {
        int m = 0;
        if (phase == 1) m |= 0b1111 << FIRST_ANCHOR;
        if (phase == 2) m |= 0b111111 << FIRST_TENTACLE;
        if (phase <= 3) m |= (1 << CORE) | (1 << SHELL);
        return m;
    }

    public boolean partAlive(int i) {
        return (entityData.get(PARTS_ALIVE) & (1 << i)) != 0;
    }

    private void setPartAlive(int i, boolean alive) {
        int m = entityData.get(PARTS_ALIVE);
        entityData.set(PARTS_ALIVE, alive ? m | (1 << i) : m & ~(1 << i));
    }

    /** Whether a part can be struck right now (dead parts and a closed core's shell rules). */
    public boolean partPickable(int i) {
        if (!partAlive(i) || isInvulnerablePhase()) return false;
        if (i == CORE) return coreOpen();
        if (i == SHELL) return !coreOpen();
        return true;
    }

    public AmaraPart part(int i) {
        return parts[i];
    }

    public float partHealth(int i) {
        return partHealth[i];
    }

    public int brokenCysts() {
        int n = 0;
        for (int i = 0; i < TENTACLES; i++) if (!partAlive(FIRST_TENTACLE + i)) n++;
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

    /**
     * Where part {@code i} sits (its feet), relative to her own position. Shared by the server's
     * hitboxes and the client's model so the rings and cysts are drawn where they can be struck.
     */
    public Vec3 partOffset(int i, float partialTick) {
        float t = tickCount + partialTick;
        if (i < FIRST_TENTACLE) {
            // Low enough to reach: their boxes bob between one and four blocks off the floor.
            double a = t * 0.02 + i * Math.PI / 2;
            return new Vec3(Math.cos(a) * ANCHOR_ORBIT, 0.6 + Math.sin(t * 0.05 + i) * 0.8, Math.sin(a) * ANCHOR_ORBIT);
        }
        if (i < CORE) {
            double a = (i - FIRST_TENTACLE) * Math.PI / 3 + Math.PI / 6;
            return new Vec3(Math.cos(a) * CYST_RING, 0, Math.sin(a) * CYST_RING);
        }
        if (i == CORE) return new Vec3(0, MASS_CENTER - 0.95, 0);
        return new Vec3(0, MASS_CENTER - 3.1, 0);
    }

    private void placeParts() {
        for (int i = 0; i < parts.length; i++) {
            AmaraPart p = parts[i];
            Vec3 at = position().add(partOffset(i, 0));
            p.xo = p.xOld = p.getX();
            p.yo = p.yOld = p.getY();
            p.zo = p.zOld = p.getZ();
            p.setPos(at);
        }
    }

    private void resetPartHealth() {
        float scale = AmaraBalance.scaled(1, SNConfig.AMARA_HEALTH_PER_PLAYER.get(), challengers().size());
        for (int i = 0; i < PART_COUNT; i++) {
            partHealth[i] = i < FIRST_TENTACLE ? ANCHOR_HEALTH * scale : i < CORE ? CYST_HEALTH * scale : 0;
        }
    }

    // --- arena ----------------------------------------------------------------------------

    public @Nullable ArenaController arena() {
        return level() instanceof ServerLevel server && arenaId != null ? ArenaSavedData.get(server).get(arenaId) : null;
    }

    public void bindArena(ArenaController arena, List<BlockPos> wellSites) {
        this.arenaId = arena.id();
        arena.setBoss(getUUID());
        wells.clear();
        wells.addAll(wellSites);
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
        triggerAnim("action", "emerge");
        playSound(AllSounds.AMARA_EMERGE.get(), 5.0f, 0.6f);
        AmaraCinematics.emergence(this);
    }

    private void finishEmergence() {
        setState(IDLE);
        int n = Math.max(1, challengers().size());
        float max = AmaraBalance.scaled(SNConfig.AMARA_HEALTH.get(), SNConfig.AMARA_HEALTH_PER_PLAYER.get(), n);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
        setHealth(max);
        resetPartHealth();
        bossBar.setVisible(true);
        updateBossBar();
        scheduler.delay(40);
        playSound(AllSounds.AMARA_ROAR.get(), 4.0f, 0.7f);
    }

    /** Test and preview hook: skip whatever she is doing (emerging, transforming) and fight. */
    public void skipToFight() {
        if (state() == EMERGING) finishEmergence();
        else if (state() == TRANSITION) finishTransition();
    }

    // --- ticking --------------------------------------------------------------------------

    @Override
    public void tick() {
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
                ArenaController made = AmaraSummoning.openArena(level, blockPosition());
                if (made == null) {
                    discard();
                    return;
                }
                bindArena(made, AmaraSummoning.raiseWells(level, made));
            } else {
                retreat(level, "message.supernaturalcraft.amara.arena_gone");
                return;
            }
            arena = arena();
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0) refreshBossBarViewers(level, arena);
        if (tickCount % 10 == 0) entityData.set(LIT_WELLS, (byte) countLitWells(level));
        drift(arena);

        switch (state()) {
            case EMERGING -> {
                if (--stateTimer <= 0) finishEmergence();
            }
            case TRANSITION -> tickTransition(level, arena);
            case DYING -> tickDying(level, arena);
            case EXPOSED -> {
                tickLight(level, arena);
                if (--stateTimer <= 0) endExposure();
            }
            default -> {
                if (arena.checkAbandoned(level, SNConfig.FAILURE_SECONDS.get() * 20)) {
                    retreat(level, "message.supernaturalcraft.amara.victorious");
                    return;
                }
                tickLight(level, arena);
                tickCombat();
            }
        }
    }

    /** Hovers over her arena's heart; low when exposed or in the final form, high in the first phase. */
    private void drift(ArenaController arena) {
        Vec3 c = arena.centerVec();
        // Her core (MASS_CENTER up) must come within reach when it opens: exposed she sinks into
        // the ground, and in the third phase she hangs low over it.
        double hover = switch (phase()) {
            case 1 -> 0.5;
            case 2 -> 0.0;
            case 3 -> -2.0;
            default -> 0.0;
        };
        if (exposed()) hover = -2.5;
        if (state() == EMERGING) hover = -6.0 + 6.5 * Math.min(1, (EMERGE_TICKS - stateTimer) / 120.0);
        double sway = phase() == 4 ? 0 : 1.6;
        Vec3 want = new Vec3(c.x + Math.cos(tickCount * 0.01) * sway, c.y + hover + Math.sin(tickCount * 0.04) * 0.25,
                c.z + Math.sin(tickCount * 0.01) * sway);
        if (phase() == 4 && state() != DYING) {
            LivingEntity t = attackTarget();
            if (t != null && scheduler.current() == null && distanceToSqr(t) > 16) {
                Vec3 to = t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.12);
                want = new Vec3(getX() + to.x, c.y, getZ() + to.z);
            } else {
                want = new Vec3(getX(), c.y, getZ());
            }
            if (t != null) lookAtFlat(t);
        } else {
            setYRot(0);
            setYBodyRot(0);
            setYHeadRot(0);
        }
        // She is not pushed about by physics: she glides where she wills.
        setPos(position().add(want.subtract(position()).scale(0.08)));
        setDeltaMovement(Vec3.ZERO);
    }

    private void lookAtFlat(Entity t) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(t.getX() - getX()), t.getZ() - getZ()));
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    private void tickCombat() {
        if (phase() == 4) finalFormTicks++;
        LivingEntity target = attackTarget();
        setTarget(target);
        if (phase() == 2 && brokenCysts() > 0 && --regrowTimer <= 0) regrowCysts();
        scheduler.tick();
        if (tickCount % 140 == 0 && random.nextFloat() < 0.5f) playSound(AllSounds.AMARA_AMBIENT.get(), 3.0f, 0.6f);
    }

    /** Light as a resource: held lights burn around their bearers, and the dark eats everyone else. */
    private void tickLight(ServerLevel level, ArenaController arena) {
        List<ServerPlayer> players = challengers();
        if (tickCount % 5 == 0) {
            for (ServerPlayer p : players) {
                if (p.getMainHandItem().is(AllTags.Items.HELD_LIGHT_SOURCES) || p.getOffhandItem().is(AllTags.Items.HELD_LIGHT_SOURCES)) {
                    TempLights.place(level, BlockPos.containing(p.getEyePosition()), 11, 8);
                }
            }
        }
        if (tickCount % Consumption.INTERVAL == 0) {
            for (ServerPlayer p : players) Consumption.tick(this, p, AmaraVoid.inVoid(level, p));
        }
    }

    private int countLitWells(ServerLevel level) {
        int n = 0;
        for (BlockPos w : wells) {
            BlockState s = level.getBlockState(w);
            if (s.is(AllBlocks.LIGHT_WELL.get()) && s.getValue(LightWellBlock.LIT)) n++;
        }
        return n;
    }

    /** Within {@code radius} blocks (flat) of one of her wells that still burns. */
    public boolean nearLitWell(Vec3 at, double radius) {
        for (BlockPos w : wells) {
            BlockState s = level().getBlockState(w);
            if (!s.is(AllBlocks.LIGHT_WELL.get()) || !s.getValue(LightWellBlock.LIT)) continue;
            double dx = at.x - (w.getX() + 0.5), dz = at.z - (w.getZ() + 0.5);
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    public boolean coreLit() {
        BlockPos at = BlockPos.containing(parts[CORE].getBoundingBox().getCenter());
        return level().getBrightness(LightLayer.BLOCK, at) >= AmaraBalance.LIT;
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

    // --- AttackScheduler.Host -------------------------------------------------------------

    @Override
    public List<AttackScheduler.Option<AmaraEntity>> attackPool() {
        return AmaraAttacks.pool(phase());
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
            case 1 -> 45;
            case 2 -> 35;
            case 3 -> 30;
            default -> 22;
        };
        return isEnraged() ? Math.round(gap * 0.6f) : gap;
    }

    @Override
    public float windupScale() {
        return isEnraged() ? 0.8f : 1.0f;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<AmaraEntity> attack) {
        if (state() == EXPOSED || isInvulnerablePhase()) return;
        switch (stage) {
            case WINDUP -> {
                setState(WINDUP);
                if (attack != null) triggerAnim("action", attack.animation);
            }
            case ACTIVE -> setState(ACTIVE);
            case RECOVER -> setState(RECOVER);
            case IDLE -> setState(IDLE);
        }
    }

    // --- damage ---------------------------------------------------------------------------

    /** A hit on one of her parts. */
    public boolean hurtPart(AmaraPart part, DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (isInvulnerablePhase() || !partPickable(part.index) || isDarkness(source)) {
            deflect();
            return false;
        }
        if (source.getEntity() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
        return switch (part.kind) {
            case ANCHOR, TENTACLE -> hurtBreakable(part.index, source, amount);
            case CORE -> hurtCore(source, amount);
            case SHELL -> {
                deflect();
                yield false;
            }
        };
    }

    private static boolean isDarkness(DamageSource source) {
        return source.getEntity() != null && source.getEntity().getType().is(AllTags.Entities.DARKNESS);
    }

    private boolean hurtBreakable(int i, DamageSource source, float amount) {
        partHealth[i] -= BossDamage.isExact(source) ? amount : amount * (Holy.isHoly(source) ? 1.5f : 1f);
        ServerLevel level = (ServerLevel) level();
        Vec3 at = parts[i].getBoundingBox().getCenter();
        level.sendParticles(AllParticles.VOID_MOTE.get(), at.x, at.y, at.z, 6, 0.4, 0.4, 0.4, 0.02);
        if (partHealth[i] > 0) {
            playSound(AllSounds.AMARA_HURT.get(), 0.6f, 1.6f);
            return true;
        }
        setPartAlive(i, false);
        level.sendParticles(AllParticles.VOID_MOTE.get(), at.x, at.y, at.z, 40, 0.6, 0.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        playSound(AllSounds.AMARA_PART_BREAK.get(), 2.0f, 0.7f);
        if (phase() == 1 && aliveAnchors() == 0) expose(P1_EXPOSE_TICKS);
        if (phase() == 2) {
            if (brokenCysts() == 1) regrowTimer = CYST_REGROW_TICKS;
            if (brokenCysts() >= CYSTS_TO_EXPOSE) expose(P2_EXPOSE_TICKS);
        }
        return true;
    }

    public int aliveAnchors() {
        int n = 0;
        for (int i = 0; i < ANCHORS; i++) if (partAlive(FIRST_ANCHOR + i)) n++;
        return n;
    }

    /** Through the core to her own health, by the light rules, never past a threshold. */
    public boolean hurtCore(DamageSource source, float amount) {
        amount = BossDamage.scaleAndCap(source, amount, AmaraBalance.damageMultiplier(litWells(), Holy.isHoly(source), coreLit()),
                SNConfig.AMARA_HIT_CAP.get().floatValue());
        int phase = phase();
        float floor = phase < 4 ? getMaxHealth() * THRESHOLDS[phase - 1] : 1.0f;
        boolean crosses = getHealth() - amount <= floor;
        if (crosses) amount = Math.max(0, getHealth() - floor);
        routing = true;
        boolean hurt;
        try {
            hurt = amount <= 0 || super.hurt(source, amount);
        } finally {
            routing = false;
        }
        if (crosses && isAlive()) {
            if (phase < 4) beginTransition(phase + 1);
            else beginDying();
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (routing || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (phase() == 4 && !isInvulnerablePhase() && !isDarkness(source)) {
            if (source.getEntity() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
            return hurtCore(source, amount);
        }
        if (source.getEntity() instanceof Player) deflect();
        return false;
    }

    private void deflect() {
        if (tickCount % 8 == 0) playSound(AllSounds.AMARA_DEFLECT.get(), 1.0f, 0.6f);
    }

    @Override
    public boolean isPickable() {
        // Until her final form she is struck only through her parts.
        return phase() == 4 && !isInvulnerablePhase();
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

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.AMARA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.AMARA_DEATH.get();
    }

    // --- exposure, phases, death ----------------------------------------------------------

    public void expose(int ticks) {
        scheduler.cancel();
        setState(EXPOSED);
        stateTimer = ticks;
        triggerAnim("action", "expose");
        playSound(AllSounds.AMARA_HURT.get(), 4.0f, 0.5f);
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.amara.exposed").withStyle(ChatFormatting.GOLD), true);
        }
    }

    private void endExposure() {
        setState(IDLE);
        triggerAnim("action", "close");
        if (phase() == 1) {
            for (int i = 0; i < ANCHORS; i++) setPartAlive(FIRST_ANCHOR + i, true);
            resetPartHealth();
        }
        scheduler.delay(30);
    }

    private void regrowCysts() {
        for (int i = 0; i < TENTACLES; i++) setPartAlive(FIRST_TENTACLE + i, true);
        resetPartHealth();
        playSound(AllSounds.AMARA_ROAR.get(), 2.0f, 1.2f);
    }

    public void beginTransition(int to) {
        scheduler.cancel();
        entityData.set(PHASE, (byte) to);
        setState(TRANSITION);
        stateTimer = TRANSITION_TICKS[to];
        entityData.set(PARTS_ALIVE, maskFor(to));
        resetPartHealth();
        refreshDimensions();
        triggerAnim("action", "transform_" + to);
        playSound(AllSounds.AMARA_ROAR.get(), 5.0f, to == 4 ? 0.5f : 0.7f);
        AmaraCinematics.transition(this, to);
        updateBossBar();
        for (ServerPlayer p : challengers()) {
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            if (away.length() < 9) {
                away = away.normalize();
                p.setDeltaMovement(away.x * 1.4, 0.4, away.z * 1.4);
                p.hurtMarked = true;
            }
        }
    }

    private void tickTransition(ServerLevel level, ArenaController arena) {
        if (stateTimer % 4 == 0) {
            level.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + MASS_CENTER, getZ(), 24, 2.0, 2.0, 2.0, 0.1);
        }
        if (stateTimer == TRANSITION_TICKS[phase()] / 2) {
            arena.setPhase(phase());
            ArenaEvents.broadcast(level, arena, true);
        }
        if (--stateTimer <= 0) finishTransition();
    }

    private void finishTransition() {
        setState(IDLE);
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
        triggerAnim("action", "death");
        playSound(AllSounds.AMARA_DEATH.get(), 5.0f, 0.6f);
        AmaraCinematics.death(this);
    }

    private void tickDying(ServerLevel level, ArenaController arena) {
        int elapsed = DEATH_TICKS - stateTimer;
        if (elapsed % 3 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.5, getY() + 2, getZ() + Math.sin(a) * 1.5,
                    0, Math.cos(a) * 0.5, 0.3, Math.sin(a) * 0.5, 1.0);
            level.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + 2, getZ(), 10, 1.0, 1.5, 1.0, 0.08);
        }
        if (--stateTimer <= 0) {
            ServerPlayer killer = lastPlayerAttacker == null ? null : (ServerPlayer) level.getPlayerByUUID(lastPlayerAttacker);
            if (killer != null) setLastHurtByPlayer(killer);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 4, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 2, getZ(), 200, 2.0, 2.5, 2.0, 0.3);
            releaseChallengers();
            arena.beginRestore(true);
            AmaraCinematics.victory(this);
            // Her eclipse dies with her: the sun comes back.
            Eclipses.end(level);
            setHealth(0);
            die(killer != null ? damageSources().playerAttack(killer) : damageSources().magic());
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    /** Everyone fell or fled: she withdraws into the dark, and the eclipse runs out its time. */
    private void retreat(ServerLevel level, String messageKey) {
        ArenaController arena = arena();
        level.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + 2, getZ(), 120, 2, 2, 2, 0.1);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
            }
        }
        releaseChallengers();
        if (arena != null) arena.beginRestore(false);
        Eclipses.lock(level, false);
        bossBar.removeAllPlayers();
        discard();
    }

    private void releaseChallengers() {
        for (ServerPlayer p : challengers()) Consumption.release(p);
        // Her shades go back into the dark with her.
        for (AmaraShade s : level().getEntitiesOfClass(AmaraShade.class, getBoundingBox().inflate(64))) s.unmake();
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    // --- presentation ---------------------------------------------------------------------

    private void updateBossBar() {
        bossBar.setName(Component.translatable("entity.supernaturalcraft.amara.phase" + phase()).withStyle(ChatFormatting.LIGHT_PURPLE));
        bossBar.setColor(phase() == 4 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE);
        bossBar.setCreateWorldFog(true);
    }

    private void clientEffects() {
        if (random.nextFloat() < 0.6f) {
            double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 2;
            level().addParticle(AllParticles.VOID_MOTE.get(), getX() + Math.cos(a) * r, getY() + MASS_CENTER + random.nextGaussian(),
                    getZ() + Math.sin(a) * r, -Math.cos(a) * 0.05, 0.01, -Math.sin(a) * 0.05);
        }
        if (coreOpen() && random.nextFloat() < 0.4f) {
            level().addParticle(AllParticles.GRACE.get(), getX(), getY() + MASS_CENTER, getZ(),
                    random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return phase() == 4 ? EntityDimensions.scalable(1.6f, 5.5f) : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) refreshDimensions();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    // --- GeckoLib -------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation mass = RawAnimation.begin().thenLoop("animation.amara.idle");
        RawAnimation open = RawAnimation.begin().thenLoop("animation.amara.idle_open");
        RawAnimation form = RawAnimation.begin().thenLoop("animation.amara.form_idle");
        controllers.add(new AnimationController<>(this, "base", 8, state -> {
            if (state() == EMERGING || state() == DYING) return PlayState.STOP;
            if (phase() == 4) return state.setAndContinue(form);
            return state.setAndContinue(coreOpen() ? open : mass);
        }));
        AnimationController<AmaraEntity> action = new AnimationController<>(this, "action", 4, state -> PlayState.STOP);
        for (String name : AmaraAnimations.TRIGGERED) {
            boolean hold = name.equals("emerge") || name.equals("death") || name.equals("expose");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold("animation.amara." + name)
                    : RawAnimation.begin().thenPlay("animation.amara." + name));
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
        tag.putByte("Phase", (byte) phase());
        tag.putByte("State", state());
        tag.putInt("StateTimer", stateTimer);
        tag.putInt("Parts", entityData.get(PARTS_ALIVE));
        ListTag ws = new ListTag();
        wells.forEach(w -> ws.add(LongTag.valueOf(w.asLong())));
        tag.put("Wells", ws);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Arena")) arenaId = tag.getUUID("Arena");
        entityData.set(PHASE, (byte) Mth.clamp(tag.getByte("Phase"), 1, 4));
        byte s = tag.getByte("State");
        setState(s == EMERGING || s == DYING || s == TRANSITION ? s : IDLE);
        stateTimer = tag.getInt("StateTimer");
        if (tag.contains("Parts")) entityData.set(PARTS_ALIVE, tag.getInt("Parts"));
        wells.clear();
        for (Tag t : tag.getList("Wells", Tag.TAG_LONG)) wells.add(BlockPos.of(((LongTag) t).getAsLong()));
        resetPartHealth();
        updateBossBar();
        bossBar.setVisible(s != EMERGING);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        updateBossBar();
    }
}
