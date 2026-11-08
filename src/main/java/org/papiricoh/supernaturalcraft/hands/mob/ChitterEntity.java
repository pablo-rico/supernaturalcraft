package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import software.bernie.geckolib.animation.AnimationState;

import java.util.EnumSet;

/**
 * A Chitter (v0.42): a human hollowed out by the thing that came to live in them, a mouth of tiny teeth and a neck that will not
 * keep still ({@link S11Assets#CHITTER}). They wake every so often ({@link ChittersRules}) and hunt in packs from the deep caves:
 * the first to see prey screeches and the pack comes running; they crawl when they charge, pounce from a few blocks off and bite.
 * Fire burns the thing out of them twice as fast. A pack that rises somewhere new weaves a nest there ({@link ChitterNests}).
 */
public class ChitterEntity extends HandsMob {

    private int pounceCooldown = 40, screechCooldown;
    private boolean pouncing;

    public ChitterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HandsMobRules.CHITTER_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, HandsMobRules.CHITTER_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, HandsMobRules.CHITTER_SPEED)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.SAFE_FALL_DISTANCE, 6);
    }

    @Override
    protected S11Assets.Rig rig() {
        return S11Assets.CHITTER;
    }

    @Override
    protected String loop(AnimationState<HandsMob> state) {
        if (!state.isMoving()) return "idle";
        return getTarget() != null || isAggressive() ? "crawl" : "walk";
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Pounce());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity old = getTarget();
        super.setTarget(target);
        if (target != null && old == null && !level().isClientSide && screechCooldown <= 0) screech(target);
    }

    /** Calls the pack onto {@code prey}. */
    public void screech(LivingEntity prey) {
        screechCooldown = 200;
        play("screech");
        playSound(HandsRegistry.chitter("screech"), 1.4f, 0.9f + random.nextFloat() * 0.2f);
        for (ChitterEntity c : level().getEntitiesOfClass(ChitterEntity.class, getBoundingBox().inflate(HandsMobRules.SCREECH_CALL),
                c -> c != this && c.isAlive() && c.getTarget() == null)) {
            c.screechCooldown = 200;
            c.setTarget(prey);
        }
    }

    private class Pounce extends Goal {
        Pounce() {
            setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            if (t == null || !t.isAlive() || !onGround() || pounceCooldown > 0) return false;
            double d = distanceTo(t);
            return d >= HandsMobRules.POUNCE_MIN && d <= HandsMobRules.POUNCE_MAX && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (t == null) return;
            pounceCooldown = HandsMobRules.POUNCE_COOLDOWN + random.nextInt(30);
            Vec3 d = t.position().subtract(position());
            Vec3 flat = new Vec3(d.x, 0, d.z).normalize().scale(Math.min(1.1, d.horizontalDistance() * 0.2));
            setDeltaMovement(flat.x, 0.42 + Math.max(0, d.y) * 0.08, flat.z);
            pouncing = true;
            play("pounce");
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (pounceCooldown > 0) pounceCooldown--;
        if (screechCooldown > 0) screechCooldown--;
        if (pouncing) {
            LivingEntity t = getTarget();
            if (t != null && distanceToSqr(t) < 2.2 * 2.2) {
                pouncing = false;
                t.hurt(damageSources().mobAttack(this), HandsMobRules.POUNCE_DAMAGE);
            } else if (onGround() && getDeltaMovement().y <= 0) {
                pouncing = false;
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        play("bite");
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) amount *= HandsMobRules.FIRE_MULTIPLIER;
        return super.hurt(source, amount);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        // A pack that rises somewhere new (in the deep, or on a case) weaves its nest there.
        if ((reason == MobSpawnType.EVENT || reason == MobSpawnType.NATURAL) && level instanceof ServerLevel server) {
            ChitterNests.weave(server, blockPosition(), random);
        }
        return out;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return HandsRegistry.chitter("chitter");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HandsRegistry.chitter("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HandsRegistry.chitter("death");
    }

    public static boolean canSpawn(EntityType<ChitterEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return reason != MobSpawnType.NATURAL || Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
