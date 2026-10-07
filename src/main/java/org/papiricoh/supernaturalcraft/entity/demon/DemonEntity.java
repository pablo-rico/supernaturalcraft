package org.papiricoh.supernaturalcraft.entity.demon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Shared behaviour of every lesser demon.
 *
 * <p><b>Smoking out:</b> the first time a demon drops below a third of its health it may abandon
 * its vessel — a short animation, then a column of black smoke and it is gone, with no drops. A
 * devil's trap (or the Bind sigil) prevents this, which is the whole point of trapping a demon
 * before you finish it.
 */
public abstract class DemonEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> SMOKING =
            SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int SMOKE_DURATION = 24;
    public static final float SMOKE_THRESHOLD = 0.34f;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean smokeRolled;
    private int smokeTicks;

    protected DemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    protected abstract String animPrefix();

    /** Chance, rolled once, that the demon flees its vessel when badly hurt. */
    protected float smokeChance() {
        return 0.6f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SMOKING, false);
    }

    public boolean isSmoking() {
        return entityData.get(SMOKING);
    }

    public boolean isTrapped() {
        return hasEffect(AllMobEffects.TRAPPED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isSmoking()) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(AllParticles.DEMON_SMOKE.get(), getX() + (random.nextDouble() - 0.5) * 0.3,
                            getEyeY() + 0.1, getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.12 + random.nextDouble() * 0.08, 0);
                }
            }
            return;
        }
        if (isSmoking()) {
            tickSmoking();
        } else if (!smokeRolled && getHealth() < getMaxHealth() * SMOKE_THRESHOLD && isAlive()) {
            smokeRolled = true;
            if (!isTrapped() && random.nextFloat() < smokeChance()) {
                startSmoking();
            }
        }
    }

    protected void startSmoking() {
        entityData.set(SMOKING, true);
        smokeTicks = 0;
        getNavigation().stop();
        triggerAnim("action", "smoke_out");
        playSound(AllSounds.DEMON_SMOKE.get(), 1.0f, 0.8f + random.nextFloat() * 0.2f);
    }

    private void tickSmoking() {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (isTrapped()) {
            // The trap holds: the demon is forced back into its vessel.
            entityData.set(SMOKING, false);
            return;
        }
        if (++smokeTicks >= SMOKE_DURATION && level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getEyeY(), getZ(), 60, 0.25, 1.5, 0.25, 0.08);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
            server.playSound(null, blockPosition(), AllSounds.DEMON_SMOKE.get(), SoundSource.HOSTILE, 1.4f, 0.6f);
            discard();
        }
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isSmoking();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", "attack");
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.DEMON_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.DEMON_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.DEMON_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SmokeRolled", smokeRolled);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        smokeRolled = tag.getBoolean("SmokeRolled");
    }

    // --- GeckoLib -------------------------------------------------------------------------

    protected RawAnimation anim(String name) {
        return RawAnimation.begin().thenLoop("animation." + animPrefix() + "." + name);
    }

    protected RawAnimation once(String name) {
        return RawAnimation.begin().thenPlay("animation." + animPrefix() + "." + name);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = anim("idle"), walk = anim("walk"), trapped = anim("trapped");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (isTrapped()) return state.setAndContinue(trapped);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<DemonEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim("attack", once("attack"));
        action.triggerableAnim("smoke_out", RawAnimation.begin().thenPlayAndHold("animation." + animPrefix() + ".smoke_out"));
        registerExtraTriggers(action);
        controllers.add(action);
    }

    protected void registerExtraTriggers(AnimationController<DemonEntity> action) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
