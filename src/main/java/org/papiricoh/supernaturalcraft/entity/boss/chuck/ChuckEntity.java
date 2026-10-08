package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorRewards;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.arena.ChuckArenas;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Chuck, the Author: the last boss. Five phases, one per {@link Chapter}; he writes a new arena for each.
 *
 * <pre>EMERGING (he stands, straightens his flannel; the cabin unwrites into Eden) → P1 Eden (the man, flannel)
 *   → P2 Hell and the Cage (the suit) → P3 the Chorus's storm (he comes apart into the light: rings, halo, two hands)
 *   → P4 the Scribe's library → P5 the Blank Page → FINALE (Dean, Sam and Castiel hold him) → DYING (he approves)</pre>
 *
 * <p><b>Damage policy</b> ({@link #damageable}). As a man he takes damage like any boss (holy/mundane multipliers, a hit
 * cap, exact Colt rounds, no phase skipping). As the light he turns every blow aside unless a window is open: tear the
 * floating manuscript pages (3), break his rings' weak points (4), contradict his narration (5). Never while a chapter
 * is being written, nor during the finale, except the one blow it waits for.
 *
 * <p><b>Fourth wall.</b> His boss bar lies (refills, fake values, other names) until the chapter's script first breaks;
 * the HUD is rewritten now and then; the credits roll once, falsely, in chapter 4. The client shows all of it from
 * {@code AuthorFxPayload}s ({@link ChuckFx}).
 *
 * <p>Clips: the man's on the {@code action} controller, the light's on {@code divine}; {@link #triggerAnim} routes the
 * base class's triggers to whichever form he wears ({@link #divine()}, synced, switched halfway through a transition).
 */
public class ChuckEntity extends LuciferEntity implements ChuckLook {

    public static final int MAX_PHASE = 5;
    /** The light's hitbox, times the man's (0.6 × 1.9 → 3 × 9.5 blocks). */
    public static final float DIVINE_HITBOX = 5f;

    /** {@link #finaleStage()}: none yet; the brothers and the angel step out of the light; they hold him; the blow is
     * awaited (the next hit by a hunter ends him); he approves and fades. */
    public static final byte FINALE_NONE = 0, FINALE_ARRIVAL = 1, FINALE_HOLD = 2, FINALE_AWAIT_BLOW = 3, FINALE_APPROVAL = 4;
    /** {@link #gravityMode()}. */
    public static final byte GRAVITY_NORMAL = 0, GRAVITY_LOW = 1, GRAVITY_INVERTED = 2;

    /** Names the bar may lie with (all {@code entity.supernaturalcraft.chuck.bar.*}). */
    public static final List<String> BAR_LIES = List.of("god", "unknown", "author", "chuck", "carver", "reader");

    private static final EntityDataAccessor<Boolean> WINDOW = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RULES = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> GRAVITY = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ORDER = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> SCRIPT = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> FINALE = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> WHITENESS = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.FLOAT);
    /** The look he wears right now (switched halfway through a transition, not when the phase changes). */
    private static final EntityDataAccessor<Byte> OUTFIT = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FORM = SynchedEntityData.defineId(ChuckEntity.class, EntityDataSerializers.BOOLEAN);

    private static final Map<String, String> HUMAN_ALIAS = Map.of("line_sweep", "type_air", "type_rain", "type_air", "narrate", "point",
            "echo_call", "point", "recoil", "backhand");
    private static final Map<String, String> DIVINE_ALIAS = Map.of("type_air", "type_rain", "point", "narrate", "shove", "narrate",
            "backhand", "narrate", "throw_glass", "narrate", "emerge", "reveal");

    private boolean rematch, writingIgnored;
    /** The phase whose arena has been written (0: none yet), and ticks since it began. */
    private int writtenPhase, chapterTicks;
    private long nextWritingLine;
    private final ChuckWindows windows = new ChuckWindows();
    private final ChuckWindows.Rings rings = new ChuckWindows.Rings();
    private long coreResetAt;
    // Chapter 3's pages and chapter 4's weak points.
    private final List<UUID> pages = new ArrayList<>();
    private int pageRound;
    private long pagesReturnAt;
    private final Map<Integer, UUID> nodes = new HashMap<>();
    private @Nullable UUID leftHand, rightHand;
    // Rules.
    private int activeRule, lastRule;
    private long ruleUntil;
    private final Map<UUID, LivingEntity> ruled = new HashMap<>();
    private List<BlockPos> lights = List.of();
    private long lightsAt;
    // Backspace.
    private final Map<UUID, PositionTrail> trails = new HashMap<>();
    // Chapter 5.
    private @Nullable NarrationJudge.Order lastOrder;
    private int attacksSinceNarration;
    private float whiteSent = -1;
    // The fourth wall.
    private long refillStart = -1, nextRefill, nextLie, lieUntil, nextRename, renamedUntil, nextHud, creditsUntil;
    private float lieValue;
    private @Nullable String renamed;
    private boolean brokenThisChapter, fakeCreditsRolled;
    // The finale.
    private int finaleTicks;
    private final List<UUID> allies = new ArrayList<>();
    private boolean dyingClip;
    // Scenery of the current attack (a line's words, an echo).
    private final List<UUID> tracked = new ArrayList<>();

    public ChuckEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 2000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINDOW, false);
        builder.define(RULES, 0);
        builder.define(GRAVITY, GRAVITY_NORMAL);
        builder.define(ORDER, (byte) -1);
        builder.define(SCRIPT, 0f);
        builder.define(FINALE, FINALE_NONE);
        builder.define(WHITENESS, 0f);
        builder.define(OUTFIT, (byte) Chapter.Outfit.FLANNEL.ordinal());
        builder.define(FORM, false);
    }

    /** The Author fighting in {@code level} with this id, if any (server side). */
    public static @Nullable ChuckEntity find(Level level, @Nullable UUID id) {
        return level instanceof ServerLevel server && id != null && server.getEntity(id) instanceof ChuckEntity c ? c : null;
    }

    // --- synced state (read by the client) ---------------------------------------------------------------------

    /** The chapter this phase is. */
    public Chapter chapter() {
        return Chapter.ofPhase(phase());
    }

    /** Whether a damage window is open (always true in the human chapters' sense: they take damage anyway). */
    public boolean windowOpen() {
        return entityData.get(WINDOW);
    }

    protected void setWindowOpen(boolean open) {
        entityData.set(WINDOW, open);
    }

    /** The rules rewritten right now ({@link AuthorRules} bits). */
    public int rules() {
        return entityData.get(RULES);
    }

    protected void setRules(int rules) {
        entityData.set(RULES, rules);
    }

    /** {@link #GRAVITY_NORMAL}, {@link #GRAVITY_LOW} or {@link #GRAVITY_INVERTED}. */
    public byte gravityMode() {
        return entityData.get(GRAVITY);
    }

    protected void setGravityMode(byte mode) {
        entityData.set(GRAVITY, mode);
    }

    /** The order he is narrating in chapter 5 (an ordinal of {@link NarrationJudge.Order}), or -1. */
    public byte narratedOrder() {
        return entityData.get(ORDER);
    }

    protected void setNarratedOrder(byte order) {
        entityData.set(ORDER, order);
    }

    /** How broken the script is, 0-1 (cracks in him; read by the renderer and the page shader). */
    public float scriptBroken() {
        return entityData.get(SCRIPT);
    }

    protected void setScriptBroken(float f) {
        entityData.set(SCRIPT, f);
    }

    /** The finale's stage ({@link #FINALE_NONE} … {@link #FINALE_APPROVAL}). */
    public byte finaleStage() {
        return entityData.get(FINALE);
    }

    protected void setFinaleStage(byte stage) {
        entityData.set(FINALE, stage);
    }

    /** How far the world has gone to blank paper, 0-1 (the page shader's white-out in chapter 5). */
    public float whiteness() {
        return entityData.get(WHITENESS);
    }

    protected void setWhiteness(float f) {
        entityData.set(WHITENESS, f);
    }

    /** The man's outfit right now: flannel in Eden, the suit from halfway into Hell. */
    @Override
    public Chapter.Outfit outfit() {
        Chapter.Outfit[] all = Chapter.Outfit.values();
        return all[Math.max(0, Math.min(all.length - 1, entityData.get(OUTFIT)))];
    }

    /** Whether he is the light: from halfway into chapter 3 (the man comes apart first). */
    @Override
    public boolean divine() {
        return entityData.get(FORM);
    }

    @Override
    public float crack() {
        return scriptBroken();
    }

    private void wear(Chapter chapter) {
        boolean was = divine();
        entityData.set(OUTFIT, (byte) chapter.outfit.ordinal());
        entityData.set(FORM, chapter.divine());
        if (!was && chapter.divine()) {
            stopTriggeredAnim("action", null);
            triggerAnim("divine", "reveal");
        }
    }

    // --- his numbers -------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return ChuckBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.AUTHOR_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.AUTHOR_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.AUTHOR_DAMAGE_FACTOR.get().floatValue();
    }

    /** The Colt is no more than any blow against the Author: his hard cap. */
    @Override
    public float exactCap() {
        return BossDamage.coltCap(trueMaxHealth(), true);
    }

    /** He never circles like Lucifer: as the light he hangs over the arena's heart ({@link #hoverPoint}). */
    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    protected boolean walks() {
        return !divine();
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return ChuckAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return ChuckBalance.attackGap(phase);
    }

    @Override
    protected int emergeTicks() {
        return ChuckBalance.EMERGE_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return ChuckBalance.TRANSITION_TICKS;
    }

    @Override
    protected int deathTicks() {
        return ChuckBalance.DEATH_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return ChuckAnimations.HUMAN;
    }

    @Override
    protected List<String> triggeredAnimations() {
        return ChuckAnimations.HUMAN_TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.chuck.bar." + Chapter.ofPhase(phase).id();
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (Chapter.ofPhase(phase)) {
            case EDEN -> BossEvent.BossBarColor.YELLOW;
            case HELL -> BossEvent.BossBarColor.RED;
            case STORM -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return phase >= 3 ? AllParticles.GOLDEN_MOTE.get() : AllParticles.INK_LETTER.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.CHUCK_WRITE.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.CHUCK_LAUGH.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.CHUCK_REVEAL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.CHUCK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.CHUCK_APPROVE.get();
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.CHUCK_TYPE.get();
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.CHUCK_APPROVE.get();
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.CHUCK_ERASE.get();
    }

    public boolean rematch() {
        return rematch;
    }

    /** "Another draft": the same fight, without the first-time rewards. */
    public void setRematch(boolean rematch) {
        this.rematch = rematch;
    }

    // --- the arena he writes --------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.AUTHOR_ARENA_RADIUS.get(), ArenaTheme.AUTHOR);
    }

    /** Halfway through a transition: the old page unwrites and the new chapter is written over it. */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        beginChapter(level, arena, Chapter.ofPhase(phase));
    }

    private void beginChapter(ServerLevel level, ArenaController arena, Chapter chapter) {
        writtenPhase = chapter.phase();
        chapterTicks = 0;
        long now = level.getGameTime();
        ChuckArenas.begin(level, arena, chapter);
        ChuckFx.chapterTitle(this, chapter);
        ChuckFx.narrate(this, "narration.supernaturalcraft.chuck.chapter." + chapter.id(), 100);
        wear(chapter);
        Vec3 spawn = ChuckArenas.spawnPoint(arena, chapter);
        if (!chapter.divine()) setPos(spawn.x, spawn.y, spawn.z);
        resetChapterState(now);
        if (chapter == Chapter.BLANK) {
            setWhiteness(ChuckBalance.whiteness(0));
            whiteSent = whiteness();
            ChuckFx.whiteOut(this, whiteness(), 60);
        }
    }

    private void resetChapterState(long now) {
        windows.close();
        setWindowOpen(false);
        discardTargets();
        rings.reset();
        coreResetAt = 0;
        pagesReturnAt = 0;
        brokenThisChapter = false;
        setScriptBroken(0);
        nextWritingLine = now + 60;
        nextRefill = now + 200;
        nextLie = now + 300;
        nextRename = now + 300;
        nextHud = now + 400;
    }

    /** Whether the page is still being written (the fight waits; he narrates). */
    public boolean writing() {
        ArenaController arena = arena();
        return !writingIgnored && arena != null && ChuckArenas.writing(arena);
    }

    /** Test hook: fight on even while the arena writes itself. */
    public void setWritingIgnored(boolean ignored) {
        writingIgnored = ignored;
    }

    /** Where the light hangs in the current chapter (it bobs a little). */
    public Vec3 hoverPoint(ArenaController arena) {
        Vec3 spawn = ChuckArenas.spawnPoint(arena, chapter());
        return spawn.add(0, 1.0 + 0.35 * Math.sin(level().getGameTime() * 0.05), 0);
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        return divine() ? hoverPoint(arena) : ChuckArenas.spawnPoint(arena, chapter());
    }

    // --- cinematics and the shape of the fight ---------------------------------------------------------------------

    @Override
    protected void playEmergence() {
        ChuckCinematics.emergence(this);
    }

    @Override
    protected void playTransition(int to) {
        ChuckCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        ChuckCinematics.death(this);
    }

    /** He stands, straightens his flannel; partway through, the cabin starts to unwrite and Eden is written. */
    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        int elapsed = ChuckBalance.EMERGE_TICKS - stateTimer;
        if (!(level() instanceof ServerLevel level)) return;
        // The trigger sent as he was summoned can beat his first appearance on the clients.
        if (elapsed == 3) triggerAnim("action", "emerge");
        if (elapsed % 4 == 0) level.sendParticles(AllParticles.PAGE_SCRAP.get(), getX(), getY() + 1, getZ(), 3, 0.8, 0.8, 0.8, 0.02);
        ArenaController arena = arena();
        if (elapsed == ChuckBalance.EMERGE_WRITE_AT && arena != null && writtenPhase == 0) beginChapter(level, arena, Chapter.EDEN);
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.INK_LETTER.get(), getRandomX(2), getY() + random.nextDouble() * 2.2, getRandomZ(2), 0, 0.03, 0);
    }

    @Override
    protected void onTransitionStart(int to) {
        clearRules();
        discardTargets();
        dropTracked();
        narrate(null);
        windows.close();
        setWindowOpen(false);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + 1.4, getZ(), 50, 1, 1.2, 1, 0.1);
        }
    }

    /** The man stands; the light rises to its place. */
    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        getNavigation().stop();
        if (divine()) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
        } else {
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        if (level() instanceof ServerLevel level && elapsed % 5 == 0) {
            level.sendParticles(AllParticles.PAGE_SCRAP.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 1, 1, 1, 0.05);
        }
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (elapsed == 20 && finaleStage() == FINALE_APPROVAL) {
            triggerAnim("action", "approve");
            playSound(AllSounds.CHUCK_APPROVE.get(), 3f, 1f);
        }
        if (elapsed == ChuckBalance.DEATH_TICKS - 80) {
            dyingClip = true;
            triggerAnim("action", "death");
        }
        setWhiteness(Math.min(1f, Math.max(whiteness(), elapsed / (float) ChuckBalance.DEATH_TICKS)));
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.5, getZ(), 3, 0, 0, 0, 0);
            level.sendParticles(AllParticles.PAGE_SCRAP.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 200, 2, 3, 2, 0.3);
            level.sendParticles(AllParticles.GOLDEN_MOTE.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 120, 1.5, 3, 1.5, 0.2);
            return;
        }
        level.sendParticles(AllParticles.GOLDEN_MOTE.get(), getX(), getY() + random.nextDouble() * getBbHeight(), getZ(), 4, 1, 0.5, 1, 0.03);
        level.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + random.nextDouble() * getBbHeight(), getZ(), 2, 1, 0.5, 1, 0.02);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + 1, getZ(), 80, 0.8, 1.2, 0.8, 0.08);
    }

    /** Everyone fell: "Let's try another draft." The arena unwrites and the cabin comes back, with him at home. */
    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        ArenaController arena = arena();
        Vec3 door = arena != null ? arena.centerVec() : position();
        clearRules();
        super.returnToCage(level, messageKey.replace(".lucifer.", ".chuck."));
        if (arena != null) ChuckArenas.cabinReturns(level, arena);
        AuthorRewards.defeat(level, door);
    }

    /** Every hunter who took part, fallen ones included (they are still online), but not creative onlookers. */
    private static List<ServerPlayer> everyoneWhoFought(ServerLevel level, ArenaController arena) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : arena.participants()) {
            if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) out.add(p);
        }
        return out;
    }

    /** The test is passed: the credits, the rewards, and the brothers and the angel go back into the light. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        ChuckFx.credits(this, 600);
        ChuckArenas.cabinReturns(level, arena);
        ChuckCinematics.victory(this);
        AuthorRewards.victory(level, everyoneWhoFought(level, arena), arena.centerVec(), rematch);
        for (HunterAllyEntity ally : allies()) ally.fade();
    }

    // --- ticking -------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        long now = level.getGameTime();
        byte s = state();
        // Spawned by egg or command: no emergence wrote Eden.
        if (writtenPhase == 0 && s != EMERGING) beginChapter(level, arena, Chapter.ofPhase(phase()));
        chapterTicks++;
        ChuckArenas.tick(level, arena, chapter(), chapterTicks);
        if (s == DYING) return;

        if (divine()) {
            ensureHands(level);
            if (s != TRANSITION) hover(arena);
        }
        if (now % ChuckBalance.TRAIL_EVERY == 0) recordTrails(now);
        tickRules(level, arena, now);

        boolean writing = writing();
        if (finaleStage() != FINALE_NONE) {
            tickFinale(now);
        } else if (writing || creditsUntil > now) {
            scheduler().delay(20);
            if (writing && s != TRANSITION && s != EMERGING && now >= nextWritingLine) {
                nextWritingLine = now + ChuckBalance.WRITING_NARRATION_EVERY;
                ChuckFx.narrate(this, "narration.supernaturalcraft.chuck.writing." + random.nextInt(6), 80);
                triggerAnim("action", "type_air");
            }
        }
        if (s != TRANSITION && s != EMERGING && !writing && finaleStage() == FINALE_NONE) {
            if (chapter() == Chapter.STORM && divine()) tickPages(level, arena, now);
            if (chapter() == Chapter.LIBRARY && divine()) tickRings(level, now);
            if (chapter() == Chapter.BLANK && now % 10 == 0) tickWhiteness();
            tickFourthWall(now);
        }
        boolean open = windows.isOpen(now);
        if (open != windowOpen()) setWindowOpen(open);
    }

    /** As the light: hang over the arena's heart, drifting a little. */
    private void hover(ArenaController arena) {
        setNoGravity(true);
        getNavigation().stop();
        Vec3 want = hoverPoint(arena);
        Vec3 at = position().lerp(want, 0.08);
        setPos(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
    }

    private void recordTrails(long now) {
        List<ServerPlayer> hunters = challengers();
        for (ServerPlayer p : hunters) trail(p).record(now, p.getX(), p.getY(), p.getZ());
        if (trails.size() > hunters.size() + 4) trails.keySet().removeIf(id -> hunters.stream().noneMatch(p -> p.getUUID().equals(id)));
    }

    /** Where {@code who} has been (Backspace's memory of them). */
    public PositionTrail trail(LivingEntity who) {
        return trails.computeIfAbsent(who.getUUID(), k -> PositionTrail.covering(ChuckBalance.BACKSPACE_TICKS + 20, ChuckBalance.TRAIL_EVERY));
    }

    // --- damage -----------------------------------------------------------------------------------------------------

    /**
     * Whether a blow from {@code source} may land now: as a man, yes; as the light, only in a window; never while the page
     * is written; in the finale, only a hunter's blow once it is awaited.
     */
    public boolean damageable(DamageSource source) {
        byte finale = finaleStage();
        if (finale == FINALE_AWAIT_BLOW) return source.getEntity() instanceof Player;
        if (finale != FINALE_NONE) return false;
        if (writing()) return false;
        return !chapter().divine() || windowOpen();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (BossDamage.passesThrough(source)) return super.hurt(source, amount);
        if (!damageable(source)) {
            if (source.getEntity() instanceof Player && tickCount % 8 == 0) playSound(AllSounds.CHUCK_ERASE.get(), 0.8f, 1.5f);
            return false;
        }
        return super.hurt(source, amount);
    }

    /** The floor of chapter 5: the finale begins instead of his death; once it awaits the blow, the blow kills. */
    @Override
    protected boolean interceptDeath() {
        if (finaleStage() == FINALE_AWAIT_BLOW) return false;
        beginFinale();
        return true;
    }

    /** A window of {@code ticks} in which blows land (test hook, and every way the script breaks). */
    public void openWindow(int ticks) {
        openWindow(ticks, getEyePosition(), true);
    }

    private void openWindow(int ticks, Vec3 at, boolean crackAnim) {
        long now = level().getGameTime();
        windows.open(now, ticks);
        setWindowOpen(true);
        brokenThisChapter = true;
        refillStart = -1;
        ChuckFx.crack(this, at, ticks);
        if (crackAnim) triggerAnim("action", "crack");
        playSound(AllSounds.CHUCK_CRACK.get(), 3f, 1f);
    }

    public ChuckWindows windows() {
        return windows;
    }

    // --- chapter 3: the manuscript's pages -----------------------------------------------------------------------

    private void tickPages(ServerLevel level, ArenaController arena, long now) {
        pages.removeIf(id -> !(level.getEntity(id) instanceof AuthorTargetEntity t) || !t.isAlive());
        if (pages.isEmpty() && !windowOpen() && now >= pagesReturnAt) spawnPages(level);
        Vec3 c = arena.centerVec();
        double r = arena.radius() * ChuckBalance.PAGE_ORBIT;
        double floor = ChuckArenas.spawnPoint(arena, chapter()).y;
        for (UUID id : pages) {
            if (!(level.getEntity(id) instanceof AuthorTargetEntity t)) continue;
            int n = Math.max(1, pageRound);
            double a = t.index() * Math.PI * 2 / n + chapterTicks * 0.004;
            double y = floor + ChuckBalance.PAGE_HEIGHT + Math.sin(chapterTicks * 0.05 + t.index()) * 0.8;
            t.setPos(c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r);
            t.setYRot((float) Math.toDegrees(a) + 90f);
            t.setShielded(ChuckBalance.shielded(t.index(), chapterTicks));
        }
    }

    /** Writes a round of manuscript pages around the arena. */
    public void spawnPages(ServerLevel level) {
        int n = ChuckBalance.pages(Math.max(1, challengers().size()));
        pageRound = n;
        for (int i = 0; i < n; i++) {
            AuthorTargetEntity t = AllEntities.AUTHOR_TARGET.get().create(level);
            if (t == null) continue;
            t.setKind(AuthorTargetEntity.PAGE);
            t.bind(this, i);
            t.moveTo(getX(), getY() + 3, getZ(), 0, 0);
            level.addFreshEntity(t);
            pages.add(t.getUUID());
            minions().add(t.getUUID());
        }
        setScriptBroken(0);
        playSound(AllSounds.CHUCK_WRITE.get(), 3f, 0.8f);
    }

    public List<AuthorTargetEntity> pageTargets() {
        return targets(pages);
    }

    // --- chapter 4: the rings ------------------------------------------------------------------------------------

    private void tickRings(ServerLevel level, long now) {
        if (coreResetAt > 0 && now >= coreResetAt) {
            rings.reset();
            coreResetAt = 0;
            setScriptBroken(0);
            playSound(AllSounds.CHUCK_WRITE.get(), 3f, 0.6f);
        }
        if (!rings.repairDue(now).isEmpty()) setScriptBroken(rings.brokenShare());
        placeNodes(level, now);
    }

    /** Every unbroken weak point stands where {@link ChuckGeometry} puts it, now. */
    public void placeNodes(ServerLevel level, long now) {
        for (int r = 0; r < ChuckBones.RINGS; r++) {
            for (int n = 0; n < ChuckBones.NODES; n++) {
                int key = r * ChuckBones.NODES + n;
                UUID id = nodes.get(key);
                AuthorTargetEntity t = id != null && level.getEntity(id) instanceof AuthorTargetEntity e && e.isAlive() ? e : null;
                if (rings.broken(r, n)) {
                    if (t != null) t.discard();
                    nodes.remove(key);
                    continue;
                }
                if (t == null) {
                    t = AllEntities.AUTHOR_TARGET.get().create(level);
                    if (t == null) continue;
                    t.setKind(AuthorTargetEntity.NODE);
                    t.setRing((byte) r);
                    t.setNode((byte) n);
                    t.bind(this, key);
                    t.moveTo(nodeAt(r, n, now));
                    level.addFreshEntity(t);
                    nodes.put(key, t.getUUID());
                    minions().add(t.getUUID());
                }
                Vec3 at = nodeAt(r, n, now);
                t.setPos(at.x, at.y, at.z);
            }
        }
    }

    /** The feet of weak point {@code node} of ring {@code ring} at game time {@code t} (its box is centred on the node). */
    public Vec3 nodeAt(int ring, int node, double t) {
        double[] w = ChuckGeometry.toWorld(ChuckGeometry.nodeOffset(ring, node, t), yBodyRot);
        return new Vec3(getX() + w[0], getY() + w[1] - ChuckGeometry.NODE_SIZE / 2, getZ() + w[2]);
    }

    public List<AuthorTargetEntity> nodeTargets() {
        return targets(nodes.values());
    }

    public ChuckWindows.Rings rings() {
        return rings;
    }

    /** A page tore, or a weak point broke. */
    public void targetBroken(AuthorTargetEntity t) {
        if (!(level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (t.kind() == AuthorTargetEntity.PAGE) {
            pages.remove(t.getUUID());
            setScriptBroken(1f - pages.size() / (float) Math.max(1, pageRound));
            if (pages.isEmpty()) {
                openWindow(ChuckBalance.PAGE_WINDOW, t.position(), true);
                pagesReturnAt = now + ChuckBalance.PAGE_WINDOW + ChuckBalance.PAGE_RETURN;
            }
            return;
        }
        nodes.remove(t.ring() * ChuckBones.NODES + t.node());
        switch (rings.breakNode(t.ring(), t.node(), now, ChuckBalance.RING_REPAIR)) {
            case RING -> openWindow(ChuckBalance.RING_WINDOW, t.position(), true);
            case ALL -> {
                openWindow(ChuckBalance.CORE_WINDOW, position().add(0, ChuckGeometry.CORE_Y, 0), true);
                coreResetAt = now + ChuckBalance.CORE_WINDOW;
            }
            default -> {
            }
        }
        setScriptBroken(rings.brokenShare());
    }

    private List<AuthorTargetEntity> targets(java.util.Collection<UUID> ids) {
        List<AuthorTargetEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : ids) if (level.getEntity(id) instanceof AuthorTargetEntity t && t.isAlive()) out.add(t);
        return out;
    }

    private void discardTargets() {
        for (AuthorTargetEntity t : targets(pages)) t.discard();
        for (AuthorTargetEntity t : targets(nodes.values())) t.discard();
        pages.clear();
        nodes.clear();
    }

    // --- chapter 5: narration ----------------------------------------------------------------------------------------

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (chapter() == Chapter.BLANK && attacksSinceNarration >= ChuckBalance.NARRATE_EVERY) {
            attacksSinceNarration = 0;
            return ChuckAttacks.Narration::new;
        }
        if (!divine() && (distanceToSqr(target) > 18 * 18 || noSightTicks > 80)) {
            noSightTicks = 0;
            return ChuckAttacks.Elsewhere::new;
        }
        return null;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.IDLE && attack == null) attacksSinceNarration++;
    }

    /** Test hook: the next attack in chapter 5 is a narration. */
    public void forceNarrationNext() {
        attacksSinceNarration = ChuckBalance.NARRATE_EVERY;
    }

    /** He is narrating {@code order} now (null: he stopped). */
    public void narrate(@Nullable NarrationJudge.Order order) {
        setNarratedOrder((byte) (order == null ? -1 : order.ordinal()));
        if (order != null) lastOrder = order;
    }

    public @Nullable NarrationJudge.Order lastOrder() {
        return lastOrder;
    }

    /** {@code who} did the opposite of his line: the script cracks (a fixed share of chapter 5) and a window opens. */
    public void contradicted(LivingEntity who) {
        if (finaleStage() != FINALE_NONE || isInvulnerablePhase()) return;
        setHealth(Math.max(1f, getHealth() - ChuckBalance.crack(getMaxHealth())));
        acceptHealth();
        setScriptBroken(Math.min(1f, scriptBroken() + ChuckBalance.CRACK_SHARE));
        triggerAnim("action", "recoil");
        openWindow(ChuckBalance.CRACK_WINDOW, getEyePosition(), false);
        if (getHealth() <= 1.0001f) beginFinale();
    }

    private void tickWhiteness() {
        float w = ChuckBalance.whiteness(ChuckBalance.progress(MAX_PHASE, getHealth(), getMaxHealth()));
        if (Math.abs(w - whiteness()) > 0.005f) setWhiteness(w);
        if (Math.abs(w - whiteSent) >= 0.05f) {
            whiteSent = w;
            ChuckFx.whiteOut(this, w, 40);
        }
    }

    // --- rules -----------------------------------------------------------------------------------------------------

    public int lastRule() {
        return lastRule;
    }

    /** Rewrites {@code rule} for {@code ticks}; gravity rules weigh on {@code hunters}. */
    public void applyRule(int rule, int ticks, List<? extends LivingEntity> hunters) {
        if (!(level() instanceof ServerLevel level)) return;
        endRule(false);
        ArenaController arena = arena();
        activeRule = rule;
        lastRule = rule;
        ruleUntil = level.getGameTime() + ticks;
        setRules(rule);
        ChuckFx.rule(this, rule, ticks);
        triggerAnim("action", "rewrite");
        playSound(AllSounds.CHUCK_REWRITE.get(), 3f, 1f);
        byte mode = rule == AuthorRules.GRAVITY_LOW ? GRAVITY_LOW : rule == AuthorRules.GRAVITY_INVERTED ? GRAVITY_INVERTED : GRAVITY_NORMAL;
        if (mode != GRAVITY_NORMAL) {
            setGravityMode(mode);
            for (LivingEntity h : hunters) {
                ChuckGravity.apply(this, h, mode);
                ruled.put(h.getUUID(), h);
                ChuckFx.gravity(this, h, mode, ticks);
            }
            if (mode == GRAVITY_INVERTED && arena != null) ChuckArenas.tempCeiling(level, arena, ticks);
        }
        if (rule == AuthorRules.FLOOR_LAVA && arena != null) {
            ChuckArenas.lavaZone(level, arena, arena.centerVec(), (int) (arena.radius() * 0.85), ticks);
        }
    }

    /** Every rule back as it was, and every hunter's gravity their own (falls forgiven for a while). */
    public void clearRules() {
        endRule(true);
    }

    private void endRule(boolean announce) {
        byte mode = gravityMode();
        for (LivingEntity e : ruled.values()) {
            ChuckGravity.release(e, ChuckBalance.NO_FALL_AFTER);
            if (mode != GRAVITY_NORMAL) ChuckFx.gravity(this, e, GRAVITY_NORMAL, 0);
        }
        ruled.clear();
        if (activeRule != 0 && announce) ChuckFx.rule(this, 0, 0);
        activeRule = 0;
        setRules(0);
        setGravityMode(GRAVITY_NORMAL);
    }

    private void tickRules(ServerLevel level, ArenaController arena, long now) {
        if (activeRule == 0) return;
        if (now >= ruleUntil) {
            clearRules();
            return;
        }
        if (activeRule == AuthorRules.GRAVITY_INVERTED) {
            double cap = ChuckArenas.spawnPoint(arena, chapter()).y + ChuckBalance.CEILING_HEIGHT;
            for (LivingEntity e : ruled.values()) {
                if (e.isAlive() && e.getY() > cap) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
                    e.teleportTo(e.getX(), cap, e.getZ());
                }
            }
        }
        if (now % 10 != 0) return;
        if (activeRule == AuthorRules.WATER_BURNS) {
            for (ServerPlayer p : challengers()) {
                if (ChuckArenas.isWater(level, arena, p.blockPosition()) || ChuckArenas.isWater(level, arena, BlockPos.containing(p.getEyePosition()))) {
                    LuciferAttacks.hit(this, p, AllDamageTypes.REWRITTEN, 3);
                    p.igniteForSeconds(2);
                }
            }
        }
        if (activeRule == AuthorRules.LIGHT_HURTS) {
            if (now - lightsAt >= 40) {
                lights = ChuckArenas.lightSources(level, arena);
                lightsAt = now;
            }
            for (ServerPlayer p : challengers()) {
                for (BlockPos light : lights) {
                    if (light.distToCenterSqr(p.position()) < 4 * 4) {
                        LuciferAttacks.hit(this, p, AllDamageTypes.REWRITTEN, 2);
                        break;
                    }
                }
            }
        }
    }

    // --- the fourth wall --------------------------------------------------------------------------------------------

    /** The bar lies: it refills itself, shows a number that is not his health, until the chapter's script breaks. */
    @Override
    protected float bossBarProgress() {
        float truth = super.bossBarProgress();
        long now = level().getGameTime();
        if (refillStart >= 0) {
            int t = (int) (now - refillStart);
            if (t < ChuckBalance.REFILL_TICKS) return ChuckBalance.refillLie(truth, t);
            refillStart = -1;
        }
        if (now < lieUntil) return lieValue;
        return truth;
    }

    @Override
    protected Component bossBarName(int phase) {
        String key = renamed != null ? "entity.supernaturalcraft.chuck.bar." + renamed : bossBarKey(phase);
        return Component.translatable(key).withStyle(Chapter.ofPhase(phase).divine() ? ChatFormatting.WHITE : ChatFormatting.GOLD);
    }

    private void tickFourthWall(long now) {
        int phase = phase();
        if (phase >= 2 && !brokenThisChapter && refillStart < 0 && now >= nextRefill) {
            refillStart = now;
            nextRefill = now + ChuckBalance.REFILL_EVERY + random.nextInt(200);
        }
        if (phase >= 4 && now >= nextLie) {
            nextLie = now + 300 + random.nextInt(300);
            if (random.nextBoolean()) {
                lieUntil = now + 30;
                lieValue = 0.05f + random.nextFloat() * 0.9f;
            }
        }
        if (phase >= 3 && renamed == null && now >= nextRename) {
            nextRename = now + 400 + random.nextInt(400);
            renamed = BAR_LIES.get(random.nextInt(BAR_LIES.size()));
            renamedUntil = now + 60 + random.nextInt(40);
            updateBossBar();
        } else if (renamed != null && now >= renamedUntil) {
            renamed = null;
            updateBossBar();
        }
        if (phase >= 2 && now >= nextHud) {
            nextHud = now + ChuckBalance.HUD_REWRITE_EVERY + random.nextInt(600);
            ChuckFx.hudRewrite(this, ChuckBalance.HUD_REWRITE_TICKS);
        }
        if (phase == 4 && !fakeCreditsRolled && ChuckBalance.progress(4, getHealth(), getMaxHealth()) >= ChuckBalance.FAKE_CREDITS_AT) {
            rollFakeCredits(now);
        }
        if (creditsUntil > 0 && now >= creditsUntil) {
            creditsUntil = 0;
            bossEvent().setVisible(true);
            ChuckFx.narrate(this, "narration.supernaturalcraft.chuck.not_like_this", 80);
            playSound(AllSounds.CHUCK_LAUGH.get(), 3f, 0.8f);
        }
    }

    /** "THE END." The credits roll; a while later, "…no. Not like this." Once, in chapter 4. */
    public void rollFakeCredits(long now) {
        fakeCreditsRolled = true;
        creditsUntil = now + ChuckBalance.FAKE_CREDITS_TICKS;
        scheduler().cancel();
        clearRules();
        bossEvent().setVisible(false);
        ChuckFx.fakeCredits(this, ChuckBalance.FAKE_CREDITS_TICKS);
    }

    public boolean fakeCreditsRolled() {
        return fakeCreditsRolled;
    }

    // --- the finale ---------------------------------------------------------------------------------------------

    /** At chapter 5's floor: the light holds; Dean, Sam and Castiel step out of it. */
    public void beginFinale() {
        if (finaleStage() != FINALE_NONE || !(level() instanceof ServerLevel level)) return;
        scheduler().cancel();
        clearRules();
        discardTargets();
        dropTracked();
        narrate(null);
        windows.close();
        setWindowOpen(false);
        setFinaleStage(FINALE_ARRIVAL);
        finaleTicks = 0;
        renamed = "unknown";
        updateBossBar();
        spawnAllies(level);
        ChuckCinematics.finale(this);
        playSound(AllSounds.HUNTER_ALLY_ARRIVE.get(), 4f, 1f);
    }

    private void spawnAllies(ServerLevel level) {
        ArenaController arena = arena();
        Vec3 c = arena != null ? ChuckArenas.spawnPoint(arena, chapter()) : position();
        // In front of him, in a shallow fan, so the camera sees them against the light rather than lost inside it.
        double r = getBbWidth() / 2 + 3.5;
        for (byte who = 0; who < 3; who++) {
            HunterAllyEntity ally = AllEntities.HUNTER_ALLY.get().create(level);
            if (ally == null) continue;
            double a = Math.toRadians(yBodyRot) + (who - 1) * 0.45;
            Vec3 at = LuciferAttacks.floorAt(this, new Vec3(c.x - Math.sin(a) * r, c.y, c.z + Math.cos(a) * r));
            ally.moveTo(at.x, at.y, at.z, 0, 0);
            ally.setWho(who);
            ally.bind(this);
            ally.face(position());
            level.addFreshEntity(ally);
            allies.add(ally.getUUID());
            level.sendParticles(AllParticles.GOLDEN_MOTE.get(), at.x, at.y + 1, at.z, 40, 0.4, 1, 0.4, 0.1);
        }
    }

    public List<HunterAllyEntity> allies() {
        List<HunterAllyEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : allies) if (level.getEntity(id) instanceof HunterAllyEntity a && !a.isRemoved()) out.add(a);
        return out;
    }

    private void tickFinale(long now) {
        scheduler().delay(40);
        getNavigation().stop();
        finaleTicks++;
        for (int i = 0; i < ChuckCinematics.FINALE_AT.length; i++) {
            if (finaleTicks == ChuckCinematics.FINALE_AT[i]) ChuckCinematics.finaleLine(this, i);
        }
        byte stage = finaleStage();
        if (stage == FINALE_ARRIVAL && finaleTicks >= ChuckBalance.FINALE_ARRIVAL) {
            setFinaleStage(FINALE_HOLD);
            triggerAnim("action", "held");
        } else if (stage == FINALE_HOLD && finaleTicks >= ChuckBalance.FINALE_ARRIVAL + ChuckBalance.FINALE_HOLD) {
            setFinaleStage(FINALE_AWAIT_BLOW);
            ChuckCinematics.urge(this);
        } else if (stage == FINALE_AWAIT_BLOW && finaleTicks % 200 == 0) {
            ChuckCinematics.urge(this);
        }
    }

    /** Test hook: straight to the awaited blow. */
    public void skipFinaleToBlow() {
        if (finaleStage() == FINALE_NONE) beginFinale();
        finaleTicks = ChuckBalance.FINALE_ARRIVAL + ChuckBalance.FINALE_HOLD;
        setFinaleStage(FINALE_AWAIT_BLOW);
    }

    /** The blow landed: he approves, and fades into the page. */
    @Override
    protected void beginDying() {
        if (finaleStage() != FINALE_NONE) setFinaleStage(FINALE_APPROVAL);
        clearRules();
        discardTargets();
        dropTracked();
        windows.close();
        setWindowOpen(false);
        narrate(null);
        dyingClip = false;
        renamed = null;
        super.beginDying();
    }

    // --- hands and scenery ------------------------------------------------------------------------------------------

    /** His left or right hand, if it is out. */
    public @Nullable AuthorHandEntity hand(boolean left) {
        UUID id = left ? leftHand : rightHand;
        return level() instanceof ServerLevel s && id != null && s.getEntity(id) instanceof AuthorHandEntity h && h.isAlive() ? h : null;
    }

    private void ensureHands(ServerLevel level) {
        if (tickCount % 10 != 0) return;
        for (boolean left : new boolean[]{true, false}) {
            if (hand(left) != null) continue;
            AuthorHandEntity h = AllEntities.AUTHOR_HAND.get().create(level);
            if (h == null) continue;
            h.setLeft(left);
            h.bind(this);
            Vec3 at = handRest(h);
            h.moveTo(at.x, at.y, at.z, getYRot(), 0);
            level.addFreshEntity(h);
            if (left) leftHand = h.getUUID();
            else rightHand = h.getUUID();
            minions().add(h.getUUID());
            level.sendParticles(AllParticles.GOLDEN_MOTE.get(), at.x, at.y, at.z, 60, 1.5, 1.5, 1.5, 0.1);
        }
    }

    /** Where a free hand floats: apart from the light, at shoulder height, a little ahead. */
    public Vec3 handRest(AuthorHandEntity hand) {
        double[] w = ChuckGeometry.toWorld(new double[]{hand.left() ? 7.5 : -7.5, 6.0, 1.5}, yBodyRot);
        return position().add(w[0], w[1], w[2]);
    }

    /** Remembers scenery of the current attack, to clear it if the attack is cut short. */
    public void track(Entity e) {
        tracked.add(e.getUUID());
        minions().add(e.getUUID());
    }

    public List<FloatingWordEntity> trackedWords() {
        List<FloatingWordEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : tracked) if (level.getEntity(id) instanceof FloatingWordEntity w && w.isAlive()) out.add(w);
        return out;
    }

    public void dropTrackedWords() {
        for (FloatingWordEntity w : trackedWords()) w.discard();
        tracked.removeIf(id -> !(level() instanceof ServerLevel s) || !(s.getEntity(id) instanceof Entity e) || e.isRemoved());
    }

    private void dropTracked() {
        if (level() instanceof ServerLevel level) {
            for (UUID id : tracked) {
                Entity e = level.getEntity(id);
                if (e instanceof InkEchoEntity echo) echo.dissolve();
                else if (e != null) e.discard();
            }
        }
        tracked.clear();
    }

    /** The bosses (entity paths) the challengers have beaten, whose echoes he may write; all of them if nobody has any. */
    public List<String> beatenBosses() {
        List<String> out = new ArrayList<>();
        List<ServerPlayer> hunters = challengers();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (b == BossProgression.Boss.CHUCK || b.optional) continue;
            for (ServerPlayer p : hunters) {
                var adv = p.server.getAdvancements().get(SupernaturalCraft.asResource(b.advancement));
                if (adv != null && p.getAdvancements().getOrStartProgress(adv).isDone()) {
                    out.add(b.entity);
                    break;
                }
            }
        }
        if (out.isEmpty()) {
            for (BossProgression.Boss b : BossProgression.Boss.values()) if (b != BossProgression.Boss.CHUCK && !b.optional) out.add(b.entity);
        }
        return out;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel && reason.shouldDestroy()) {
            clearRules();
            discardTargets();
            dropTracked();
            ArenaController arena = arena();
            if (arena != null) ChuckArenas.forget(arena);
        }
        super.remove(reason);
    }

    // --- presentation -------------------------------------------------------------------------------------------

    /** Development preview and tests: jump to a chapter's look and state, without its transition or its writing. */
    @Override
    public void forceLook(int phase) {
        super.forceLook(phase);
        Chapter chapter = Chapter.ofPhase(phase);
        writtenPhase = chapter.phase();
        chapterTicks = 0;
        wear(chapter);
        resetChapterState(level().getGameTime());
        updateBossBar();
    }

    /** Development preview: write the current chapter's arena now. */
    public void writeChapterNow() {
        ArenaController arena = arena();
        if (level() instanceof ServerLevel level && arena != null) beginChapter(level, arena, chapter());
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions d = super.getDefaultDimensions(pose);
        return divine() ? d.scale(DIVINE_HITBOX) : d;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FORM.equals(key)) refreshDimensions();
    }

    /**
     * Routes the base class's triggers (always on {@code action}) to the form he wears: the man's clips on
     * {@code action}, the light's on {@code divine}, through a few aliases; a clip his form lacks is dropped.
     */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller)) {
            if ("death".equals(anim) && finaleStage() == FINALE_APPROVAL && !dyingClip) anim = "approve";
            if (divine()) {
                String d = DIVINE_ALIAS.getOrDefault(anim, anim);
                if (!ChuckAnimations.DIVINE_TRIGGERED.contains(d)) return;
                controller = "divine";
                anim = d;
            } else {
                String h = HUMAN_ALIAS.getOrDefault(anim, anim);
                if (!ChuckAnimations.HUMAN_TRIGGERED.contains(h)) return;
                anim = h;
            }
        }
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(ChuckAnimations.HUMAN + "idle");
        RawAnimation walk = RawAnimation.begin().thenLoop(ChuckAnimations.HUMAN + "walk");
        RawAnimation hover = RawAnimation.begin().thenLoop(ChuckAnimations.DIVINE + "idle");
        RawAnimation drift = RawAnimation.begin().thenLoop(ChuckAnimations.DIVINE + "drift");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == TRANSITION || s == DYING) return PlayState.STOP;
            if (divine()) return state.setAndContinue(state.isMoving() ? drift : hover);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<ChuckEntity> man = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : ChuckAnimations.HUMAN_TRIGGERED) {
            boolean hold = name.equals("transform_3") || name.equals("death");
            man.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(ChuckAnimations.HUMAN + name)
                    : RawAnimation.begin().thenPlay(ChuckAnimations.HUMAN + name));
        }
        controllers.add(man);
        AnimationController<ChuckEntity> light = new AnimationController<>(this, "divine", 3, state -> PlayState.STOP);
        for (String name : ChuckAnimations.DIVINE_TRIGGERED) {
            boolean hold = name.equals("held") || name.equals("approve") || name.equals("death");
            light.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(ChuckAnimations.DIVINE + name)
                    : RawAnimation.begin().thenPlay(ChuckAnimations.DIVINE + name));
        }
        controllers.add(light);
    }

    // --- persistence -------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Rematch", rematch);
        tag.putInt("Written", writtenPhase);
        tag.putInt("ChapterTicks", chapterTicks);
        tag.putByte("Outfit", entityData.get(OUTFIT));
        tag.putBoolean("Divine", divine());
        tag.putBoolean("FakeCredits", fakeCreditsRolled);
        tag.putByte("Finale", finaleStage());
        tag.putLong("Window", windows.until());
        if (leftHand != null) tag.putUUID("LeftHand", leftHand);
        if (rightHand != null) tag.putUUID("RightHand", rightHand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rematch = tag.getBoolean("Rematch");
        writtenPhase = tag.getInt("Written");
        chapterTicks = tag.getInt("ChapterTicks");
        if (tag.contains("Outfit")) entityData.set(OUTFIT, tag.getByte("Outfit"));
        entityData.set(FORM, tag.getBoolean("Divine"));
        fakeCreditsRolled = tag.getBoolean("FakeCredits");
        // A finale interrupted by a reload resumes at the awaited blow.
        if (tag.getByte("Finale") != FINALE_NONE) setFinaleStage(FINALE_AWAIT_BLOW);
        if (tag.contains("Window")) windows.restore(tag.getLong("Window"));
        leftHand = tag.hasUUID("LeftHand") ? tag.getUUID("LeftHand") : null;
        rightHand = tag.hasUUID("RightHand") ? tag.getUUID("RightHand") : null;
    }
}
