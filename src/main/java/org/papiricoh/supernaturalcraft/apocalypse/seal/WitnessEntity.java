package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.weapon.Holy;

/**
 * A Witness (v0.40, the Seals slice): the restless ghost of someone a hunter failed, risen with the seal. It goes for the hunter
 * the seal called (the event sets its target), its grasp chills (slowness) and now and then it wails (a moment of darkness for
 * whoever is close). Holy harm hurts it twice over. A Witness Ward ({@code SealRites}) keeps it off. It fades when its seal is
 * decided or, raised by an egg, at dawn.
 */
public class WitnessEntity extends SealCreature {

    /** Ticks between wails, the wail's reach and its darkness. */
    public static final int WAIL_EVERY = 160, WAIL_DARKNESS = 50;
    public static final double WAIL_REACH = 6;
    private int wailIn = WAIL_EVERY;

    public WitnessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    @Override
    public String rig() {
        return SealAssets.WITNESS;
    }

    @Override
    protected int variants() {
        return SealAssets.WITNESS_VARIANTS;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !SealRites.warded(e)));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (SealRites.warded(target)) {
            setTarget(null);
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            play("grasp");
            if (target instanceof LivingEntity l) l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, Holy.isHoly(source) ? amount * 2 : amount);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 4 == 0) level.sendParticles(AllParticles.WITNESS_WISP.get(), getX(), getY() + 1.2, getZ(), 1, 0.25, 0.5, 0.25, 0.01);
        if (getTarget() != null && SealRites.warded(getTarget())) setTarget(null);
        if (getTarget() != null && --wailIn <= 0) {
            wailIn = WAIL_EVERY + random.nextInt(60);
            play("wail");
            level.playSound(null, blockPosition(), AllSounds.apocalypse("seal.witness_wail"), SoundSource.HOSTILE, 1.2f, 0.9f + random.nextFloat() * 0.2f);
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(this) <= WAIL_REACH * WAIL_REACH && !p.isCreative() && !p.isSpectator() && !SealRites.warded(p)) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, WAIL_DARKNESS, 0));
                }
            }
        }
        // Daylight is no place for them: one raised by an egg fades at dawn (those an event raised go with it).
        if (sealEvent() == 0 && level.isDay() && level.canSeeSky(blockPosition()) && tickCount > 200) fade();
    }

    /** Fades away (no drops). */
    public void fade() {
        if (!(level() instanceof ServerLevel level)) return;
        level.sendParticles(AllParticles.WITNESS_WISP.get(), getX(), getY() + 1, getZ(), 16, 0.3, 0.8, 0.3, 0.03);
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? AllSounds.apocalypse("seal.witness_wail") : null;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return sealEvent() == 0 && super.removeWhenFarAway(distance);
    }
}
