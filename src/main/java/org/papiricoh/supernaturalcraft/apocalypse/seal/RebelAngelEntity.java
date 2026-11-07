package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * An angel who sides with Lucifer (v0.40, the Seals slice): Uriel's escort and the killers at the angel seals. It hunts hunters
 * and the Host's own angels alike (never demons: they are on the same side now), cuts with its blade and, now and then, smites
 * whoever stands close (a burst of grace that throws them back). An angel all the same ({@code #angels}): only an angel blade
 * kills it properly.
 */
public class RebelAngelEntity extends SealCreature {

    /** Ticks between smites, its reach and its blow. */
    public static final int SMITE_EVERY = 140;
    public static final double SMITE_REACH = 4;
    public static final float SMITE_DAMAGE = 6;
    private int smiteIn = SMITE_EVERY;

    public RebelAngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    @Override
    public String rig() {
        return SealAssets.REBEL;
    }

    @Override
    protected int variants() {
        return SealAssets.REBEL_VARIANTS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, RebelAngelEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AngelEntity.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) play("slash");
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && --smiteIn <= 0 && distanceToSqr(t) <= SMITE_REACH * SMITE_REACH) {
            smiteIn = SMITE_EVERY + random.nextInt(60);
            play("smite");
            level.playSound(null, blockPosition(), AllSounds.apocalypse("uriel.smite"), SoundSource.HOSTILE, 1.0f, 1.3f);
            level.sendParticles(AllParticles.GRACE.get(), t.getX(), t.getY() + 1, t.getZ(), 14, 0.3, 0.6, 0.3, 0.08);
            t.hurt(damageSources().mobAttack(this), SMITE_DAMAGE);
            Vec3 away = t.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0e-4) {
                away = away.normalize();
                t.push(away.x * 0.9, 0.35, away.z * 0.9);
                t.hurtMarked = true;
            }
        }
    }

    /** Its whole death clip plays ({@link SealAssets#CLIP_TICKS}): the wings of light burn out before it goes. */
    @Override
    protected void tickDeath() {
        if (++deathTime == 1) play("death");
        if (deathTime >= SealAssets.CLIP_TICKS.getOrDefault("death", 20) && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return sealEvent() == 0 && super.removeWhenFarAway(distance);
    }
}
