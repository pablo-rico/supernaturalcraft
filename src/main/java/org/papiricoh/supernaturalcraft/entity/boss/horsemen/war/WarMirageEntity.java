package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of War's mirages: every one wears a black-eyed demon's face. The hostile ones fight; the innocent ones (people he
 * has made look like demons) never attack and kneel where they stand: that is the tell. Striking an innocent hurts the
 * striker instead and feeds War's fury.
 */
public class WarMirageEntity extends Monster implements GeoEntity {

    public static final int LIFETIME = 600;
    private static final EntityDataAccessor<Boolean> INNOCENT = SynchedEntityData.defineId(WarMirageEntity.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID owner;

    public WarMirageEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INNOCENT, false);
    }

    public boolean isInnocent() {
        return entityData.get(INNOCENT);
    }

    public void setInnocent(boolean innocent) {
        entityData.set(INNOCENT, innocent);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
            @Override
            public boolean canUse() {
                return !isInnocent() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return !isInnocent() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (isInnocent()) {
            setTarget(null);
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        if (level() instanceof ServerLevel level) {
            boolean ownerGone = owner == null || !(level.getEntity(owner) instanceof WarEntity war) || !war.isAlive();
            if (ownerGone || tickCount > LIFETIME) vanish(level);
        }
    }

    public void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInnocent() && source.getEntity() instanceof ServerPlayer striker && level() instanceof ServerLevel level) {
            // The blow comes back on whoever struck.
            WarEntity war = owner != null && level.getEntity(owner) instanceof WarEntity w ? w : null;
            WarIllusions.betrayed(level, war, striker, amount);
            striker.displayClientMessage(Component.translatable("message.supernaturalcraft.war.innocent").withStyle(ChatFormatting.RED), true);
            vanish(level);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isInnocent() && super.doHurtTarget(target);
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 15, 0.3, 0.6, 0.3, 0.02);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** The black-eyed demon's clips: the innocent ones kneel (its trapped pose, lowered by the renderer). */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.black_eyed_demon.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.black_eyed_demon.walk");
        RawAnimation kneel = RawAnimation.begin().thenLoop("animation.black_eyed_demon.trapped");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (isInnocent()) return state.setAndContinue(kneel);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Innocent", isInnocent());
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setInnocent(tag.getBoolean("Innocent"));
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }
}
