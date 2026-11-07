package org.papiricoh.supernaturalcraft.entity.alpha;

import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The Alpha werewolf (v0.39): a hulking grey-silver wolf-man, a head taller than any of his kind, who never turns back. Only silver
 * puts him down for good (anything else and he breaks off, flees and mends, like his children). His ways: the {@code leap} onto a
 * hunter from far off (a ring of earth round where he lands), the {@code slam} of both arms when one stands close, and the
 * {@code howl} that brings his pack running.
 */
public class AlphaWerewolfEntity extends WerewolfEntity implements Alpha {

    static final String ID = "alpha_werewolf", PREFIX = "animation." + ID + ".";
    private final AlphaCore core = new AlphaCore(PactRules.Alpha.WEREWOLF);
    private int leap = -1, slam = -1;
    private boolean airborne;

    public AlphaWerewolfEntity(EntityType<? extends AlphaWerewolfEntity> type, Level level) {
        super(type, level);
        xpReward = 160;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WerewolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, AlphaRules.health(PactRules.Alpha.WEREWOLF))
                .add(Attributes.ATTACK_DAMAGE, AlphaRules.melee(PactRules.Alpha.WEREWOLF))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    public PactRules.Alpha alpha() {
        return PactRules.Alpha.WEREWOLF;
    }

    @Override
    public AlphaCore core() {
        return core;
    }

    /** The first of them is always the wolf. */
    @Override
    public boolean nightNow() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        core.tick(this);
        if (isRemoved() || fleeing()) {
            leap = slam = -1;
            airborne = false;
            return;
        }
        long now = level.getGameTime();
        LivingEntity t = getTarget();
        if (leap >= 0) tickLeap(level, t);
        else if (airborne) tickLanding(level);
        else if (slam >= 0) tickSlam(level);
        else if (t != null && t.isAlive()) {
            double d = distanceTo(t);
            if (core.ready("howl", now) && (getHealth() < getMaxHealth() * 0.75f || tickCount > 100)) howl(level, t, now);
            else if (d >= AlphaRules.LEAP_MIN && d <= AlphaRules.LEAP_MAX && core.ready("leap", now)) beginLeap(t, now);
            else if (d < AlphaRules.SLAM_RADIUS && core.ready("slam", now)) beginSlam(now);
        }
    }

    // --- the leap ------------------------------------------------------------------------------------------------------------

    private void beginLeap(LivingEntity t, long now) {
        core.use("leap", now, AlphaRules.LEAP_COOLDOWN);
        leap = 10;
        getNavigation().stop();
        triggerAnim("action", "leap");
        AlphaCore.fx(this, 3, t, 30);
    }

    private void tickLeap(ServerLevel level, LivingEntity t) {
        if (leap-- > 0) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (t != null) getLookControl().setLookAt(t);
            return;
        }
        leap = -1;
        if (t == null) return;
        Vec3 to = t.position().subtract(position());
        double h = to.horizontalDistance();
        Vec3 dir = to.multiply(1, 0, 1).normalize();
        double v = Math.min(1.6, 0.18 * h + 0.4);
        setDeltaMovement(dir.x * v, 0.75 + Math.max(0, to.y) * 0.08, dir.z * v);
        hurtMarked = true;
        airborne = true;
    }

    private void tickLanding(ServerLevel level) {
        if (!onGround() || getDeltaMovement().y > 0.01) return;
        airborne = false;
        ring(level, AlphaRules.LEAP_RADIUS, AlphaRules.LEAP_DAMAGE);
    }

    // --- the slam ------------------------------------------------------------------------------------------------------------

    private void beginSlam(long now) {
        core.use("slam", now, AlphaRules.SLAM_COOLDOWN);
        slam = 12;
        getNavigation().stop();
        triggerAnim("action", "slam");
    }

    private void tickSlam(ServerLevel level) {
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (--slam > 0) return;
        slam = -1;
        ring(level, AlphaRules.SLAM_RADIUS, AlphaRules.SLAM_DAMAGE);
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_werewolf_slam"), SoundSource.HOSTILE, 1.8f, 1.0f);
        AlphaCore.fx(this, 5, null, 12);
    }

    /** Everyone but his own kind within {@code radius}, struck and thrown back. */
    private void ring(ServerLevel level, float radius, float damage) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(radius, 2, radius),
                e -> e != this && e.isAlive() && !(e instanceof WerewolfEntity))) {
            if (e.position().subtract(position()).horizontalDistance() > radius) continue;
            if (e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator())) continue;
            e.hurt(damageSources().mobAttack(this), damage);
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 1.1, 0.45, away.z * 1.1);
            e.hurtMarked = true;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 40, radius / 2, 0.1,
                radius / 2, 0.2);
    }

    // --- the howl ------------------------------------------------------------------------------------------------------------

    private void howl(ServerLevel level, LivingEntity t, long now) {
        core.use("howl", now, AlphaRules.HOWL_COOLDOWN);
        triggerAnim("action", "howl");
        level.playSound(null, blockPosition(), AllSounds.purgatory("alpha_werewolf_howl"), SoundSource.HOSTILE, 4.0f, 0.9f);
        int pack = level.getEntitiesOfClass(WerewolfEntity.class, getBoundingBox().inflate(32), w -> w != this && w.isAlive()).size();
        for (int i = 0; i < AlphaRules.HOWL_PACK && pack < AlphaRules.PACK_CAP; i++, pack++) {
            WerewolfEntity w = AllEntities.WEREWOLF.get().create(level);
            if (w == null) continue;
            double a = random.nextDouble() * Math.PI * 2, d = 10 + random.nextDouble() * 4;
            Vec3 at = AlphaSpawns.ground(level, position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            if (at == null) continue;
            w.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            w.forceForm(true);
            w.setTarget(t);
            level.addFreshEntity(w);
        }
        for (WerewolfEntity w : level.getEntitiesOfClass(WerewolfEntity.class, getBoundingBox().inflate(32), w -> w != this && w.isAlive())) {
            w.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            if (w.getTarget() == null) w.setTarget(t);
        }
        AlphaCore.fx(this, 4, t, 40);
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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
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
                run = RawAnimation.begin().thenLoop(PREFIX + "run");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (state.isMoving()) return state.setAndContinue(getTarget() != null || fleeing() ? run : walk);
            return state.setAndContinue(idle);
        }));
        AnimationController<AlphaWerewolfEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : AlphaAssets.triggered(ID)) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }
}
