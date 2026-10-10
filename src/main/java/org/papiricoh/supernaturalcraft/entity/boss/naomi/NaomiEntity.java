package org.papiricoh.supernaturalcraft.entity.boss.naomi;

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
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.ReprogrammingRoomLayout;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.arena.RoomGround;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
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
 * Naomi, who runs Heaven's reprogramming (v0.18): an optional mini-boss in the clinical wing of a hunter's own Heaven. Two phases,
 * half her true health each (30 000 alone):
 *
 * <pre>EMERGING (she steps out of the light, composed) → P1 the Clinic → P2 the Red Lights (the room's phase cells) → DYING</pre>
 *
 * <p>Her room ({@link RoomGround}) is a plot's own in Heaven ({@link NaomiSummoning#summonInRoom}); called by egg or command
 * anywhere else it is written round her through the arena first. Her eight attacks are {@link NaomiAttacks}: the reprogramming
 * chairs ({@link ReprogrammingChairEntity}, a struggle against the drill), her guards (while two stand she takes
 * {@link NaomiBalance#GUARD_WARD}), training tests ({@link TrainingTest}) and her console (she heals there until six blows land on
 * it). A conditioned hunter ({@code CONDITIONED}) strikes her, and every angel, for {@link NaomiBalance#CONDITIONED_FACTOR}.
 * Optional: she blocks nothing on the road to the Cage nor to the Author ({@code BossProgression.Boss.optional}).
 */
public class NaomiEntity extends LuciferEntity {

    public static final int MAX_PHASE = NaomiBalance.PHASES;

    /** Her hand drill is out (the renderer shows the {@code drill} bone). */
    private static final EntityDataAccessor<Boolean> DRILL = SynchedEntityData.defineId(NaomiEntity.class, EntityDataSerializers.BOOLEAN);
    /** She is at her console (the base controller loops {@code recalibrate}). */
    private static final EntityDataAccessor<Boolean> RECALIBRATING = SynchedEntityData.defineId(NaomiEntity.class, EntityDataSerializers.BOOLEAN);

    // the room
    private @Nullable RoomGround room;
    private @Nullable BlockPos roomOrigin;
    private boolean writeRoom, emerged;
    private @Nullable UUID owner;
    // the chairs, the guards, the test
    private final List<UUID> chairs = new ArrayList<>();
    private final List<UUID> guards = new ArrayList<>();
    private @Nullable TrainingTest test;
    private boolean angelStrapped;
    // reeling (a parry, a broken console) or stunned (an unexpected result)
    private long staggeredUntil;
    private float staggerVulnerability = 1f;
    // the console
    private int consoleHits;
    private float recalHealed;
    private final Map<UUID, Long> lastConsoleHit = new HashMap<>();
    /** Tests: hunters she sees beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public NaomiEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 700;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VANILLA_BASE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRILL, false);
        builder.define(RECALIBRATING, false);
    }

    /** Whether her hand drill is out (synced). */
    public boolean drillOut() {
        return entityData.get(DRILL);
    }

    public void setDrill(boolean out) {
        entityData.set(DRILL, out);
    }

    /** Whether she is at her console (synced). */
    public boolean recalibrating() {
        return entityData.get(RECALIBRATING);
    }

    // --- her numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return NaomiBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.NAOMI_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.NAOMI_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.NAOMI_DAMAGE_FACTOR.get().floatValue();
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
        return NaomiAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return NaomiBalance.attackGap(phase);
    }

    @Override
    protected int emergeTicks() {
        return NaomiBalance.EMERGE_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return NaomiBalance.TRANSITION_TICKS;
    }

    @Override
    protected int deathTicks() {
        return NaomiBalance.DEATH_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return NaomiAnimations.PREFIX;
    }

    @Override
    protected List<String> triggeredAnimations() {
        return NaomiAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.naomi";
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase >= MAX_PHASE ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase >= MAX_PHASE ? ChatFormatting.RED : ChatFormatting.WHITE);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.END_ROD;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.heaven("heaven.arrive");
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.heaven("naomi.console");
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.heaven("naomi.wipe");
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.heaven("naomi.ambient");
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.heaven("naomi.death");
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.heaven("naomi.console");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.heaven("naomi.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.heaven("naomi.death");
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.END_ROD, getRandomX(1.0), getY() + random.nextDouble() * 2.0, getRandomZ(1.0), 0, 0.03, 0);
        level().addParticle(ParticleTypes.CLOUD, getRandomX(1.2), getY() + 0.1, getRandomZ(1.2), 0, 0.01, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.4, getZ(), 2, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 120, 1.0, 1.2, 1.0, 0.25);
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 2, 0.1, 0.05, 0.1, 0.05);
    }

    /** She doesn't walk at her console, while she reels, or while her room is still being written. */
    @Override
    protected boolean walks() {
        return !recalibrating() && !isStaggered() && !writingRoom();
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        return room != null ? room.feet(ReprogrammingRoomLayout.NAOMI_SPOT) : super.tetherPoint(arena);
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Where her room is and whether it must be written round her (an egg or a command far from any Heaven). */
    public void setRoom(BlockPos origin, boolean write) {
        this.roomOrigin = origin.immutable();
        this.writeRoom = write;
        this.room = null;
    }

    /** The hunter whose Heaven this is (the plot's owner), if any. */
    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    /** Spawned by egg: her room round wherever she stands. */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        ArenaController arena = LuciferSummoning.openArena(level, blockPosition(), NaomiBalance.ARENA_RADIUS, ArenaTheme.REPROGRAMMING, false);
        if (arena != null && roomOrigin == null) setRoom(blockPosition(), true);
        return arena;
    }

    @Override
    protected void playEmergence() {
        NaomiCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        NaomiCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        NaomiCinematics.death(this);
    }

    /** The red lights: whatever was under way stops (a test fails, the chairs open), the room changes. */
    @Override
    protected void onTransitionStart(int to) {
        if (!(level() instanceof ServerLevel level)) return;
        endTest(level, false);
        releaseChairs();
        endRecalibration();
        setDrill(false);
        staggeredUntil = 0;
        angelStrapped = false;
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    /** Halfway through her change: the room's phase cells (the lights go red). */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        room(arena).phase(level, arena, phase);
        playSound(AllSounds.heaven("naomi.wipe"), 3f, 0.6f);
    }

    /** She kneels; near the end the light leaves her eyes and mouth. */
    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (elapsed == NaomiBalance.DEATH_LIGHT && level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 60, 0.1, 0.1, 0.1, 0.3);
        }
    }

    /** The fight was lost: nothing is left behind. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        releaseChairs();
        super.returnToCage(level, messageKey.replace(".lucifer.", ".naomi."));
    }

    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            endTest(level, false);
            releaseChairs();
            dismiss(level, guards);
            dismiss(level, chairs);
        }
        endRecalibration();
        setDrill(false);
        super.beginDying();
    }

    private void dismiss(ServerLevel level, List<UUID> ids) {
        for (UUID id : List.copyOf(ids)) {
            Entity e = level.getEntity(id);
            if (e != null) {
                level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 16, 0.3, 0.8, 0.3, 0.03);
                e.discard();
            }
            minions().remove(id);
        }
        ids.clear();
    }

    /** At the end of her fall: the victory, her spoils per hunter, and the hunters' standing in Heaven. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        NaomiCinematics.victory(this);
        NaomiSpoils.drop(level, challengers(), position().add(0, 1.2, 0), random);
        recordVictory();
    }

    /**
     * Her fall in a hunter's Heaven counts for the plot's owner: a victory in their standing and the lift to the office opens
     * ({@link HeavenPlots#onNaomiDefeated}). Called by egg or command (no owner), nothing changes in anyone's Heaven.
     */
    public void recordVictory() {
        if (owner == null) return;
        for (LivingEntity h : hunters()) {
            if (h instanceof ServerPlayer p && p.getUUID().equals(owner)) {
                HeavenPlots.onNaomiDefeated(p);
                return;
            }
        }
        HeavenPlots.onNaomiDefeated(owner);
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        boolean built = tickRoom(level, arena);
        byte s = state();
        if (s == EMERGING) return;
        if (!emerged) {
            emerged = true;
            NaomiCinematics.arrived(this);
        }
        if (s == DYING || s == TRANSITION || !built) return;
        if (chairs.isEmpty()) placeChairs(level);
        if (test != null && test.tick(this, level)) test = null;
        if (isStaggered()) {
            getNavigation().stop();
            if (scheduler().current() == null) scheduler().delay(5);
        }
    }

    // --- the room -----------------------------------------------------------------------------------

    private RoomGround room(ArenaController arena) {
        if (room == null) {
            if (roomOrigin == null) setRoom(arena.center(), true);
            room = RoomGround.at(roomOrigin);
        }
        return room;
    }

    /** The room, once pinned (null before her first tick in an arena). */
    public @Nullable RoomGround room() {
        return room;
    }

    /** The room, pinned or not yet (round where it will be). */
    private RoomGround roomOrNear() {
        if (room != null) return room;
        return RoomGround.at(roomOrigin != null ? roomOrigin : blockPosition());
    }

    /** Whether the room (or its change of phase) is still being written: the fight waits for it. */
    public boolean writingRoom() {
        return room != null && room.writing();
    }

    /** Writes the room; true once it stands (the fight may go on). */
    private boolean tickRoom(ServerLevel level, ArenaController arena) {
        RoomGround g = room(arena);
        if (!g.begun()) {
            if (writeRoom) g.build();
            else g.standing();
        }
        if (!g.writing()) return true;
        if (g.tick(level, arena)) {
            writeRoom = false;
            for (LivingEntity p : hunters()) RoomGround.unstick(level, p);
            RoomGround.unstick(level, this);
            return true;
        }
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING) {
            if (scheduler().current() == null) scheduler().delay(10);
            getNavigation().stop();
        }
        return false;
    }

    /** Test and preview hook: the room written now and the chairs placed. */
    public void buildRoomNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        RoomGround g = room(arena);
        if (!g.begun()) {
            if (writeRoom) g.build();
            else g.standing();
        }
        g.finish(level, arena);
        writeRoom = false;
        if (chairs.isEmpty()) placeChairs(level);
    }

    private Vec3 at(LayoutPoint p) {
        return roomOrNear().feet(p);
    }

    // --- the chairs ---------------------------------------------------------------------------------

    private void placeChairs(ServerLevel level) {
        for (LayoutPoint p : ReprogrammingRoomLayout.CHAIRS) {
            ReprogrammingChairEntity chair = AllEntities.REPROGRAMMING_CHAIR.get().create(level);
            if (chair == null) continue;
            Vec3 at = at(p);
            chair.moveTo(at.x, at.y, at.z, 0, 0);
            chair.setNoGravity(true);
            chair.serve(getUUID());
            level.addFreshEntity(chair);
            chairs.add(chair.getUUID());
            minions().add(chair.getUUID());
        }
    }

    /** Her chairs standing now. */
    public List<ReprogrammingChairEntity> chairs() {
        List<ReprogrammingChairEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : chairs) if (level.getEntity(id) instanceof ReprogrammingChairEntity c && !c.isRemoved()) out.add(c);
        return out;
    }

    /** The free chair nearest {@code victim}, if any. */
    public @Nullable ReprogrammingChairEntity freeChairNear(LivingEntity victim) {
        ReprogrammingChairEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ReprogrammingChairEntity c : chairs()) {
            if (c.strapped()) continue;
            double d = c.distanceToSqr(victim);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Who she would strap in now: an angel of rank II or more first, else the most isolated hunter not already in a chair. */
    public @Nullable Player strapCandidate() {
        List<Player> free = new ArrayList<>();
        for (LivingEntity h : hunters()) {
            if (h instanceof Player p && p.isAlive() && !(p.getVehicle() instanceof ReprogrammingChairEntity)) free.add(p);
        }
        if (free.isEmpty()) return null;
        for (Player p : free) {
            Allegiance a = Allegiances.get(p);
            if (a.isAngel() && a.rank() >= NaomiBalance.STRAP_ANGEL_RANK) return p;
        }
        double[][] xz = new double[free.size()][];
        for (int i = 0; i < free.size(); i++) xz[i] = new double[]{free.get(i).getX(), free.get(i).getZ()};
        return free.get(Math.max(0, NaomiBalance.mostIsolated(xz)));
    }

    /** Her line as the strap's warning lands on {@code victim}: an angel is told it is one of hers. */
    void announceStrap(Player victim) {
        Allegiance a = Allegiances.get(victim);
        boolean ours = a.isAngel() && a.rank() >= NaomiBalance.STRAP_ANGEL_RANK;
        victim.displayClientMessage(ours ? NaomiAttacks.oneOfOurs() : Component.translatable("message.supernaturalcraft.naomi.sit")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
    }

    /** Straps {@code victim} into {@code chair}. @return whether they are in */
    public boolean strapInto(ReprogrammingChairEntity chair, Player victim) {
        if (!chair.strap(victim, phase())) return false;
        Allegiance a = Allegiances.get(victim);
        if (a.isAngel() && a.rank() >= NaomiBalance.STRAP_ANGEL_RANK) angelStrapped = true;
        return true;
    }

    private void releaseChairs() {
        for (ReprogrammingChairEntity c : chairs()) if (c.strapped()) c.free(null, false);
    }

    // --- the guards ---------------------------------------------------------------------------------

    /** Where {@code count} guards step out (the alcoves, in order). */
    public List<Vec3> guardSpots(int count) {
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < count && i < ReprogrammingRoomLayout.GUARD_SPAWNS.size(); i++) out.add(at(ReprogrammingRoomLayout.GUARD_SPAWNS.get(i)));
        return out;
    }

    /** Her guards come out at {@code spots}. */
    public List<HeavenGuardEntity> callGuards(List<Vec3> spots) {
        List<HeavenGuardEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (Vec3 at : spots) {
            HeavenGuardEntity g = AllEntities.HEAVEN_GUARD.get().create(level);
            if (g == null) continue;
            g.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            g.serve(getUUID());
            level.addFreshEntity(g);
            guards.add(g.getUUID());
            minions().add(g.getUUID());
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 20, 0.3, 1.0, 0.3, 0.02);
            out.add(g);
        }
        playSound(AllSounds.heaven("naomi.guards"), 2.5f, 1.0f);
        for (LivingEntity h : hunters()) {
            if (h instanceof Player p) p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.guards")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return out;
    }

    /** Her guards standing now. */
    public List<HeavenGuardEntity> guards() {
        List<HeavenGuardEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : guards) if (level.getEntity(id) instanceof HeavenGuardEntity g && g.isAlive()) out.add(g);
        return out;
    }

    public int guardsStanding() {
        return guards().size();
    }

    void guardFell(HeavenGuardEntity guard) {
        guards.remove(guard.getUUID());
        minions().remove(guard.getUUID());
    }

    // --- the training test --------------------------------------------------------------------------

    /** The test floor's spots. */
    public List<Vec3> copySpots() {
        List<Vec3> out = new ArrayList<>();
        for (LayoutPoint p : ReprogrammingRoomLayout.COPY_SPOTS) out.add(at(p));
        return out;
    }

    /** Starts a training test for {@code subject} (nothing if one runs). */
    public @Nullable TrainingTest beginTest(LivingEntity subject) {
        if (test != null || !(level() instanceof ServerLevel level)) return null;
        test = TrainingTest.begin(this, level, subject, copySpots());
        fx(new HeavenFxPayload(getId(), HeavenFxPayload.TRAINING_TEST, 0, 0, position(), NaomiBalance.TEST_TICKS, ""));
        return test;
    }

    public @Nullable TrainingTest test() {
        return test;
    }

    public boolean testRunning() {
        return test != null && !test.over();
    }

    /** Test hook: works the test out now (as a tick would). */
    public void tickTest() {
        if (test != null && level() instanceof ServerLevel level && test.tick(this, level)) test = null;
    }

    private void endTest(ServerLevel level, boolean passed) {
        if (test != null) test.end(this, level, passed);
        test = null;
    }

    void kneelerTouched(TrainingCopyEntity copy, Player by) {
        if (test != null) test.touch(copy, by);
    }

    void copyFell(TrainingCopyEntity copy, @Nullable Player by) {
        minions().remove(copy.getUUID());
        if (test != null) test.fell(this, copy, by);
    }

    /** "Unexpected result.": the test was passed; she is stunned and takes more for a while. */
    public void unexpectedResult() {
        staggeredUntil = level().getGameTime() + NaomiBalance.PASS_STUN;
        staggerVulnerability = NaomiBalance.PASS_VULNERABILITY;
        scheduler().cancel();
        scheduler().delay(NaomiBalance.PASS_STUN);
        getNavigation().stop();
        triggerAnim("action", NaomiAnimations.STAGGER);
        for (LivingEntity h : hunters()) {
            if (h instanceof Player p) p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.unexpected")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);
        }
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.4, getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        }
    }

    // --- the console --------------------------------------------------------------------------------

    /** The console block. */
    public BlockPos consolePos() {
        return roomOrNear().at(ReprogrammingRoomLayout.CONSOLE);
    }

    /** Where she stands to work it (just south of it, facing it). */
    public Vec3 consoleStand() {
        return roomOrNear().feet(ReprogrammingRoomLayout.CONSOLE.offset(0, 0, 1));
    }

    /** Whether a recalibration is worth it: she has lost at least a tenth of her phase. */
    public boolean worthRecalibrating() {
        float top = phase() <= 1 ? getMaxHealth() : getMaxHealth() * threshold(phase() - 1);
        float bottom = getMaxHealth() * threshold(phase());
        return getHealth() < top - (top - bottom) * NaomiBalance.RECAL_WORTH;
    }

    /** She is at her console: the block is there (written through the arena if the room lacks it), the count starts. */
    public void beginRecalibration() {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 stand = consoleStand();
        if (position().distanceToSqr(stand) > 1.5 * 1.5) blinkTo(stand);
        BlockPos console = consolePos();
        ArenaController arena = arena();
        if (!level.getBlockState(console).is(AllBlocks.REPROGRAMMING_CONSOLE.get()) && arena != null) {
            arena.mutate(level, console, AllBlocks.REPROGRAMMING_CONSOLE.get().defaultBlockState(), 0);
        }
        float yaw = 180f;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        getNavigation().stop();
        consoleHits = 0;
        recalHealed = 0;
        entityData.set(RECALIBRATING, true);
        playSound(AllSounds.heaven("naomi.console"), 2f, 1.0f);
        for (LivingEntity h : hunters()) {
            if (h instanceof Player p) p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.recalibrating")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** One second at her console. @return true health healed */
    public float recalibrationSecond() {
        if (!recalibrating() || consoleBroken()) return 0;
        float heal = NaomiBalance.recalibrationSecond(trueMaxHealth(), recalHealed);
        float before = trueHealth();
        healTrue(heal);
        float healed = trueHealth() - before;
        recalHealed += heal;
        if (level() instanceof ServerLevel level && heal > 0) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }
        return healed;
    }

    /** A blow on her console by {@code by} at {@code pos}. @return whether it counted */
    public boolean consoleHit(BlockPos pos, @Nullable Entity by) {
        if (!recalibrating() || !pos.equals(consolePos()) || !(level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        if (by != null) {
            Long last = lastConsoleHit.get(by.getUUID());
            if (last != null && now - last < NaomiBalance.CONSOLE_HIT_COOLDOWN) return false;
            lastConsoleHit.put(by.getUUID(), now);
        }
        consoleHits++;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.2);
        playSound(AllSounds.heaven("naomi.console"), 1.2f, 1.4f + consoleHits * 0.08f);
        if (by instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.console_hit", consoleHits, NaomiBalance.CONSOLE_HITS)
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return true;
    }

    public int consoleHits() {
        return consoleHits;
    }

    public boolean consoleBroken() {
        return consoleHits >= NaomiBalance.CONSOLE_HITS;
    }

    /** The recalibration is over: the console was broken off, or it has given all it gives. */
    public boolean recalibrationOver() {
        return consoleBroken() || recalHealed >= NaomiBalance.RECAL_CAP * trueMaxHealth() - 1e-3f;
    }

    /** She leaves her console; if it was broken off her, she reels. */
    public void endRecalibration() {
        if (!recalibrating()) return;
        entityData.set(RECALIBRATING, false);
        if (consoleBroken() && state() != DYING) {
            stagger(null, "message.supernaturalcraft.naomi.console_broken");
            for (LivingEntity h : hunters()) {
                if (h instanceof Player p) p.displayClientMessage(Component.translatable("message.supernaturalcraft.naomi.console_broken")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    /** The Naomi whose console stands at {@code pos}, if any. */
    public static @Nullable NaomiEntity atConsole(ServerLevel level, BlockPos pos) {
        ArenaController arena = ArenaSavedData.get(level).at(Vec3.atCenterOf(pos));
        if (arena == null || arena.bossId() == null) return null;
        return level.getEntity(arena.bossId()) instanceof NaomiEntity n && n.isAlive() ? n : null;
    }

    // --- reeling ------------------------------------------------------------------------------------

    public boolean isStaggered() {
        return level().getGameTime() < staggeredUntil;
    }

    /** She reels: whatever she was doing stops, and she is open. */
    public void stagger(@Nullable ServerPlayer by, String messageKey) {
        staggeredUntil = level().getGameTime() + NaomiBalance.STAGGER_TICKS;
        staggerVulnerability = NaomiBalance.STAGGER_VULNERABILITY;
        scheduler().cancel();
        scheduler().delay(NaomiBalance.STAGGER_TICKS);
        getNavigation().stop();
        triggerAnim("action", NaomiAnimations.STAGGER);
        playSound(AllSounds.heaven("naomi.hurt"), 2.0f, 0.8f);
        if (by != null) by.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD), true);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.7, getZ(), 24, 0.4, 0.5, 0.4, 0.3);
        }
    }

    // --- healing and moving -------------------------------------------------------------------------

    /** Heals {@code amount} true health, never past the start of the phase she is in. */
    public void healTrue(float amount) {
        if (amount <= 0 || isInvulnerablePhase() || !isAlive()) return;
        float ceiling = phase() <= 1 ? getMaxHealth() : getMaxHealth() * threshold(phase() - 1);
        setHealth(Math.min(ceiling, getHealth() + amount / healthScale()));
        acceptHealth();
    }

    /** A flicker of white and she stands at {@code to}. */
    public void blinkTo(Vec3 to) {
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.05);
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 1, to.z, 20, 0.3, 0.8, 0.3, 0.05);
        playSound(AllSounds.heaven("heaven.arrive"), 1.2f, 1.5f);
    }

    // --- the hunters --------------------------------------------------------------------------------

    /** The hunters she sees: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of her hunters. */
    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** Never a hunter already strapped in, if another is free. */
    @Override
    public @Nullable LivingEntity attackTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : challengers()) {
            if (p.getVehicle() instanceof ReprogrammingChairEntity) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best != null ? best : super.attackTarget();
    }

    /** Sends one of the fight's moments to every hunter near. */
    public void fx(HeavenFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    /** Sends a moment to one hunter (fake players have no client). */
    public void fxTo(ServerPlayer p, HeavenFxPayload payload) {
        if (!(p instanceof FakePlayer) && p.connection != null) PacketDistributor.sendToPlayer(p, payload);
    }

    // --- damage -------------------------------------------------------------------------------------

    /**
     * Beyond Lucifer's own: two guards standing ward her (×0.6); reeling ×1.3 and stunned by an unexpected result ×1.4; and a
     * conditioned hunter's blows land at ×0.6.
     */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        v *= NaomiBalance.ward(guardsStanding());
        if (isStaggered()) v *= staggerVulnerability;
        if (source.getEntity() instanceof LivingEntity by && by.hasEffect(AllMobEffects.CONDITIONED)) v *= NaomiBalance.CONDITIONED_FACTOR;
        return v;
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack she uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; an angel of rank II or more is strapped in first; she closes the gap when the hunter runs. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (!angelStrapped) {
            Player p = strapCandidate();
            if (p != null) {
                Allegiance a = Allegiances.get(p);
                if (a.isAngel() && a.rank() >= NaomiBalance.STRAP_ANGEL_RANK && freeChairNear(p) != null) {
                    angelStrapped = true;
                    return NaomiAttacks.StrapIn::new;
                }
            }
        }
        if (!recalibrating() && (distanceToSqr(target) > 14 * 14 || noSightTicks > 60)) {
            noSightTicks = 0;
            return NaomiAttacks.Approach::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only her own clips: Lucifer's names that she lacks ({@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !NaomiAnimations.TRIGGERED.contains(anim)) return;
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
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + NaomiAnimations.IDLE);
        RawAnimation walk = RawAnimation.begin().thenLoop(pre + NaomiAnimations.WALK);
        RawAnimation typing = RawAnimation.begin().thenLoop(pre + NaomiAnimations.RECALIBRATE);
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == DYING) return PlayState.STOP;
            if (recalibrating()) return state.setAndContinue(typing);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<NaomiEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : NaomiAnimations.TRIGGERED) {
            action.triggerableAnim(name, NaomiAnimations.HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(pre + name)
                    : RawAnimation.begin().thenPlay(pre + name));
        }
        controllers.add(action);
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (roomOrigin != null) tag.put("RoomOrigin", NbtUtils.writeBlockPos(roomOrigin));
        tag.putBoolean("WriteRoom", writeRoom);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("AngelStrapped", angelStrapped);
        tag.put("Chairs", uuids(chairs));
        tag.put("Guards", uuids(guards));
        if (test != null && !test.over()) tag.put("Test", test.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        roomOrigin = NbtUtils.readBlockPos(tag, "RoomOrigin").orElse(null);
        writeRoom = tag.getBoolean("WriteRoom");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        emerged = tag.getBoolean("Emerged");
        angelStrapped = tag.getBoolean("AngelStrapped");
        read(tag.getList("Chairs", Tag.TAG_INT_ARRAY), chairs);
        read(tag.getList("Guards", Tag.TAG_INT_ARRAY), guards);
        minions().addAll(chairs);
        minions().addAll(guards);
        test = tag.contains("Test") ? TrainingTest.load(tag.getCompound("Test")) : null;
        if (test != null) {
            minions().addAll(test.kneelers());
            minions().addAll(test.hostiles());
        }
        // The room is pinned again on the next tick (already standing, unless it was still being written).
        room = null;
        entityData.set(RECALIBRATING, false);
        entityData.set(DRILL, false);
    }

    private static ListTag uuids(List<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) list.add(NbtUtils.createUUID(id));
        return list;
    }

    private static void read(ListTag list, List<UUID> into) {
        into.clear();
        for (Tag t : list) into.add(NbtUtils.loadUUID(t));
    }
}
