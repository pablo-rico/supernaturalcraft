package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.balance.Balance;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaEvents;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllItems;
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
import java.util.function.Supplier;

/**
 * Lucifer. Four phases, each ending at a quarter of his health:
 *
 * <pre>EMERGING → P1 Vessel → T → P2 Fallen → T → P3 Cage Wrath → T (Archangel Unbound) → P4 → DYING</pre>
 *
 * <p><b>Damage policy.</b> Invulnerable while emerging, transforming or dying. Anything not holy
 * (holy damage types, or a melee hit with a {@code #holy_weapons} item) is cut by the configured
 * multiplier; every hit is soft-capped against his true max health ({@link BossDamage#softCap}); hits
 * during an attack's recovery are worth 25% more; and no hit can carry him past a phase threshold —
 * burst damage can't skip a phase. Health lost any other way goes through {@link BossHealthGuard}.
 *
 * <p><b>Health (v0.15).</b> Every boss built on him keeps {@link #VANILLA_BASE} vanilla health and its true
 * health, from the power curve ({@link Balance#bossHealth}), in {@link #healthScale()}.
 */
public class LuciferEntity extends Monster implements GeoEntity, LuciferLook, SpellHooks.Bindable,
        AttackScheduler.Host<LuciferEntity>, CappedBoss {

    public static final byte EMERGING = 0, IDLE = 1, WINDUP = 2, ACTIVE = 3, RECOVER = 4, TRANSITION = 5, DYING = 6;
    public static final int EMERGE_TICKS = 120, TRANSITION_TICKS = 80, FINAL_TRANSITION_TICKS = 160, DEATH_TICKS = 200;
    private static final float[] THRESHOLDS = {0.75f, 0.5f, 0.25f};
    /** Vanilla max health of every boss on this base: the bar is a proportion, true health is this times the scale. */
    public static final float VANILLA_BASE = 1000f;
    /** His shield as he takes to the air for the last phase, as a share of his vanilla max health. */
    public static final float LAST_PHASE_ABSORPTION = 0.15f;

    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(LuciferEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(LuciferEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final AttackScheduler<LuciferEntity> scheduler = new AttackScheduler<>(this, this);
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable UUID arenaId;
    protected int stateTimer;
    protected int noSightTicks;
    protected int attacksSinceSmite;
    private float orbitAngle;
    private @Nullable UUID lastPlayerAttacker;
    private final List<UUID> minions = new ArrayList<>();
    private final BossHealthGuard guard = new BossHealthGuard();
    /** True health per point of vanilla health; fixed for the challengers present when the fight starts. */
    private float healthScale = -1f;
    /** The last phase's floor has been reached (its finale, or the death, has been started once). */
    private boolean lastFloorReached;
    private @Nullable BossProgression.Boss progressionBoss;

    public LuciferEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 500;
        setPersistenceRequired();
        bossBar.setDarkenScreen(true);
        bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, (byte) 1);
        builder.define(STATE, IDLE);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    @Override
    public int lookPhase() {
        return phase();
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

    public boolean isEnraged() {
        return phase() == maxPhase() && getHealth() < getMaxHealth() * 0.1f;
    }

    // --- what a variant may change (Lucifer Uncaged) ----------------------------------------
    // Every default below is exactly Lucifer's own fight.

    /** The last phase: reaching its floor kills him. */
    public int maxPhase() {
        return 4;
    }

    /** The share of his health at which {@code phase} ends (phases before the last). */
    protected float threshold(int phase) {
        return THRESHOLDS[phase - 1];
    }

    /** Which of the great enemies he is on the power curve (by default, the one his entity type names). */
    public BossProgression.Boss progressionBoss() {
        if (progressionBoss == null) {
            BossProgression.Boss b = BossProgression.Boss.byEntity(BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath());
            progressionBoss = b != null ? b : BossProgression.Boss.LUCIFER;
        }
        return progressionBoss;
    }

    /** Extra true health per challenger beyond the first, as a fraction. */
    protected double healthPerExtraPlayer() {
        return SNConfig.LUCIFER_HEALTH_PER_PLAYER.get();
    }

    /** His true max health for {@code players} challengers: the power curve, plus a share per extra challenger. */
    public float trueHealthFor(int players) {
        return ProgressionScale.healthFor(Balance.bossHealth(progressionBoss()), Math.max(1, players), (float) healthPerExtraPlayer());
    }

    /**
     * Real health per point of vanilla health. Vanilla health is capped at 1024, so he keeps the vanilla bar as a
     * proportion ({@link #VANILLA_BASE}) and scales every hit down by this before it lands. Before the fight fixes
     * it for its challengers, it is the curve's for one.
     */
    @Override
    public float healthScale() {
        return healthScale > 0 ? healthScale : trueHealthFor(1) / VANILLA_BASE;
    }

    /** Test and command hook: sets the true-health multiplier directly (normally fixed when the fight starts). */
    public void setHealthScale(float scale) {
        healthScale = scale;
    }

    /** His health in real points (vanilla health times {@link #healthScale()}). */
    public float trueHealth() {
        return getHealth() * healthScale();
    }

    @Override
    public float trueMaxHealth() {
        return getMaxHealth() * healthScale();
    }

    protected float mundaneMultiplier() {
        return SNConfig.LUCIFER_MUNDANE_MULTIPLIER.get().floatValue();
    }

    /** This boss's own factor on top of the curve's attack multiplier (its config section's {@code damageFactor}). */
    protected float damageFactor() {
        return SNConfig.LUCIFER_DAMAGE_FACTOR.get().floatValue();
    }

    /** Scales the damage of every attack he makes: the power curve's multiplier, and his own factor. */
    public float attackDamageMultiplier() {
        return Balance.bossDamage(progressionBoss()) * damageFactor();
    }

    /** The phase in which he takes to the air and circles his target. */
    public boolean isAerialPhase() {
        return phase() == maxPhase();
    }

    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return LuciferAttacks.pool(phase);
    }

    protected int baseGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 30;
            case 3 -> 25;
            default -> 20;
        };
    }

    public float scale(int phase) {
        return scaleFor(phase);
    }

    protected int emergeTicks() {
        return EMERGE_TICKS;
    }

    /** "animation.lucifer." — the prefix of every clip in his animation file. */
    protected String animationPrefix() {
        return "animation.lucifer.";
    }

    protected List<String> triggeredAnimations() {
        return LuciferAnimations.TRIGGERED;
    }

    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.lucifer.bar.phase" + phase;
    }

    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (phase) {
            case 1 -> BossEvent.BossBarColor.RED;
            case 2 -> BossEvent.BossBarColor.PURPLE;
            case 3 -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    protected net.minecraft.core.particles.ParticleOptions phaseParticle(int phase) {
        return switch (phase) {
            case 2 -> AllParticles.ASH.get();
            case 3 -> AllParticles.FROST.get();
            default -> AllParticles.GRACE.get();
        };
    }

    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition());
    }

    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        ArenaTerrain.apply(level, arena, phase);
    }

    protected void playEmergence() {
        LuciferCinematics.emergence(this);
    }

    protected void playTransition(int to) {
        LuciferCinematics.transition(this, to);
    }

    protected void playDeath() {
        LuciferCinematics.death(this);
    }

    /** At the very end of his death, before the spoils fall. */
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        LuciferCinematics.victory(this, arena);
    }

    /** As a transformation begins (Lucifer gathers his shield and takes to the air for the last). */
    protected void onTransitionStart(int to) {
        if (to == 4) {
            setAbsorptionAmount(getMaxHealth() * LAST_PHASE_ABSORPTION);
            setNoGravity(true);
        }
    }

    /** Each tick of his rise from the ground while untouchable. */
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
    }

    /** Each tick of his death throes; {@code elapsed} counts up from 0. */
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(elapsed < 40);
        setDeltaMovement(0, elapsed < 40 ? -0.1 : getDeltaMovement().y, 0);
    }

    /** The fight was lost: what he leaves behind as he goes back (the key, cracked). */
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, new ItemStack(AllItems.CRACKED_KEY.get())));
    }

    protected int deathTicks() {
        return DEATH_TICKS;
    }

    /** How much more a hit lands for than usual: 25% more during an attack's recovery. */
    protected float vulnerability(DamageSource source) {
        return state() == RECOVER ? 1.25f : 1f;
    }

    /** How long the transformation into {@code to} lasts. */
    protected int transitionTicks(int to) {
        return to == maxPhase() ? FINAL_TRANSITION_TICKS : TRANSITION_TICKS;
    }

    /** Each tick of a transformation: Lucifer rises into the air for his last. */
    protected void tickTransitionMotion(int elapsed, boolean last) {
        if (last) {
            setDeltaMovement(0, elapsed < 110 ? 0.06 : 0, 0);
        } else {
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
    }

    protected SoundEvent emergeSound() {
        return AllSounds.LUCIFER_EMERGE.get();
    }

    protected SoundEvent roarSound() {
        return AllSounds.LUCIFER_ROAR.get();
    }

    protected SoundEvent transformSound() {
        return AllSounds.LUCIFER_TRANSFORM.get();
    }

    /** Now and then between attacks. */
    protected SoundEvent ambientBossSound() {
        return AllSounds.LUCIFER_AMBIENT.get();
    }

    /** As his long death begins. */
    protected SoundEvent dyingSound() {
        return AllSounds.LUCIFER_DEATH.get();
    }

    /** A blow turned aside while he can't be hurt. */
    protected SoundEvent deflectSound() {
        return AllSounds.LUCIFER_DEFLECT.get();
    }

    /** The light pouring out of him as he dies; {@code last} is the final burst. */
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.5, getZ(), 3, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.5, getZ(), 200, 1.5, 2.0, 1.5, 0.3);
            return;
        }
        double a = random.nextDouble() * Math.PI * 2;
        level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 0.5, getY() + 1.4, getZ() + Math.sin(a) * 0.5,
                0, Math.cos(a) * 0.6, 0.4 + random.nextDouble() * 0.6, Math.sin(a) * 0.6, 1.0);
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.5, getZ(), 6, 0.4, 0.8, 0.4, 0.05);
    }

    /** Whether he walks after his target between attacks (a boss at a lectern stays put). */
    protected boolean walks() {
        return true;
    }

    /** Client side, each tick he rises: hellfire and smoke at his feet. */
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.HELLFIRE.get(), getRandomX(1.5), getY() + 0.1, getRandomZ(1.5), 0, 0.15, 0);
        level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(1.5), getY() + 0.2, getRandomZ(1.5), 0, 0.05, 0);
    }

    /** Where he is put back when he strays to the arena's edge. */
    protected Vec3 tetherPoint(ArenaController arena) {
        Vec3 c = arena.centerVec();
        return new Vec3(c.x, c.y + (isAerialPhase() ? 5 : 1), c.z);
    }

    /** What the boss bar shows as full: his health, by default. */
    protected float bossBarProgress() {
        return getHealth() / getMaxHealth();
    }

    /** The boss bar's title in {@code phase}. */
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == maxPhase() ? ChatFormatting.WHITE : ChatFormatting.RED);
    }

    /** His boss bar, for a variant that writes on it. */
    protected final ServerBossEvent bossEvent() {
        return bossBar;
    }

    /**
     * Reaching the floor of his last phase: true to keep him alive at that floor (untouchable, the variant's own
     * finale takes over and calls {@link #beginDying()} itself); false, by default, to die at once.
     */
    protected boolean interceptDeath() {
        return false;
    }

    public AttackScheduler<LuciferEntity> scheduler() {
        return scheduler;
    }

    // --- arena ----------------------------------------------------------------------------

    public @Nullable ArenaController arena() {
        return level() instanceof ServerLevel server && arenaId != null ? ArenaSavedData.get(server).get(arenaId) : null;
    }

    public void bindArena(ArenaController arena) {
        this.arenaId = arena.id();
        arena.setBoss(getUUID());
    }

    /** Players fighting him: arena participants inside the dome, else anyone close by. */
    public List<ServerPlayer> challengers() {
        if (!(level() instanceof ServerLevel server)) return List.of();
        ArenaController arena = arena();
        List<ServerPlayer> out = new ArrayList<>();
        if (arena != null) {
            for (ServerPlayer p : arena.livingParticipants(server)) {
                if (!p.isCreative()) out.add(p);
            }
        } else {
            for (ServerPlayer p : server.players()) {
                if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) < 48 * 48) out.add(p);
            }
        }
        return out;
    }

    public List<UUID> minions() {
        return minions;
    }

    // --- spawning -------------------------------------------------------------------------

    /** Called by the summoning ritual: rise out of the ground, untouchable, with the cinematic. */
    public void beginEmergence() {
        setState(EMERGING);
        stateTimer = emergeTicks();
        triggerAnim("action", "emerge");
        playSound(emergeSound(), 4.0f, 0.9f);
        playEmergence();
    }

    private void finishEmergence() {
        setState(IDLE);
        scaleHealthToChallengers();
        bossBar.setVisible(true);
        scheduler.delay(40);
        playSound(roarSound(), 3.0f, 1.1f);
    }

    /** As the fight starts: true health for the challengers present, at full health. */
    protected void scaleHealthToChallengers() {
        fixHealthScale();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(VANILLA_BASE);
        setHealth(VANILLA_BASE);
        guard.accept(this);
    }

    /** Fixes the health scale for the challengers present (keeps his health's share as it is). */
    protected void fixHealthScale() {
        healthScale = trueHealthFor(challengers().size()) / VANILLA_BASE;
    }

    /** Whether the fight has fixed his health scale yet (see {@link #setHealthScale}). */
    protected boolean healthScaled() {
        return healthScale > 0;
    }

    /** His own code (or a command, or a test) has just changed his health: it is legitimate, not another mod's. */
    @Override
    public void acceptHealth() {
        guard.accept(this);
    }

    // --- ticking --------------------------------------------------------------------------

    /**
     * Before anything else this tick: health lost outside his own pipeline (another mod's {@code setHealth}, damage by a
     * share of health) becomes one capped blow, and a blow left at a phase floor still starts the change.
     */
    @Override
    public void tick() {
        if (!level().isClientSide && !dead) {
            guard.tick(this, this);
            checkFloor();
        }
        super.tick();
    }

    /** A blow (or a guarded drop) that left him at the current phase's floor starts the transition or his death. */
    private void checkFloor() {
        if (isInvulnerablePhase() || !isAlive()) return;
        int phase = phase();
        if (phase < maxPhase()) {
            if (getHealth() <= getMaxHealth() * threshold(phase) + 1e-3f) reachFloor(phase);
        } else if (getHealth() <= 1.0001f) {
            if (!lastFloorReached) reachFloor(phase);
        } else {
            lastFloorReached = false;
        }
    }

    private void reachFloor(int phase) {
        if (phase < maxPhase()) {
            beginTransition(phase + 1);
        } else {
            lastFloorReached = true;
            if (!interceptDeath()) beginDying();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            clientEffects();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        // Spawned by egg or command (no emergence): his health scales for whoever is there on his first tick.
        if (!healthScaled() && state() != EMERGING) fixHealthScale();
        ArenaController arena = arena();
        if (arenaId == null || arena == null || !arena.isActive()) {
            if (arenaId == null) {
                // Spawned by egg or command: build a Cage around wherever he stands.
                ArenaController made = openOwnArena(level);
                if (made == null) {
                    discard();
                    return;
                }
                bindArena(made);
            } else {
                returnToCage(level, "message.supernaturalcraft.lucifer.cage_gone");
                return;
            }
            arena = arena();
        }
        bossBar.setProgress(bossBarProgress());
        if (tickCount % 20 == 0) refreshBossBarViewers(level, arena);
        tetherToArena(arena);

        switch (state()) {
            case EMERGING -> {
                tickEmergence();
                if (--stateTimer <= 0) finishEmergence();
            }
            case TRANSITION -> tickTransition(level, arena);
            case DYING -> tickDying(level, arena);
            default -> {
                if (arena.checkAbandoned(level, SNConfig.FAILURE_SECONDS.get() * 20)) {
                    returnToCage(level, "message.supernaturalcraft.lucifer.victorious");
                    return;
                }
                tickCombat();
            }
        }
        if (isAerialPhase() && state() != DYING) {
            setNoGravity(true);
        }
    }

    private void tickCombat() {
        LivingEntity target = attackTarget();
        setTarget(target);
        if (target != null) {
            getLookControl().setLookAt(target, 30, 30);
            noSightTicks = hasLineOfSight(target) ? 0 : noSightTicks + 1;
        }
        scheduler.tick();
        BossAttack<LuciferEntity> attack = scheduler.current();
        boolean rooted = attack != null && !attack.movesBoss();
        if (isAerialPhase()) {
            if (!rooted || scheduler.stage() == AttackScheduler.Stage.RECOVER) hover(target);
            else setDeltaMovement(getDeltaMovement().scale(0.6));
        } else if (rooted) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        } else if (attack == null && target != null && walks()) {
            if (distanceToSqr(target) > 25) getNavigation().moveTo(target, 1.0);
            else getNavigation().stop();
        }
        if (tickCount % 120 == 0 && random.nextFloat() < 0.5f) playSound(ambientBossSound(), 2.0f, 1.0f);
    }

    /** P4 movement: circle the target a few blocks up, drifting rather than pathing. */
    private void hover(@Nullable LivingEntity target) {
        Vec3 anchor = target != null ? target.position() : (arena() != null ? arena().centerVec() : position());
        orbitAngle += 0.012f;
        Vec3 want = anchor.add(Math.cos(orbitAngle) * 8, 5.5 + Math.sin(tickCount * 0.05) * 0.6, Math.sin(orbitAngle) * 8);
        Vec3 delta = want.subtract(position());
        Vec3 v = getDeltaMovement().scale(0.85).add(delta.normalize().scale(Math.min(0.06, delta.length() * 0.02)));
        if (v.length() > 0.45) v = v.normalize().scale(0.45);
        setDeltaMovement(v);
    }

    private void tetherToArena(ArenaController arena) {
        if (arena.horizontalDistance(position()) > arena.radius() - 1.5) {
            Vec3 c = tetherPoint(arena);
            teleportTo(c.x, c.y, c.z);
        }
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
    public List<AttackScheduler.Option<LuciferEntity>> attackPool() {
        return pool(phase());
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
        int gap = baseGap(phase());
        return isEnraged() ? Math.round(gap * 0.6f) : gap;
    }

    @Override
    public float windupScale() {
        return isEnraged() ? 0.8f : 1.0f;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        switch (stage) {
            case WINDUP -> {
                setState(WINDUP);
                if (attack != null) triggerAnim("action", attack.animation);
            }
            case ACTIVE -> setState(ACTIVE);
            case RECOVER -> setState(RECOVER);
            case IDLE -> {
                if (!isInvulnerablePhase()) setState(IDLE);
                if (attack == null) attacksSinceSmite++;
            }
        }
    }

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (phase() == 4 && attacksSinceSmite >= 4) {
            attacksSinceSmite = 0;
            return LuciferAttacks.Smite::new;
        }
        if (phase() < 4 && (distanceToSqr(target) > 18 * 18 || noSightTicks > 60)) {
            noSightTicks = 0;
            return LuciferAttacks.Teleport::new;
        }
        return null;
    }

    // --- damage ---------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (BossDamage.passesThrough(source)) {
            boolean killed = super.hurt(source, amount);
            guard.accept(this);
            return killed;
        }
        if (isInvulnerablePhase() || source.is(AllTags.DamageTypes.LUCIFER_IMMUNE)) {
            if (source.getEntity() instanceof Player) deflect();
            return false;
        }
        if (source.getEntity() != null && source.getEntity().getType().is(AllTags.Entities.CAGE_DWELLERS)) return false;
        float mult = (Holy.isHoly(source) ? 1f : mundaneMultiplier()) * vulnerability(source);
        if (BossDamage.isExact(source)) amount = Math.min(amount, exactCap());
        else if (exactBlow(source)) amount = Math.min(amount, Balance.hardCap(trueMaxHealth()));
        else amount = BossDamage.softCap(source, amount, mult, trueMaxHealth());
        amount /= healthScale();

        // Never past the next threshold in one blow.
        int phase = phase();
        float floor = phase < maxPhase() ? getMaxHealth() * threshold(phase) : 1.0f;
        boolean crosses = getHealth() - amount <= floor;
        if (crosses) amount = Math.max(0, getHealth() - floor);
        if (source.getEntity() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
        if (scheduler.current() instanceof LuciferAttacks.Drain drain) drain.hits++;

        boolean hurt = amount <= 0 || super.hurt(source, amount);
        guard.accept(this);
        if (crosses && isAlive()) reachFloor(phase);
        return hurt;
    }

    /**
     * A blow (not the Colt's, which has {@link #exactCap}) that skips his multipliers and soft cap but never the hard cap:
     * whatever a variant lets through (Michael's own lance thrown back at him).
     */
    protected boolean exactBlow(DamageSource source) {
        return false;
    }

    /**
     * The vanilla health no blow may take him below now ({@link CappedBoss}): the phase's threshold, 1 in the last; while
     * he is untouchable, all of it.
     */
    @Override
    public float vanillaFloor() {
        if (isInvulnerablePhase()) return getMaxHealth();
        int phase = phase();
        return phase < maxPhase() ? getMaxHealth() * threshold(phase) : 1.0f;
    }

    private void deflect() {
        if (tickCount % 10 == 0) playSound(deflectSound(), 1.0f, 0.8f);
    }

    @Override
    public boolean onBound(int durationTicks) {
        if (state() == RECOVER) {
            scheduler.extendRecover(durationTicks / 2);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks / 2, 4));
            if (level() instanceof ServerLevel server) {
                server.sendParticles(AllParticles.SIGIL.get(), getX(), getY() + 1.5, getZ(), 30, 0.6, 1.0, 0.6, 0.05);
            }
            return true;
        }
        deflect();
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        var e = effect.getEffect();
        return e.equals(MobEffects.GLOWING) || e.equals(MobEffects.MOVEMENT_SLOWDOWN) || e.equals(MobEffects.WEAKNESS);
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
        return AllSounds.LUCIFER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.LUCIFER_DEATH.get();
    }

    // --- transitions ----------------------------------------------------------------------

    /** Development preview only: jump straight to a phase's look, with no transition. */
    public void forceLook(int phase) {
        entityData.set(PHASE, (byte) phase);
        setState(IDLE);
        refreshDimensions();
    }

    public void beginTransition(int to) {
        scheduler.cancel();
        getNavigation().stop();
        entityData.set(PHASE, (byte) to);
        setState(TRANSITION);
        stateTimer = transitionTicks(to);
        refreshDimensions();
        triggerAnim("action", "transform_" + to);
        playSound(transformSound(), 4.0f, 0.8f);
        playTransition(to);
        updateBossBar();
        // Shove everyone back to give the transformation room.
        for (ServerPlayer p : challengers()) {
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            if (away.length() < 8) {
                away = away.normalize();
                p.setDeltaMovement(away.x * 1.6, 0.5, away.z * 1.6);
                p.hurtMarked = true;
            }
        }
        onTransitionStart(to);
    }

    private void tickTransition(ServerLevel level, ArenaController arena) {
        boolean last = phase() == maxPhase();
        int total = transitionTicks(phase());
        int elapsed = total - stateTimer;
        tickTransitionMotion(elapsed, last);
        if (elapsed == total / 2) {
            applyTerrain(level, arena, phase());
            arena.setPhase(phase());
            ArenaEvents.broadcast(level, arena, true);
            playSound(roarSound(), 4.0f, last ? 0.7f : 1.0f);
        }
        if (elapsed % 4 == 0) {
            var particle = phaseParticle(phase());
            level.sendParticles(particle, getX(), getY() + getBbHeight() * 0.6, getZ(), 20, 1.2, 1.2, 1.2, 0.08);
        }
        if (--stateTimer <= 0) {
            setState(IDLE);
            scheduler.delay(30);
        }
    }

    /** Starts his long death (a variant that intercepted it calls this when its own finale ends). */
    protected void beginDying() {
        scheduler.cancel();
        setState(DYING);
        stateTimer = deathTicks();
        triggerAnim("action", "death");
        playSound(dyingSound(), 5.0f, 0.9f);
        playDeath();
        minions.clear();
    }

    private void tickDying(ServerLevel level, ArenaController arena) {
        int elapsed = deathTicks() - stateTimer;
        tickDyingMotion(elapsed);
        if (elapsed % 3 == 0) dyingParticles(level, false);
        if (--stateTimer <= 0) {
            ServerPlayer killer = lastPlayerAttacker == null ? null : (ServerPlayer) level.getPlayerByUUID(lastPlayerAttacker);
            if (killer != null) setLastHurtByPlayer(killer);
            dyingParticles(level, true);
            arena.beginRestore(true);
            onDefeated(level, arena);
            setHealth(0);
            guard.accept(this);
            die(killer != null ? damageSources().playerAttack(killer) : damageSources().magic());
        }
    }

    @Override
    protected void tickDeath() {
        // The long death already played while he was alive; vanish at once.
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    /** The fight is lost: everyone fell or fled. He walks back into his Cage and leaves the key cracked. */
    protected void returnToCage(ServerLevel level, String messageKey) {
        ArenaController arena = arena();
        Vec3 at = arena != null ? arena.centerVec().add(0, 1.5, 0) : position();
        leaveBehind(level, at);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 60, 0.6, 1.2, 0.6, 0.05);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
            }
        }
        if (arena != null) arena.beginRestore(false);
        discardMinions(level);
        bossBar.removeAllPlayers();
        discard();
    }

    private void discardMinions(ServerLevel level) {
        for (UUID id : minions) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
        minions.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server && reason.shouldDestroy()) discardMinions(server);
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    // --- presentation ---------------------------------------------------------------------

    protected void updateBossBar() {
        int phase = phase();
        bossBar.setName(bossBarName(phase));
        bossBar.setColor(bossBarColor(phase));
        bossBar.setCreateWorldFog(phase >= 3);
    }

    private void clientEffects() {
        int phase = phase();
        if (phase >= 2 && random.nextFloat() < 0.3f) {
            var particle = phaseParticle(phase);
            level().addParticle(particle, getRandomX(1.2), getY() + random.nextDouble() * getBbHeight(), getRandomZ(1.2), 0, 0.02, 0);
        }
        if (state() == EMERGING && random.nextFloat() < 0.8f) clientEmergenceParticles();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float s = scale(phase());
        return super.getDefaultDimensions(pose).scale(s, s);
    }

    public static float scaleFor(int phase) {
        return switch (phase) {
            case 1 -> 1.15f;
            case 2 -> 1.25f;
            case 3 -> 1.35f;
            default -> 1.6f;
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) refreshDimensions();
    }

    // --- GeckoLib -------------------------------------------------------------------------

    private RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop(animationPrefix() + name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = loop("idle"), walk = loop("walk"), fly = loop("idle_fly");
        RawAnimation wingsIdle = loop("wings_idle"), wingsFly = loop("wings_fly");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            if (state() == EMERGING || state() == TRANSITION || state() == DYING) return PlayState.STOP;
            if (isAerialPhase()) return state.setAndContinue(fly);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        controllers.add(new AnimationController<>(this, "wings", 8, state -> {
            if (phase() < 2) return PlayState.STOP;
            return state.setAndContinue(isAerialPhase() ? wingsFly : wingsIdle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            boolean hold = name.equals("emerge") || name.equals("death");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(animationPrefix() + name)
                    : RawAnimation.begin().thenPlay(animationPrefix() + name));
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
        tag.putFloat("HealthScale", healthScale);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Arena")) arenaId = tag.getUUID("Arena");
        entityData.set(PHASE, (byte) Mth.clamp(tag.getByte("Phase"), 1, maxPhase()));
        byte s = tag.getByte("State");
        // An interrupted transition or attack resumes as idle; emergence and death finish.
        setState(s == EMERGING || s == DYING || s == TRANSITION ? s : IDLE);
        stateTimer = tag.getInt("StateTimer");
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : -1f;
        guard.accept(this);
        updateBossBar();
        bossBar.setVisible(s != EMERGING);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        updateBossBar();
    }
}
