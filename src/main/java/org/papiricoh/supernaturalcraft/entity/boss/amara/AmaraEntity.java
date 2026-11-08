package org.papiricoh.supernaturalcraft.entity.boss.amara;

import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.balance.Balance;
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
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossHealthGuard;
import org.papiricoh.supernaturalcraft.entity.boss.CappedBoss;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
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
 * Amara, the Darkness (v0.42: the season 11 rework). Six phases, her body growing up as Creation comes undone:
 *
 * <pre>EMERGING → I The Mass → II The Girl → III The Woman → IV Siblings (Chuck) → V The Void → VI The Dying Sun → peace</pre>
 *
 * <p><b>I, the Mass.</b> Twelve {@link AmaraPart}s: four orbiting ANCHORs, six TENTACLE cysts, the CORE and the SHELL that
 * hides it. Break every anchor, or three cysts, and she sinks, EXPOSED, core open. <b>II-VI</b>: her own body is struck
 * (the {@link AmaraAssets#RIG_FORM form rig}): the girl hides among decoys, the woman duels, in IV her brother stands in the town
 * and his light ({@link #chuckLight()}) makes her bleed while she tears at it, in V the town crumbles into islands over the Void
 * ({@code AmaraGround}), in VI the world's sun dies with a countdown ({@link #countdown()}; at zero she rages). At the end she
 * does not die: she kneels, Chuck comes to her, and they make peace ({@link #DEATH_TICKS}); the sun comes back for everyone.
 *
 * <p><b>Damage to the core</b> follows {@link AmaraBalance#damageMultiplier}: the four Light Wells,
 * holy strikes and light on the core all count. Hits use her vulnerability multiplier
 * ({@link BossDamage#scale}), and none carries her past a phase threshold. Her true health (the power
 * curve's) is {@link #VANILLA_BASE} vanilla points times {@link #healthScale()}.
 */
public class AmaraEntity extends Monster implements GeoEntity, AttackScheduler.Host<AmaraEntity>, CappedBoss, AmaraLook {

    public static final byte EMERGING = 0, IDLE = 1, WINDUP = 2, ACTIVE = 3, RECOVER = 4, TRANSITION = 5, DYING = 6, EXPOSED = 7;
    /** The last phase; her "death" is the reconciliation ({@link #DEATH_TICKS} long). */
    public static final int MAX_PHASE = 6;
    public static final int EMERGE_TICKS = 360, DEATH_TICKS = 320;
    /** How long the change into each phase lasts (index = the phase she becomes). */
    public static final int[] TRANSITION_TICKS = {0, 0, 160, 200, 220, 260, 220};
    /** Phase VI's countdown (2 minutes), and how long the girl stays open once found. */
    public static final int COUNTDOWN_TICKS = 2400, FOUND_TICKS = 100;
    /** Phase IV: how often Chuck's light burns her, how long it lasts, what it multiplies; and how long he lies dark if she puts it out. */
    public static final int CHUCK_SHINE_EVERY = 160, CHUCK_SHINE_TICKS = 60, CHUCK_DARK_TICKS = 200;
    public static final float CHUCK_SHINE_MULT = 1.6f, CHUCK_DARK_HEAL = 0.06f, CHAINS_BROKEN_LIGHT = 0.15f;
    /** Phase VI: what her rage multiplies, once the countdown is out. */
    public static final float RAGE_DAMAGE = 1.3f, RAGE_GAP = 0.5f, RAGE_WINDUP = 0.7f;
    public static final int P1_EXPOSE_TICKS = 240, P2_EXPOSE_TICKS = 200, CYST_REGROW_TICKS = 600, CYSTS_TO_EXPOSE = 3;
    /** What an anchor and a cyst hold, as shares of her true max health. */
    public static final float ANCHOR_SHARE = 0.05f, CYST_SHARE = 0.04f;
    /** Her vanilla max health: the bar is a proportion, true health is this times the scale. */
    public static final float VANILLA_BASE = 1000f;
    private static final float[] THRESHOLDS = {0.84f, 0.68f, 0.52f, 0.36f, 0.18f};

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
    private static final EntityDataAccessor<Float> CHUCK_LIGHT = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COUNTDOWN = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.FLOAT);
    /** Bits: 1 hidden (phase II), 2 enraged (phase VI), 4 her sky's eyes open (phase V below half). */
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(AmaraEntity.class, EntityDataSerializers.BYTE);

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
    private final BossHealthGuard guard = new BossHealthGuard();
    private final org.papiricoh.supernaturalcraft.entity.boss.DifficultyLock difficulty = new org.papiricoh.supernaturalcraft.entity.boss.DifficultyLock();
    /** True health per point of vanilla health; fixed for the challengers present when she emerges. */
    private float healthScale = -1f;
    private boolean lastFloorReached;
    // v0.42
    private @Nullable org.papiricoh.supernaturalcraft.entity.boss.amara.arena.AmaraGround ground;
    private @Nullable UUID chuckId;
    private int countdownLeft = COUNTDOWN_TICKS, chuckShine, chuckDark, hurts;
    /** A line held back a moment (her answer to her brother), and when; and whether Chuck's first light was spoken for. */
    private int pendingVoice = -1, pendingVoiceAt;
    private boolean chuckSpoke;
    /** The Hands spent on her rite beyond the three it needs: each one weakens her at the start ({@link AmaraBalance#handWeakening}). */
    private int extraHands;

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
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
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
        builder.define(CHUCK_LIGHT, 0f);
        builder.define(COUNTDOWN, 1f);
        builder.define(FLAGS, (byte) 0);
    }

    private boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    private void setFlag(int bit, boolean on) {
        byte f = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? f | bit : f & ~bit));
    }

    // --- state ----------------------------------------------------------------------------

    @Override
    public int phase() {
        return entityData.get(PHASE);
    }

    // --- AmaraLook ------------------------------------------------------------------------------------------------------

    @Override
    public int form() {
        return switch (phase()) {
            case 1 -> MASS;
            case 2 -> GIRL;
            case 5 -> VOID;
            default -> state() == DYING ? SPENT : WOMAN;
        };
    }

    @Override
    public int skyStage() {
        if (state() == DYING) return 5;
        return switch (phase()) {
            case 3, 4 -> 1;
            case 5 -> flag(4) ? 3 : 2;
            case 6 -> 4;
            default -> 0;
        };
    }

    @Override
    public float chuckLight() {
        return entityData.get(CHUCK_LIGHT);
    }

    @Override
    public float countdown() {
        return entityData.get(COUNTDOWN);
    }

    @Override
    public boolean enraged() {
        return flag(2);
    }

    /** Phase II: curled into smoke among her decoys (untouchable until found). */
    public boolean hidden() {
        return flag(1);
    }

    public void setHidden(boolean on) {
        setFlag(1, on);
    }

    /** How many blows have landed on her (attacks that a few blows break count from this). */
    public int hurtCounter() {
        return hurts;
    }

    /** She mends a share of her vanilla max health (the soul she drank), never past her phase's start. */
    public void healShare(float share) {
        int phase = phase();
        float ceiling = phase == 1 ? getMaxHealth() : getMaxHealth() * THRESHOLDS[phase - 2];
        setHealth(Math.min(ceiling, getHealth() + getMaxHealth() * share));
        guard.accept(this);
    }

    /** How many of her fight's minions an attack calls at the fight's difficulty. */
    public int minionCount(int base) {
        return difficulty().minions(base);
    }

    public @Nullable org.papiricoh.supernaturalcraft.entity.boss.amara.arena.AmaraGround ground() {
        return ground;
    }

    /** Her brother in the town (phases IV-VI), if he is there. */
    public org.papiricoh.supernaturalcraft.darkness.chuck.@Nullable ChuckAllyEntity chuck() {
        if (chuckId == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(chuckId) instanceof org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity c && c.isAlive() ? c : null;
    }

    /** She tears at his light. */
    public void tearChuck(float amount) {
        var c = chuck();
        if (c == null || chuckDark > 0) return;
        float l = Math.max(0f, chuckLight() - amount);
        setChuckLight(c, l);
        if (l <= 0f) {
            // She put it out: he lies dark a while and she drinks what she took.
            chuckDark = CHUCK_DARK_TICKS;
            healShare(CHUCK_DARK_HEAL);
            for (ServerPlayer p : challengers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.amara.chuck_dark").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    /** The hunters broke her grip on him: some of his light comes back. */
    public void chainsBroken() {
        var c = chuck();
        if (c != null) setChuckLight(c, chuckLight() + CHAINS_BROKEN_LIGHT);
        playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("wound"), 2f, 1.4f);
    }

    private void setChuckLight(org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity c, float l) {
        l = Math.max(0f, Math.min(1f, l));
        c.setLight(l);
        entityData.set(CHUCK_LIGHT, l);
        AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.CHUCK_LIGHT, c.position(), 0, l, 40);
    }

    /** A voiced line in the overlay ({@code message.supernaturalcraft.amara.voice.<n>}: the art's table in {@code AmaraOverlay}). */
    public void voice(int n) {
        AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.VOICE, position(), n, 0, 90);
    }

    /** Plays a one-shot clip on whichever rig she wears now. */
    public void act(String clip) {
        triggerAnim(form() == MASS ? "action" : "form_action", clip);
    }

    /**
     * Moves her toward {@code to} (her later bodies glide; they do not walk): at once if {@code instant} (a shadow step), else one
     * eased step of her drift.
     */
    public void glideTo(Vec3 to, boolean instant) {
        if (instant) {
            setPos(to);
            setDeltaMovement(Vec3.ZERO);
        } else {
            setPos(position().add(to.subtract(position()).scale(0.5)));
        }
    }

    /** The Hands spent beyond the three the rite needs (set by the rite). */
    public void setExtraHands(int n) {
        extraHands = Math.max(0, n);
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
        return exposed();
    }

    public boolean isEnraged() {
        return enraged();
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

    /** Parts that live in a phase: in the Mass (phase I) the anchors, the cysts, core and shell; none after. */
    private static int maskFor(int phase) {
        if (phase != 1) return 0;
        return (0b1111 << FIRST_ANCHOR) | (0b111111 << FIRST_TENTACLE) | (1 << CORE) | (1 << SHELL);
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
        float trueMax = trueMaxHealth();
        for (int i = 0; i < PART_COUNT; i++) {
            partHealth[i] = i < FIRST_TENTACLE ? ANCHOR_SHARE * trueMax : i < CORE ? CYST_SHARE * trueMax : 0;
        }
    }

    // --- health ---------------------------------------------------------------------------

    /** Her true max health for {@code players} challengers. */
    public float trueHealthFor(int players) {
        float base = org.papiricoh.supernaturalcraft.balance.Balance.bossHealth(BossProgression.Boss.AMARA);
        return AmaraBalance.scaled(base, SNConfig.AMARA_HEALTH_PER_PLAYER.get(), Math.max(1, players)) * difficulty().health();
    }

    /** The fight's difficulty (v0.24): the world's until the fight starts, then fixed until it ends. */
    public org.papiricoh.supernaturalcraft.balance.BossDifficulty difficulty() {
        return difficulty.get(level());
    }

    /** Test and command hook: fixes the fight's difficulty. */
    public void setDifficulty(org.papiricoh.supernaturalcraft.balance.BossDifficulty d) {
        difficulty.set(d);
    }

    /** Peaceful is a story mode: great enemies stay (vanilla would remove every {@code Monster}). */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** True health per point of vanilla health (before she emerges, the curve's for one challenger). */
    @Override
    public float healthScale() {
        return healthScale > 0 ? healthScale : trueHealthFor(1) / VANILLA_BASE;
    }

    /** Test hook: sets the true-health multiplier directly (normally fixed when she emerges). */
    public void setHealthScale(float scale) {
        healthScale = scale;
        resetPartHealth();
    }

    public float trueHealth() {
        return getHealth() * healthScale();
    }

    @Override
    public float trueMaxHealth() {
        return getMaxHealth() * healthScale();
    }

    /** The vanilla health no blow may take her below now: the phase's threshold, 1 in the last; untouchable, all of it. */
    @Override
    public float vanillaFloor() {
        if (isInvulnerablePhase()) return getMaxHealth();
        int phase = phase();
        return phase < MAX_PHASE ? getMaxHealth() * THRESHOLDS[phase - 1] : 1.0f;
    }

    @Override
    public void acceptHealth() {
        guard.accept(this);
    }

    /** What every attack of hers is multiplied by: the power curve's, and her config factor. */
    public float attackDamageMultiplier() {
        return org.papiricoh.supernaturalcraft.balance.Balance.bossDamage(BossProgression.Boss.AMARA)
                * SNConfig.AMARA_DAMAGE_FACTOR.get().floatValue() * difficulty().damage() * (enraged() ? RAGE_DAMAGE : 1f);
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
        difficulty.lock(level(), challengers());
        healthScale = trueHealthFor(n) / VANILLA_BASE;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(VANILLA_BASE);
        // Every Hand spent beyond the rite's three weakens her from the start (never past her first phase's floor).
        setHealth(VANILLA_BASE * (1f - AmaraBalance.handWeakening(extraHands)));
        guard.accept(this);
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
        if (ground != null && level() instanceof ServerLevel level && arena() != null) ground.finish(level, arena());
    }

    // --- ticking --------------------------------------------------------------------------

    @Override
    public void tick() {
        // Health lost outside her own pipeline becomes one capped blow, before vanilla can start a death.
        if (!level().isClientSide && !dead) {
            guard.tick(this, this);
            checkFloor();
        }
        super.tick();
        placeParts();
    }

    /** A blow (or a guarded drop) that left her at the phase's floor starts the change, or her death. */
    private void checkFloor() {
        if (isInvulnerablePhase() || !isAlive()) return;
        int phase = phase();
        if (phase < MAX_PHASE) {
            if (getHealth() <= getMaxHealth() * THRESHOLDS[phase - 1] + 1e-3f) beginTransition(phase + 1);
        } else if (getHealth() <= 1.0001f) {
            if (!lastFloorReached) {
                lastFloorReached = true;
                beginDying();
            }
        } else {
            lastFloorReached = false;
        }
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
        if (ground == null) {
            ground = org.papiricoh.supernaturalcraft.entity.boss.amara.arena.AmaraGround.pin(arena);
            ground.buildTown();
        }
        Integer fell = ground.tick(level, arena);
        if (fell != null) {
            AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.CRUMBLE, arena.centerVec(), 0,
                    arena.radius() - 1 - fell * 2, 30);
            if (fell % 3 == 0) playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("crumble"), 4f, 0.6f);
        }
        drift(arena);
        if (state() != EMERGING && state() != DYING) tickLater(level, arena);

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

    /**
     * Where she glides: the Mass hovers over her arena's heart (sinking when exposed); her later bodies follow their target at
     * their own pace (the girl quick, the woman steady, the Void high over the islands, the spent sun low and slow).
     */
    private void drift(ArenaController arena) {
        Vec3 c = arena.centerVec();
        double hover = exposed() ? -2.5 : 0.5;
        if (state() == EMERGING) hover = -6.0 + 6.5 * Math.min(1, (EMERGE_TICKS - stateTimer) / 120.0);
        Vec3 want = new Vec3(c.x + Math.cos(tickCount * 0.01) * 1.6, c.y + hover + Math.sin(tickCount * 0.04) * 0.25,
                c.z + Math.sin(tickCount * 0.01) * 1.6);
        if (phase() >= 2 && state() != EMERGING) {
            LivingEntity t = attackTarget();
            double speed = switch (phase()) {
                case 2 -> 0.22;
                case 5 -> 0.10;
                case 6 -> 0.07;
                default -> 0.14;
            };
            double y = phase() == 5 ? c.y + 9 + Math.sin(tickCount * 0.03) * 0.6 : c.y + (phase() == 6 ? 0.2 : 0.6) + Math.sin(tickCount * 0.05) * 0.2;
            boolean held = (scheduler.current() != null && !scheduler.current().movesBoss()) || state() == DYING || exposed();
            if (t != null && !held && distanceToSqr(t) > (phase() == 3 ? 6 : 20)) {
                Vec3 to = t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(speed);
                want = new Vec3(getX() + to.x, y, getZ() + to.z);
            } else {
                want = new Vec3(getX(), y, getZ());
            }
            if (t != null) lookAtFlat(t);
            if (arena.horizontalDistance(want) > arena.radius() - 3) want = new Vec3(c.x, y, c.z);
            setPos(position().add(want.subtract(position()).scale(0.25)));
            setDeltaMovement(Vec3.ZERO);
            return;
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
        LivingEntity target = attackTarget();
        setTarget(target);
        if (phase() == 1 && brokenCysts() > 0 && --regrowTimer <= 0) regrowCysts();
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

    /** Each tick of her later phases, whatever she is doing: Chuck's light, the Void's sky, the dying sun's countdown. */
    private void tickLater(ServerLevel level, ArenaController arena) {
        int phase = phase();
        if (pendingVoice >= 0 && tickCount >= pendingVoiceAt) {
            voice(pendingVoice);
            pendingVoice = -1;
        }
        var c = chuck();
        if (phase >= 4 && c != null) {
            if (chuckDark > 0 && --chuckDark == 0) setChuckLight(c, 0.4f);
            if (chuckShine > 0) chuckShine--;
            if (chuckDark == 0 && chuckLight() > 0.3f && tickCount % CHUCK_SHINE_EVERY == 0) {
                // His light burns her: for a while every blow bites deeper.
                chuckShine = CHUCK_SHINE_TICKS;
                if (!chuckSpoke) {
                    chuckSpoke = true;
                    voice(10);
                }
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.CHUCK_LIGHT, c.position(), 0, chuckLight(), CHUCK_SHINE_TICKS);
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            }
        }
        if (phase == 5) {
            boolean eyes = getHealth() < getMaxHealth() * (THRESHOLDS[3] + THRESHOLDS[4]) / 2f;
            if (eyes != flag(4)) {
                setFlag(4, eyes);
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SKY, position(), 0, skyStage(), 60);
            }
        }
        if (phase == 6 && state() != TRANSITION) {
            if (countdownLeft > 0) countdownLeft--;
            float cd = countdownLeft / (float) COUNTDOWN_TICKS;
            if (tickCount % 10 == 0) entityData.set(COUNTDOWN, cd);
            if (tickCount % 20 == 0) org.papiricoh.supernaturalcraft.dyingsun.DyingSun.fightOverride(level.getServer(), cd);
            if (countdownLeft == COUNTDOWN_TICKS / 2) voice(16);
            if (countdownLeft % 600 == 0 && countdownLeft > 0) {
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SUN_DIE, position(), 0, cd, 40);
                playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("heartbeat"), 4f, 0.6f + (1 - cd) * 0.4f);
            }
            if (countdownLeft == 0 && !enraged()) {
                setFlag(2, true);
                act("enrage");
                voice(17);
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.TITLE, position(), 7, 0, 60);
                playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("enrage"), 5f, 0.6f);
            }
        }
    }

    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            if (org.papiricoh.supernaturalcraft.allegiance.BossTwists.amaraIgnores(p, level().getGameTime())) continue;
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
            case 2 -> 28;
            case 3 -> 30;
            case 4 -> 34;
            case 5 -> 32;
            default -> 26;
        };
        gap = org.papiricoh.supernaturalcraft.balance.BossDifficulty.ticks(gap, difficulty().gap());
        return isEnraged() ? Math.round(gap * RAGE_GAP) : gap;
    }

    @Override
    public float windupScale() {
        return (isEnraged() ? RAGE_WINDUP : 1.0f) * difficulty().windup();
    }

    @Override
    public float recoverScale() {
        return difficulty().recover();
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<AmaraEntity> attack) {
        if (state() == EXPOSED || isInvulnerablePhase()) return;
        switch (stage) {
            case WINDUP -> {
                setState(WINDUP);
                if (attack != null) act(attack.animation);
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
        amount = org.papiricoh.supernaturalcraft.allegiance.BossTwists.amaraDamage(source, amount);
        float damage = BossDamage.scale(source, amount, Holy.isHoly(source) ? 1.5f : 1f);
        if (BossDamage.isExact(source)) damage = Math.min(damage, exactCap());
        damage = Balance.perHitLimit(damage, trueMaxHealth());
        partHealth[i] = Math.max(0, partHealth[i] - damage);
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
        if (phase() == 1 && aliveAnchors() == 0 && !exposed()) expose(P1_EXPOSE_TICKS);
        if (phase() == 1 && parts[i].kind == AmaraPart.Kind.TENTACLE) {
            if (brokenCysts() == 1) regrowTimer = CYST_REGROW_TICKS;
            if (brokenCysts() >= CYSTS_TO_EXPOSE && !exposed()) expose(P2_EXPOSE_TICKS);
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
        amount = org.papiricoh.supernaturalcraft.allegiance.BossTwists.amaraDamage(source, amount);
        float mult = AmaraBalance.damageMultiplier(litWells(), Holy.isHoly(source), phase() == 1 ? coreLit() : bodyLit());
        if (chuckShine > 0) mult *= CHUCK_SHINE_MULT;
        if (phase() >= 2 && exposed()) mult *= 1.5f;
        amount = BossDamage.scale(source, amount, mult) / healthScale();
        hurts++;
        int phase = phase();
        float floor = phase < MAX_PHASE ? getMaxHealth() * THRESHOLDS[phase - 1] : 1.0f;
        boolean crosses = getHealth() - amount <= floor;
        if (crosses) amount = Math.max(0, getHealth() - floor);
        routing = true;
        boolean hurt;
        try {
            hurt = amount <= 0 || super.hurt(source, amount);
        } finally {
            routing = false;
        }
        guard.accept(this);
        if (crosses && isAlive()) {
            if (phase < MAX_PHASE) beginTransition(phase + 1);
            else beginDying();
        }
        return hurt;
    }

    /** Whether light falls on her body (her later forms), as on the core. */
    public boolean bodyLit() {
        return level().getBrightness(LightLayer.BLOCK, BlockPos.containing(getBoundingBox().getCenter())) >= AmaraBalance.LIT;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (routing) return super.hurt(source, amount);
        if (BossDamage.passesThrough(source)) {
            boolean killed = super.hurt(source, amount);
            guard.accept(this);
            return killed;
        }
        if (phase() >= 2 && !isInvulnerablePhase() && !isDarkness(source)) {
            if (source.getEntity() instanceof ServerPlayer p) lastPlayerAttacker = p.getUUID();
            if (hidden()) {
                // Found: she is startled out of the smoke, open a while.
                if (!(source.getEntity() instanceof Player)) return false;
                setHidden(false);
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.FOUND, position(), 0, 3f, 20);
                playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("child_scream"), 3f, 1.4f);
                expose(FOUND_TICKS);
                voice(2);
                return true;
            }
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
        // The Mass is struck only through her parts; her later bodies are struck directly.
        return phase() >= 2 && !isInvulnerablePhase();
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
        act(phase() == 1 ? "expose" : "kneel");
        playSound(AllSounds.AMARA_HURT.get(), 4.0f, 0.5f);
        for (ServerPlayer p : challengers()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.amara.exposed").withStyle(ChatFormatting.GOLD), true);
        }
    }

    private void endExposure() {
        setState(IDLE);
        if (phase() == 1) triggerAnim("action", "close");
        if (phase() == 1) {
            for (int i = 0; i < ANCHORS; i++) setPartAlive(FIRST_ANCHOR + i, true);
            for (int i = 0; i < TENTACLES; i++) setPartAlive(FIRST_TENTACLE + i, true);
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
        setHidden(false);
        if (to == 2) triggerAnim("action", "shatter");
        else act(switch (to) {
            case 3 -> "become_woman";
            case 5 -> "become_void";
            case 6 -> "become_sun";
            default -> "scream";
        });
        if (to == 2) AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SHATTER, position().add(0, MASS_CENTER, 0), 0, 6f, 60);
        playSound(AllSounds.AMARA_ROAR.get(), 5.0f, to >= 5 ? 0.5f : 0.7f);
        AmaraCinematics.transition(this, to);
        updateBossBar();
        onPhaseStart(to);
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

    /** What each new phase brings into the town as it begins. */
    private void onPhaseStart(int to) {
        if (!(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (to == 4 && arena != null && chuck() == null) {
            // Her brother comes into the town; and, if he was taken before her eyes, the Devil she held.
            var c = org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.CHUCK_ALLY.get().create(level);
            if (c != null) {
                Vec3 at = AmaraAttacks.floorAt(this, arena.centerVec().add(0, 0, 7));
                c.moveTo(at.x, at.y, at.z, 180, 0);
                c.setRole(org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity.ALLY);
                c.setLight(1f);
                level.addFreshEntity(c);
                chuckId = c.getUUID();
                entityData.set(CHUCK_LIGHT, 1f);
            }
            boolean taken = false;
            for (ServerPlayer p : challengers()) {
                taken |= org.papiricoh.supernaturalcraft.darkness.Darknesses.step(p).atLeast(org.papiricoh.supernaturalcraft.darkness.DarknessRules.Step.TAKEN);
            }
            if (taken) {
                org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity.freed(level, AmaraAttacks.floorAt(this, arena.centerVec().add(-6, 0, 5)));
                pendingVoice = 11;
                pendingVoiceAt = tickCount + 160;
            }
        }
        if (to == 5 && ground != null) ground.beginVoid();
        switch (to) {
            case 2 -> voice(1);
            case 3 -> voice(4);
            case 4 -> {
                voice(7);
                pendingVoice = 8;
                pendingVoiceAt = tickCount + 70;
            }
            case 5 -> voice(12);
            case 6 -> voice(15);
            default -> {
            }
        }
        if (to == 6) {
            countdownLeft = COUNTDOWN_TICKS;
            entityData.set(COUNTDOWN, 1f);
        }
        AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SKY, position(), 0, skyStage(), 80);
    }

    private void tickTransition(ServerLevel level, ArenaController arena) {
        if (stateTimer % 4 == 0) {
            double y = phase() == 2 ? MASS_CENTER : getBbHeight() * 0.6;
            level.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + y, getZ(), 24, 2.0, 2.0, 2.0, 0.1);
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

    /** The end: she kneels, her brother comes to her, they make peace and go (no death: the reconciliation). */
    private void beginDying() {
        scheduler.cancel();
        setHidden(false);
        setState(DYING);
        stateTimer = DEATH_TICKS;
        act("kneel");
        voice(18);
        playSound(org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.amara("reconcile"), 5.0f, 0.8f);
        AmaraCinematics.death(this);
        AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SKY, position(), 0, 5, 120);
        if (level() instanceof ServerLevel level) org.papiricoh.supernaturalcraft.dyingsun.DyingSun.fightOverride(level.getServer(), -1f);
    }

    private void tickDying(ServerLevel level, ArenaController arena) {
        int elapsed = DEATH_TICKS - stateTimer;
        var c = chuck();
        if (c == null && elapsed == 1) {
            // He comes to her even if he never stood in the town (a fight called by command).
            c = org.papiricoh.supernaturalcraft.darkness.DarknessRegistry.CHUCK_ALLY.get().create(level);
            if (c != null) {
                Vec3 at = position().add(4, 0, 0);
                c.moveTo(at.x, at.y, at.z, 90, 0);
                c.setRole(org.papiricoh.supernaturalcraft.darkness.chuck.ChuckAllyEntity.ALLY);
                level.addFreshEntity(c);
                chuckId = c.getUUID();
            }
        }
        if (c != null) {
            if (elapsed < 120) c.getNavigation().moveTo(getX(), getY(), getZ(), 0.8);
            else c.getNavigation().stop();
            if (elapsed == 120) {
                act("embrace");
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.RECONCILE, position().add(0, 1.2, 0), 0, 0, 160);
                AmaraFx.send(this, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.TITLE, position(), 8, 0, 120);
            }
            if (elapsed == 130) voice(19);
            if (elapsed == 170) voice(20);
            if (elapsed == 205) voice(21);
            if (elapsed == 220) {
                act("depart");
                voice(22);
            }
            if (elapsed % 10 == 0) setChuckLight(c, Math.min(1f, 0.3f + elapsed / 160f));
        }
        if (elapsed % 3 == 0) {
            double a = random.nextDouble() * Math.PI * 2;
            level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.5, getY() + 1.2, getZ() + Math.sin(a) * 1.5,
                    0, Math.cos(a) * 0.3, 0.3, Math.sin(a) * 0.3, 1.0);
            level.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + 1.2, getZ(), 6, 0.6, 1.0, 0.6, 0.04);
        }
        if (--stateTimer <= 0) {
            ServerPlayer killer = lastPlayerAttacker == null ? null : (ServerPlayer) org.papiricoh.supernaturalcraft.util.Players.byUuid(level, lastPlayerAttacker);
            if (killer != null) setLastHurtByPlayer(killer);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2, getZ(), 4, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 2, getZ(), 200, 2.0, 2.5, 2.0, 0.3);
            AmaraSpoils.deal(level, this, challengers());
            releaseChallengers();
            arena.beginRestore(true);
            AmaraCinematics.victory(this);
            if (c != null) c.leave();
            // She is at peace: the sun comes back, for everyone and for good.
            org.papiricoh.supernaturalcraft.dyingsun.DyingSun.restore(level.getServer());
            setHealth(0);
            guard.accept(this);
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
        org.papiricoh.supernaturalcraft.dyingsun.DyingSun.fightOverride(level.getServer(), -1f);
        // What the rite spent comes back through the arena's stake; her brother goes too.
        var c = chuck();
        if (c != null) c.leave();
        bossBar.removeAllPlayers();
        discard();
    }

    private void releaseChallengers() {
        for (ServerPlayer p : challengers()) Consumption.release(p);
        // Her shades go back into the dark with her.
        for (AmaraShade s : level().getEntitiesOfClass(AmaraShade.class, getBoundingBox().inflate(64))) s.unmake();
        for (var cas : level().getEntitiesOfClass(org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity.class, getBoundingBox().inflate(64),
                x -> x.role() == org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity.FIGHT)) cas.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    // --- presentation ---------------------------------------------------------------------

    private void updateBossBar() {
        bossBar.setName(Component.translatable(AmaraAssets.BAR_PREFIX + ".phase" + phase()).withStyle(ChatFormatting.LIGHT_PURPLE));
        bossBar.setColor(phase() == 4 ? BossEvent.BossBarColor.WHITE : phase() == 6 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        bossBar.setCreateWorldFog(true);
    }

    private void clientEffects() {
        if (random.nextFloat() < 0.6f) {
            double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 2;
            level().addParticle(AllParticles.VOID_MOTE.get(), getX() + Math.cos(a) * r, getY() + MASS_CENTER + random.nextGaussian(),
                    getZ() + Math.sin(a) * r, -Math.cos(a) * 0.05, 0.01, -Math.sin(a) * 0.05);
        }
        if (phase() == 1 && coreOpen() && random.nextFloat() < 0.4f) {
            level().addParticle(AllParticles.GRACE.get(), getX(), getY() + MASS_CENTER, getZ(),
                    random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (phase() == 1) return super.getDefaultDimensions(pose);
        AmaraAssets.Age age = age();
        float k = age != null ? age.scale : 1f;
        return EntityDimensions.scalable(AmaraAssets.FORM_WIDTH * k * 1.2f, AmaraAssets.FORM_HEIGHT * k);
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

    /**
     * Two rigs, two pairs of controllers: {@code base}/{@code action} drive the Mass ({@code animation.amara.*}),
     * {@code form_base}/{@code form_action} her later bodies ({@code animation.amara_form.*}); each stands still while the other
     * rig is drawn.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation mass = RawAnimation.begin().thenLoop("animation.amara.idle");
        RawAnimation open = RawAnimation.begin().thenLoop("animation.amara.idle_open");
        controllers.add(new AnimationController<>(this, "base", 8, state -> {
            if (state() == EMERGING || form() != MASS) return PlayState.STOP;
            return state.setAndContinue(coreOpen() ? open : mass);
        }));
        AnimationController<AmaraEntity> action = new AnimationController<>(this, "action", 4, state -> PlayState.STOP);
        for (String name : AmaraAssets.MASS_TRIGGERED) {
            boolean hold = name.equals("emerge") || name.equals("death") || name.equals("expose") || name.equals("shatter");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold("animation.amara." + name)
                    : RawAnimation.begin().thenPlay("animation.amara." + name));
        }
        controllers.add(action);
        String f = "animation." + AmaraAssets.RIG_FORM + ".";
        RawAnimation girl = RawAnimation.begin().thenLoop(f + "child_idle"), idle = RawAnimation.begin().thenLoop(f + "idle"),
                floatLoop = RawAnimation.begin().thenLoop(f + "float"), kneel = RawAnimation.begin().thenLoop(f + "kneel_idle"),
                embrace = RawAnimation.begin().thenLoop(f + "embrace_idle");
        controllers.add(new AnimationController<>(this, "form_base", 8, state -> {
            if (form() == MASS) return PlayState.STOP;
            if (state() == DYING) return state.setAndContinue(DEATH_TICKS - stateTimer > 120 ? embrace : kneel);
            if (exposed()) return state.setAndContinue(kneel);
            return state.setAndContinue(switch (form()) {
                case GIRL -> girl;
                case VOID -> floatLoop;
                default -> phase() >= 3 ? floatLoop : idle;
            });
        }));
        AnimationController<AmaraEntity> formAction = new AnimationController<>(this, "form_action", 3, state -> PlayState.STOP);
        for (String name : AmaraAssets.FORM_TRIGGERED) {
            boolean hold = name.equals("kneel") || name.equals("depart") || name.equals("become_sun");
            formAction.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(f + name)
                    : RawAnimation.begin().thenPlay(f + name));
        }
        controllers.add(formAction);
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
        tag.putFloat("HealthScale", healthScale);
        difficulty.save(tag);
        if (ground != null) {
            CompoundTag g = new CompoundTag();
            ground.save(g);
            tag.put("Ground", g);
        }
        if (chuckId != null) tag.putUUID("Chuck", chuckId);
        tag.putInt("Countdown", countdownLeft);
        tag.putInt("ExtraHands", extraHands);
        tag.putByte("Flags", entityData.get(FLAGS));
        ListTag ws = new ListTag();
        wells.forEach(w -> ws.add(LongTag.valueOf(w.asLong())));
        tag.put("Wells", ws);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Arena")) arenaId = tag.getUUID("Arena");
        entityData.set(PHASE, (byte) Mth.clamp(tag.getByte("Phase"), 1, MAX_PHASE));
        byte s = tag.getByte("State");
        setState(s == EMERGING || s == DYING || s == TRANSITION ? s : IDLE);
        stateTimer = tag.getInt("StateTimer");
        if (tag.contains("Parts")) entityData.set(PARTS_ALIVE, tag.getInt("Parts"));
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : -1f;
        difficulty.load(tag, healthScale > 0);
        guard.accept(this);
        wells.clear();
        for (Tag t : tag.getList("Wells", Tag.TAG_LONG)) wells.add(BlockPos.of(((LongTag) t).getAsLong()));
        resetPartHealth();
        if (tag.contains("Ground") && arena() != null) {
            ground = org.papiricoh.supernaturalcraft.entity.boss.amara.arena.AmaraGround.pin(arena());
            ground.load(tag.getCompound("Ground"));
        }
        if (tag.hasUUID("Chuck")) chuckId = tag.getUUID("Chuck");
        if (tag.contains("Countdown")) countdownLeft = tag.getInt("Countdown");
        extraHands = tag.getInt("ExtraHands");
        entityData.set(FLAGS, (byte) (tag.getByte("Flags") & ~1));
        updateBossBar();
        bossBar.setVisible(s != EMERGING);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        updateBossBar();
    }
}
