package org.papiricoh.supernaturalcraft.entity.alpha;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Alpha vampire (v0.39): the first of them, ancient and courtly, tall and gaunt in an old frock coat, a second row of fangs. A
 * vampire still (dead man's blood stuns him, only a beheading keeps him down), but faster than any, and with his own ways: a
 * blurring {@code lunge} from a distance, a {@code drain} that holds a hunter and drinks (healing him), and the {@code command}
 * that turns every vampire near on his prey (calling one more if they are few).
 */
public class AlphaVampireEntity extends VampireEntity implements Alpha {

    static final String ID = "alpha_vampire", PREFIX = "animation." + ID + ".";
    private final AlphaCore core = new AlphaCore(PactRules.Alpha.VAMPIRE);
    private int lunge = -1, drain = -1;
    private Vec3 lungeAt = Vec3.ZERO;

    public AlphaVampireEntity(EntityType<? extends AlphaVampireEntity> type, Level level) {
        super(type, level);
        xpReward = 150;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return VampireEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, AlphaRules.health(PactRules.Alpha.VAMPIRE))
                .add(Attributes.ATTACK_DAMAGE, AlphaRules.melee(PactRules.Alpha.VAMPIRE))
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public PactRules.Alpha alpha() {
        return PactRules.Alpha.VAMPIRE;
    }

    @Override
    public AlphaCore core() {
        return core;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        core.tick(this);
        if (isRemoved() || stunned() || downed()) {
            lunge = drain = -1;
            return;
        }
        long now = level.getGameTime();
        LivingEntity t = getTarget();
        if (lunge >= 0) tickLunge(level, t);
        else if (drain >= 0) tickDrain(level, t);
        else if (t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (core.ready("command", now)) command(level, t, now);
            else if (d >= AlphaRules.LUNGE_MIN && d <= AlphaRules.LUNGE_MAX && core.ready("lunge", now) && hasLineOfSight(t)) beginLunge(t, now);
            else if (d < 2.6 && core.ready("drain", now)) beginDrain(t, now);
        }
    }

    // --- the lunge ----------------------------------------------------------------------------------------------------------

    private void beginLunge(LivingEntity t, long now) {
        core.use("lunge", now, AlphaRules.LUNGE_COOLDOWN);
        lunge = AlphaRules.LUNGE_WINDUP;
        lungeAt = t.position();
        getNavigation().stop();
        triggerAnim("action", "lunge");
        AlphaCore.fx(this, 0, t, AlphaRules.LUNGE_WINDUP + 6);
    }

    private void tickLunge(ServerLevel level, LivingEntity t) {
        if (lunge-- > 0) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (t != null) getLookControl().setLookAt(t);
            return;
        }
        Vec3 to = (t != null && t.isAlive() ? t.position() : lungeAt).subtract(position());
        Vec3 dir = to.multiply(1, 0, 1).normalize();
        double dist = Math.min(to.horizontalDistance(), AlphaRules.LUNGE_MAX + 1);
        setDeltaMovement(dir.x * Math.min(2.4, dist * 0.32), 0.25, dir.z * Math.min(2.4, dist * 0.32));
        hurtMarked = true;
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_vampire_lunge"), SoundSource.HOSTILE, 1.4f, 1.0f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
        if (t != null && t.isAlive() && t.distanceTo(this) < dist + 2.5) {
            t.hurt(damageSources().mobAttack(this), AlphaRules.LUNGE_DAMAGE);
            t.addEffect(new MobEffectInstance(org.papiricoh.supernaturalcraft.registry.AllMobEffects.BLEEDING, BLEED_TICKS, 1), this);
        }
        lunge = -1;
    }

    // --- the draining bite --------------------------------------------------------------------------------------------------

    private void beginDrain(LivingEntity t, long now) {
        core.use("drain", now, AlphaRules.DRAIN_COOLDOWN);
        drain = AlphaRules.DRAIN_BEATS * AlphaRules.DRAIN_EVERY;
        triggerAnim("action", "drain");
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, drain + 10, 4), this);
        AlphaCore.fx(this, 1, t, drain);
    }

    private void tickDrain(ServerLevel level, LivingEntity t) {
        if (t == null || !t.isAlive() || t.distanceTo(this) > 4) {
            drain = -1;
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(t);
        if (drain % AlphaRules.DRAIN_EVERY == 0) {
            t.invulnerableTime = 0;
            if (t.hurt(damageSources().mobAttack(this), AlphaRules.DRAIN_DAMAGE)) heal(AlphaRules.DRAIN_DAMAGE);
            level.playSound(null, blockPosition(), AllSounds.VAMPIRE_BITE.get(), SoundSource.HOSTILE, 1.0f, 0.7f);
        }
        if (--drain <= 0) drain = -1;
    }

    // --- the command --------------------------------------------------------------------------------------------------------

    private void command(ServerLevel level, LivingEntity t, long now) {
        core.use("command", now, AlphaRules.COMMAND_COOLDOWN);
        triggerAnim("action", "command");
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_vampire_command"), SoundSource.HOSTILE, 2.0f, 1.0f);
        int nest = 0;
        for (VampireEntity v : level.getEntitiesOfClass(VampireEntity.class, getBoundingBox().inflate(AlphaRules.COMMAND_REACH),
                v -> v != this && v.isAlive() && !(v instanceof Alpha))) {
            v.setTarget(t);
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            v.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            nest++;
        }
        if (nest < 2) {
            VampireEntity v = AllEntities.VAMPIRE.get().create(level);
            if (v != null) {
                Vec3 at = position().add(random.nextDouble() * 6 - 3, 0, random.nextDouble() * 6 - 3);
                v.moveTo(at.x, getY(), at.z, random.nextFloat() * 360, 0);
                v.setTarget(t);
                level.addFreshEntity(v);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, getY() + 1, at.z, 20, 0.3, 0.8, 0.3, 0.02);
            }
        }
        AlphaCore.fx(this, 2, t, 30);
    }

    // --- dying, and the rest ------------------------------------------------------------------------------------------------

    /** Its hide: every blow scaled down ({@link AlphaCore#scaled}); its kind's own rules still apply on top. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, level().isClientSide ? amount : core.scaled(source, amount));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead) core.slain(this, source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        core.bar().removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        core.bar().removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        core.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        core.load(tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk"),
                run = RawAnimation.begin().thenLoop(PREFIX + "run"), stunnedLoop = RawAnimation.begin().thenLoop(PREFIX + "stunned");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (stunned() || downed()) return state.setAndContinue(stunnedLoop);
            if (state.isMoving()) return state.setAndContinue(getTarget() != null ? run : walk);
            return state.setAndContinue(idle);
        }));
        AnimationController<AlphaVampireEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : AlphaAssets.triggered(ID)) {
            action.triggerableAnim(clip, clip.equals("decapitated") ? RawAnimation.begin().thenPlayAndHold(PREFIX + clip)
                    : RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }
}
