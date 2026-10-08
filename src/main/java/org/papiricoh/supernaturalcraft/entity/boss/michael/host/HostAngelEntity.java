package org.papiricoh.supernaturalcraft.entity.boss.michael.host;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A soldier of the Host of Heaven: an angel in its vessel (a dark suit, a trench coat or fatigues, {@link #vessel()}),
 * with a light breastplate, an angel blade and a tower shield. Each company has a captain (helm, plume, wings open).
 *
 * <p>Sent by Michael ({@link #master()}), a soldier holds its slot in its company's {@link HostFormation} (Michael keeps
 * one per company and gives the orders): a shield wall between him and the hunters (blows from the front glance off the
 * shields), a wedge that charges, a ring round a lone hunter. Hunters with Heaven's mark are hunted first. When its
 * captain falls the company breaks: slower, no formation, every soldier for itself. Without a master (an egg) it is a plain
 * melee mob.
 */
public class HostAngelEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> CAPTAIN = SynchedEntityData.defineId(HostAngelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> VESSEL = SynchedEntityData.defineId(HostAngelEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> STANCE = SynchedEntityData.defineId(HostAngelEntity.class, EntityDataSerializers.BYTE);
    public static final int VESSELS = 3;
    /** What it is doing, for its clips: holding a slot, walling up, broken. */
    public static final byte STANCE_FORMED = 0, STANCE_WALL = 1, STANCE_BROKEN = 2;
    public static final float CAPTAIN_HEALTH = 90, SOLDIER_HEALTH = 40;
    /** Damage taken through a raised shield wall from the front, as a share. */
    public static final float WALL_SHARE = 0.25f;
    private static final ResourceLocation PACE = SupernaturalCraft.asResource("host_pace");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID master;
    private int company, slot, companySize = 1, swingCooldown;
    private boolean hadMaster;

    public HostAngelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, SOLDIER_HEALTH)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAPTAIN, false);
        builder.define(VESSEL, (byte) 0);
        builder.define(STANCE, STANCE_FORMED);
    }

    public boolean isCaptain() {
        return entityData.get(CAPTAIN);
    }

    public void setCaptain(boolean captain) {
        entityData.set(CAPTAIN, captain);
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(captain ? CAPTAIN_HEALTH : SOLDIER_HEALTH);
            setHealth(getMaxHealth());
        }
    }

    /** Which vessel it wears: 0 a dark suit, 1 a trench coat, 2 a soldier's fatigues. */
    public int vessel() {
        return entityData.get(VESSEL);
    }

    public void setVessel(int vessel) {
        entityData.set(VESSEL, (byte) Math.floorMod(vessel, VESSELS));
    }

    public byte stance() {
        return entityData.get(STANCE);
    }

    private void setStance(byte stance) {
        if (stance() != stance) entityData.set(STANCE, stance);
    }

    public void setMaster(UUID master) {
        this.master = master;
        hadMaster = true;
    }

    public @Nullable UUID master() {
        return master;
    }

    /** Puts it in company {@code company}, slot {@code slot} of {@code size}. */
    public void enlist(int company, int slot, int size) {
        this.company = company;
        this.slot = slot;
        this.companySize = Math.max(1, size);
    }

    public int company() {
        return company;
    }

    public int slot() {
        return slot;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Free (no master, or the company broken): a plain melee soldier.
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return free() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return free() && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, p -> !org.papiricoh.supernaturalcraft.allegiance.Kin.isAngel(p)) {
            @Override
            public boolean canUse() {
                return master == null && super.canUse();
            }
        });
    }

    private @Nullable MichaelEntity michael() {
        if (master == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(master) instanceof MichaelEntity m && m.isAlive() ? m : null;
    }

    /** Whether it fights for itself: no master, or its captain has fallen. */
    public boolean free() {
        if (master == null) return true;
        MichaelEntity m = michael();
        return m == null || m.formation(company).disordered();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (swingCooldown > 0) swingCooldown--;
        if (master == null) return;
        MichaelEntity m = michael();
        if (m == null || m.state() == MichaelEntity.DYING) {
            // Its general is gone: it burns out.
            if (hadMaster && level() instanceof ServerLevel level) {
                level.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                discard();
            }
            return;
        }
        HostFormation formation = m.formation(company);
        pace(formation.speed());
        LivingEntity foe = m.hostTarget(this);
        setTarget(foe);
        if (foe == null) return;
        if (formation.disordered()) {
            setStance(STANCE_BROKEN);
            return;
        }
        double[] at = formation.slot(slot, companySize);
        if (at == null) return;
        Vec3 slotPos = slotPosition(m, foe, formation.order(), at);
        double far = slotPos.distanceToSqr(position());
        boolean charging = formation.order() == HostFormation.Order.CHARGE;
        if (far > 1.2 && !(charging && distanceToSqr(foe) < 2.5 * 2.5)) {
            getNavigation().moveTo(slotPos.x, slotPos.y, slotPos.z, 1.0);
        } else {
            getNavigation().stop();
        }
        getLookControl().setLookAt(foe, 30, 30);
        setStance(formation.order() == HostFormation.Order.SHIELD_WALL && far < 4 ? STANCE_WALL : STANCE_FORMED);
        if (distanceToSqr(foe) < reach() * reach() && swingCooldown <= 0) swing(foe, charging);
    }

    /** Where its slot is now: the formation's frame faces from Michael toward the foe. */
    static Vec3 slotPosition(MichaelEntity m, LivingEntity foe, HostFormation.Order order, double[] slot) {
        Vec3 forward = foe.position().subtract(m.position()).multiply(1, 0, 1);
        if (forward.lengthSqr() < 0.01) forward = new Vec3(0, 0, 1);
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        Vec3 origin = switch (order) {
            // The wall stands a few blocks short of the hunters, between them and him.
            case SHIELD_WALL -> foe.position().subtract(forward.scale(Math.min(4, Math.max(1.5, foe.distanceTo(m) * 0.35))));
            case CHARGE, ENCIRCLE -> foe.position();
        };
        return origin.add(right.scale(slot[0])).add(forward.scale(slot[1]));
    }

    private double reach() {
        return 2.4;
    }

    private void pace(float multiplier) {
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        AttributeModifier current = speed.getModifier(PACE);
        double want = multiplier - 1;
        if (current != null && Math.abs(current.amount() - want) < 1e-4) return;
        speed.removeModifier(PACE);
        if (Math.abs(want) > 1e-4) speed.addTransientModifier(new AttributeModifier(PACE, want, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** One cut of its blade, Michael's strength behind it. */
    private void swing(LivingEntity foe, boolean charging) {
        swingCooldown = charging ? 18 : 26;
        triggerAnim("action", "slash");
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * MichaelBalance.HOST_DAMAGE_MULTIPLIER;
        foe.hurt(damageSources().mobAttack(this), damage);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (master == null) return super.doHurtTarget(target);
        boolean hit = target.hurt(damageSources().mobAttack(this),
                (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * MichaelBalance.HOST_DAMAGE_MULTIPLIER);
        if (hit) triggerAnim("action", "slash");
        return hit;
    }

    /** A shield wall turns blows from the front. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (stance() == STANCE_WALL && source.getEntity() != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) {
            Vec3 to = source.getEntity().position().subtract(position()).multiply(1, 0, 1);
            Vec3 facing = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.01 && to.normalize().dot(facing.normalize()) > 0.3) {
                amount *= WALL_SHARE;
                if (!level().isClientSide) {
                    triggerAnim("action", "block");
                    level().playSound(null, blockPosition(), AllSounds.HOST_SHIELD.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
                }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && master != null) {
            MichaelEntity m = michael();
            if (m != null && isCaptain()) m.captainFell(company);
            triggerAnim("action", "die");
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 24, 0.3, 0.8, 0.3, 0.08);
            }
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return master == null && super.removeWhenFarAway(distance);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.HOST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.HOST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.HOST_DEATH.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String p = MichaelAnimations.HOST;
        RawAnimation idle = RawAnimation.begin().thenLoop(p + "idle"), march = RawAnimation.begin().thenLoop(p + "march");
        RawAnimation wall = RawAnimation.begin().thenLoop(p + "shield_wall"), broken = RawAnimation.begin().thenLoop(p + "disordered");
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (isDeadOrDying()) return PlayState.STOP;
            return switch (stance()) {
                case STANCE_WALL -> state.setAndContinue(state.isMoving() ? march : wall);
                case STANCE_BROKEN -> state.setAndContinue(state.isMoving() ? march : broken);
                default -> state.setAndContinue(state.isMoving() ? march : idle);
            };
        }));
        AnimationController<HostAngelEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : MichaelAnimations.triggered(MichaelAnimations.HOST_CLIPS)) {
            action.triggerableAnim(name, MichaelAnimations.HOLDS.contains(name)
                    ? RawAnimation.begin().thenPlayAndHold(p + name) : RawAnimation.begin().thenPlay(p + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Captain", isCaptain());
        tag.putByte("Vessel", (byte) vessel());
        if (master != null) tag.putUUID("Master", master);
        tag.putInt("Company", company);
        tag.putInt("Slot", slot);
        tag.putInt("CompanySize", companySize);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CAPTAIN, tag.getBoolean("Captain"));
        setVessel(tag.getByte("Vessel"));
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        hadMaster = master != null;
        company = tag.getInt("Company");
        slot = tag.getInt("Slot");
        companySize = Math.max(1, tag.getInt("CompanySize"));
    }
}
