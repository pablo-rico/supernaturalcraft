package org.papiricoh.supernaturalcraft.entity.chain;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.List;

/**
 * The Pit's rank and file ({@link ChainMobAssets}): black-eyed demons in other bodies, each with its own trade. They are
 * {@link BlackEyedDemon}s to the Chain (they can hold a rank), smoke out of their vessels and fear a devil's trap like any
 * lesser demon; only their blows differ.
 */
public final class ChainDemons {

    private ChainDemons() {
    }

    /** Shared: a melee brawler that also runs its trade from {@link #trade}. */
    public abstract static class Base extends BlackEyedDemon {
        protected final Windup windup = new Windup();
        protected int cooldown = 40;

        Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        protected abstract ChainMobAssets.Mob assets();

        @Override
        protected String animPrefix() {
            return assets().id();
        }

        @Override
        protected List<String> extraClips() {
            return assets().roleClips();
        }

        protected int hitTick(String clip) {
            return assets().hitTicks().getOrDefault(clip, 6);
        }

        protected double meleeSpeed() {
            return 1.1;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(3, new MeleeAttackGoal(this, meleeSpeed(), false));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            registerTargets();
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            windup.tick();
            if (cooldown > 0) cooldown--;
            LivingEntity foe = getTarget();
            if (foe == null || !foe.isAlive() || isSmoking() || isTrapped() || windup.busy() || cooldown > 0) return;
            trade(foe, distanceTo(foe));
        }

        /** Its trade, once the cooldown is up: start something and set {@link #cooldown}, or do nothing this tick. */
        protected abstract void trade(LivingEntity foe, double distance);

        /** Faster the higher its rank in the Chain, as the black-eyed demon's flick. */
        protected int scaled(int ticks) {
            return ticks * 4 / (Math.max(0, officerRank()) + 4);
        }

        protected void play(String clip) {
            triggerAnim("action", clip);
        }

        protected DamageSource blow() {
            return damageSources().mobAttack(this);
        }

