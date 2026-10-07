package org.papiricoh.supernaturalcraft.entity.boss.horsemen.death;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A reaper, hooded and floating: only a hunter whose clock is nearly out (under twenty seconds), or who is in limbo, or
 * anyone once the world of the dead has taken the arena, can see one (the renderer decides). Its touch takes time off
 * your clock; killing one winds your clock back to full.
 */
public class ReaperEntity extends Monster implements GeoEntity {

    /** Seconds a reaper's touch takes off its victim's clock. */
    public static final int CLOCK_THEFT_TICKS = 100;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;

    public ReaperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public void setMaster(UUID master) {
        this.master = master;
    }

    public @Nullable DeathEntity master(ServerLevel level) {
        return master != null && level.getEntity(master) instanceof DeathEntity d && d.isAlive() ? d : null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && master(level) == null) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 12, 0.3, 0.8, 0.3, 0.02);
            discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(level() instanceof ServerLevel level)) return false;
        boolean hit = target.hurt(AllDamageTypes.source(level, AllDamageTypes.REAPED, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (hit && target instanceof ServerPlayer p) {
            DeathEntity death = master(level);
            if (death != null) death.stealTime(p, CLOCK_THEFT_TICKS);
            triggerAnim("action", "attack");
            playSound(AllSounds.REAPER_ATTACK.get(), 1.0f, 1.0f);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            DeathEntity death = master(level);
            if (death != null && source.getEntity() instanceof ServerPlayer p) death.reaperFell(p);
            level.sendParticles(AllParticles.SOUL_WISP.get(), getX(), getY() + 1.2, getZ(), 20, 0.3, 0.7, 0.3, 0.05);
        }
    }

    @Override
    protected void tickDeath() {
        if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.REAPER_AMBIENT.get();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.reaper.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.reaper.walk");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().thenPlay("animation.reaper.attack")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Master")) master = tag.getUUID("Master");
    }

    /** Whether {@code e} is a reaper (for the client's visibility rules without loading this class's statics). */
    public static boolean is(LivingEntity e) {
        return e instanceof ReaperEntity;
    }
}
