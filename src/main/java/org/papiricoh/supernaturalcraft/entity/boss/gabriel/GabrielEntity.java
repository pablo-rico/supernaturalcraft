package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena.ChannelGround;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena.ChannelLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.GabrielFxPayload;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gabriel, the Trickster (v0.14): an archangel hiding as a pagan god of mischief, who drags his hunters into TV Land. Four
 * phases, one {@link Channel} each, a quarter of his true health each (600 vanilla × {@code healthScale}, about 1500 alone):
 *
 * <pre>EMERGING ("Welcome to TV Land!") → P1 CH 2 the sitcom → P2 CH 5 the game show → P3 CH 7 the hospital
 *   → P4 CH 9 the commercial (five spokesmen, the six-winged shadow) → DYING ("...or was it?")</pre>
 *
 * <p>Every phase change is a commercial break (Lucifer's transition): at its midpoint the channel flips ({@link #applyTerrain}:
 * static and the channel's title card for the hunters, the set rewritten on the same footprint by {@link ChannelGround}, a
 * new costume). Each channel has its own rule, run from {@link #customServerAiStep}:
 * <ul>
 *   <li>the sitcom's laugh track: lit, he can't be touched and plays his gags; dark, he takes ×1.3; nobody hitting him for
 *   a while in a dark spell, he takes a bow and the applause heals him;</li>
 *   <li>the game show's quiz: a lore question, three platforms, a buzzer; each hunter judged by where they stand;</li>
 *   <li>the hospital's heart monitor: a blow on the beep is critical; nurse doubles heal him if they reach him;</li>
 *   <li>the commercial's five spokesmen: only the real one casts the six-winged shadow; the podiums shuffle.</li>
 * </ul>
 * Optional: he blocks nothing on the road to the Cage nor to the Author ({@code BossProgression.Boss.optional}).
 */
public class GabrielEntity extends LuciferEntity {

    public static final int MAX_PHASE = GabrielBalance.PHASES;

    /** The channel whose costume and set are shown (it flips halfway through a commercial break, not at its start). */
    private static final EntityDataAccessor<Byte> SHOWN = SynchedEntityData.defineId(GabrielEntity.class, EntityDataSerializers.BYTE);
    /** The LAUGH sign is lit (his idle is the laugh loop). */
    private static final EntityDataAccessor<Boolean> LAUGHING = SynchedEntityData.defineId(GabrielEntity.class, EntityDataSerializers.BOOLEAN);

    /** The SIGN payload's signs. */
    public static final int SIGN_LAUGH = 0, SIGN_APPLAUSE = 1, SIGN_ON_AIR = 2;
    /** What a struck double does to whoever struck it. */
    public static final int PUNISH_TELEPORT = 0, PUNISH_PIE = 1, PUNISH_DAMAGE = 2;

    private float healthScale = 1f;
    private boolean scaled;
    private @Nullable ChannelGround ground;

    /** Game time the current channel went on air (its set down), -1 until then. */
    private long channelStart = -1;
    private boolean titled;
    // the sitcom
    private boolean laughSent;
    private long lastPlayerHit = -1;
    private int applaudedCycle = -1;
    // the game show
    private long nextQuiz = -1, quizEnds = -1;
    private int quizQuestion = -1, lastQuestion = -1;
    private int[] quizDeal = {0, 1, 2};
    private final List<ServerPlayer> quizHunters = new ArrayList<>();
    private long stunnedUntil;
    // the hospital
    private long lastBeat = -1;
    private int beatPeriod = GabrielBalance.BEAT_SLOWEST;
    private long nextNurses = -1;
    // the commercial
    private final List<UUID> doubles = new ArrayList<>();
    private int realPodium;
    private long nextShuffle = -1, shuffleAt = -1;
    private final Map<UUID, UUID> tells = new HashMap<>();
    // gags and timed props
    private final List<Peel> peels = new ArrayList<>();
    private final List<Restore> restores = new ArrayList<>();
    /** Tests: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    /** A banana peel on the floor, until a game time. */
    private record Peel(BlockPos pos, long until) {
    }

    /** A block of the set put back at a game time (a trapdoor closing). */
    private record Restore(BlockPos pos, BlockState state, long at) {
    }

    public GabrielEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 800;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, GabrielBalance.BASE_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHOWN, (byte) 0);
        builder.define(LAUGHING, false);
    }

    /** The channel on the air (the phase's: its rule runs). */
    public Channel channel() {
        return Channel.ofPhase(phase());
    }

    /** The channel shown: its set and costume (it changes halfway through the commercial break). */
    public Channel shownChannel() {
        Channel[] all = Channel.values();
        return all[Math.max(0, Math.min(all.length - 1, entityData.get(SHOWN)))];
    }

    /** What he wears now: his own jacket while he arrives and falls, else the shown channel's costume. */
    public Channel.Costume costume() {
        byte s = state();
        return s == EMERGING || s == DYING ? Channel.Costume.JACKET : shownChannel().costume;
    }

    /** Whether the LAUGH sign is lit (synced; the client's idle is the laugh loop). */
    public boolean laughing() {
        return entityData.get(LAUGHING);
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return GabrielBalance.threshold(phase);
    }

    @Override
    protected float healthScale() {
        return healthScale;
    }

    /** Test and command hook. */
    public void setHealthScale(float scale) {
        healthScale = scale;
        scaled = true;
    }

    @Override
    protected void scaleHealthToChallengers() {
        healthScale = GabrielBalance.healthScale(SNConfig.GABRIEL_HEALTH_MULTIPLIER.get(), SNConfig.GABRIEL_HEALTH_PER_PLAYER.get(),
                challengers().size());
        scaled = true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(GabrielBalance.BASE_HEALTH);
        setHealth((float) GabrielBalance.BASE_HEALTH);
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.GABRIEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return SNConfig.GABRIEL_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.GABRIEL_DAMAGE_MULTIPLIER.get().floatValue();
    }

    /**
     * What one of his blows (or gags) deals {@code victim}: his multiplier, and a demon is the episode's villain
     * ({@link GabrielBalance#DEMON_DAMAGE_TAKEN}).
     */
    public float blowTo(Entity victim, float amount) {
        float a = amount * attackDamageMultiplier();
        return Kin.isDemon(victim) ? a * GabrielBalance.DEMON_DAMAGE_TAKEN : a;
    }

    /** Strikes {@code victim} with one of his tricks. */
    public boolean strike(LivingEntity victim, float amount) {
        return victim.hurt(AllDamageTypes.source(level(), AllDamageTypes.SPELL, this), blowTo(victim, amount));
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return 1f;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return GabrielAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return GabrielBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.gabriel.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return GabrielAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.gabriel.bar." + Channel.ofPhase(phase).id();
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(ChatFormatting.YELLOW);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.GABRIEL_WELCOME.get();
    }

    /** At the end of his entrance and halfway through every commercial break: a snap (the client plays the static itself). */
    @Override
    protected SoundEvent roarSound() {
        return AllSounds.GABRIEL_SNAP.get();
    }

    /** A commercial break begins: the audience applauds the end of the segment. */
    @Override
    protected SoundEvent transformSound() {
        return AllSounds.GABRIEL_APPLAUSE.get();
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.GABRIEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.GABRIEL_DEATH.get();
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.GABRIEL_LAUGH_TRACK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.GABRIEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.GABRIEL_DEATH.get();
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.NOTE, getRandomX(1.2), getY() + 0.5 + random.nextDouble() * 1.5, getRandomZ(1.2), random.nextDouble(), 0, 0);
        level().addParticle(ParticleTypes.FIREWORK, getRandomX(1.0), getY() + random.nextDouble() * 2, getRandomZ(1.0), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 1, getZ(), 120, 1.0, 1.4, 1.0, 0.2);
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 80, 1.0, 1.4, 1.0, 0.1);
            return;
        }
        level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.2, getZ(), 4, 0.4, 0.7, 0.4, 0.04);
    }

    /** He never leaves his podium in the commercial, nor walks while stunned or hosting the quiz. */
    @Override
    protected boolean walks() {
        return channel() != Channel.COMMERCIAL && !isStunned() && !quizActive();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.GABRIEL_ARENA_RADIUS.get(), ArenaTheme.TV_LAND);
    }

    @Override
    protected void playEmergence() {
        GabrielCinematics.intro(this);
        if (level() instanceof ServerLevel level) {
            say(level, "message.supernaturalcraft.gabriel.welcome");
            fx(new GabrielFxPayload(getId(), GabrielFxPayload.CHANNEL, Channel.SITCOM.ordinal(), 0, position(), 30));
        }
    }

    @Override
    protected void playTransition(int to) {
        GabrielCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        GabrielCinematics.death(this);
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.TITLE, channel().ordinal(), 1, position(), 100));
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_ON_AIR, 0, position(), 0));
    }

    /** The commercial break begins: whatever was on stops, the hunters are shoved back, he snaps his fingers. */
    @Override
    protected void onTransitionStart(int to) {
        // Not Lucifer's (no shield, no flight).
        if (!(level() instanceof ServerLevel level)) return;
        closeQuiz(level, false);
        stopChannel(level);
        triggerAnim("action", "snap");
        say(level, "message.supernaturalcraft.gabriel.break");
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_ON_AIR, 0, position(), 0));
    }

    /** He stands through the break (Lucifer would rise into the air for his last). */
    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** Halfway through the break: the channel flips. */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        flipTo(level, arena, Channel.ofPhase(phase));
    }

    /** The fight was lost: the bait is gone with him, nothing is left. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        stopChannel(level);
        super.returnToCage(level, messageKey.replace(".lucifer.", ".gabriel."));
    }

    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            closeQuiz(level, false);
            stopChannel(level);
        }
        super.beginDying();
    }

    /** At the end of his death: his spoils, per hunter, and his last word. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        GabrielCinematics.victory(this);
        GabrielSpoils.drop(level, challengers(), position().add(0, 1.2, 0), random);
        say(level, "message.supernaturalcraft.gabriel.or_was_it");
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level) discardDoubles(level);
        super.remove(reason);
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (!scaled && state() != EMERGING) scaleHealthToChallengers();
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        long now = level.getGameTime();
        tickGround(level, arena);
        tickRestores(level, arena, now);
        byte s = state();
        if (s == TRANSITION) {
            if (tickCount % 20 == 0) commercialBreakSecond(challengers());
            return;
        }
        if (s == DYING || s == EMERGING || writingSet()) {
            if (laughing()) entityData.set(LAUGHING, false);
            return;
        }
        if (channelStart < 0) startChannel(level, now);
        if (isStunned()) {
            getNavigation().stop();
            if (scheduler().current() == null) scheduler().delay(5);
        }
        switch (channel()) {
            case SITCOM -> tickSitcom(level, now);
            case GAME_SHOW -> tickGameShow(level, now);
            case HOSPITAL -> tickHospital(level, now);
            case COMMERCIAL -> tickCommercial(level, arena, now);
        }
        for (ServerPlayer p : challengers()) slipCheck(p);
        peels.removeIf(peel -> {
            if (now < peel.until()) return false;
            clearPeel(level, arena, peel.pos());
            return true;
        });
    }

    /** The channel's clock starts (its set is down): the title card if the break didn't show it, ON AIR. */
    private void startChannel(ServerLevel level, long now) {
        channelStart = now;
        Channel ch = channel();
        if (!titled) fx(new GabrielFxPayload(getId(), GabrielFxPayload.TITLE, ch.ordinal(), 0, position(), 60));
        titled = true;
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_ON_AIR, 1, position(), 0));
        laughSent = false;
        applaudedCycle = -1;
        nextQuiz = now + GabrielBalance.FIRST_QUIZ;
        lastBeat = -1;
        nextNurses = now + GabrielBalance.NURSE_EVERY / 2;
        nextShuffle = -1;
        if (ch == Channel.COMMERCIAL) beginCommercial(level);
    }

    /** Ticks into the channel on the air (0 before it starts). */
    public int channelTicks() {
        return channelStart < 0 ? 0 : (int) (level().getGameTime() - channelStart);
    }

    /** Test hook: as if the channel had been on the air for {@code ticks}. */
    public void setChannelClock(int ticks) {
        channelStart = level().getGameTime() - ticks;
        titled = true;
    }

    // --- the sets -----------------------------------------------------------------------------------

    private ChannelGround ground(ServerLevel level, ArenaController arena) {
        if (ground == null) ground = ChannelGround.pin(level, arena);
        return ground;
    }

    /** The sets, once pinned (null before the first tick in an arena). */
    public @Nullable ChannelGround ground() {
        return ground;
    }

    private void tickGround(ServerLevel level, ArenaController arena) {
        ChannelGround g = ground(level, arena);
        if (g.current() == null) g.begin(shownChannel());
        if (!g.writing()) return;
        if (g.tick(level, arena)) {
            // The new set is down: nobody is left standing inside a wall of it.
            for (ServerPlayer p : challengers()) unstick(level, p);
            unstick(level, this);
        }
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING) {
            // The show waits while the set changes round it.
            if (scheduler().current() == null) scheduler().delay(10);
            getNavigation().stop();
        }
    }

    /** Lifts {@code e} out of any block a set was written into, to the first free space above (at most a set's height). */
    static void unstick(ServerLevel level, Entity e) {
        if (level.noCollision(e, e.getBoundingBox())) return;
        for (int up = 1; up <= ChannelLayouts.TOP + 2; up++) {
            if (level.noCollision(e, e.getBoundingBox().move(0, up, 0))) {
                if (e instanceof net.neoforged.neoforge.common.util.FakePlayer) e.moveTo(e.getX(), e.getY() + up, e.getZ());
                else e.teleportTo(e.getX(), e.getY() + up, e.getZ());
                return;
            }
        }
    }

    /** Whether a set is still being written: the fight waits for it. */
    public boolean writingSet() {
        return ground != null && ground.writing();
    }

    /** The channel flips to {@code ch}: static and the title card for the hunters, the new set, the new costume. */
    private void flipTo(ServerLevel level, ArenaController arena, Channel ch) {
        stopChannel(level);
        entityData.set(SHOWN, (byte) ch.ordinal());
        ground(level, arena).begin(ch);
        channelStart = -1;
        titled = true;
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.CHANNEL, ch.ordinal(), 0, position(), 30));
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.TITLE, ch.ordinal(), 0, position(), 60));
        if (ch == Channel.COMMERCIAL) triggerAnim("action", "wings_reveal");
    }

    /**
     * Development preview and tests: straight to a phase's look, with no break: its channel is shown (costume and, once
     * the sets are pinned, its set written over the next ticks).
     */
    @Override
    public void forceLook(int phase) {
        super.forceLook(phase);
        Channel ch = Channel.ofPhase(phase);
        if (level() instanceof ServerLevel level && arena() != null) flipTo(level, arena(), ch);
        else entityData.set(SHOWN, (byte) ch.ordinal());
    }

    /** Test and preview hook: puts {@code ch} on the air at once (its set written now, its costume on). */
    public void layChannelNow(Channel ch) {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        ChannelGround g = ground(level, arena);
        if (g.current() != ch) flipTo(level, arena, ch);
        g.finish(level, arena);
    }

    /** Whatever the channel had going stops: doubles, peels, trapdoors, signs. */
    private void stopChannel(ServerLevel level) {
        ArenaController arena = arena();
        discardDoubles(level);
        if (arena != null) {
            for (Peel p : peels) clearPeel(level, arena, p.pos());
            for (Restore r : restores) arena.mutate(level, r.pos(), r.state(), 0);
            popOutOfPits(level);
        }
        peels.clear();
        restores.clear();
        quizHunters.clear();
        quizQuestion = -1;
        tells.clear();
        shuffleAt = -1;
        stunnedUntil = 0;
        if (laughing()) entityData.set(LAUGHING, false);
        if (laughSent) fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_LAUGH, 0, position(), 0));
        laughSent = false;
    }

    private void tickRestores(ServerLevel level, ArenaController arena, long now) {
        if (restores.isEmpty()) return;
        boolean any = false;
        for (var it = restores.iterator(); it.hasNext(); ) {
            Restore r = it.next();
            if (now < r.at()) continue;
            arena.mutate(level, r.pos(), r.state(), 0);
            it.remove();
            any = true;
        }
        if (any) popOutOfPits(level);
    }

    // --- CH 2: the sitcom ---------------------------------------------------------------------------

    /** Whether the LAUGH sign is lit now: the sitcom on the air, in a lit spell. */
    public boolean laughingNow() {
        byte s = state();
        return phase() == 1 && channelStart >= 0 && s != EMERGING && s != TRANSITION && s != DYING
                && GabrielBalance.laughing(channelTicks());
    }

    private void tickSitcom(ServerLevel level, long now) {
        boolean lit = GabrielBalance.laughing(channelTicks());
        if (lit != laughing() || !laughSent) {
            entityData.set(LAUGHING, lit);
            laughSent = true;
            fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_LAUGH, lit ? 1 : 0, position(),
                    lit ? GabrielBalance.LAUGH_TICKS : GabrielBalance.QUIET_TICKS));
            if (lit) playSound(AllSounds.GABRIEL_LAUGH_TRACK.get(), 2.5f, 1.0f);
        }
        if (!lit) {
            int t = channelTicks();
            int cycle = Math.floorDiv(t, GabrielBalance.LAUGH_TICKS + GabrielBalance.QUIET_TICKS);
            long quietFrom = channelStart + GabrielBalance.quietStart(t);
            long quietSince = Math.max(quietFrom, lastPlayerHit);
            if (cycle != applaudedCycle && now - quietSince >= GabrielBalance.APPLAUSE_AFTER) {
                applaudedCycle = cycle;
                applause(level);
            }
        }
    }

    /** Nobody laid a finger on him in the quiet: he takes a bow, the audience applauds, he heals. */
    public void applause(ServerLevel level) {
        healTrue(GabrielBalance.APPLAUSE_HEAL * trueMaxHealth());
        triggerAnim("action", "host_gesture");
        playSound(AllSounds.GABRIEL_APPLAUSE.get(), 3.0f, 1.0f);
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.SIGN, SIGN_APPLAUSE, 1, position(), 50));
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.2, getZ(), 6, 0.5, 0.3, 0.5, 0.02);
    }

    /** Drops a banana peel at {@code pos} (air over a solid floor) for its time. @return whether it lies there */
    public boolean dropPeel(ServerLevel level, BlockPos pos) {
        ArenaController arena = arena();
        if (arena == null || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) return false;
        if (!arena.mutate(level, pos, Blocks.YELLOW_CARPET.defaultBlockState(), 0)) return false;
        peels.add(new Peel(pos.immutable(), level.getGameTime() + GabrielBalance.PEEL_TICKS));
        return true;
    }

    public int peels() {
        return peels.size();
    }

    private void clearPeel(ServerLevel level, ArenaController arena, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.YELLOW_CARPET)) arena.mutate(level, pos, Blocks.AIR.defaultBlockState(), 0);
    }

    /** Whoever steps on a peel slips: slowed, shoved off their feet, and the peel is gone. @return whether they slipped */
    public boolean slipCheck(LivingEntity e) {
        if (!(level() instanceof ServerLevel level) || peels.isEmpty()) return false;
        BlockPos at = e.blockPosition();
        for (var it = peels.iterator(); it.hasNext(); ) {
            Peel p = it.next();
            if (!p.pos().equals(at)) continue;
            it.remove();
            ArenaController arena = arena();
            if (arena != null) clearPeel(level, arena, p.pos());
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GabrielBalance.SLIP_TICKS, 2));
            Vec3 look = e.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 0.01) look = new Vec3(0, 0, 1);
            look = look.normalize();
            e.push(look.x * GabrielBalance.SLIP_SHOVE, 0.45, look.z * GabrielBalance.SLIP_SHOVE);
            e.hurtMarked = true;
            level.playSound(null, at, AllSounds.GABRIEL_LAUGH_TRACK.get(), SoundSource.HOSTILE, 1.2f, 1.2f);
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.2, e.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
            return true;
        }
        return false;
    }

    // --- CH 5: the game show ------------------------------------------------------------------------

    private void tickGameShow(ServerLevel level, long now) {
        if (quizActive()) {
            if (scheduler().current() == null) scheduler().delay(10);
            if (now >= quizEnds) endQuiz();
        } else if (now >= nextQuiz) {
            List<ServerPlayer> hunters = challengers();
            if (!hunters.isEmpty()) startQuiz(hunters);
            else nextQuiz = now + 40;
        }
    }

    public boolean quizActive() {
        return quizQuestion >= 0;
    }

    /** The question asked now (an index in {@link QuizBank#QUESTIONS}), -1 between rounds. */
    public int quizQuestion() {
        return quizQuestion;
    }

    /** The platform holding the right answer this round. */
    public int quizRightPlatform() {
        return QuizBank.rightPlatform(quizDeal);
    }

    /** A round of the quiz for {@code hunters}: his attacks pause, the question goes up, the buzzer comes later. */
    public void startQuiz(List<ServerPlayer> hunters) {
        if (!(level() instanceof ServerLevel level)) return;
        scheduler().cancel();
        scheduler().delay(GabrielBalance.QUIZ_ANSWER_TICKS);
        Random r = new Random(random.nextLong());
        QuizBank.Question last = lastQuestion >= 0 ? QuizBank.QUESTIONS.get(lastQuestion) : null;
        QuizBank.Question q = QuizBank.pick(r, last);
        quizQuestion = QuizBank.QUESTIONS.indexOf(q);
        lastQuestion = quizQuestion;
        quizDeal = QuizBank.deal(r);
        quizEnds = level.getGameTime() + GabrielBalance.QUIZ_ANSWER_TICKS;
        quizHunters.clear();
        quizHunters.addAll(hunters);
        GabrielFxPayload ask = new GabrielFxPayload(getId(), GabrielFxPayload.QUIZ, quizQuestion, QuizBank.pack(quizDeal), position(),
                GabrielBalance.QUIZ_ANSWER_TICKS);
        for (ServerPlayer p : hunters) PacketDistributor.sendToPlayer(p, ask);
        triggerAnim("action", "host_gesture");
        playSound(AllSounds.GABRIEL_DING.get(), 2.0f, 1.4f);
    }

    /** The buzzer: every hunter of the round is judged by the platform they stand on. */
    public void endQuiz() {
        if (!(level() instanceof ServerLevel level)) return;
        closeQuiz(level, true);
    }

    private void closeQuiz(ServerLevel level, boolean judge) {
        if (!quizActive()) return;
        int right = quizRightPlatform();
        boolean anyRight = false;
        Set<Integer> opened = new java.util.HashSet<>();
        for (ServerPlayer p : List.copyOf(quizHunters)) {
            if (!judge || !p.isAlive() || p.isRemoved()) {
                PacketDistributor.sendToPlayer(p, new GabrielFxPayload(getId(), GabrielFxPayload.QUIZ, -1, 0, p.position(), 0));
                continue;
            }
            int on = platformUnder(p);
            boolean ok = on == right;
            if (ok) {
                anyRight = true;
                // The ding and the buzzer are the client's (it plays them as the panel closes).
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GabrielBalance.QUIZ_STRENGTH_TICKS, 0));
            } else if (on >= 0) {
                if (opened.add(on)) openTrapdoor(level, on);
                strike(p, GabrielBalance.QUIZ_WRONG_DAMAGE);
            } else {
                mallet(level, p);
            }
            PacketDistributor.sendToPlayer(p, new GabrielFxPayload(getId(), GabrielFxPayload.QUIZ, -1, ok ? 1 : 0, p.position(), 0));
        }
        if (judge) {
            triggerAnim("action", "buzzer_slam");
            if (anyRight) stun(GabrielBalance.QUIZ_STUN_TICKS);
        }
        quizQuestion = -1;
        quizHunters.clear();
        nextQuiz = level.getGameTime() + GabrielBalance.QUIZ_EVERY;
    }

    /** The platform {@code e} stands on (0 RED, 1 BLUE, 2 YELLOW), -1 if none. */
    public int platformUnder(Entity e) {
        ArenaController arena = arena();
        if (ground == null || arena == null) return -1;
        BlockPos c = arena.center();
        int dx = e.getBlockX() - c.getX(), dz = e.getBlockZ() - c.getZ();
        int p = ChannelLayouts.platformAt(dx, dz);
        if (p < 0) return -1;
        BlockPos top = ground.at(dx, 0, dz);
        if (top == null) return -1;
        double above = e.getY() - (top.getY() + 1);
        return above >= -0.5 && above <= 2.5 ? p : -1;
    }

    /** Where platform {@code index}'s centre top block is in the world (null before the sets are pinned). */
    public @Nullable BlockPos platformCentre(int index) {
        return ground != null ? ground.at(ChannelLayouts.PLATFORMS.get(index)) : null;
    }

    /** A wrong answer: the platform's trapdoor drops open into the foam pit, and shuts again a moment later. */
    public void openTrapdoor(ServerLevel level, int index) {
        ArenaController arena = arena();
        if (ground == null || arena == null) return;
        BlockState block = HorsemenGround.state(ChannelLayouts.PLATFORM_BLOCKS.get(index));
        long at = level.getGameTime() + GabrielBalance.TRAPDOOR_TICKS;
        for (ChannelLayouts.Spot s : ChannelLayouts.platformCells(index)) {
            BlockPos pos = ground.at(s);
            if (pos == null) continue;
            if (arena.mutate(level, pos, Blocks.AIR.defaultBlockState(), 0)) restores.add(new Restore(pos, block, at));
        }
        level.playSound(null, platformCentre(index) != null ? platformCentre(index) : blockPosition(), net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_OPEN,
                SoundSource.BLOCKS, 2.0f, 0.7f);
    }

    /** No answer at all: the giant mallet comes down on them. */
    public void mallet(ServerLevel level, LivingEntity e) {
        strike(e, GabrielBalance.QUIZ_WRONG_DAMAGE);
        e.push(0, 0.9, 0);
        e.hurtMarked = true;
        level.playSound(null, e.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4f, 1.3f);
        level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 1, e.getZ(), 2, 0.2, 0.2, 0.2, 0);
    }

    /** Anyone fallen through a trapdoor is back on the floor when it shuts (never shut in under the set). */
    private void popOutOfPits(ServerLevel level) {
        ArenaController arena = arena();
        if (ground == null || arena == null) return;
        List<LivingEntity> all = new ArrayList<>(challengers());
        all.addAll(quizHunters);
        for (LivingEntity e : all) {
            BlockPos c = arena.center();
            int dx = e.getBlockX() - c.getX(), dz = e.getBlockZ() - c.getZ();
            if (ChannelLayouts.platformAt(dx, dz) < 0) continue;
            BlockPos top = ground.at(dx, 0, dz);
            if (top != null && e.getY() < top.getY() + 0.5 && level.getBlockState(top).isSolid()) {
                e.teleportTo(e.getX(), top.getY() + 1.0, e.getZ());
            }
        }
    }

    /** A right answer leaves him reeling: no attacks, no walking, and he takes more. */
    public void stun(int ticks) {
        stunnedUntil = level().getGameTime() + ticks;
        scheduler().cancel();
        scheduler().delay(ticks);
        getNavigation().stop();
        triggerAnim("action", "hit");
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.0, getZ(), 20, 0.4, 0.3, 0.4, 0.2);
        }
    }

    public boolean isStunned() {
        return level().getGameTime() < stunnedUntil;
    }

    // --- CH 7: the hospital -------------------------------------------------------------------------

    private void tickHospital(ServerLevel level, long now) {
        if (lastBeat < 0 || now - lastBeat >= beatPeriod) beat(level);
        if (now >= nextNurses) {
            nextNurses = now + GabrielBalance.NURSE_EVERY;
            spawnNurses(level);
        }
    }

    /** The heart monitor beeps; the next beep comes sooner the lower he is. */
    public void beat(ServerLevel level) {
        lastBeat = level.getGameTime();
        beatPeriod = GabrielBalance.beatPeriod(GabrielBalance.hospitalProgress(getHealth() / getMaxHealth()));
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.BEAT, 0, 0, position(), beatPeriod));
        playSound(AllSounds.GABRIEL_MONITOR_BEEP.get(), 1.6f, 1.0f);
    }

    /** Whether a blow now lands on the beat (the hospital on the air, within the window of a beep). */
    public boolean onBeat() {
        return phase() == 3 && lastBeat >= 0 && GabrielBalance.onBeat((int) (level().getGameTime() - lastBeat), beatPeriod);
    }

    /** Nurses get up from the beds and go to him. */
    public List<GabrielDoubleEntity> spawnNurses(ServerLevel level) {
        List<GabrielDoubleEntity> out = new ArrayList<>();
        ArenaController arena = arena();
        if (ground == null || arena == null) return out;
        List<ChannelLayouts.Spot> beds = new ArrayList<>(ChannelLayouts.BEDS);
        java.util.Collections.shuffle(beds, new Random(random.nextLong()));
        for (int i = 0; i < GabrielBalance.NURSES && i < beds.size(); i++) {
            BlockPos at = ground.anywhere(level, arena, new ChannelLayouts.Spot(beds.get(i).dx(), 1, beds.get(i).dz()));
            if (at == null) continue;
            GabrielDoubleEntity nurse = spawnDouble(level, GabrielDoubleEntity.Role.NURSE, Vec3.atBottomCenterOf(at));
            if (nurse != null) out.add(nurse);
        }
        return out;
    }

    /** A nurse reached him: he heals, she goes. */
    public void nurseArrives(GabrielDoubleEntity nurse) {
        healTrue(GabrielBalance.NURSE_HEAL * trueMaxHealth());
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.2, getZ(), 5, 0.4, 0.3, 0.4, 0.02);
            level.sendParticles(ParticleTypes.POOF, nurse.getX(), nurse.getY() + 1, nurse.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        }
        playSound(AllSounds.GABRIEL_MONITOR_BEEP.get(), 1.2f, 1.6f);
        doubles.remove(nurse.getUUID());
        nurse.discard();
    }

    /** Heals {@code amount} true health, never past the start of the phase he is in. */
    public void healTrue(float amount) {
        float ceiling = phase() <= 1 ? getMaxHealth() : getMaxHealth() * threshold(phase() - 1);
        setHealth(Math.min(ceiling, getHealth() + amount / healthScale()));
    }

    // --- CH 9: the commercial -----------------------------------------------------------------------

    private void tickCommercial(ServerLevel level, ArenaController arena, long now) {
        if (shuffleAt >= 0 && now >= shuffleAt) shuffle(level);
        else if (nextShuffle >= 0 && now >= nextShuffle) shuffle(level);
        // He stays at his podium.
        Vec3 spot = podiumSpot(realPodium);
        if (spot != null) {
            if (position().distanceToSqr(spot) > 0.5 * 0.5) teleportTo(spot.x, spot.y, spot.z);
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            getNavigation().stop();
        }
        if (tickCount % GabrielBalance.REVEAL_REFRESH == 0) reveal();
    }

    /** Where spokesman spot {@code podium} is (feet), or null before the sets are pinned. */
    public @Nullable Vec3 podiumSpot(int podium) {
        ArenaController arena = arena();
        if (ground == null || arena == null || !(level() instanceof ServerLevel level)) return null;
        ChannelLayouts.Spot s = ChannelLayouts.PODIUMS.get(Math.floorMod(podium, ChannelLayouts.PODIUMS.size()));
        BlockPos top = ground.at(s.dx(), 1, s.dz());
        if (top == null) top = ground.anywhere(level, arena, new ChannelLayouts.Spot(s.dx(), 1, s.dz()));
        return top != null ? Vec3.atBottomCenterOf(top.above()) : null;
    }

    /** The commercial begins: four spokesmen join him at the podiums. */
    public void beginCommercial(ServerLevel level) {
        shuffle(level);
    }

    /** The real one's podium. */
    public int realPodium() {
        return realPodium;
    }

    /** The spokesmen doubles standing now. */
    public List<GabrielDoubleEntity> spokesmen() {
        List<GabrielDoubleEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : doubles) {
            if (level.getEntity(id) instanceof GabrielDoubleEntity d && d.isAlive() && d.role() == GabrielDoubleEntity.Role.SPOKESMAN) out.add(d);
        }
        return out;
    }

    /** Whether the podiums are about to shuffle (a blow on the real one). */
    public boolean shufflePending() {
        return shuffleAt >= 0;
    }

    /** Everyone to a podium again, at random: the doubles topped up to four, the shadow sent, free will's tells. */
    public void shuffle(ServerLevel level) {
        shuffleAt = -1;
        nextShuffle = level.getGameTime() + GabrielBalance.SHUFFLE_EVERY;
        List<GabrielDoubleEntity> men = spokesmen();
        while (men.size() < GabrielBalance.SPOKESMEN - 1) {
            GabrielDoubleEntity d = spawnDouble(level, GabrielDoubleEntity.Role.SPOKESMAN, position());
            if (d == null) break;
            men.add(d);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ChannelLayouts.PODIUMS.size(); i++) order.add(i);
        java.util.Collections.shuffle(order, new Random(random.nextLong()));
        realPodium = order.getFirst();
        Vec3 c = arena() != null ? arena().centerVec() : position();
        place(level, this, podiumSpot(realPodium), c);
        for (int i = 0; i < men.size() && i + 1 < order.size(); i++) place(level, men.get(i), podiumSpot(order.get(i + 1)), c);
        triggerAnim("action", "spokesman_pose");
        for (GabrielDoubleEntity d : men) d.triggerAnim("action", "spokesman_pose");
        playSound(AllSounds.GABRIEL_SNAP.get(), 2.0f, 1.0f);
        reveal();
        sendTells(level);
    }

    private static void place(ServerLevel level, LivingEntity e, @Nullable Vec3 at, Vec3 centre) {
        if (at == null) return;
        level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
        float yaw = LuciferAttacks.yawTo(at, centre);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.teleportTo(at.x, at.y, at.z);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1, at.z, 10, 0.3, 0.6, 0.3, 0.02);
    }

    /** The real one's six-winged shadow, at his feet, for everyone. */
    private void reveal() {
        fx(new GabrielFxPayload(getId(), GabrielFxPayload.REVEAL, 0, 0, position(), GabrielBalance.REVEAL_REFRESH + 10));
    }

    /** Free will (v0.13): every human hunter sees through one more double. */
    private void sendTells(ServerLevel level) {
        tells.clear();
        List<GabrielDoubleEntity> men = spokesmen();
        if (men.isEmpty()) return;
        for (ServerPlayer p : challengers()) tellFor(p, men);
    }

    /** Picks (and sends) the double {@code p} sees through, if they have free will. @return it, or null */
    public @Nullable GabrielDoubleEntity tellFor(ServerPlayer p, List<GabrielDoubleEntity> men) {
        if (!Kin.freeWill(p) || men.isEmpty()) return null;
        GabrielDoubleEntity d = men.get(random.nextInt(men.size()));
        tells.put(p.getUUID(), d.getUUID());
        PacketDistributor.sendToPlayer(p, new GabrielFxPayload(d.getId(), GabrielFxPayload.FREE_WILL_TELL, 0, 0, d.position(),
                GabrielBalance.SHUFFLE_EVERY + 20));
        return d;
    }

    /** A double was struck by {@code by}: a spokesman's punishment and a shuffle; any other simply goes. */
    public void doubleStruck(GabrielDoubleEntity d, @Nullable LivingEntity by) {
        doubles.remove(d.getUUID());
        if (!(level() instanceof ServerLevel level) || d.role() != GabrielDoubleEntity.Role.SPOKESMAN) return;
        if (by != null) punish(level, by, random.nextInt(3));
        playSound(AllSounds.GABRIEL_LAUGH_TRACK.get(), 2.5f, 1.1f);
        shuffleAt = level.getGameTime() + GabrielBalance.STRUCK_SHUFFLE_DELAY;
    }

    /** Fooled: a short teleport, a pie in the face, or a blow. */
    public void punish(ServerLevel level, LivingEntity who, int kind) {
        switch (kind) {
            case PUNISH_TELEPORT -> {
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 to = LuciferAttacks.floorAt(this, who.position().add(Math.cos(a) * GabrielBalance.PUNISH_TELEPORT, 0,
                        Math.sin(a) * GabrielBalance.PUNISH_TELEPORT));
                ArenaController arena = arena();
                if (arena != null && arena.horizontalDistance(to) > arena.radius() - 3) to = LuciferAttacks.floorAt(this, arena.centerVec());
                level.sendParticles(ParticleTypes.POOF, who.getX(), who.getY() + 1, who.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                // A fake player's connection ignores teleports (machines of other mods, tests): move it instead.
                if (who instanceof net.neoforged.neoforge.common.util.FakePlayer) who.moveTo(to.x, to.y, to.z);
                else who.teleportTo(to.x, to.y, to.z);
                playSound(AllSounds.GABRIEL_SNAP.get(), 1.5f, 1.3f);
            }
            case PUNISH_PIE -> PieProjectile.splat(level, who);
            default -> strike(who, GabrielBalance.DOUBLE_PUNISH_DAMAGE);
        }
    }

    // --- doubles ------------------------------------------------------------------------------------

    /** One of his doubles in {@code role}, at {@code at}, in the channel's costume. */
    public @Nullable GabrielDoubleEntity spawnDouble(ServerLevel level, GabrielDoubleEntity.Role role, Vec3 at) {
        GabrielDoubleEntity d = AllEntities.GABRIEL_DOUBLE.get().create(level);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
        d.finalizeSpawn(level, level.getCurrentDifficultyAt(d.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        d.setRole(role);
        d.setCostume(role == GabrielDoubleEntity.Role.NURSE ? Channel.Costume.LAB_COAT : shownChannel().costume);
        d.setOwner(getUUID());
        level.addFreshEntity(d);
        doubles.add(d.getUUID());
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1, at.z, 10, 0.3, 0.6, 0.3, 0.02);
        return d;
    }

    /** His doubles of every role standing now. */
    public List<GabrielDoubleEntity> doubles() {
        List<GabrielDoubleEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : doubles) if (level.getEntity(id) instanceof GabrielDoubleEntity d && d.isAlive()) out.add(d);
        return out;
    }

    /** A double went on its own (lifetime, revealed): forget it. */
    void forget(GabrielDoubleEntity d) {
        doubles.remove(d.getUUID());
    }

    private void discardDoubles(ServerLevel level) {
        for (UUID id : List.copyOf(doubles)) {
            Entity e = level.getEntity(id);
            if (e != null) {
                level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.6, 0.3, 0.02);
                e.discard();
            }
        }
        doubles.clear();
    }

    // --- allegiance (v0.13) -------------------------------------------------------------------------

    /** Each second of a commercial break: an angel's Grace drains. */
    public void commercialBreakSecond(List<ServerPlayer> hunters) {
        if (state() != TRANSITION) return;
        for (ServerPlayer p : hunters) {
            if (Allegiances.get(p).isAngel()) Allegiances.addEssence(p, -GabrielBalance.ANGEL_GRACE_DRAIN);
        }
    }

    // --- damage -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && laughingNow()) {
            // The audience is laughing: nothing touches him.
            if (source.getEntity() instanceof Player && tickCount % 10 == 0) playSound(deflectSound(), 1.0f, 1.3f);
            return false;
        }
        boolean beat = onBeat() && source.getEntity() instanceof Player;
        float before = getHealth();
        boolean hurt = super.hurt(source, amount);
        if (getHealth() < before && source.getEntity() instanceof Player && level() instanceof ServerLevel level) {
            lastPlayerHit = level.getGameTime();
            if (beat) level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.4, getZ(), 24, 0.4, 0.6, 0.4, 0.3);
            if (channel() == Channel.COMMERCIAL && state() != TRANSITION && state() != DYING && shuffleAt < 0) {
                shuffleAt = level.getGameTime() + GabrielBalance.STRUCK_SHUFFLE_DELAY;
            }
        }
        return hurt;
    }

    /** Beyond Lucifer's own: the dark spell of the sitcom, a stun, a blow on the beep. */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (phase() == 1 && channelStart >= 0 && !GabrielBalance.laughing(channelTicks())) v *= GabrielBalance.QUIET_VULNERABILITY;
        if (isStunned()) v *= GabrielBalance.STUN_VULNERABILITY;
        if (source.getEntity() instanceof Player && onBeat()) v *= GabrielBalance.BEAT_CRIT;
        return v;
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack he uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; else, a snap of the fingers to close the gap (never off his podium in the commercial). */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (channel() != Channel.COMMERCIAL && (distanceToSqr(target) > 16 * 16 || noSightTicks > 60)) {
            noSightTicks = 0;
            return GabrielAttacks.SnapStep::new;
        }
        return null;
    }

    /** Sends one of TV Land's moments to every hunter near. */
    public void fx(GabrielFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    /** Gabriel says {@code key} to everyone near. */
    public void say(ServerLevel level, String key) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
        }
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only his own clips: Lucifer's names that he lacks ({@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !GabrielAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    /** Clips that hold their last frame. */
    static final List<String> HOLDS = List.of("emerge", "death", "wings_reveal", "spokesman_pose");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        RawAnimation laugh = RawAnimation.begin().thenLoop(pre + "laugh");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == DYING) return PlayState.STOP;
            if (state.isMoving()) return state.setAndContinue(walk);
            return state.setAndContinue(laughing() ? laugh : idle);
        }));
        AnimationController<GabrielEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : GabrielAssets.TRIGGERED) {
            action.triggerableAnim(name, HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("HealthScale", healthScale);
        tag.putBoolean("Scaled", scaled);
        tag.putByte("Shown", entityData.get(SHOWN));
        ListTag list = new ListTag();
        for (UUID id : doubles) list.add(NbtUtils.createUUID(id));
        tag.put("Doubles", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : 1f;
        scaled = tag.getBoolean("Scaled");
        entityData.set(SHOWN, tag.contains("Shown") ? tag.getByte("Shown") : (byte) (phase() - 1));
        doubles.clear();
        for (Tag t : tag.getList("Doubles", Tag.TAG_INT_ARRAY)) doubles.add(NbtUtils.loadUUID(t));
    }
}
