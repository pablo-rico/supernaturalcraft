package org.papiricoh.supernaturalcraft.entity.chain;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;
import org.papiricoh.supernaturalcraft.entity.angel.AngelEntity;
import org.papiricoh.supernaturalcraft.entity.angel.FallenAngelEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The Host's rank and file and the Grigori ({@link ChainMobAssets}): angels in vessels of their own, each with its own duty.
 * To everything else they are angels ({@link AngelEntity}: the blink, the shadow of the wings, only an angel blade truly
 * kills them); a Grigori is one of the fallen.
 */
public final class ChainAngels {

    private ChainAngels() {
    }

    private static List<String> clips(ChainMobAssets.Mob mob) {
        return Stream.concat(AngelAssets.CLIPS.stream().filter(ChainMobAssets.ANGEL_CLIPS::contains), mob.roleClips().stream()).toList();
    }

    /** Shared plumbing: its own rig, a windup and a cooldown for its duty. */
    public abstract static class Base extends AngelEntity {
        protected final Windup windup = new Windup();
        protected int cooldown = 40;

        Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 16;
        }

        protected abstract ChainMobAssets.Mob assets();

        @Override
        protected String animPrefix() {
            return assets().id();
        }

        @Override
        protected List<String> clips() {
            return ChainAngels.clips(assets());
        }

        @Override
        protected int vessels() {
            return 1;
        }

        protected int hitTick(String clip) {
            return assets().hitTicks().getOrDefault(clip, 6);
        }

        protected int scaled(int ticks) {
            return ticks * 4 / (Math.max(0, officerRank()) + 4);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            windup.tick();
            if (cooldown > 0) cooldown--;
            LivingEntity foe = getTarget();
            if (foe == null || !foe.isAlive() || windup.busy() || cooldown > 0) return;
            duty(foe, distanceTo(foe));
        }

        protected abstract void duty(LivingEntity foe, double distance);

        protected void play(String clip) {
            triggerAnim("action", clip);
            flashWings();
        }