        /** Strikes everything hostile to it within {@code radius} of {@code centre}. */
        protected void area(Vec3 centre, double radius, float damage, double lift) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius + 2),
                    e -> e != this && e.isAlive() && e.position().distanceTo(centre) <= radius && !Kin.sameSide(this, e))) {
                e.hurt(blow(), damage);
                Vec3 push = e.position().subtract(centre).multiply(1, 0, 1).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 0.6, lift, push.z * 0.6));
                e.hurtMarked = true;
            }
        }
    }

    /** A hulking possessed labourer: lumbers, then charges; up close it brings both fists down. */
    public static class Brute extends Base {
        private int charging;

        public Brute(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 20;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 70).add(Attributes.ATTACK_DAMAGE, 9)
                    .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                    .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1.0);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.BRUTE;
        }

        @Override
        protected float smokeChance() {
            return 0.3f;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (charging > 0 && getTarget() instanceof LivingEntity foe) {
                charging--;
                Vec3 to = foe.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.55);
                setDeltaMovement(to.x, getDeltaMovement().y, to.z);
                if (distanceTo(foe) < 1.8) {
                    charging = 0;
                    foe.hurt(blow(), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.3f);
                    foe.knockback(1.8, -to.x, -to.z);
                    foe.push(0, 0.4, 0);
                    foe.hurtMarked = true;
                }
            }
        }

        @Override
        protected void trade(LivingEntity foe, double distance) {
            if (distance < 3.2) {
                play("slam");
                cooldown = scaled(90);
                windup.start(hitTick("slam"), () -> {
                    area(position(), 3.5, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.45);
                    if (level() instanceof ServerLevel l) {
                        l.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.1, getZ(), 2, 1, 0, 1, 0);
                        l.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 0.6f);
                    }
                });
            } else if (distance > 6 && distance < 16 && getSensing().hasLineOfSight(foe)) {
                play("charge");
                cooldown = scaled(140);
                windup.start(6, () -> charging = 20);
            }
        }
    }

    /** A cult priest: keeps its distance, hurls burning sulfur and, when cornered, whirls its censer into a choking cloud. */
    public static class SulfurPriest extends Base {
        public SulfurPriest(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 14;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.ATTACK_DAMAGE, 4)
                    .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ARMOR, 1).add(Attributes.FOLLOW_RANGE, 32);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.SULFUR_PRIEST;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            // Keeps a few blocks between it and its mark.
            if (getTarget() instanceof LivingEntity foe && distanceTo(foe) < 5 && !windup.busy()) {
                Vec3 away = position().subtract(foe.position()).multiply(1, 0, 1).normalize().scale(5);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.15);
            }
        }

        @Override
        protected void trade(LivingEntity foe, double distance) {
            if (distance < 4.5) {
                play("smoke");
                cooldown = scaled(200);
                windup.start(hitTick("smoke"), () -> {
                    AreaEffectCloud cloud = new AreaEffectCloud(level(), getX(), getY(), getZ());
                    cloud.setRadius(3.5f);
                    cloud.setDuration(100);
                    cloud.setRadiusPerTick(-0.02f);
                    cloud.setParticle(AllParticles.DEMON_SMOKE.get());
                    cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60));
                    cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 60));
                    cloud.setOwner(this);
                    level().addFreshEntity(cloud);
                });
            } else if (distance < 22 && getSensing().hasLineOfSight(foe)) {
                play("cast");
                getLookControl().setLookAt(foe);
                cooldown = scaled(50);
                windup.start(hitTick("cast"), () -> {
                    if (foe.isAlive()) ChainMissile.shoot(AllEntities.SULFUR_BALL.get(), this, foe, 1.1f);
                });
            }
        }
    }

    /** A gaunt flayer: folds into black smoke and comes out behind its mark, knives first. */
    public static class Flayer extends Base {
        private int primed;

        public Flayer(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 14;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.ATTACK_DAMAGE, 7)
                    .add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ARMOR, 1).add(Attributes.FOLLOW_RANGE, 32);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.FLAYER;
        }

        @Override
        protected double meleeSpeed() {
            return 1.25;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && primed > 0 && target instanceof LivingEntity victim) {
                // Out of the smoke: the first cut lands twice.
                primed = 0;
                victim.hurt(blow(), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
                play("strike");
            }
            return hit;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (primed > 0) primed--;
        }

        @Override
        protected void trade(LivingEntity foe, double distance) {
            if (distance < 4 || distance > 18) return;
            play("vanish");
            cooldown = scaled(110);
            windup.start(hitTick("vanish"), () -> {
                if (!foe.isAlive() || !(level() instanceof ServerLevel l)) return;
                l.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.04);
                Vec3 behind = foe.position().subtract(Vec3.directionFromRotation(0, foe.getYRot()).scale(1.6));
                if (randomTeleport(behind.x, behind.y, behind.z, false)) {
                    l.sendParticles(AllParticles.DEMON_SMOKE.get(), getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.04);
                    getLookControl().setLookAt(foe, 180, 90);
                    primed = 40;
                }
            });
        }
    }

    /** The Pit's jailer: throws its hook on a chain, drags its mark in and pins it there. */
    public static class Tormentor extends Base {
        public Tormentor(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 16;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 45).add(Attributes.ATTACK_DAMAGE, 6)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ARMOR, 4).add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                    .add(Attributes.FOLLOW_RANGE, 32);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.TORMENTOR;
        }

        @Override
        protected void trade(LivingEntity foe, double distance) {
            if (distance < 5 || distance > 15 || !getSensing().hasLineOfSight(foe)) return;
            play("hook");
            getLookControl().setLookAt(foe);
            cooldown = scaled(120);
            windup.start(hitTick("hook"), () -> {
                if (foe.isAlive()) ChainMissile.shoot(AllEntities.TORMENTOR_HOOK.get(), this, foe, 1.4f);
            });
        }
    }
}
