package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.network.HorsemenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.function.Supplier;

/**
 * What the four Horsemen share, built on {@link LuciferEntity}'s fight through its hooks: true health above the vanilla
 * cap (like Metatron's), three phases (four for Death) with a themed arena laid over the ground as he arrives, and in his
 * last phase he mounts his horse (a synced flag: the model shows its {@code steed} group and the hitbox grows). Every
 * victory, rematches included, leaves his ring, his trophy and his horse, untamed.
 */
public abstract class HorsemanEntity extends LuciferEntity {

    private static final EntityDataAccessor<Boolean> MOUNTED = SynchedEntityData.defineId(HorsemanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation MOUNTED_SPEED = SupernaturalCraft.asResource("horseman_mounted");
    private static final EntityDimensions MOUNTED_SIZE = EntityDimensions.scalable(1.4f, 3.0f).withEyeHeight(2.7f);

    private float healthScale = 1f;
    private boolean scaled, groundLaid;
    private @Nullable HorsemenGround ground;

    protected HorsemanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HorsemenBalance.MID_BASE_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    public abstract HorsemanKind kind();

    /** The ground he lays over his arena as he arrives (centred on it, relative to the surface). */
    protected abstract List<ArenaCell> groundPlan(int radius, long seed);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOUNTED, false);
    }

    public boolean isMounted() {
        return entityData.get(MOUNTED);
    }

    public void setMounted(boolean mounted) {
        entityData.set(MOUNTED, mounted);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOUNTED_SPEED);
            if (mounted) speed.addTransientModifier(new AttributeModifier(MOUNTED_SPEED, HorsemenBalance.MOUNTED_SPEED - 1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MOUNTED.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return isMounted() ? MOUNTED_SIZE : super.getDefaultDimensions(pose);
    }

    public String id() {
        return kind().id();
    }

    /** The seed of his ground: the same arena is laid the same way again after a reload. */
    protected long groundSeed(ArenaController arena) {
        return arena.center().asLong() * 31 + kind().ordinal();
    }

    protected int arenaRadius() {
        return SNConfig.HORSEMEN_ARENA_RADIUS.get();
    }

    // --- his numbers ---------------------------------------------------------------------------------

    @Override
    public int maxPhase() {
        return kind().phases;
    }

    @Override
    protected float threshold(int phase) {
        return HorsemenBalance.threshold(maxPhase(), phase);
    }

    @Override
    protected float healthScale() {
        return healthScale;
    }

    /** Test hook. */
    public void setHealthScale(float scale) {
        healthScale = scale;
        scaled = true;
    }

    protected double healthMultiplier() {
        return SNConfig.HORSEMEN_HEALTH_MULTIPLIER.get();
    }

    @Override
    protected void scaleHealthToChallengers() {
        boolean death = kind() == HorsemanKind.DEATH;
        healthScale = HorsemenBalance.healthScale(healthMultiplier(), SNConfig.HORSEMEN_HEALTH_PER_PLAYER.get(), challengers().size());
        scaled = true;
        double base = HorsemenBalance.baseHealth(death);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(base);
        setHealth((float) base);
    }

    @Override
    protected float mundaneMultiplier() {
        return SNConfig.HORSEMEN_MUNDANE_MULTIPLIER.get().floatValue();
    }

    @Override
    protected float hitCap() {
        return SNConfig.HORSEMEN_HIT_CAP.get().floatValue();
    }

    @Override
    public float attackDamageMultiplier() {
        return SNConfig.HORSEMEN_DAMAGE_MULTIPLIER.get().floatValue();
    }

    @Override
    public boolean isAerialPhase() {
        return false;
    }

    @Override
    public float scale(int phase) {
        return 1.0f;
    }

    @Override
    protected int baseGap(int phase) {
        return HorsemenBalance.attackGap(maxPhase(), phase);
    }

    @Override
    protected int emergeTicks() {
        return HorsemenBalance.EMERGE_TICKS;
    }

    @Override
    protected int deathTicks() {
        return HorsemenBalance.DEATH_TICKS;
    }

    @Override
    protected int transitionTicks(int to) {
        return to == maxPhase() ? HorsemenBalance.MOUNT_TICKS : HorsemenBalance.TRANSITION_TICKS;
    }

    @Override
    protected String animationPrefix() {
        return "animation." + id() + ".";
    }

    @Override
    protected List<String> triggeredAnimations() {
        return HorsemenAnimations.triggered(id());
    }

    @Override
    protected String bossBarKey(int phase) {
        return "entity.supernaturalcraft." + id() + ".phase" + phase;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor(int phase) {
        return switch (kind()) {
            case WAR -> BossEvent.BossBarColor.RED;
            case FAMINE -> phase == maxPhase() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW;
            case PESTILENCE -> BossEvent.BossBarColor.GREEN;
            case DEATH -> phase == maxPhase() ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE;
        };
    }

    @Override
    protected Component bossBarName(int phase) {
        ChatFormatting style = switch (kind()) {
            case WAR -> ChatFormatting.RED;
            case FAMINE -> ChatFormatting.GOLD;
            case PESTILENCE -> ChatFormatting.GREEN;
            case DEATH -> ChatFormatting.GRAY;
        };
        return Component.translatable(bossBarKey(phase)).withStyle(style);
    }

    @Override
    protected ParticleOptions phaseParticle(int phase) {
        return ParticleTypes.SMOKE;
    }

    protected abstract SoundEvent ambientSound();

    protected abstract SoundEvent hurtSound();

    protected abstract SoundEvent deathSound();

    @Override
    protected SoundEvent ambientBossSound() {
        return ambientSound();
    }

    @Override
    protected SoundEvent emergeSound() {
        return ambientSound();
    }

    @Override
    protected SoundEvent roarSound() {
        return ambientSound();
    }

    @Override
    protected SoundEvent dyingSound() {
        return deathSound();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return deathSound();
    }

    // --- the fight's shape -----------------------------------------------------------------------------

    @Override
    protected @Nullable ArenaController openOwnArena(ServerLevel level) {
        return LuciferSummoning.openArena(level, blockPosition(), arenaRadius(), kind().theme);
    }

    @Override
    protected void applyTerrain(ServerLevel level, ArenaController arena, int phase) {
    }

    @Override
    protected void playEmergence() {
        HorsemenCinematics.intro(this);
    }

    @Override
    protected void playTransition(int to) {
        if (to == maxPhase()) HorsemenCinematics.mountUp(this);
        else HorsemenCinematics.transition(this, to);
    }

    @Override
    protected void playDeath() {
        HorsemenCinematics.death(this);
    }

    @Override
    protected void onTransitionStart(int to) {
        if (to == maxPhase()) {
            setMounted(true);
            if (level() instanceof ServerLevel level) {
                fx(level, new HorsemenFxPayload(getId(), HorsemenFxPayload.MOUNT, kind().ordinal(), 0, position(), HorsemenBalance.MOUNT_TICKS));
                level.playSound(null, blockPosition(), org.papiricoh.supernaturalcraft.registry.AllSounds.STEED_NEIGH.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 3f, 0.8f);
            }
        }
    }

    @Override
    protected void tickTransitionMotion(int elapsed, boolean last) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (last && level() instanceof ServerLevel level && elapsed % 5 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.8, 0.1, 0.8, 0.02);
        }
    }

    @Override
    protected void tickEmergence() {
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel level && tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.1, getZ(), 3, 0.6, 0.1, 0.6, 0.01);
        }
    }

    @Override
    protected void clientEmergenceParticles() {
        level().addParticle(ParticleTypes.SMOKE, getRandomX(1.2), getY() + random.nextDouble() * 2, getRandomZ(1.2), 0, 0.02, 0);
    }

    @Override
    protected void tickDyingMotion(int elapsed) {
        setNoGravity(false);
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
    }

    @Override
    protected void dyingParticles(ServerLevel level, boolean last) {
        if (last) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 80, 0.8, 1.0, 0.8, 0.05);
            return;
        }
        level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.2, getZ(), 4, 0.4, 0.6, 0.4, 0.02);
    }

    @Override
    protected Vec3 tetherPoint(ArenaController arena) {
        Vec3 c = arena.centerVec();
        Vec3 out = position().subtract(c).multiply(1, 0, 1);
        if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
        Vec3 at = c.add(out.normalize().scale(6));
        BlockPos floor = ArenaTerrain.surface((ServerLevel) level(), arena, (int) Math.floor(at.x), (int) Math.floor(at.z));
        return new Vec3(at.x, floor != null ? floor.getY() + 1 : c.y + 1, at.z);
    }

    /** At the end of his death: his ring, his trophy and his horse, every time. */
    @Override
    protected void onDefeated(ServerLevel level, ArenaController arena) {
        HorsemenCinematics.victory(this);
        dropSpoils(level, position().add(0, 2.1, 0));
    }

    public void dropSpoils(ServerLevel level, Vec3 at) {
        spawnItem(level, at, new ItemStack(kind().ring()));
        spawnItem(level, at, new ItemStack(kind().trophy()));
        HorsemanSteedEntity steed = AllEntities.HORSEMAN_STEED.get().create(level);
        if (steed != null) {
            steed.moveTo(at.x, at.y, at.z, getYRot(), 0);
            steed.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            steed.setKind(kind());
            level.addFreshEntity(steed);
        }
    }

    protected static void spawnItem(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** The fight was lost: he rides away, leaving nothing (Death gives back the rings he was offered). */
    @Override
    protected void leaveBehind(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 40, 0.6, 1.0, 0.6, 0.04);
    }

    @Override
    protected void returnToCage(ServerLevel level, String messageKey) {
        super.returnToCage(level, messageKey.replace(".lucifer.", "." + id() + "."));
    }

    // --- attacks ---------------------------------------------------------------------------------------

    /** Gap closer when the target runs or hides: he crosses the arena to it (Death steps through the shadows). */
    @Override
    public @Nullable Supplier<BossAttack<LuciferEntity>> forcedAttack(LivingEntity target) {
        if (distanceToSqr(target) > 16 * 16 || noSightTicks > 80) {
            noSightTicks = 0;
            return HorsemenAttacks.Close::new;
        }
        return null;
    }

    // --- ticking ---------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        if (!scaled && state() != EMERGING) scaleHealthToChallengers();
        super.customServerAiStep();
        if (isRemoved() || !(level() instanceof ServerLevel level)) return;
        ArenaController arena = arena();
        if (arena == null || !arena.isActive()) return;
        tickGround(level, arena);
        if (state() != DYING) tickFight(level, arena);
    }

    /** His own mechanics, each tick of the fight (not while dying). */
    protected void tickFight(ServerLevel level, ArenaController arena) {
    }

    private void tickGround(ServerLevel level, ArenaController arena) {
        if (ground == null) ground = HorsemenGround.pin(level, arena, groundPlan(arena.radius(), groundSeed(arena)));
        if (!groundLaid && ground.tick(level, arena)) {
            groundLaid = true;
            onGroundLaid(level, arena);
        }
    }

    /** His ground, pinned to the arena (whether or not it is all down yet). */
    protected @Nullable HorsemenGround ground() {
        return ground;
    }

    /** Once his ground is down (his standards, Death's world, the swamp's vials). */
    protected void onGroundLaid(ServerLevel level, ArenaController arena) {
    }

    /** Test and preview hook: lays the whole ground now. */
    public void layGroundNow() {
        if (!(level() instanceof ServerLevel level) || arena() == null) return;
        ArenaController arena = arena();
        if (ground == null) ground = HorsemenGround.pin(level, arena, groundPlan(arena.radius(), groundSeed(arena)));
        if (!groundLaid) {
            ground.finish(level, arena);
            groundLaid = true;
            onGroundLaid(level, arena);
        }
    }

    public boolean groundLaid() {
        return groundLaid;
    }

    /** Sends a Horsemen effect to every challenger (and anyone close). */
    public void fx(ServerLevel level, HorsemenFxPayload payload) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 80 * 80) PacketDistributor.sendToPlayer(p, payload);
        }
    }

    public void scheduleDelay(int ticks) {
        scheduler().delay(ticks);
    }

    // --- GeckoLib --------------------------------------------------------------------------------------

    /** The clip he stands in when not walking (Famine sits in his wheelchair). */
    protected String stillClip() {
        return "idle";
    }

    /** The clip he moves in (Famine rolls his wheelchair). */
    protected String movingClip() {
        return "walk";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String p = animationPrefix();
        RawAnimation walk = RawAnimation.begin().thenLoop(p + "walk");
        RawAnimation mountedIdle = RawAnimation.begin().thenLoop(p + "mounted_idle");
        RawAnimation gallop = RawAnimation.begin().thenLoop(p + "mounted_gallop");
        controllers.add(new AnimationController<>(this, "base", 6, state -> {
            byte s = state();
            if (s == EMERGING || s == TRANSITION || s == DYING) return PlayState.STOP;
            if (isMounted()) return state.setAndContinue(state.isMoving() ? gallop : mountedIdle);
            String still = state.isMoving() ? movingClip() : stillClip();
            if (!still.equals("idle") && !still.equals("walk")) return state.setAndContinue(RawAnimation.begin().thenLoop(p + still));
            return state.setAndContinue(state.isMoving() ? walk : RawAnimation.begin().thenLoop(p + "idle"));
        }));
        AnimationController<HorsemanEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        action.triggerableAnim("emerge", RawAnimation.begin().thenPlay(p + "intro"));
        for (int to = 2; to <= maxPhase(); to++) {
            action.triggerableAnim("transform_" + to, RawAnimation.begin().thenPlay(p + (to == maxPhase() ? "mount" : "transition")));
        }
        action.triggerableAnim("death", RawAnimation.begin().thenPlayAndHold(p + "death"));
        for (String name : triggeredAnimations()) {
            if (name.equals("death") || name.equals("intro")) continue;
            action.triggerableAnim(name, RawAnimation.begin().thenPlay(p + name));
        }
        controllers.add(action);
    }

    // --- persistence -----------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Mounted", isMounted());
        tag.putFloat("HealthScale", healthScale);
        tag.putBoolean("Scaled", scaled);
        tag.putBoolean("GroundLaid", groundLaid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMounted(tag.getBoolean("Mounted"));
        healthScale = tag.contains("HealthScale") ? tag.getFloat("HealthScale") : 1f;
        scaled = tag.getBoolean("Scaled");
        groundLaid = tag.getBoolean("GroundLaid");
    }
}