        /** Strikes everything not of Heaven's side within {@code radius} of {@code centre}. */
        protected void area(Vec3 centre, double radius, float damage, double lift) {
            DamageSource smite = AllDamageTypes.source(level(), AllDamageTypes.SMITE, this);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius + 2),
                    e -> e != this && e.isAlive() && e.position().distanceTo(centre) <= radius && !Kin.sameSide(this, e))) {
                e.hurt(smite, damage);
                Vec3 push = e.position().subtract(centre).multiply(1, 0, 1).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(push.x * 0.5, lift, push.z * 0.5));
                e.hurtMarked = true;
            }
        }
    }

    /** A soldier of the Host: a tower shield raised against blows from the front, and a wide cut of its sword. */
    public static class Templar extends Base {
        public static final float BLOCKED = 0.2f;
        private int blocking;

        public Templar(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return AngelEntity.createAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.ARMOR, 8)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.TEMPLAR;
        }

        @Override
        protected Set<String> heldClips() {
            return Set.of("smitten", "block");
        }

        public boolean blocking() {
            return blocking > 0;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            Vec3 from = source.getSourcePosition();
            if (blocking() && from != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
                Vec3 to = from.subtract(position()).multiply(1, 0, 1).normalize();
                if (to.dot(Vec3.directionFromRotation(0, getYRot())) > 0.4) {
                    level().playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1, 0.8f);
                    amount *= BLOCKED;
                }
            }
            return super.hurt(source, amount);
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (blocking > 0 && --blocking == 0) stopTriggeredAnim("action", "block");
        }

        @Override
        protected void duty(LivingEntity foe, double distance) {
            if (distance < 3.2) {
                play("slash");
                cooldown = scaled(70);
                windup.start(hitTick("slash"), () -> {
                    Vec3 front = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.6));
                    area(front, 2.4, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 0.15);
                });
            } else if (distance < 12) {
                play("block");
                blocking = 40;
                cooldown = scaled(110);
            }
        }
    }

    /** A herald: keeps away and hurls lances of light. */
    public static class Herald extends Base {
        public Herald(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return AngelEntity.createAttributes().add(Attributes.MAX_HEALTH, 34).add(Attributes.ARMOR, 2)
                    .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 6);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.HERALD;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (getTarget() instanceof LivingEntity foe && distanceTo(foe) < 6 && !windup.busy()) {
                Vec3 away = position().subtract(foe.position()).multiply(1, 0, 1).normalize().scale(6);
                getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.2);
            }
        }

        @Override
        protected void duty(LivingEntity foe, double distance) {
            if (distance < 4 || distance > 26 || !getSensing().hasLineOfSight(foe)) return;
            play("throw");
            getLookControl().setLookAt(foe);
            cooldown = scaled(55);
            windup.start(hitTick("throw"), () -> {
                if (foe.isAlive()) ChainMissile.shoot(AllEntities.LIGHT_LANCE.get(), this, foe, 2.2f);
            });
        }
    }

    /** A choirmaster: its hymn mends the angels around it and rings in the ears of their foes. */
    public static class Choirmaster extends Base {
        public static final double HYMN = 10;

        public Choirmaster(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return AngelEntity.createAttributes().add(Attributes.MAX_HEALTH, 36).add(Attributes.ARMOR, 2)
                    .add(Attributes.ATTACK_DAMAGE, 4);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.CHOIRMASTER;
        }

        @Override
        protected void duty(LivingEntity foe, double distance) {
            if (distance > HYMN) return;
            play("hymn");
            cooldown = scaled(160);
            windup.start(hitTick("hymn"), () -> {
                if (!(level() instanceof ServerLevel l)) return;
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HYMN), LivingEntity::isAlive)) {
                    if (Kin.sameSide(this, e)) {
                        e.heal(6);
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0), this);
                    } else if (e.distanceTo(this) < 6) {
                        e.hurt(l.damageSources().sonicBoom(this), 4);
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80), this);
                    }
                }
                l.sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.2, getZ(), 12, 1.2, 0.4, 1.2, 1);
                l.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 30, HYMN / 3, 0.5, HYMN / 3, 0.02);
                l.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2f, 1.6f);
            });
        }
    }

    /** A dominion: slow and towering, its hammer of judgement shakes the ground around it. */
    public static class Dominion extends Base {
        public Dominion(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 24;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return AngelEntity.createAttributes().add(Attributes.MAX_HEALTH, 80).add(Attributes.ARMOR, 10)
                    .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                    .add(Attributes.STEP_HEIGHT, 1.0);
        }

        @Override
        protected ChainMobAssets.Mob assets() {
            return ChainMobAssets.DOMINION;
        }

        @Override
        protected boolean canBlink() {
            return false;
        }

        @Override
        protected void duty(LivingEntity foe, double distance) {
            if (distance > 5.5) return;
            play("judgement");
            cooldown = scaled(140);
            windup.start(hitTick("judgement"), () -> {
                Vec3 front = position().add(Vec3.directionFromRotation(0, getYRot()).scale(1.5));
                area(front, 4, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8f, 0.6);
                if (level() instanceof ServerLevel l) {
                    l.sendParticles(ParticleTypes.FLASH, front.x, front.y + 0.2, front.z, 1, 0, 0, 0, 0);
                    l.sendParticles(ParticleTypes.END_ROD, front.x, front.y + 0.2, front.z, 40, 2, 0.1, 2, 0.05);
                    l.playSound(null, blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.8f, 1.4f);
                }
            });
        }
    }

    /** A Grigori, a Watcher gone rogue: fades from sight, and where its hand closes it drinks the soul. */
    public static class Grigori extends FallenAngelEntity {
        private final Windup windup = new Windup();
        private int cooldown = 60;

        public Grigori(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            xpReward = 16;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return FallenAngelEntity.createAttributes().add(Attributes.MAX_HEALTH, 34).add(Attributes.ATTACK_DAMAGE, 7)
                    .add(Attributes.MOVEMENT_SPEED, 0.3);
        }

        @Override
        protected String animPrefix() {
            return ChainMobAssets.GRIGORI.id();
        }

        @Override
        protected List<String> clips() {
            return ChainAngels.clips(ChainMobAssets.GRIGORI);
        }

        @Override
        protected int vessels() {
            return 1;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity victim && random.nextFloat() < 0.35f) {
                triggerAnim("action", "drain");
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100), this);
                victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1), this);
                heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5f);
            }
            return hit;
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            windup.tick();
            if (cooldown > 0) cooldown--;
            LivingEntity foe = getTarget();
            if (foe == null || cooldown > 0 || windup.busy() || distanceTo(foe) > 14) return;
            triggerAnim("action", "fade");
            cooldown = 180 * 4 / (Math.max(0, officerRank()) + 4);
            windup.start(ChainMobAssets.GRIGORI.hitTicks().get("fade"), () -> addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 70)));
        }
    }
}
