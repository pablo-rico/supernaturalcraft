package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Azazel, the Yellow-Eyed Demon: the first boss a hunter meets. Called up by a ritual wherever it is
 * drawn, he fights in a man's body until it cracks and the smoke shows through.
 *
 * <pre>EMERGING (smoke takes shape; Colt's rails rise) → P1 The Yellow-Eyed Demon → T → P2 Smoke and Fire → DYING</pre>
 *
 * <p>Samuel Colt's rails at the centre of the arena hold him whenever he is inside them while they
 * are charged ({@link RailTrap}): he will not walk in, but he can be knocked in, shot in with the Colt,
 * or baited across them while he rushes as smoke. Built on {@link LuciferEntity}'s fight (states,
 * arena, boss bar, damage policy) through its hooks.
 */
public class AzazelEntity extends LuciferEntity implements SpellHooks.Exorcisable {

    public static final int MAX_PHASE = AzazelBalance.PHASES, AZAZEL_EMERGE_TICKS = 80, AZAZEL_DEATH_TICKS = 120;
    /** When, during his emergence, the rails come up out of the ground. */
    private static final int RAILS_AT = 30;
    public static final int COLT_STUN_TICKS = 40;

    private static final EntityDataAccessor<Boolean> SMOKE = SynchedEntityData.defineId(AzazelEntity.class, EntityDataSerializers.BOOLEAN);

    private final RailTrap trap = new RailTrap();
    private final List<UUID> possessed = new ArrayList<>();
    private int attacksSinceDash;
    /** Only Colt's iron (and a Colt round) may hold him: set while those effects are applied. */
    private boolean allowHold;
    private int onPaintedTrap;

