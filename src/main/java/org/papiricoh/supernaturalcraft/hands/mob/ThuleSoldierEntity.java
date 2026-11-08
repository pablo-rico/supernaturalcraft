package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import software.bernie.geckolib.animation.AnimationState;

import java.util.EnumSet;

/**
 * A Thule soldier (v0.42): one of the Society's dead, raised in a rotted grey field uniform with the black sun on his arm and a
 * rifle with a bayonet ({@link S11Assets#THULE_SOLDIER}). He shoots from a distance (an aim, then a crack and a puff of smoke: the
 * old rifle misses more the farther the mark) and stabs whoever closes in. Undead ({@code #minecraft:undead}): he needs no air and
 * holy things bite; a Thule officer raises him again from where he fell ({@link ThuleCorpses}).
 */
public class ThuleSoldierEntity extends HandsMob {

    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(ThuleSoldierEntity.class, EntityDataSerializers.BOOLEAN);

    private int riseTicks, shotCooldown = 40, aim = -1;

    public ThuleSoldierEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HandsMobRules.SOLDIER_HEALTH)
                .add(Attributes.ARMOR, HandsMobRules.SOLDIER_ARMOR)
                .add(Attributes.ATTACK_DAMAGE, HandsMobRules.SOLDIER_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, HandsMobRules.SOLDIER_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected S11Assets.Rig rig() {
        return S11Assets.THULE_SOLDIER;
    }

    @Override
    protected String loop(AnimationState<HandsMob> state) {
        if (aiming()) return "aim_idle";
        return state.isMoving() ? "walk" : "idle";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AIMING, false);
    }

    public boolean aiming() {
        return entityData.get(AIMING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Rifle());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ThuleSoldierEntity.class, ThuleOfficerEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Stands up from the grave: still for {@link HandsMobRules#RISE_TICKS} while its {@code rise} clip plays. */
    public void rise() {
        riseTicks = HandsMobRules.RISE_TICKS;
        play("rise");
        playSound(HandsRegistry.thule("rise"), 1.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    public boolean rising() {
        return riseTicks > 0;
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || riseTicks > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (riseTicks > 0) {
            riseTicks--;
            getNavigation().stop();
        }
        if (shotCooldown > 0) shotCooldown--;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        play("stab");
        return super.doHurtTarget(target);
    }

    /** The rifle: an aim, then one shot along the line of sight. */
    private class Rifle extends Goal {
        Rifle() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean inRange(LivingEntity t) {
            double d = distanceTo(t);
            return d >= HandsMobRules.SHOT_MIN && d <= HandsMobRules.SHOT_MAX && hasLineOfSight(t);
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && riseTicks <= 0 && shotCooldown <= 0 && inRange(t);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = getTarget();
            return aim >= 0 && t != null && t.isAlive();
        }

        @Override
        public void start() {
            aim = 0;
            entityData.set(AIMING, true);
            getNavigation().stop();
        }

        @Override
        public void stop() {
            aim = -1;
            entityData.set(AIMING, false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) {
                aim = -1;
                return;
            }
            getLookControl().setLookAt(t, 30f, 30f);
            if (++aim < HandsMobRules.AIM_TICKS) return;
            aim = -1;
            shotCooldown = HandsMobRules.SHOT_COOLDOWN + random.nextInt(30);
            shoot(t);
        }
    }

    private void shoot(LivingEntity t) {
        play("shoot");
        playSound(HandsRegistry.thule("shoot"), 1.6f, 0.9f + random.nextFloat() * 0.2f);
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 from = getEyePosition(), to = t.getEyePosition().subtract(0, 0.3, 0);
        Vec3 dir = to.subtract(from).normalize();
        server.sendParticles(ParticleTypes.SMOKE, from.x + dir.x * 0.9, from.y - 0.2 + dir.y * 0.9, from.z + dir.z * 0.9, 4, 0.05, 0.05, 0.05, 0.01);
        double d = from.distanceTo(to);
        for (double s = 1.5; s < d; s += 1.5) server.sendParticles(ParticleTypes.CRIT, from.x + dir.x * s, from.y + dir.y * s, from.z + dir.z * s, 1, 0, 0, 0, 0);
        if (hasLineOfSight(t) && random.nextDouble() < HandsMobRules.hitChance(d)) {
            t.hurt(damageSources().mobAttack(this), HandsMobRules.SHOT_DAMAGE);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) ThuleCorpses.fell(level(), blockPosition());
        super.die(source);
    }

    /** The dead need no air. */
    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return HandsRegistry.thule("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HandsRegistry.thule("death");
    }
}
