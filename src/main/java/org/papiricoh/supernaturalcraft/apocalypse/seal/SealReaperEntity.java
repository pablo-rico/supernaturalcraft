package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.allegiance.Kin;

/**
 * A reaper at a seal (v0.40, the Seals slice): bound by the demons, who murder them one by one to break it. They never fight;
 * a hunter's blows pass through them (only demons, the fallen and rebel angels, or the world itself can hurt them). Keep them
 * alive.
 */
public class SealReaperEntity extends SealCreature {

    public SealReaperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    @Override
    public String rig() {
        return SealAssets.REAPER;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Whether {@code by} can hurt a reaper: no hunter can; demons, the fallen, rebels and the world can. */
    public static boolean canHurt(Entity by) {
        if (by == null) return true;
        if (by instanceof Player) return false;
        return Kin.isDemon(by) || Kin.isFallen(by) || by instanceof RebelAngelEntity;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!canHurt(source.getEntity())) return false;
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive()) play("touch");
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        play("fade");
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        }
        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