    public AzazelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SMOKE, false);
    }

    public RailTrap trap() {
        return trap;
    }

    public List<UUID> possessed() {
        return possessed;
    }

    /** Whether he is a rush of smoke right now (untouchable, and not drawn). */
    public boolean isSmoke() {
        return entityData.get(SMOKE);
    }

    public void setSmoke(boolean smoke) {
        entityData.set(SMOKE, smoke);
    }

    // --- his numbers -------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return MAX_PHASE;
    }

    @Override
    protected float threshold(int phase) {
        return AzazelBalance.threshold(phase);
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.AZAZEL_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return SNConfig.AZAZEL_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.AZAZEL_DAMAGE_MULTIPLIER.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    protected void scaleHealthToChallengers() {
        double max = AzazelBalance.health(SNConfig.AZAZEL_HEALTH.get(), SNConfig.AZAZEL_HEALTH_PER_PLAYER.get(), challengers().size());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(1024, max));
        setHealth((float) Math.min(1024, max));
    }

    @Override
    protected List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return AzazelAttacks.pool(phase);
    }

    @Override
    protected int baseGap(int phase) {
        return AzazelBalance.attackGap(phase);
    }

    @Override
    public float scale(int phase) {
        return 1.0f;
    }

    @Override
    protected int emergeTicks() {
        return AZAZEL_EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return AZAZEL_DEATH_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return TRANSITION_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return "animation.azazel.";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return AzazelAnimations.TRIGGERED;
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft.azazel.phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return phase == 1 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED;
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return AllParticles.YELLOW_SMOKE.get();
    }

    @Override
    protected SoundEvent emergeSound() {
        return AllSounds.AZAZEL_SMOKE.get();
    }

    @Override
    protected SoundEvent roarSound() {
        return AllSounds.AZAZEL_LAUGH.get();
    }

    @Override
    protected SoundEvent transformSound() {
        return AllSounds.AZAZEL_SMOKE.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.AZAZEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.AZAZEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.AZAZEL_DEATH.get();
    }

    // --- the fight's shape --------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), SNConfig.AZAZEL_ARENA_RADIUS.get(), ArenaTheme.SULFUR);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
        if (phase >= 2) AzazelTerrain.scorch(level, arena);
    }

    @Override
    protected void playEmergence() {
        AzazelCinematics.emergence(this);
    }

    @Override
    protected void playTransition(int to) {
        AzazelCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        AzazelCinematics.death(this);
    }

    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        AzazelCinematics.victory(this);
        releasePossessed(level);
    }

    @Override
    protected void onTransitionStart(int to) {
        setSmoke(false);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1.4, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
        }
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (level() instanceof ServerLevel level && elapsed % 4 == 0) {
            // The vessel splits and the smoke screams out of the cracks.
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1.6, getZ(), 3, 0.2, 0.3, 0.2, 0.08);
        }
    }

    /** The smoke gathers into a man; halfway through, Colt's rails come up through the ground. */
    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        int elapsed = AZAZEL_EMERGE_TICKS - stateTimer;
        if (!(level() instanceof ServerLevel level)) return;
        if (elapsed < 40 && elapsed % 3 == 0) {
            double h = elapsed / 40.0 * 2.0;
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + h, getZ(), 4, 0.6 - h * 0.2, 0.3, 0.6 - h * 0.2, 0.02);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 3, 0.8, 0.1, 0.8, 0.01);
        }
        ArenaController arena = arena();
        if (elapsed == RAILS_AT && arena != null) trap.build(level, arena);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(false);
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    /** The smoke is torn out of the vessel and drawn down between the rails. */
    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1, getZ(), 160, 1.2, 1.2, 1.2, 0.2);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 80, 1.5, 0.3, 1.5, 0.05);
            return;
        }
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1.6, getZ(), 6, 0.2, 0.2, 0.2, 0.04);
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 0.1, getZ(), 4, 1.0, 0.05, 1.0, 0.0);
    }

    /** Straying to the edge puts him back halfway in: never into the rails. */
    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        Vec3 c = arena.centerVec();
        Vec3 out = position().subtract(c).multiply(1, 0, 1);
        if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
        Vec3 at = c.add(out.normalize().scale(Math.max(RailTrapLayout.RADIUS + 3, arena.radius() * 0.5)));
        BlockPos floor = ArenaTerrain.surface((ServerLevel) level(), arena, (int) Math.floor(at.x), (int) Math.floor(at.z));
        return new Vec3(at.x, floor != null ? floor.getY() + 1 : c.y + 1, at.z);
    }

    /** Abandoned, he simply leaves as smoke: the offerings were common, and nothing is left behind. */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1, getZ(), 80, 0.5, 1.0, 0.5, 0.1);
        releasePossessed(level);
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", ".azazel."));
    }

    // --- the rails -----------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        long now = level.getGameTime();
        // Spawned by egg or command, he never emerged: lay the rails now, and step out of them.
        if (!trap.built() && state() != EMERGING) {
            trap.build(level, arena);
            if (RailTrap.inside(arena, position())) {
                Vec3 out = tetherPoint(arena);
                teleportTo(out.x, out.y, out.z);
            }
        }
        trap.tick(level, arena, now);
        byte s = state();
        if (s != EMERGING && s != TRANSITION && s != DYING && trap.charged() && RailTrap.inside(arena, position())) {
            catchInRails(level, arena, now);
        }
        burnPaintedTraps(level);
        if (isSmoke() && tickCount % 2 == 0) {
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1, getZ(), 4, 0.3, 0.6, 0.3, 0.03);
        }
    }

    /** Samuel Colt's iron closes on him. */
    public void catchInRails(ServerLevel level, ArenaController arena, long now) {
        int hold = SNConfig.AZAZEL_TRAP_SECONDS.get() * 20;
        if (!trap.spring(level, arena, now, hold, SNConfig.AZAZEL_TRAP_RECHARGE_SECONDS.get() * 20)) return;
        setSmoke(false);
        scheduler().cancel();
        scheduler().delay(hold);
        getNavigation().stop();
        allowHold = true;
        addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, hold, 0, false, true, true));
        allowHold = false;
        triggerAnim("action", "trapped");
        playSound(AllSounds.AZAZEL_HURT.get(), 3.0f, 0.5f);
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1, getZ(), 40, 0.3, 0.8, 0.3, 0.06);
        for (ServerPlayer p : challengers()) ChorusRewards.award(p, "main/railroaded");
    }

    public boolean isHeld() {
        return level() instanceof ServerLevel level && trap.holding(level.getGameTime());
    }

    @Override
    protected float vulnerability(DamageSource source) {
        return AzazelBalance.vulnerability(state() == RECOVER, isHeld(), SNConfig.AZAZEL_TRAPPED_VULNERABILITY.get());
    }

    /** A painted devil's trap does not hold a demon prince: he burns it off the floor. */
    private void burnPaintedTraps(ServerLevel level) {
        BlockPos at = blockPosition();
        var state = level.getBlockState(at);
        if (!state.is(AllBlocks.DEVILS_TRAP.get())) {
            onPaintedTrap = 0;
            return;
        }
        if (++onPaintedTrap < 20) return;
        onPaintedTrap = 0;
        BlockPos centre = DevilsTrapBlock.center(at, state);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.getX() + 0.5, centre.getY() + 0.2, centre.getZ() + 0.5, 30, 1.0, 0.1, 1.0, 0.02);
        level.sendParticles(AllParticles.YELLOW_SMOKE.get(), centre.getX() + 0.5, centre.getY() + 0.2, centre.getZ() + 0.5, 20, 1.0, 0.1, 1.0, 0.02);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = centre.offset(dx, 0, dz);
                if (level.getBlockState(p).is(AllBlocks.DEVILS_TRAP.get())) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        playSound(AllSounds.AZAZEL_LAUGH.get(), 2.0f, 1.0f);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        Holder<MobEffect> e = effect.getEffect();
        if (e.is(AllMobEffects.TRAPPED.getKey()) || e.is(AllMobEffects.STUNNED.getKey())) return allowHold;
        return super.canBeAffected(effect);
    }

    // --- damage --------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        boolean colt = source.is(AllDamageTypes.COLT);
        if (isSmoke() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!colt) return false;
            setSmoke(false);
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && colt && !isInvulnerablePhase()) coltStun(source.getEntity());
        return hurt;
    }

    /** A Colt round staggers him: a step back, his attack broken, a moment unable to move. */
    private void coltStun(@Nullable Entity shooter) {
        if (shooter != null) {
            Vec3 away = position().subtract(shooter.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 0.01) move(MoverType.SELF, away.normalize().scale(1.5));
        }
        scheduler().cancel();
        scheduler().delay(COLT_STUN_TICKS);
        allowHold = true;
        addEffect(new MobEffectInstance(AllMobEffects.STUNNED, COLT_STUN_TICKS, 0, false, true, true));
        allowHold = false;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.YELLOW_SMOKE.get(), getX(), getY() + 1.5, getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        }
    }

    @Override
    public boolean onExorcised(float potency) {
        if (isInvulnerablePhase()) return false;
        invulnerableTime = 0;
        return hurt(AllDamageTypes.source(level(), AllDamageTypes.SMITE, null), 10f * potency);
    }

    // --- attacks -------------------------------------------------------------------------------------

    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (phase() >= 2 && attacksSinceDash >= AzazelBalance.DASH_EVERY) {
            attacksSinceDash = 0;
            return AzazelAttacks.SmokeDash::new;
        }
        if (distanceToSqr(target) > 18 * 18 || noSightTicks > 100) {
            noSightTicks = 0;
            return AzazelAttacks.Blink::new;
        }
        return null;
    }

    @Override
    public void onStage(AttackScheduler.Stage stage, @Nullable BossAttack<LuciferEntity> attack) {
        super.onStage(stage, attack);
        if (stage == AttackScheduler.Stage.IDLE && attack == null) attacksSinceDash++;
    }

    // --- possession ----------------------------------------------------------------------------------

    /** Lets go of every creature his smoke still rides. */
    public void releasePossessed(ServerLevel level) {
        for (UUID id : possessed) {
            if (level.getEntity(id) instanceof LivingEntity l) l.removeEffect(AllMobEffects.POSSESSED);
        }
        possessed.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) releasePossessed(level);
        super.remove(reason);
    }

    // --- persistence ---------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Rails", trap.save());
        tag.putInt("SinceDash", attacksSinceDash);
        ListTag list = new ListTag();
        for (UUID id : possessed) list.add(NbtUtils.createUUID(id));
        tag.put("Possessed", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        trap.load(tag.getCompound("Rails"));
        attacksSinceDash = tag.getInt("SinceDash");
        possessed.clear();
        for (Tag t : tag.getList("Possessed", Tag.TAG_INT_ARRAY)) possessed.add(NbtUtils.loadUUID(t));
    }
}
