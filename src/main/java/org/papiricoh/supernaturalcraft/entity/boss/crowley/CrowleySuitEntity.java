package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * One of Crowley's suits (v0.35): a black-eyed demon in a black suit with a folder under its arm, the King of the Crossroads'
 * civil service. Fights hand to hand; leaves as smoke when it dies. Never an officer of the chain (not black-eyed for
 * {@code ChainEvents.factionOf}); at odds with the Pit's demons ({@link CrowleyRetinue#rivals}).
 */
public class CrowleySuitEntity extends Monster implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public CrowleySuitEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CrowleyBalance.SUIT_HEALTH)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, CrowleyBalance.SUIT_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CrowleySuitEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, CrowleyRetinue::rivals));
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
    public boolean doHurtTarget(Entity target) {
        triggerAnim("action", "strike");
        return super.doHurtTarget(target);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level) {
            triggerAnim("action", "smoke");
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1.2, getZ(), 24, 0.3, 0.8, 0.3, 0.06);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.crowley("suit_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.crowley("suit_death");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + CrowleyAssets.SUIT + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk");
        controllers.add(new AnimationController<>(this, "base", 5, s -> s.setAndContinue(s.isMoving() ? walk : idle)));
        AnimationController<CrowleySuitEntity> action = new AnimationController<>(this, "action", 2, s -> PlayState.STOP);
        action.triggerableAnim("strike", RawAnimation.begin().thenPlay(pre + "strike"));
        action.triggerableAnim("smoke", RawAnimation.begin().thenPlay(pre + "smoke"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Whether a living thing is fair game for a suit (used by the garrison's rivalry checks in tests). */
    public static boolean foe(LivingEntity e) {
        return e instanceof Player || CrowleyRetinue.rivals(e);
    }
}
