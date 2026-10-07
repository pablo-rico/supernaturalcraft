package org.papiricoh.supernaturalcraft.entity.hellhound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A hellhound: the dogs that come to collect on a crossroads deal. Unseen, as on the show: only a
 * shimmer in the air, the smoke of its breath and its burning paw prints give it away, until it is
 * revealed (holy water, any holy wound, the Reveal sigil), or unless you carry the Eclipse Sight.
 * Hunts in packs; its bite holds you in place.
 */
public class HellhoundEntity extends Monster implements GeoEntity, SpellHooks.Revealable {

    public static final int REVEAL_TICKS = 600;
    private static final EntityDataAccessor<Boolean> REVEALED = SynchedEntityData.defineId(HellhoundEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int revealTicks;

    public HellhoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(HellhoundEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- being seen -------------------------------------------------------------------------------

    public boolean isRevealed() {
        return entityData.get(REVEALED);
    }

    /** Makes it visible to everyone for {@code ticks}. */
    public void reveal(int ticks) {
        revealTicks = Math.max(revealTicks, ticks);
        entityData.set(REVEALED, true);
    }

    @Override
    public void onRevealed() {
        reveal(REVEAL_TICKS);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 0.8, getZ(), 30, 0.6, 0.5, 0.6, 0.05);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.is(AllTags.DamageTypes.HOLY)) onRevealed();
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            triggerAnim("action", "bite");
            playSound(AllSounds.HELLHOUND_BITE.get(), 1.0f, 0.8f + random.nextFloat() * 0.2f);
            if (target instanceof LivingEntity living) {
                // The jaws hold: a moment pinned to the ground.
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                living.igniteForSeconds(2);
            }
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            clientTraces();
        } else if (revealTicks > 0 && --revealTicks == 0) {
            entityData.set(REVEALED, false);
        }
    }

    /** What gives an unseen hound away: breath, burning prints, the air bending around it. */
    private void clientTraces() {
        if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 0.002 && tickCount % 4 == 0) {
            double side = (tickCount / 4) % 2 == 0 ? 0.25 : -0.25;
            double yaw = Math.toRadians(yBodyRot);
            double px = getX() + Math.cos(yaw) * side, pz = getZ() + Math.sin(yaw) * side;
            level().addParticle(AllParticles.HELLFIRE.get(), px, getY() + 0.05, pz, 0, 0.005, 0);
            level().addParticle(ParticleTypes.SMOKE, px, getY() + 0.1, pz, 0, 0.01, 0);
        }
        if (random.nextInt(10) == 0) {
            double yaw = Math.toRadians(getYHeadRot() + 90);
            level().addParticle(ParticleTypes.SMOKE, getX() + Math.cos(yaw) * 0.8, getY() + 0.9, getZ() + Math.sin(yaw) * 0.8,
                    Math.cos(yaw) * 0.04, 0.02, Math.sin(yaw) * 0.04);
        }
    }

    // --- sound -------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.HELLHOUND_GROWL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.HELLHOUND_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.HELLHOUND_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP, 0.25f, 0.6f);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    // --- GeckoLib ----------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.hellhound.idle");
        RawAnimation walk = RawAnimation.begin().thenLoop("animation.hellhound.walk");
        RawAnimation run = RawAnimation.begin().thenLoop("animation.hellhound.run");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (!state.isMoving()) return state.setAndContinue(idle);
            return state.setAndContinue(getTarget() != null ? run : walk);
        }));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("bite", RawAnimation.begin().thenPlay("animation.hellhound.bite")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Revealed", revealTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        revealTicks = tag.getInt("Revealed");
        entityData.set(REVEALED, revealTicks > 0);
    }
}
