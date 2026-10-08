package org.papiricoh.supernaturalcraft.entity.allegiance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A soldier of the Host who answers a General of the Host (an angel player of rank IV, v0.13): follows its owner, fights
 * what they fight (and what fights them), hunts demons near them, and goes back to Heaven after {@link #LIFETIME}. It
 * never turns on a player. Drawn exactly as a {@link HostAngelEntity} (same model, same {@code action} clips).
 */
public class HostAllyEntity extends HostAngelEntity {

    public static final int LIFETIME = 1200;
    private static final double LEASH = 48;

    private @Nullable UUID ownerId;
    private int life = LIFETIME;

    public HostAllyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    /** One soldier for {@code owner} at {@code at} (a captain wears the plume and has more health). */
    public static @Nullable HostAllyEntity summon(ServerLevel level, Player owner, Vec3 at, boolean captain) {
        HostAllyEntity ally = AllEntities.HOST_ALLY.get().create(level);
        if (ally == null) return null;
        ally.ownerId = owner.getUUID();
        ally.setVessel(level.random.nextInt(VESSELS));
        ally.setCaptain(captain);
        ally.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        level.addFreshEntity(ally);
        level.sendParticles(AllParticles.GRACE.get(), at.x, at.y + 1, at.z, 20, 0.3, 0.8, 0.3, 0.05);
        return ally;
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
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new FollowOwner());
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerFight(true));
        targetSelector.addGoal(2, new OwnerFight(false));
        targetSelector.addGoal(3, new HurtByTargetGoal(this, Player.class));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !(e instanceof Player) && Kin.isDemon(e) && !e.getType().is(AllTags.Entities.BOSSES)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Player) && !(target instanceof HostAllyEntity) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return (ownerId != null && ownerId.equals(other.getUUID())) || other instanceof HostAllyEntity || super.isAlliedTo(other);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", "slash");
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Player owner = owner();
        if (--life <= 0 || owner == null || !owner.isAlive() || owner.distanceToSqr(this) > LEASH * LEASH) dismiss();
    }

    /** Back to Heaven in a burst of light. */
    public void dismiss() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 24, 0.3, 0.6, 0.3, 0.06);
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
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        life = tag.contains("Life") ? tag.getInt("Life") : LIFETIME;
    }

    /** Keeps near its general; far behind, it is simply at their side. */
    private class FollowOwner extends Goal {
        FollowOwner() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            return o != null && getTarget() == null && o.distanceToSqr(HostAllyEntity.this) > 16;
        }

        @Override
        public void tick() {
            Player o = owner();
            if (o == null) return;
            if (o.distanceToSqr(HostAllyEntity.this) > 24 * 24) {
                teleportTo(o.getX(), o.getY(), o.getZ());
                getNavigation().stop();
            } else {
                getNavigation().moveTo(o, 1.2);
            }
        }
    }

    /** Goes for whatever last hurt its general ({@code defend}) or whatever its general last struck. */
    private class OwnerFight extends TargetGoal {
        private final boolean defend;
        private int seen;
        private @Nullable LivingEntity pick;

        OwnerFight(boolean defend) {
            super(HostAllyEntity.this, false);
            this.defend = defend;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player o = owner();
            if (o == null) return false;
            pick = defend ? o.getLastHurtByMob() : o.getLastHurtMob();
            int stamp = defend ? o.getLastHurtByMobTimestamp() : o.getLastHurtMobTimestamp();
            return pick != null && stamp != seen && canAttack(pick, TargetingConditions.DEFAULT) && HostAllyEntity.this.canAttack(pick);
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
