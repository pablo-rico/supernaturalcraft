package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseGround;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;
import org.papiricoh.supernaturalcraft.network.RaphaelFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weather.StormLock;
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
 * Raphael, the archangel of the storm (v0.16): an optional superboss after the three Horsemen, called down by a rite in a
 * thunderstorm into an abandoned house. Three phases, a third of his true health each (26 000 alone):
 *
 * <pre>EMERGING (a bolt, and he kneels in it, wings spread) → P1 the Storm → P2 the Healer (his garrison's threads of grace)
 *   → P3 the Wrath of Heaven (the roof torn off, his wings' shadow in every flash, his veins alight) → DYING</pre>
 *
 * <p>The house ({@link HouseGround}) is written round the rite as he arrives; the storm is held over it ({@link StormLock}) until
 * the arena closes. Four rings of holy oil ({@link OilRings}) lie on its floor, poured again at every phase: fire lights a ring, and
 * a lit ring round him holds him ({@link RaphaelBalance#TRAPPED_VULNERABILITY}, no attacks, no healing) until he breaks out. In the
 * second phase his garrison's threads heal him, a share of his true health a second each, until a hunter stands in a thread or
 * its angel falls. Optional: he blocks nothing on the road to the Cage nor to the Author ({@code BossProgression.Boss.optional}).
 */
public class RaphaelEntity extends LuciferEntity {

    public static final int MAX_PHASE = RaphaelBalance.PHASES;

    /** His wings are shown (his arrival, phase 3, his death): the renderer shows the {@code wings} bone. */
    private static final EntityDataAccessor<Boolean> WINGS = SynchedEntityData.defineId(RaphaelEntity.class, EntityDataSerializers.BOOLEAN);
    /** His eyes and veins glow (phase 3 and his death). */
    private static final EntityDataAccessor<Boolean> VEINS = SynchedEntityData.defineId(RaphaelEntity.class, EntityDataSerializers.BOOLEAN);
    /** A lit ring of holy oil holds him (the {@code trapped} loop). */
    private static final EntityDataAccessor<Boolean> TRAPPED = SynchedEntityData.defineId(RaphaelEntity.class, EntityDataSerializers.BOOLEAN);

    private @Nullable HouseGround ground;
    private final OilRings rings = new OilRings();
    private boolean ringsPending = true, emerged;
    private long trappedUntil, trapImmuneUntil, staggeredUntil;
    private int trapRing = -1;
    // the garrison
    private final List<UUID> garrison = new ArrayList<>();
    private final Map<UUID, Integer> posts = new HashMap<>();
    private final List<Integer> fallen = new ArrayList<>();
    private final Map<UUID, Boolean> threads = new HashMap<>();
    private boolean garrisonCalled;
    private int raisesLeft = RaphaelBalance.RAISES, healClock;
    /** Tests: hunters the storm and the threads see beyond the challengers (fake players are not in the level's list). */
    private final List<LivingEntity> tracked = new ArrayList<>();
    /** Tests: an attack to use next, whatever the pool says. */
    private @Nullable Supplier<BossAttack<LuciferEntity>> queued;

    public RaphaelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 900;
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
        builder.define(WINGS, false);
        builder.define(VEINS, false);
        builder.define(TRAPPED, false);
    }

    /** Whether his wings are shown (synced). */
    public boolean wingsShown() {
        return entityData.get(WINGS);
    }

    /** Whether his eyes and veins glow (synced). */
    public boolean veinsLit() {
        return entityData.get(VEINS);
    }

    /** Whether a lit ring of holy oil holds him (synced). */
    public boolean trapped() {
        return entityData.get(TRAPPED);
    }

    // --- his numbers --------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return RaphaelBalance.threshold(phase);
    }

    @Override
    protected double healthPerExtraPlayer() {
        return SNConfig.RAPHAEL_HEALTH_PER_PLAYER.get();
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.RAPHAEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float damageFactor() {
        return SNConfig.RAPHAEL_DAMAGE_FACTOR.get().floatValue();
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
        return RaphaelAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return RaphaelBalance.attackGap(phase);
    }

    @Override
    protected String animationPrefix() {
        return "animation.raphael.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return RaphaelAssets.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.raphael";
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return BossEvent.BossBarColor.BLUE;
    }

    @Override
    protected Component bossBarName(int phase) {
        return Component.translatable(bossBarKey(phase)).withStyle(phase == maxPhase() ? ChatFormatting.WHITE : ChatFormatting.AQUA);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.ELECTRIC_SPARK;
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.RAPHAEL_ARRIVE.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.RAPHAEL_THUNDER.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.RAPHAEL_WINGS.get();
    }

    @Override
    protected SoundEvent ambientBossSound() {
        return AllSounds.RAPHAEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent dyingSound() {
        return AllSounds.RAPHAEL_DEATH.get();
    }

    @Override
    protected SoundEvent deflectSound() {
        return AllSounds.RAPHAEL_WINGS.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.RAPHAEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.RAPHAEL_DEATH.get();
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.2), getY() + random.nextDouble() * 2.2, getRandomZ(1.2), 0, 0.05, 0);
        level().addParticle(ParticleTypes.CLOUD, getRandomX(1.5), getY() + 0.1, getRandomZ(1.5), 0, 0.02, 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 160, 1.5, 1.8, 1.5, 0.3);
            return;
        }
        double a = random.nextDouble() * Math.PI * 2;
        level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * 0.5, getY() + 1.2, getZ() + Math.sin(a) * 0.5,
                0, Math.cos(a) * 0.5, 0.3 + random.nextDouble() * 0.5, Math.sin(a) * 0.5, 1.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.4, getZ(), 4, 0.4, 0.7, 0.4, 0.05);
    }

    /** He doesn't walk while a ring holds him, while he reels, or while his house is still being written. */
    @Override
    protected boolean walks() {
        return !trapped() && !isStaggered() && !writingHouse();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    /** Spawned by egg or command: the house and the storm round wherever he stands. */
    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        ArenaController arena = LuciferSummoning.openArena(level, blockPosition(), SNConfig.RAPHAEL_ARENA_RADIUS.get(), ArenaTheme.STORM);
        if (arena != null) StormLock.force(level, arena);
        return arena;
    }

    @Override
    protected void playEmergence() {
        RaphaelCinematics.intro(this);
        entityData.set(WINGS, true);
        if (level() instanceof ServerLevel) bolt(position());
    }

    @Override
    protected void playTransition(int to) {
        RaphaelCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        RaphaelCinematics.death(this);
    }

    /** A phase changes: whatever held him lets go; the garrison ascends before the last; the wings open for the wrath. */
    @Override
    protected void onTransitionStart(int to) {
        // Not Lucifer's (no shield, no flight).
        if (!(level() instanceof ServerLevel level)) return;
        if (trapped()) release(level, false);
        staggeredUntil = 0;
        if (to == 2) {
            garrisonCalled = false;
            raisesLeft = RaphaelBalance.RAISES;
            fallen.clear();
        }
        if (to >= MAX_PHASE) {
            dismissGarrison(level);
            entityData.set(WINGS, true);
            entityData.set(VEINS, true);
            triggerAnim("action", "wings_reveal");
        }
    }

    /**
     * Development preview and tests: straight to a phase's look, with no transition (the wrath's wings and veins, and, once his
     * house is pinned, its roof torn off).
     */
    @Override
    public void forceLook(int phase) {
        super.forceLook(phase);
        boolean wrath = phase >= MAX_PHASE;
        entityData.set(WINGS, wrath);
        entityData.set(VEINS, wrath);
        ArenaController arena = arena();
        if (wrath && arena != null) ground(arena).tearOffRoof();
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        getNavigation().stop();
    }

    @Override
    protected int deathTicks() {
        return RaphaelBalance.DEATH_TICKS;
    }

    /** He kneels; near the end he bursts into light and his wings burn into the floor. */
    @Override
    protected void tickDyingMotion(int elapsed) {
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (elapsed == RaphaelBalance.DEATH_BURST && level() instanceof ServerLevel level) {
            ArenaController arena = arena();
            flash(position(), 1);
            playSound(AllSounds.RAPHAEL_THUNDER.get(), 5f, 0.6f);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 4, 0.5, 0.5, 0.5, 0);
            if (arena != null) burnWings(level, arena);
        }
    }

    /** The shape of his wings burnt into the floor round him (the arena gives the floor back). */
    public int burnWings(ServerLevel level, ArenaController arena) {
        BlockPos feet = blockPosition();
        int n = 0;
        for (int side : new int[]{-1, 1}) {
            for (int i = 1; i <= 7; i++) {
                int width = (int) Math.round(2.6 * Math.sin(Math.PI * i / 8.0));
                for (int dz = -width; dz <= width / 2 + 1; dz++) {
                    if ((i + dz) % 4 == 3) continue;
                    BlockPos pos = feet.offset(side * i, -1, dz + i / 3);
                    if (!level.getBlockState(pos).isSolid()) continue;
                    if (arena.mutate(level, pos, (i + dz) % 3 == 0 ? Blocks.COAL_BLOCK.defaultBlockState()
                            : Blocks.BLACK_CONCRETE.defaultBlockState(), 0)) n++;
                }
            }
        }
        return n;
    }

    /** Halfway through a change of phase: the rings are poured again; for the last, the roof is torn off. */
    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        HouseGround g = ground(arena);
        if (phase >= MAX_PHASE) {
            g.tearOffRoof();
            level.sendParticles(ParticleTypes.CLOUD, arena.centerVec().x, arena.centerVec().y + HouseLayout.CEILING + 1,
                    arena.centerVec().z, 120, HouseLayout.HALF_X, 1.5, HouseLayout.HALF_Z, 0.2);
            playSound(AllSounds.RAPHAEL_THUNDER.get(), 5f, 0.5f);
        }
        ringsPending = true;
    }

    /** The fight was lost: nothing is left but the storm passing. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        cutAllThreads();
        super.returnToCage(level, messageKey.replace(".lucifer.", ".raphael."));
    }

    @Override
    protected void beginDying() {
        if (level() instanceof ServerLevel level) {
            if (trapped()) release(level, false);
            dismissGarrison(level);
        }
        entityData.set(WINGS, true);
        entityData.set(VEINS, true);
        super.beginDying();
    }

    /** At the end of his death: the victory, and his spoils, per hunter. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        RaphaelCinematics.victory(this);
        RaphaelSpoils.drop(level, challengers(), position().add(0, 1.2, 0), random);
    }

    // --- ticking ------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        long now = level.getGameTime();
        boolean built = tickGround(level, arena);
        byte s = state();
        if (s == EMERGING) return;
        if (!emerged) {
            emerged = true;
            if (phase() < MAX_PHASE) entityData.set(WINGS, false);
            fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.TITLE, phase(), 0, position(), 70));
        }
        if (built && ringsPending && s != DYING) {
            ringsPending = false;
            rings.layAll(level, arena, ground);
        }
        if (s == DYING || s == TRANSITION) {
            if (!threads.isEmpty()) cutAllThreads();
            return;
        }
        if (!built) return;
        rings.tick(level);
        if (now % 2 == 0) rings.checkSparks(level, arena, ground);
        tickTrap(level, arena, now);
        if (isStaggered() || trapped()) {
            getNavigation().stop();
            if (scheduler().current() == null) scheduler().delay(5);
        }
        if (phase() == 2) tickThreads(level);
        else if (!threads.isEmpty()) cutAllThreads();
    }

    // --- the house ----------------------------------------------------------------------------------

    private HouseGround ground(ArenaController arena) {
        if (ground == null) ground = HouseGround.pin(arena);
        return ground;
    }

    /** The house, once pinned (null before his first tick in an arena). */
    public @Nullable HouseGround ground() {
        return ground;
    }

    /** Whether the house (or the tearing of its roof) is still being written: the fight waits for it. */
    public boolean writingHouse() {
        return ground != null && ground.writing();
    }

    /** Writes the house; true once it stands (the fight may go on). */
    private boolean tickGround(ServerLevel level, ArenaController arena) {
        HouseGround g = ground(arena);
        if (!g.begun()) {
            g.build();
            if (phase() >= MAX_PHASE) g.tearOffRoof();
        }
        if (!g.writing()) return true;
        if (g.tick(level, arena)) {
            // The house is down: nobody is left standing inside a wall of it.
            for (LivingEntity p : hunters()) HouseGround.unstick(level, p);
            HouseGround.unstick(level, this);
            return true;
        }
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING) {
            if (scheduler().current() == null) scheduler().delay(10);
            getNavigation().stop();
        }
        return false;
    }

    /** Test and preview hook: the house written now (and the rings poured). */
    public void buildHouseNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        HouseGround g = ground(arena);
        if (!g.begun()) g.build();
        g.finish(level, arena);
        if (ringsPending) {
            ringsPending = false;
            rings.layAll(level, arena, g);
        }
    }

    // --- the holy oil -------------------------------------------------------------------------------

    public OilRings rings() {
        return rings;
    }

    /** The Raphael whose house holds {@code pos}, if any. */
    public static @Nullable RaphaelEntity holding(ServerLevel level, BlockPos pos) {
        ArenaController arena = ArenaSavedData.get(level).at(Vec3.atCenterOf(pos));
        if (arena == null || arena.bossId() == null) return null;
        return level.getEntity(arena.bossId()) instanceof RaphaelEntity r && r.isAlive() ? r : null;
    }

    /** Fire touches the oil at {@code pos}: its ring is lit. @return whether it caught */
    public boolean igniteAt(ServerLevel level, BlockPos pos) {
        ArenaController arena = arena();
        if (ground == null || arena == null) return false;
        return rings.ignite(level, arena, ground, OilRings.ringAt(ground, pos));
    }

    /** Test hook: lights ring {@code ring}. */
    public boolean ignite(int ring) {
        ArenaController arena = arena();
        return level() instanceof ServerLevel level && ground != null && arena != null && rings.ignite(level, arena, ground, ring);
    }

    private void tickTrap(ServerLevel level, ArenaController arena, long now) {
        if (trapped()) {
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            if (now >= trappedUntil || (now % 10 == 0 && !HolyOilFireBlock.enclosed(level, position()))) release(level, true);
            return;
        }
        if (now < trapImmuneUntil || !rings.anyBurning() || now % 2 != 0) return;
        if (HolyOilFireBlock.enclosed(level, position())) trap(level);
    }

    /** A lit ring round him holds him: no attacks, no healing, and he takes more. */
    public void trap(ServerLevel level) {
        long now = level.getGameTime();
        int ticks = SNConfig.RAPHAEL_TRAP_TICKS.get();
        trappedUntil = now + ticks;
        trapRing = ground != null ? OilRings.ringAt(ground, blockPosition()) : -1;
        entityData.set(TRAPPED, true);
        scheduler().cancel();
        scheduler().delay(ticks);
        getNavigation().stop();
        cutAllThreads();
        playSound(AllSounds.RAPHAEL_TRAPPED.get(), 3f, 1.0f);
        Vec3 at = trapRing >= 0 && ground != null ? OilRings.centre(ground, trapRing) : position();
        fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.TRAP, getId(), 0, at, ticks));
        for (LivingEntity h : hunters()) {
            if (h instanceof ServerPlayer p) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.raphael.trapped").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    /** He breaks out: the ring goes out with a clap of thunder ({@code burst}), and none holds him again for a while. */
    public void release(ServerLevel level, boolean burst) {
        entityData.set(TRAPPED, false);
        trapImmuneUntil = level.getGameTime() + RaphaelBalance.TRAP_IMMUNITY;
        ArenaController arena = arena();
        if (arena != null && ground != null) {
            int ring = trapRing >= 0 ? trapRing : OilRings.ringAt(ground, blockPosition());
            if (ring >= 0 && rings.state(ring) == OilRings.State.BURNING) rings.extinguish(level, arena, ground, ring);
        }
        trapRing = -1;
        fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.TRAP, getId(), 0, position(), 0));
        if (burst) {
            for (int k = 0; k < 4; k++) RaphaelAttacks.clap(this, position(), k * 90f, 4, 0);
            scheduler().delay(10);
        }
    }

    public boolean isStaggered() {
        return level().getGameTime() < staggeredUntil;
    }

    /** He reels: whatever he was doing stops, and he is open. */
    public void stagger(@Nullable ServerPlayer by, String messageKey) {
        staggeredUntil = level().getGameTime() + RaphaelBalance.STAGGER_TICKS;
        scheduler().cancel();
        scheduler().delay(RaphaelBalance.STAGGER_TICKS);
        getNavigation().stop();
        triggerAnim("action", "stagger");
        playSound(AllSounds.RAPHAEL_HURT.get(), 2.0f, 0.7f);
        if (by != null) by.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD), true);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.7, getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        }
    }

    // --- the garrison (phase 2) ---------------------------------------------------------------------

    /** Calls his garrison: an angel at each post, each holding a thread of grace to him. */
    public List<GarrisonAngelEntity> callGarrison(ServerLevel level) {
        garrisonCalled = true;
        List<GarrisonAngelEntity> out = new ArrayList<>();
        for (int i = 0; i < RaphaelBalance.GARRISON && i < HouseLayout.POSTS.size(); i++) {
            GarrisonAngelEntity a = spawnAngel(level, i);
            if (a != null) out.add(a);
        }
        playSound(AllSounds.RAPHAEL_HEAL.get(), 3f, 1.0f);
        return out;
    }

    private @Nullable GarrisonAngelEntity spawnAngel(ServerLevel level, int post) {
        GarrisonAngelEntity a = AllEntities.GARRISON_ANGEL.get().create(level);
        if (a == null) return null;
        Vec3 at;
        if (ground != null) {
            at = Vec3.atBottomCenterOf(ground.onFloor(HouseLayout.POSTS.get(post)));
        } else {
            double ang = post * Math.PI / 2;
            at = LuciferAttacks.floorAt(this, position().add(Math.cos(ang) * 5, 0, Math.sin(ang) * 5));
        }
        a.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
        a.finalizeSpawn(level, level.getCurrentDifficultyAt(a.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        a.serve(getUUID(), post);
        level.addFreshEntity(a);
        garrison.add(a.getUUID());
        posts.put(a.getUUID(), post);
        minions().add(a.getUUID());
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 2, at.z, 24, 0.2, 2, 0.2, 0.02);
        return a;
    }

    /** The angels of his garrison standing now. */
    public List<GarrisonAngelEntity> garrison() {
        List<GarrisonAngelEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel level)) return out;
        for (UUID id : garrison) if (level.getEntity(id) instanceof GarrisonAngelEntity a && a.isAlive()) out.add(a);
        return out;
    }

    public boolean garrisonCalled() {
        return garrisonCalled;
    }

    /** One of his garrison fell: its thread is cut, its post remembered (laying on hands may raise it). */
    void angelFell(GarrisonAngelEntity angel) {
        Integer post = posts.remove(angel.getUUID());
        garrison.remove(angel.getUUID());
        minions().remove(angel.getUUID());
        if (Boolean.TRUE.equals(threads.remove(angel.getUUID()))) {
            fx(new RaphaelFxPayload(angel.getId(), RaphaelFxPayload.TETHER, getId(), 0, angel.position(), 0));
        }
        if (post != null && phase() == 2) fallen.add(post);
    }

    /** Whether laying on hands has someone to raise. */
    public boolean canRaise() {
        return phase() == 2 && raisesLeft > 0 && !fallen.isEmpty();
    }

    /** Laying on hands: the first fallen angel gets up at its post. */
    public @Nullable GarrisonAngelEntity raiseFallen(ServerLevel level) {
        if (!canRaise()) return null;
        raisesLeft--;
        GarrisonAngelEntity a = spawnAngel(level, fallen.removeFirst());
        playSound(AllSounds.RAPHAEL_HEAL.get(), 2.5f, 1.2f);
        if (a != null) level.sendParticles(AllParticles.GRACE.get(), a.getX(), a.getY() + 1, a.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        return a;
    }

    /** The garrison ascends (the last phase, his death): gone in light. */
    private void dismissGarrison(ServerLevel level) {
        cutAllThreads();
        for (UUID id : List.copyOf(garrison)) {
            Entity e = level.getEntity(id);
            if (e != null) {
                level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 1.0, 0.3, 0.05);
                e.discard();
            }
            minions().remove(id);
        }
        garrison.clear();
        posts.clear();
        fallen.clear();
    }

    /** Whether a hunter stands in {@code angel}'s thread now. */
    public boolean threadBlocked(GarrisonAngelEntity angel) {
        for (LivingEntity h : hunters()) {
            if (!h.isAlive()) continue;
            double midY = (angel.getY() + getY()) / 2;
            if (Math.abs(h.getY() - midY) > 2.5) continue;
            if (RaphaelBalance.inBeam(h.getX(), h.getZ(), angel.getX(), angel.getZ(), getX(), getZ())) return true;
        }
        return false;
    }

    /** Whether {@code angel}'s thread holds now: it is close enough, he is not held, and nobody stands in it. */
    public boolean threadHolds(GarrisonAngelEntity angel) {
        return angel.isAlive() && !trapped() && angel.distanceTo(this) <= RaphaelBalance.TETHER_RANGE && !threadBlocked(angel);
    }

    /** Unbroken threads now. */
    public int activeThreads() {
        int n = 0;
        for (Boolean on : threads.values()) if (on) n++;
        return n;
    }

    private void tickThreads(ServerLevel level) {
        if (!garrisonCalled) return;
        boolean refresh = tickCount % RaphaelBalance.TETHER_REFRESH == 0;
        for (GarrisonAngelEntity a : garrison()) {
            boolean on = threadHolds(a);
            Boolean was = threads.put(a.getUUID(), on);
            if (was == null || was != on || (on && refresh)) {
                fx(new RaphaelFxPayload(a.getId(), RaphaelFxPayload.TETHER, getId(), 0, a.position(), on ? RaphaelBalance.TETHER_SHOW : 0));
            }
        }
        threads.keySet().removeIf(id -> !(level.getEntity(id) instanceof GarrisonAngelEntity a) || !a.isAlive());
        if (++healClock >= 20) {
            healClock = 0;
            mendByThreads();
        }
    }

    /** One second of the threads' healing (none while a ring holds him). @return true health healed */
    public float mendByThreads() {
        int n = activeThreads();
        if (n == 0 || trapped()) return 0;
        float heal = RaphaelBalance.tetherHeal(n, SNConfig.RAPHAEL_TETHER_HEAL.get(), trueMaxHealth());
        float before = trueHealth();
        healTrue(heal);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1.4, getZ(), 3 * n, 0.4, 0.6, 0.4, 0.02);
        }
        return trueHealth() - before;
    }

    /** Test hook: works the threads out now (as a tick would), without healing. */
    public void refreshThreads() {
        if (level() instanceof ServerLevel level) {
            int clock = healClock;
            healClock = -1000;
            tickThreads(level);
            healClock = clock;
        }
    }

    private void cutAllThreads() {
        if (threads.isEmpty()) return;
        if (level() instanceof ServerLevel level) {
            for (Map.Entry<UUID, Boolean> t : threads.entrySet()) {
                if (t.getValue() && level.getEntity(t.getKey()) instanceof GarrisonAngelEntity a) {
                    fx(new RaphaelFxPayload(a.getId(), RaphaelFxPayload.TETHER, getId(), 0, a.position(), 0));
                }
            }
        }
        threads.clear();
    }

    /** Heals {@code amount} true health, never past the start of the phase he is in, never while a ring holds him. */
    public void healTrue(float amount) {
        if (trapped() || amount <= 0) return;
        float ceiling = phase() <= 1 ? getMaxHealth() : getMaxHealth() * threshold(phase() - 1);
        setHealth(Math.min(ceiling, getHealth() + amount / healthScale()));
        acceptHealth();
    }

    // --- the storm ----------------------------------------------------------------------------------

    /** A bolt of his at {@code at}: only light (it sets nothing alight; his attacks deal their own blow), and the flash. */
    public void bolt(Vec3 at) {
        if (!(level() instanceof ServerLevel level)) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        flash(at);
    }

    /** A flash at {@code at}; in the wrath (or while his wings are out) it casts their shadow. */
    public void flash(Vec3 at) {
        flash(at, phase() >= MAX_PHASE || wingsShown() ? 1 : 0);
    }

    private void flash(Vec3 at, int wings) {
        fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.FLASH, wings, 0, at, 6));
    }

    /** A flinch of wings and he stands at {@code to}. */
    public void blinkTo(Vec3 to) {
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 20, 0.4, 0.8, 0.4, 0.05);
        fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.FLASH, 1, 0, position(), 3));
        triggerAnim("action", "blink");
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y + 1, to.z, 20, 0.4, 0.8, 0.4, 0.1);
        playSound(AllSounds.RAPHAEL_WINGS.get(), 2f, 1.1f);
    }

    /** The snap's warning: the burst and its safe band for the client. */
    public void snapFx(Vec3 centre, int ticks) {
        fx(new RaphaelFxPayload(getId(), RaphaelFxPayload.SNAP, RaphaelBalance.SNAP_RADIUS, RaphaelBalance.SNAP_SAFE_WIDTH, centre, ticks));
    }

    /** The hunters his storm and his threads see: the challengers, and anyone tracked (tests). */
    public List<LivingEntity> hunters() {
        List<LivingEntity> out = new ArrayList<>(challengers());
        tracked.removeIf(e -> !e.isAlive() || e.isRemoved());
        for (LivingEntity e : tracked) if (!out.contains(e)) out.add(e);
        return out;
    }

    /** Test hook: {@code e} counts as one of his hunters. */
    public void track(LivingEntity e) {
        if (!tracked.contains(e)) tracked.add(e);
    }

    /** Sends one of the storm's moments to every hunter near. */
    public void fx(RaphaelFxPayload payload) {
        if (!(level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96 * 96) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    // --- damage -------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        float before = getHealth();
        boolean hurt = super.hurt(source, amount);
        float taken = (before - getHealth()) * healthScale();
        if (taken > 0 && scheduler().current() instanceof RaphaelAttacks.Smite smite
                && scheduler().stage() == AttackScheduler.Stage.WINDUP && source.getEntity() instanceof LivingEntity by) {
            smite.wound(this, taken, by);
        }
        return hurt;
    }

    /** Beyond Lucifer's own: held in the holy fire he takes ×1.4; reeling, ×1.3. */
    @Override
    protected float vulnerability(DamageSource source) {
        float v = super.vulnerability(source);
        if (trapped()) v *= RaphaelBalance.TRAPPED_VULNERABILITY;
        if (isStaggered()) v *= RaphaelBalance.STAGGER_VULNERABILITY;
        return v;
    }

    // --- attacks ------------------------------------------------------------------------------------

    /** Tests and previews: the next attack he uses, whatever the pool says. */
    public void queue(Supplier<BossAttack<LuciferEntity>> attack) {
        queued = attack;
    }

    /** A queued attack first; the garrison as the healer's phase opens; a blink when the hunter runs or hides. */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (queued != null) {
            Supplier<BossAttack<LuciferEntity>> q = queued;
            queued = null;
            return q;
        }
        if (phase() == 2 && !garrisonCalled) return RaphaelAttacks.CallGarrison::new;
        if (distanceToSqr(target) > 16 * 16 || noSightTicks > 60) {
            noSightTicks = 0;
            return RaphaelAttacks.Blink::new;
        }
        return null;
    }

    // --- GeckoLib -----------------------------------------------------------------------------------

    /** Only his own clips: Lucifer's names that he lacks ({@code transform_<n>}, …) are dropped. */
    @Override
    public void triggerAnim(@Nullable String controller, String anim) {
        if ("action".equals(controller) && !RaphaelAssets.TRIGGERED.contains(anim)) return;
        if (level().isClientSide) {
            var manager = getAnimatableInstanceCache().getManagerForId(getId());
            if (controller != null) manager.tryTriggerAnimation(controller, anim);
            else manager.tryTriggerAnimation(anim);
        } else {
            GeckoLibServices.NETWORK.triggerEntityAnim(this, false, controller, anim);
        }
    }

    /** Clips that hold their last frame. */
    static final List<String> HOLDS = List.of("emerge", "death", "wings_reveal");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = animationPrefix();
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        RawAnimation held = RawAnimation.begin().thenLoop(pre + "trapped");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == DYING) return PlayState.STOP;
            if (trapped()) return state.setAndContinue(held);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<RaphaelEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : RaphaelAssets.TRIGGERED) {
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
        tag.putBoolean("Wings", wingsShown());
        tag.putBoolean("Veins", veinsLit());
        tag.putBoolean("Emerged", emerged);
        tag.putBoolean("GarrisonCalled", garrisonCalled);
        tag.putInt("RaisesLeft", raisesLeft);
        ListTag list = new ListTag();
        for (UUID id : garrison) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", id);
            t.putInt("Post", posts.getOrDefault(id, 0));
            list.add(t);
        }
        tag.put("Garrison", list);
        tag.put("Fallen", new IntArrayTag(fallen.stream().mapToInt(Integer::intValue).toArray()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(WINGS, tag.getBoolean("Wings"));
        entityData.set(VEINS, tag.getBoolean("Veins"));
        emerged = tag.getBoolean("Emerged");
        garrisonCalled = tag.getBoolean("GarrisonCalled");
        raisesLeft = tag.contains("RaisesLeft") ? tag.getInt("RaisesLeft") : RaphaelBalance.RAISES;
        garrison.clear();
        posts.clear();
        for (Tag t : tag.getList("Garrison", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            if (!c.hasUUID("Id")) continue;
            garrison.add(c.getUUID("Id"));
            posts.put(c.getUUID("Id"), c.getInt("Post"));
            minions().add(c.getUUID("Id"));
        }
        fallen.clear();
        for (int p : tag.getIntArray("Fallen")) fallen.add(p);
        // The house is pinned again and the rings poured afresh on the next tick.
        ground = null;
        ringsPending = true;
    }
}
