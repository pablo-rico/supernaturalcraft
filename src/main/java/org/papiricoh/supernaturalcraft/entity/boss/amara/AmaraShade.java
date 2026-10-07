package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.LightLayer;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A shade: a small echo of the Darkness that hunts in the dark. Light burns it, and strong light
 * (12 or more) unmakes it outright.
 */
public class AmaraShade extends Monster implements GeoEntity {

    public static final int BURN_LIGHT = AmaraBalance.LIT, UNMAKE_LIGHT = AmaraBalance.LANCE_PROOF;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AmaraShade(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.5f) {
                level().addParticle(AllParticles.VOID_MOTE.get(), getRandomX(0.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.6), 0, 0.02, 0);
            }
            return;
        }
        if (tickCount % 10 == 0) {
            int light = level().getBrightness(LightLayer.BLOCK, BlockPos.containing(getEyePosition()));
            if (light >= UNMAKE_LIGHT) {
                unmake();
            } else if (light >= BURN_LIGHT) {
                hurt(damageSources().magic(), 3f);
            }
        }
    }

    /** Light takes it apart: a puff of night and gone. */
    public void unmake() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.VOID_MOTE.get(), getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            server.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 10, 0.3, 0.6, 0.3, 0.05);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !(source.getEntity() instanceof AmaraEntity) && super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("animation.amara.form_idle");
        controllers.add(new AnimationController<>(this, "base", 5, state -> state.setAndContinue(idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
