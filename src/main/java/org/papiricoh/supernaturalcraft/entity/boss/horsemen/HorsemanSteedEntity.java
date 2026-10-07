package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Horseman's horse, left behind every time he falls: red for War, black for Famine, pale green for Pestilence, pale
 * for Death. No powers: a better horse (more health, speed and jump than any vanilla one can roll), tamed, saddled and
 * ridden like any other. They don't breed.
 */
public class HorsemanSteedEntity extends Horse implements GeoEntity {

    public static final double HEALTH = 44, SPEED = 0.36, JUMP = 1.1;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(HorsemanSteedEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public HorsemanSteedEntity(EntityType<? extends Horse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.JUMP_STRENGTH, JUMP);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    public HorsemanKind kind() {
        return HorsemanKind.byVariant(entityData.get(KIND));
    }

    public void setKind(HorsemanKind kind) {
        entityData.set(KIND, kind.ordinal());
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SPEED);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(JUMP);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setAge(0);
        setHealth(getMaxHealth());
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND) setKind(HorsemanKind.byVariant(random.nextInt(4)));
        return out;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.STEED_NEIGH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.HORSE_HURT;
    }

    @Override
    protected void playGallopSound(net.minecraft.world.level.block.SoundType sound) {
        super.playGallopSound(sound);
        if (random.nextInt(6) == 0) playSound(AllSounds.STEED_GALLOP.get(), sound.getVolume() * 0.3f, sound.getPitch());
    }

    // --- GeckoLib ---------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.horseman_steed.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.horseman_steed.walk");
        RawAnimation gallop = RawAnimation.begin().thenLoop("animation.horseman_steed.gallop");
        RawAnimation rear = RawAnimation.begin().thenPlay("animation.horseman_steed.rear");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isStanding()) return state.setAndContinue(rear);
            if (!state.isMoving()) return state.setAndContinue(idle);
            double speed = getDeltaMovement().horizontalDistance();
            return state.setAndContinue(speed > 0.2 ? gallop : walk);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- persistence ------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Horseman", kind().id());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (HorsemanKind k : HorsemanKind.values()) if (k.id().equals(tag.getString("Horseman"))) setKind(k);
    }
}
