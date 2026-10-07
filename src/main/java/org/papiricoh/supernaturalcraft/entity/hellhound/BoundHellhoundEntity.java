package org.papiricoh.supernaturalcraft.entity.hellhound;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A hellhound answering Lilith's Whistle: for a minute it runs at its holder's side, always seen,
 * and goes for whatever hurts them or whatever they strike. It never turns on a player.
 */
public class BoundHellhoundEntity extends HellhoundEntity {

    public static final int LIFETIME = 1200;
    private static final double LEASH = 48;

    private @Nullable UUID ownerId;
    private int life = LIFETIME;

    public BoundHellhoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static @Nullable BoundHellhoundEntity call(ServerLevel level, Player owner, double x, double y, double z) {
        BoundHellhoundEntity hound = AllEntities.BOUND_HELLHOUND.get().create(level);
        if (hound == null) return null;
        hound.ownerId = owner.getUUID();
        hound.moveTo(x, y, z, owner.getYRot(), 0);
        hound.reveal(40);
        level.addFreshEntity(hound);
        return hound;
    }

    public @Nullable Player owner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    public @Nullable UUID ownerId() {
        return ownerId;
    }

    public int life() {
        return life;
    }

    /** Test hook. */
    public void setLife(int ticks) {
        life = ticks;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.4, true));
        goalSelector.addGoal(5, new FollowOwner());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerFight(true));
        targetSelector.addGoal(2, new OwnerFight(false));
        targetSelector.addGoal(3, new HurtByTargetGoal(this, Player.class));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player) && !(target instanceof BoundHellhoundEntity) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return (ownerId != null && ownerId.equals(other.getUUID())) || super.isAlliedTo(other);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (tickCount % 20 == 0) reveal(40);
        Player owner = owner();
        if (--life <= 0 || owner == null || !owner.isAlive() || owner.distanceToSqr(this) > LEASH * LEASH) dismiss();
    }

    /** Back where it came from, in a puff of smoke. */
    public void dismiss() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 20, 0.4, 0.3, 0.4, 0.02);
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        life = tag.contains("Life") ? tag.getInt("Life") : LIFETIME;
    }

    /** Keeps close to its holder; too far behind, it is simply at their side. */
    private class FollowOwner extends Goal {
        FollowOwner() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            return o != null && getTarget() == null && o.distanceToSqr(BoundHellhoundEntity.this) > 36;
        }

        @Override
        public void tick() {
            Player o = owner();
            if (o == null) return;
            if (o.distanceToSqr(BoundHellhoundEntity.this) > 24 * 24) {
                teleportTo(o.getX(), o.getY(), o.getZ());
                getNavigation().stop();
            } else {
                getNavigation().moveTo(o, 1.3);
            }
        }
    }

    /** Goes for whatever last hurt its holder ({@code defend}) or whatever its holder last struck. */
    private class OwnerFight extends TargetGoal {
        private final boolean defend;
        private int seen;
        private @Nullable LivingEntity pick;

        OwnerFight(boolean defend) {
            super(BoundHellhoundEntity.this, false);
            this.defend = defend;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            if (o == null) return false;
            pick = defend ? o.getLastHurtByMob() : o.getLastHurtMob();
            int stamp = defend ? o.getLastHurtByMobTimestamp() : o.getLastHurtMobTimestamp();
            return pick != null && stamp != seen && canAttack(pick, TargetingConditions.DEFAULT) && BoundHellhoundEntity.this.canAttack(pick);
        }

        @Override
        public void start() {
            mob.setTarget(pick);
            Player o = owner();
            if (o != null) seen = defend ? o.getLastHurtByMobTimestamp() : o.getLastHurtMobTimestamp();
            super.start();
        }
    }
}
