package org.papiricoh.supernaturalcraft.british.agent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.arsenal.Hex;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A British Men of Letters field agent (v0.41): Hess's men at the compound. He keeps his distance and fires short bursts of
 * enchanted rounds (each carries the round's hex), and clubs whoever closes with his carbine's butt. Three looks
 * ({@link #variant}: the textures {@code british_agent_0..2}).
 */
public class BritishAgentEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(BritishAgentEntity.class, EntityDataSerializers.INT);
    public static final int VARIANTS = 3;
    /** A burst's damage, reach (blocks), and the rest between bursts (ticks). */
    public static final float SHOT_DAMAGE = 4f, SHOT_RANGE = 18f;
    public static final int SHOT_REST = 50;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int shotTimer = 30, firing = -1;

    public BritishAgentEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.floorMod(v, VARIANTS));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        setVariant(random.nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
            @Override
            public boolean canUse() {
                LivingEntity t = mob.getTarget();
                return t != null && mob.distanceToSqr(t) < 36 && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, BritishAgentEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("action", "melee");
        return super.doHurtTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (firing >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30, 30);
            if (++firing == BritishAgentAssets.HIT_TICKS.getOrDefault("shoot", 3)) shoot(t);
            if (firing >= BritishAgentAssets.CLIP_TICKS.getOrDefault("shoot", 10)) firing = -1;
            return;
        }
        if (t == null || !t.isAlive() || --shotTimer > 0) return;
        double d = distanceToSqr(t);
        if (d > 9 && d < SHOT_RANGE * SHOT_RANGE && hasLineOfSight(t)) {
            shotTimer = SHOT_REST + random.nextInt(20);
            firing = 0;
            triggerAnim("action", "shoot");
        } else {
            shotTimer = 10;
        }
    }

    private void shoot(@Nullable LivingEntity t) {
        if (!(level() instanceof ServerLevel level)) return;
        playSound(BritishRegistry.sound("british_agent.shot"), 1.2f, 1.1f);
        if (t == null || !t.isAlive() || !hasLineOfSight(t)) return;
        Vec3 from = getEyePosition(), to = t.getEyePosition();
        Vec3 d = to.subtract(from);
        for (double s = 0.8; s < d.length(); s += 0.9) {
            Vec3 q = from.add(d.normalize().scale(s));
            level.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        if (t.hurt(damageSources().mobProjectile(this, this), SHOT_DAMAGE)) Hex.mark(t);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return BritishRegistry.sound("british_agent.hurt");
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return BritishRegistry.sound("british_agent.death");
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String pre = "animation." + BritishAgentAssets.RIG + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(pre + "idle"), walk = RawAnimation.begin().thenLoop(pre + "walk"),
                run = RawAnimation.begin().thenLoop(pre + "run");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            if (!state.isMoving()) return state.setAndContinue(idle);
            return state.setAndContinue(getTarget() != null ? run : walk);
        }));
        AnimationController<BritishAgentEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String clip : BritishAgentAssets.TRIGGERED) {
            action.triggerableAnim(clip, clip.equals("death") ? RawAnimation.begin().thenPlayAndHold(pre + clip) : RawAnimation.begin().thenPlay(pre + clip));
        }
        controllers.add(action);
    }

    @Override
    public void die(DamageSource source) {
        triggerAnim("action", "death");
        super.die(source);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }
}
