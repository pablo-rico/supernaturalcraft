package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.leviathan.BoraxSensitive;
import org.papiricoh.supernaturalcraft.network.GodstielFxPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.pact.LabSite;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Godstiel (v0.39): Castiel, full of Purgatory's souls and, under them, the Leviathans. Five phases: four in the hall of
 * Crowley's lab (the New God, Judgment, the Cracks, the Voices) and the last out on the reservoir's deck (the Overflow), the
 * water gone black. See {@link GodstielBalance} and {@link GodstielAttacks}.
 *
 * <p>From phase III Castiel fights what is in him ({@link #struggling()}): he doubles over and takes more of every blow, and a
 * hunter who speaks to him then (right click) keeps him in it a little longer. Borax burns him then too ({@link #onBorax}).
 *
 * <p>Beaten, he does not die at once: he kneels and the hunter who opened the door decides (silence: condemn) —
 * {@link GodstielSpoils}. Forgiven, he rises as the goo pours out of him and goes; condemned, the Leviathans drag him into the
 * water. Either way his fall is a boss death (credit, shares, spoils). A rematch (by rite) ends in his plain death.
 */
public class GodstielEntity extends LuciferEntity implements BoraxSensitive {

    /** Persistent-data flag on the heretics he drags in to judge. */
    public static final String HERETIC_TAG = "sn_godstiel_heretic";

    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(GodstielEntity.class, EntityDataSerializers.BYTE);
    public static final int STRUGGLE = 1, KNEELING = 2, GONE = 4, FORGIVEN = 8, CONDEMNED = 16;

    private @Nullable BlockPos labOrigin;
    private int labQ;
    private boolean emerged, rematch;
    private int nextStruggle, struggleTicks, kneelTicks = -1, boraxCooldown;
    private final Set<UUID> spokeToHim = new HashSet<>();
    private @Nullable UUID opener, askedOf;
    private @Nullable Boolean verdictForgive;
    /** The reservoir's water still to blacken (phase V). */
    private final Deque<BlockPos> toBlacken = new ArrayDeque<>();

    public GodstielEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    protected boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    protected void setFlag(int bit, boolean on) {
        byte b = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? b | bit : b & ~bit));
    }

    public boolean struggling() {
        return flag(STRUGGLE);
    }

    /** Ticks of the inner fight left (0 when he is not in it). */
    public int struggleTicks() {
        return struggling() ? struggleTicks : 0;
    }

    public boolean kneeling() {
        return flag(KNEELING);
    }

    /** Hidden (moving to the reservoir, or sunk). */
    public boolean gone() {
        return flag(GONE);
    }

    public boolean forgiven() {
        return flag(FORGIVEN);
    }

    public boolean condemned() {
        return flag(CONDEMNED);
    }

    // --- set-up ----------------------------------------------------------------------------------------------------------

    /** At Crowley's lab: the fight walks from the hall to the reservoir (else, spawned by egg or command, it stays where he is). */
    public void bindLab(LabSite site) {
        labOrigin = site.origin().immutable();
        labQ = site.q();
    }

    public @Nullable LabSite lab() {
        return labOrigin == null ? null : new LabSite(labOrigin, labQ);
    }

    /** The hunter who opened the door: they decide his fate (else whoever struck last). */
    public void opener(@Nullable UUID who) {
        opener = who;
    }

    public @Nullable UUID opener() {
        return opener;
    }

    /** A rematch (by rite): no talk at the end, only the fight. */
    public void rematch(boolean on) {
        rematch = on;
    }

    public boolean isRematch() {
        return rematch;
    }

    // --- his numbers -----------------------------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return GodstielBalance.PHASES;
    }

    @Override
    protected float threshold(int phase) {
        return GodstielBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.GODSTIEL_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.GODSTIEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.GODSTIEL_DAMAGE_FACTOR.get().floatValue();
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
    protected int emergeTicks() {
        return GodstielCinematics.INTRO_TICKS;
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return GodstielAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return GodstielBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation." + GodstielAssets.RIG + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return GodstielAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return GodstielAssets.BAR_PREFIX + ".phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase < GodstielBalance.LEVIATHAN_PHASE ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase < GodstielBalance.LEVIATHAN_PHASE ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.GRACE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.godstiel("wings");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.godstiel("voices");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.godstiel("crack");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return phase() >= GodstielBalance.LEVIATHAN_PHASE ? AllSounds.godstiel("voices") : AllSounds.godstiel("wings");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.godstiel("death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.godstiel("hurt");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.godstiel("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.godstiel("death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(AllParticles.GRACE.get(), getRandomX(0.8), getY() + random.nextDouble() * 2, getRandomZ(0.8), 0, 0.05, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (Boolean.TRUE.equals(verdictForgive)) {
            if (!last) level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 2, 0.3, 0.6, 0.3, 0.02);
            return;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), last ? 60 : 3, 0.4, 0.8, 0.4, 0.05);
    }

    // --- the fight's shape -----------------------------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), GodstielSummoning.FREE_RADIUS, ArenaTheme.LAB);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        LabSite site = lab();
        if (site == null || phase < GodstielBalance.RESERVOIR_PHASE) return;
        for (BlockPos p : site.doors()) arena.mutate(level, p, Blocks.AIR.defaultBlockState(), 0);
        toBlacken.clear();
        for (BlockPos p : site.water()) {
            BlockPos above = p.above();
            if (level.getFluidState(p).isSource() && level.getBlockState(above).isAir()) toBlacken.add(above);
        }
    }

    /** Bars across the big doors (the fight stays in the hall until the reservoir): called as the arena opens. */
    public void barDoors(ServerLevel level, ArenaController arena) {
        LabSite site = lab();
        if (site == null) return;
        for (BlockPos p : site.doors()) {
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
        }
    }

    @Override
    protected void playEmergence() {
        GodstielCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        GodstielCinematics.transition(this, to);
    }

    @Override
    protected int transitionTicks(int to) {
        return GodstielBalance.TRANSITION;
    }

    @Override
    protected void onTransitionStart(int to) {
        endStruggle(false);
        if (to >= GodstielBalance.LEVIATHAN_PHASE) {
            triggerAnim("action", "crack");
            fx(GodstielFxPayload.CRACK, GodstielBalance.cracks(to), 0, position(), 40);
        } else {
            triggerAnim("action", "ascend");
        }
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
        if (phase() == GodstielBalance.RESERVOIR_PHASE && lab() != null) {
            if (elapsed == GodstielBalance.LEAVE_AT) {
                sound("wings", 2f, 0.7f);
                setFlag(GONE, true);
                Vec3 to = deckCentre();
                teleportTo(to.x, to.y, to.z);
                fx(GodstielFxPayload.BLACK_WATER, 0, 0, to, GodstielBalance.TRANSITION);
            }
            if (elapsed == GodstielBalance.ARRIVE_AT) {
                setFlag(GONE, false);
                triggerAnim("action", "descend");
            }
        }
        if (elapsed == GodstielBalance.ARRIVE_AT) {
            fx(GodstielFxPayload.TITLE, phase(), 0, position(), 70);
            sound(phase() >= GodstielBalance.LEVIATHAN_PHASE ? "voices" : "wings", 2f, 1f);
        }
    }

    /** Where the fight stands now: the hall in phases I-IV, the deck in V (the arena's centre without a lab). */
    public Vec3 arenaCentre() {
        LabSite site = lab();
        if (site != null) return phase() >= GodstielBalance.RESERVOIR_PHASE ? site.floor(LabAnchors.DECK) : site.floor(LabAnchors.HALL);
        ArenaController arena = arena();
        return arena != null ? Vec3.atBottomCenterOf(arena.center()) : position();
    }

    public Vec3 deckCentre() {
        LabSite site = lab();
        return site != null ? site.floor(LabAnchors.DECK) : arenaCentre();
    }

    /** Where the heretics and the doubles come: the room's spawn points (round him without a lab). */
    public List<Vec3> spawnPoints() {
        LabSite site = lab();
        if (site != null) {
            List<Vec3> s = site.spawns(phase() >= GodstielBalance.RESERVOIR_PHASE ? "deck" : "hall");
            if (!s.isEmpty()) return s;
        }
        List<Vec3> out = new ArrayList<>();
        Vec3 c = arenaCentre();
        for (int i = 0; i < LabAnchors.MIN_SPAWNS; i++) {
            double ang = i * Math.PI / 2 + Math.PI / 4;
            out.add(LuciferAttacks.floorAt(this, c.add(Math.cos(ang) * 6, 0, Math.sin(ang) * 6)));
        }
        return out;
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        return arenaCentre();
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected int deathTicks() {
        if (Boolean.TRUE.equals(verdictForgive)) return GodstielBalance.RISE_TICKS;
        if (Boolean.FALSE.equals(verdictForgive)) return GodstielBalance.SINK_TICKS;
        return GodstielBalance.DEATH_TICKS;
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".godstiel."));
    }

    @Override
    protected boolean walks() {
        return !kneeling() && !gone() && !struggling();
    }

    // --- damage ----------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (kneeling() || gone()) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return super.vulnerability(source) * (struggling() ? GodstielBalance.STRUGGLE_VULNERABILITY : 1f);
    }

    @Override
    public boolean isPickable() {
        return !gone() && super.isPickable();
    }

    @Override
    public boolean isInvisible() {
        return gone() || super.isInvisible();
    }

    /** From phase III borax burns what is in him: a bite of his health and a stagger (once in a while). */
    @Override
    public void onBorax(ServerLevel level, @Nullable Entity owner) {
        if (phase() < GodstielBalance.LEVIATHAN_PHASE || state() == DYING || state() == EMERGING || kneeling() || boraxCooldown > 0) return;
        boraxCooldown = GodstielBalance.BORAX_COOLDOWN;
        float blow = ProgressionScale.referenceHit(ProgressionScale.of(BossProgression.Boss.GODSTIEL).tier()) * GodstielBalance.BORAX_HITS;
        hurt(level.damageSources().source(AllDamageTypes.BORAX, owner), blow);
        scheduler().delay(GodstielBalance.BORAX_STAGGER);
        playSound(AllSounds.leviathan("borax_fizz"), 1.5f, 0.7f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, getX(), getY() + 1.2, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
    }

    // --- the inner fight -------------------------------------------------------------------------------------------------

    private void tickStruggle() {
        if (boraxCooldown > 0) boraxCooldown--;
        if (struggling()) {
            getNavigation().stop();
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            scheduler().delay(5);
            if (--struggleTicks <= 0) endStruggle(true);
            return;
        }
        if (state() == TRANSITION || state() == EMERGING || state() == DYING || kneeling()) return;
        float[] at = GodstielBalance.struggles();
        float fraction = getHealth() / getMaxHealth();
        boolean due = false;
        // Several marks passed at once (a big blow, a reload): one struggle for all of them.
        while (nextStruggle < at.length && fraction <= at[nextStruggle] + 1.0e-4f) {
            nextStruggle++;
            due = true;
        }
        if (due) beginStruggle();
    }

    private void beginStruggle() {
        scheduler().cancel();
        spokeToHim.clear();
        struggleTicks = GodstielBalance.STRUGGLE_TICKS;
        setFlag(STRUGGLE, true);
        triggerAnim("action", "struggle");
        sound("struggle", 2f, 1f);
        int line = nextStruggle - 1;
        fx(GodstielFxPayload.STRUGGLE, 1, 0, position(), struggleTicks);
        fx(GodstielFxPayload.VOICES, line, 0, position(), 60);
        tell("message.supernaturalcraft.godstiel.struggle." + line, ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC);
    }

    private void endStruggle(boolean recover) {
        if (!struggling()) return;
        setFlag(STRUGGLE, false);
        struggleTicks = 0;
        if (recover) triggerAnim("action", "recover");
        fx(GodstielFxPayload.STRUGGLE, 0, spokeToHim.size(), position(), 0);
    }

    /** A hunter speaks to him while he struggles (right click): he stays in it a little longer, once each, up to a cap. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return struggling() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (!struggling() || !(player instanceof ServerPlayer p) || !spokeToHim.add(p.getUUID())) return InteractionResult.PASS;
        int elapsed = GodstielBalance.struggleExtended(spokeToHim.size() - 1) - struggleTicks;
        struggleTicks = Math.max(0, GodstielBalance.struggleExtended(spokeToHim.size()) - elapsed);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.godstiel.spoke").withStyle(ChatFormatting.AQUA), true);
        fx(GodstielFxPayload.STRUGGLE, 1, spokeToHim.size(), position(), struggleTicks);
        return InteractionResult.CONSUME;
    }

    // --- the goo and the black water -------------------------------------------------------------------------------------

    /** Goo spilled in a cone from {@code from} along {@code yaw}: pools on the floor, sticky and burning, dries later. */
    public void spillGoo(Vec3 from, float yaw, double reach) {
        ArenaController arena = arena();
        if (arena == null || !(level() instanceof ServerLevel level)) return;
        BlockState goo = AllBlocks.LEVIATHAN_GOO.get().defaultBlockState();
        Vec3 dir = LuciferAttacks.dirOf(yaw);
        for (int i = 0; i < 14; i++) {
            double d = 1.5 + random.nextDouble() * (reach - 1.5), side = (random.nextDouble() - 0.5) * d * 1.2;
            Vec3 at = from.add(dir.scale(d)).add(new Vec3(-dir.z, 0, dir.x).scale(side));
            Vec3 floor = LuciferAttacks.floorAt(this, at);
            BlockPos p = BlockPos.containing(floor);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                arena.mutate(level, p, goo, GodstielBalance.GOO_TICKS);
            }
        }
    }

    private void tickBlacken(ServerLevel level) {
        ArenaController arena = arena();
        if (arena == null || toBlacken.isEmpty()) return;
        BlockState goo = AllBlocks.LEVIATHAN_GOO.get().defaultBlockState();
        for (int i = 0; i < GodstielBalance.BLACKEN_PER_TICK && !toBlacken.isEmpty(); i++) {
            BlockPos p = toBlacken.poll();
            if (level.getBlockState(p).isAir()) arena.mutate(level, p, goo, 0);
        }
    }

    // --- his people ------------------------------------------------------------------------------------------------------

    /** Heretics kneeling in the hall now. */
    public int heretics() {
        return count(e -> e.getPersistentData().getBoolean(HERETIC_TAG));
    }

    /** His Leviathan doubles about now. */
    public int doubles() {
        return count(e -> e instanceof LeviathanEntity);
    }

    private int count(java.util.function.Predicate<Entity> what) {
        if (!(level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (UUID id : minions()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive() && what.test(e)) n++;
        }
        return n;
    }

    private void dismissMinions(ServerLevel level) {
        for (UUID id : new ArrayList<>(minions())) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.8, 0.3, 0.05);
                e.discard();
            }
        }
    }

    // --- his end ---------------------------------------------------------------------------------------------------------

    @Override
    protected boolean interceptDeath() {
        if (rematch) return false;
        scheduler().cancel();
        endStruggle(false);
        setFlag(KNEELING, true);
        triggerAnim("action", "kneel");
        kneelTicks = 0;
        if (level() instanceof ServerLevel level) dismissMinions(level);
        fx(GodstielFxPayload.TITLE, GodstielBalance.PHASES, 1, position(), 70);
        return true;
    }

    /** The verdict (from {@link GodstielSpoils}): forgiven, he rises and goes; condemned, the Leviathans drag him under. */
    public void verdict(boolean forgive, @Nullable ServerPlayer by) {
        if (verdictForgive != null || !kneeling()) return;
        verdictForgive = forgive;
        setFlag(KNEELING, false);
        setFlag(forgive ? FORGIVEN : CONDEMNED, true);
        triggerAnim("action", forgive ? "rise" : "sink");
        fx(GodstielFxPayload.TITLE, GodstielBalance.PHASES, forgive ? 2 : 3, position(), 80);
        if (by != null) askedOf = by.getUUID();
        beginDying();
    }

    public @Nullable Boolean verdict() {
        return verdictForgive;
    }

    /** Who the talk was put to (only their answer counts). */
    public void askedOf(@Nullable UUID who) {
        askedOf = who;
    }

    public @Nullable UUID askedOf() {
        return askedOf;
    }

    @Override
    protected void playDeath() {
        if (verdictForgive == null) GodstielCinematics.death(this);
        else GodstielCinematics.fate(this, verdictForgive);
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        GodstielSpoils.defeated(level, this, new ArrayList<>(challengers()));
    }

    private void tickKneeling(ServerLevel level) {
        getNavigation().stop();
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        scheduler().delay(5);
        kneelTicks++;
        if (kneelTicks == GodstielBalance.KNEEL_TICKS) GodstielSpoils.offer(level, this);
        // Nobody answered (everyone gone, or a fake player in a test that never does): the default.
        if (kneelTicks > GodstielBalance.KNEEL_TICKS + GodstielBalance.TALK_TIMEOUT + 200) verdict(false, null);
    }

    // --- per tick --------------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (level() instanceof ServerLevel level && !isRemoved()) {
            if (kneeling()) tickKneeling(level);
            else tickStruggle();
            tickBlacken(level);
        }
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel)) return;
        if (state() == EMERGING || emerged) return;
        emerged = true;
        fx(GodstielFxPayload.TITLE, phase(), 0, position(), 70);
    }

    public void sound(String id, float volume, float pitch) {
        level().playSound(null, blockPosition(), AllSounds.godstiel(id), SoundSource.HOSTILE, volume, pitch);
    }

    public void fx(byte kind, int arg, int arg2, Vec3 point, int duration) {
        if (!(level() instanceof ServerLevel level)) return;
        GodstielFxPayload payload = new GodstielFxPayload(getId(), kind, arg, arg2, point, duration);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(this) < 96 * 96) Send.toPlayer(p, payload);
    }

    public void tell(String key, ChatFormatting... style) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(style), false);
        }
    }

    // --- animation -------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                hover = RawAnimation.begin().thenLoop(pre + "hover"), struggle = RawAnimation.begin().thenLoop(pre + "struggle_idle"),
                kneel = RawAnimation.begin().thenLoop(pre + "kneel_idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (state() == DYING || state() == EMERGING) return PlayState.STOP;
            if (kneeling()) return state.setAndContinue(kneel);
            if (struggling()) return state.setAndContinue(struggle);
            if (lookPhase() <= 2) return state.setAndContinue(hover);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<LuciferEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : triggeredAnimations()) {
            boolean hold = name.equals("death") || name.equals("sink") || name.equals("kneel") || name.equals("vanish");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence -----------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("Rematch", rematch);
        tag.putInt("NextStruggle", nextStruggle);
        tag.putByte("GodstielFlags", (byte) (entityData.get(FLAGS) & (FORGIVEN | CONDEMNED)));
        tag.putInt("KneelTicks", kneelTicks);
        if (labOrigin != null) {
            tag.put("LabOrigin", NbtUtils.writeBlockPos(labOrigin));
            tag.putInt("LabQ", labQ);
        }
        if (opener != null) tag.putUUID("Opener", opener);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        emerged = tag.getBoolean("Emerged");
        rematch = tag.getBoolean("Rematch");
        nextStruggle = tag.getInt("NextStruggle");
        if (tag.contains("GodstielFlags")) entityData.set(FLAGS, tag.getByte("GodstielFlags"));
        kneelTicks = tag.contains("KneelTicks") ? tag.getInt("KneelTicks") : -1;
        if (kneelTicks >= 0) {
            // Reloaded on his knees: the question is asked again.
            setFlag(KNEELING, true);
            kneelTicks = Math.min(kneelTicks, GodstielBalance.KNEEL_TICKS - 1);
        }
        labOrigin = NbtUtils.readBlockPos(tag, "LabOrigin").orElse(null);
        labQ = tag.getInt("LabQ");
        opener = tag.hasUUID("Opener") ? tag.getUUID("Opener") : null;
    }
}
