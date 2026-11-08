package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import software.bernie.geckolib.animation.AnimationState;

import java.util.EnumSet;

/**
 * A Thule officer (v0.42): a necromancer of the Society in a long black leather coat and peaked cap, a rod topped with the black
 * sun in his hand ({@link S11Assets#THULE_OFFICER}). A mini-elite, not a boss (never {@code #bosses}). He keeps his dead standing:
 * every so often, while fewer than {@link HandsMobRules#MAX_RAISED} soldiers stand near him, he chants over the freshest corpse
 * (or the bare ground) and a soldier climbs out of it. Between raisings he casts a bolt of the black sun (a hit and a withering)
 * and strikes with the rod up close. Undead, like his men.
 */
public class ThuleOfficerEntity extends HandsMob {

    private int raiseCooldown = 60, castCooldown = 40;
    private int raising = -1, casting = -1;
    private @Nullable BlockPos raiseAt;

    public ThuleOfficerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HandsMobRules.OFFICER_HEALTH)
                .add(Attributes.ARMOR, HandsMobRules.OFFICER_ARMOR)
                .add(Attributes.ATTACK_DAMAGE, HandsMobRules.OFFICER_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, HandsMobRules.OFFICER_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected S11Assets.Rig rig() {
        return S11Assets.THULE_OFFICER;
    }

    @Override
    protected String loop(AnimationState<HandsMob> state) {
        return state.isMoving() ? "walk" : "idle";
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Raise());
        goalSelector.addGoal(2, new Cast());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.9, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ThuleSoldierEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** How many of his soldiers stand near him. */
    public int soldiersNear() {
        return level().getEntitiesOfClass(ThuleSoldierEntity.class, getBoundingBox().inflate(HandsMobRules.RAISE_REACH), LivingEntity::isAlive).size();
    }

    /** Raises a soldier at {@code at} now (the goal's last beat; tests and previews too). @return the soldier, or null */
    public @Nullable ThuleSoldierEntity raiseAt(BlockPos at) {
        if (!(level() instanceof ServerLevel server)) return null;
        ThuleSoldierEntity s = HandsRegistry.THULE_SOLDIER.get().create(server);
        if (s == null) return null;
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0);
        s.finalizeSpawn(server, server.getCurrentDifficultyAt(at), MobSpawnType.MOB_SUMMONED, null);
        if (isPersistenceRequired()) s.setPersistenceRequired();
        s.setTarget(getTarget());
        server.addFreshEntity(s);
        s.rise();
        server.sendParticles(ParticleTypes.SCULK_SOUL, at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5, 14, 0.4, 0.2, 0.4, 0.02);
        return s;
    }

    /** Where to raise one: the freshest corpse in reach, else open ground beside him. */
    private BlockPos raiseSpot() {
        BlockPos corpse = ThuleCorpses.take(level(), blockPosition(), HandsMobRules.RAISE_REACH);
        if (corpse != null) return corpse;
        for (int i = 0; i < 8; i++) {
            BlockPos p = blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos q = p.above(dy);
                if (level().getBlockState(q).getCollisionShape(level(), q).isEmpty()
                        && level().getBlockState(q.above()).getCollisionShape(level(), q.above()).isEmpty()
                        && !level().getBlockState(q.below()).getCollisionShape(level(), q.below()).isEmpty()) return q;
            }
        }
        return blockPosition();
    }

    private class Raise extends Goal {
        Raise() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && raiseCooldown <= 0 && casting < 0 && soldiersNear() < HandsMobRules.MAX_RAISED;
        }

        @Override
        public boolean canContinueToUse() {
            return raising >= 0 && isAlive();
        }

        @Override
        public void start() {
            raising = 0;
            raiseAt = raiseSpot();
            getNavigation().stop();
            play("raise");
            playSound(HandsRegistry.thule("chant"), 1.2f, 0.8f);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos at = raiseAt;
            if (at != null) {
                getLookControl().setLookAt(Vec3.atCenterOf(at));
                if (level() instanceof ServerLevel server && raising % 4 == 0) {
                    server.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 3, 0.3, 0.05, 0.3, 0.01);
                }
            }
            if (++raising < HandsMobRules.RAISE_WINDUP) return;
            raising = -1;
            raiseCooldown = HandsMobRules.RAISE_COOLDOWN + random.nextInt(60);
            if (at != null) raiseAt(at);
        }

        @Override
        public void stop() {
            raising = -1;
            raiseAt = null;
        }
    }

    private class Cast extends Goal {
        Cast() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && castCooldown <= 0 && raising < 0 && distanceTo(t) <= HandsMobRules.CAST_RANGE && distanceTo(t) > 3
                    && hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return casting >= 0 && getTarget() != null;
        }

        @Override
        public void start() {
            casting = 0;
            getNavigation().stop();
            play("cast");
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30f, 30f);
            if (++casting < HandsMobRules.CAST_WINDUP) return;
            casting = -1;
            castCooldown = HandsMobRules.CAST_COOLDOWN + random.nextInt(40);
            bolt(t);
        }

        @Override
        public void stop() {
            casting = -1;
        }
    }

    /** The black sun's bolt: along the line to {@code t}, a hit and a withering if it still sees them. */
    private void bolt(LivingEntity t) {
        if (!(level() instanceof ServerLevel server)) return;
        Vec3 from = getEyePosition(), to = t.getEyePosition().subtract(0, 0.4, 0);
        Vec3 dir = to.subtract(from).normalize();
        double d = from.distanceTo(to);
        for (double s = 0.8; s < d; s += 0.6) {
            server.sendParticles(ParticleTypes.SQUID_INK, from.x + dir.x * s, from.y + dir.y * s, from.z + dir.z * s, 1, 0.02, 0.02, 0.02, 0);
        }
        if (!hasLineOfSight(t)) return;
        if (t.hurt(damageSources().indirectMagic(this, this), HandsMobRules.CAST_DAMAGE)) {
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, HandsMobRules.CAST_WITHER_TICKS, 0), this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (raiseCooldown > 0) raiseCooldown--;
        if (castCooldown > 0) castCooldown--;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        play("strike");
        return super.doHurtTarget(target);
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? HandsRegistry.thule("chant") : null;
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
