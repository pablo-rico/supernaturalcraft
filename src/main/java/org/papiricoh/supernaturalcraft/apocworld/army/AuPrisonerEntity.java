package org.papiricoh.supernaturalcraft.apocworld.army;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A prisoner of Michael's camps (v0.41): a ragged survivor of the burned world ({@link AuSoldierAssets#PRISONER}, four looks)
 * huddled in a cage, cowering when a soldier passes. Freed ({@link #free}: its cage's lock broken while the guards are down), it
 * cheers for a moment and is gone through a little rift to wherever the resistance hides. It never fights and never wanders.
 */
public class AuPrisonerEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Byte> LOOK = SynchedEntityData.defineId(AuPrisonerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FREED = SynchedEntityData.defineId(AuPrisonerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int LOOKS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos camp;
    private int freedTicks;

    public AuPrisonerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOK, (byte) 0);
        builder.define(FREED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public int look() {
        return entityData.get(LOOK);
    }

    public void setLook(int v) {
        entityData.set(LOOK, (byte) Math.floorMod(v, LOOKS));
    }

    /** Held in the camp at {@code camp}. */
    public void holdIn(BlockPos camp) {
        this.camp = camp.immutable();
    }

    public @Nullable BlockPos camp() {
        return camp;
    }

    public boolean freed() {
        return entityData.get(FREED);
    }

    /** Set free: it cheers, then the little rift takes it ({@link AuArmyRules#CHEER_TICKS}). */
    public void free() {
        if (freed()) return;
        entityData.set(FREED, true);
        freedTicks = 0;
        triggerAnim("action", "cheer");
        playSound(SoundEvents.VILLAGER_CELEBRATE, 1f, 0.8f + random.nextFloat() * 0.3f);
    }

    /** Whether a soldier is close (it cowers). */
    private boolean soldierNear() {
        return !level().getEntitiesOfClass(AuSoldierEntity.class, getBoundingBox().inflate(6)).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!freed()) {
            getNavigation().stop();
            if (tickCount % 60 == 0 && soldierNear()) triggerAnim("action", "cower");
            return;
        }
        freedTicks++;
        ServerLevel level = (ServerLevel) level();
        if (freedTicks % 4 == 0) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 3, 0.4, 0.8, 0.4, 0.01);
        }
        if (freedTicks == AuArmyRules.CHEER_TICKS - 20) {
            level.playSound(null, blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.NEUTRAL, 0.4f, 1.6f);
        }
        if (freedTicks >= AuArmyRules.CHEER_TICKS) {
            level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 40, 0.4, 0.9, 0.4, 0.2);
            level.sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 30, 0.4, 0.9, 0.4, 0.02);
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Michael's soldiers never strike their prisoners; nor does anything else in the camp.
        if (source.getEntity() instanceof AuSoldierEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !freed() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        setLook(random.nextInt(LOOKS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // --- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + AuSoldierAssets.PRISONER + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "action", 3, state -> PlayState.STOP)
                .triggerableAnim("cower", RawAnimation.begin().thenPlay(pre + "cower"))
                .triggerableAnim("cheer", RawAnimation.begin().thenPlayAndHold(pre + "cheer")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // --- persistence ---------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Look", (byte) look());
        tag.putBoolean("Freed", freed());
        if (camp != null) tag.put("Camp", NbtUtils.writeBlockPos(camp));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setLook(tag.getByte("Look"));
        entityData.set(FREED, tag.getBoolean("Freed"));
        camp = NbtUtils.readBlockPos(tag, "Camp").orElse(null);
    }
}
