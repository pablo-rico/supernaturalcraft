package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A possessed patient of the asylum (v0.35): slow, many, and given to sudden lunges. Low-ranking demons Crowley keeps in the
 * cells' old inmates. Black-eyed but never chain officers (their type is not a chain demon's).
 */
public class PossessedPatientEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PossessedPatientEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CrowleyBalance.PATIENT_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CrowleyBalance.PATIENT_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45f) {
            @Override
            public void start() {
                super.start();
                triggerAnim("action", "lunge");
            }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || CrowleyRetinue.ours(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() != null && CrowleyRetinue.ours(source.getEntity())) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && getTarget() == null && random.nextInt(200) == 0) triggerAnim("action", "twitch");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.crowley("patient_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.crowley("patient_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.crowley("patient_death");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + CrowleyAssets.PATIENT + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        AnimationController<PossessedPatientEntity> action = new AnimationController<>(this, "action", 2, s -> PlayState.STOP);
        action.triggerableAnim("lunge", RawAnimation.begin().thenPlay(pre + "lunge"));
        action.triggerableAnim("twitch", RawAnimation.begin().thenPlay(pre + "twitch"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    static boolean prey(LivingEntity e) {
        return e instanceof Player;
    }
}
