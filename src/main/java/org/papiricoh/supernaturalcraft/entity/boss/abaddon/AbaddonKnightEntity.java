package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.magic.spell.SpellHooks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Abaddon's Knight (v0.21): a big armoured demon in blackened plate ({@code #demons}). He closes in and {@code slash}es an arc
 * before him ({@link AbaddonBalance#SLASH_REACH}, the clip's moment at {@link AbaddonAssets#RETINUE_HIT_TICKS}); struck from the
 * front he may raise his shield ({@code guard}), and while it is up blows from the front glance off.
 *
 * <p><b>Possessed</b> ({@link #possessed()}, synced: the {@code possessed} loop, and FX makes him glow): Abaddon rides him. He hits
 * {@link AbaddonBalance#POSSESSED_DAMAGE} harder and moves {@link AbaddonBalance#POSSESSED_SPEED} faster, never guards, and every
 * blow on him lands on her instead ({@link AbaddonEntity#hurtThroughVessel}). Exorcise, Banish by Name or a Salt Shot on him
 * force her out ({@link SpellHooks.Expellable}).
 */
public class AbaddonKnightEntity extends Monster implements GeoEntity, SpellHooks.Expellable {

    private static final EntityDataAccessor<Boolean> POSSESSED = SynchedEntityData.defineId(AbaddonKnightEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GUARDING = SynchedEntityData.defineId(AbaddonKnightEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID possessor, master;
    /** Ticks into the current slash or guard (0: neither). */
    private int slashT, guardT, slashCooldown, guardCooldown;

    public AbaddonKnightEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, AbaddonBalance.KNIGHT_HEALTH)
                .add(Attributes.ARMOR, AbaddonBalance.KNIGHT_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, AbaddonBalance.KNIGHT_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POSSESSED, false);
        builder.define(GUARDING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new FightGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AbaddonKnightEntity.class, AbaddonHeraldEntity.class, AbaddonEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // --- Abaddon ----------------------------------------------------------------------------------------------------------

    /** He serves Abaddon (her entity's UUID). */
    public void serve(UUID abaddon) {
        master = abaddon;
    }

    public @Nullable UUID master() {
        return master;
    }

    /** Abaddon rides him (synced). */
    public boolean possessed() {
        return entityData.get(POSSESSED);
    }

    /** Whether his shield is up now (synced). */
    public boolean guarding() {
        return entityData.get(GUARDING) && guardT >= AbaddonAssets.RETINUE_HIT_TICKS.get("guard");
    }

    /** She enters ({@code abaddon}) or leaves ({@code null}) him. */
    public void setPossessor(@Nullable UUID abaddon) {
        possessor = abaddon;
        entityData.set(POSSESSED, abaddon != null);
        if (abaddon != null) {
            guardT = 0;
            entityData.set(GUARDING, false);
        }
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0.27 * (abaddon != null ? AbaddonBalance.POSSESSED_SPEED : 1));
    }

    public @Nullable AbaddonEntity rider() {
        if (possessor == null || !(level() instanceof ServerLevel level)) return null;
        return level.getEntity(possessor) instanceof AbaddonEntity a ? a : null;
    }

    @Override
    public boolean onExpelled(@Nullable Entity by, int how) {
        AbaddonEntity a = rider();
        return a != null && a.onExpelled(by, how);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || other instanceof AbaddonKnightEntity || other instanceof AbaddonHeraldEntity
                || other instanceof AbaddonEntity;
    }

    // --- damage -----------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (source.getEntity() instanceof AbaddonEntity || source.getEntity() instanceof AbaddonHeraldEntity
                || source.getEntity() instanceof AbaddonKnightEntity) return false;
        AbaddonEntity rider = rider();
        if (rider != null && !BossDamage.passesThrough(source)) {
            // Her vessel: the blow is hers.
            boolean hurt = rider.hurtThroughVessel(source, amount);
            if (hurt) playSound(AllSounds.abaddon("hurt"), 1f, 1.2f);
            return hurt;
        }
        if (guarding() && fromFront(source)) {
            playSound(SoundEvents.SHIELD_BLOCK, 1f, 0.8f);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && guardT == 0 && slashT == 0 && guardCooldown <= 0 && fromFront(source) && random.nextFloat() < AbaddonBalance.GUARD_CHANCE) {
            startGuard();
        }
        return hurt;
    }

    private boolean fromFront(DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) return false;
        Vec3 to = from.subtract(position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4) return false;
        Vec3 facing = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1).normalize();
        return to.normalize().dot(facing) > 0.25;
    }

    // --- acting -----------------------------------------------------------------------------------------------------------

    public boolean busy() {
        return slashT > 0 || guardT > 0;
    }

    public void startSlash() {
        slashT = 1;
        slashCooldown = AbaddonBalance.SLASH_COOLDOWN;
        triggerAnim("action", AbaddonAnimations.KNIGHT_SLASH);
    }

    public void startGuard() {
        if (possessed()) return;
        guardT = 1;
        guardCooldown = AbaddonBalance.GUARD_COOLDOWN;
        entityData.set(GUARDING, true);
        triggerAnim("action", AbaddonAnimations.KNIGHT_GUARD);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (possessed() && random.nextInt(6) == 0) {
                level().addParticle(AllParticles.DEMON_SMOKE.get(), getRandomX(0.5), getY() + random.nextDouble() * 2.2, getRandomZ(0.5), 0, 0.02, 0);
            }
            return;
        }
        if (slashCooldown > 0) slashCooldown--;
        if (guardCooldown > 0) guardCooldown--;
        if (slashT > 0) {
            getNavigation().stop();
            if (slashT == AbaddonAssets.RETINUE_HIT_TICKS.get("slash")) landSlash();
            if (++slashT > AbaddonAssets.RETINUE_CLIP_TICKS.get("slash")) slashT = 0;
        }
        if (guardT > 0) {
            getNavigation().stop();
            if (++guardT > AbaddonAssets.RETINUE_CLIP_TICKS.get("guard")) {
                guardT = 0;
                entityData.set(GUARDING, false);
            }
        }
        if (possessor != null && tickCount % 20 == 0 && rider() == null) setPossessor(null);
    }

    /** The slash's moment: everyone in the arc before him. */
    private void landSlash() {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (possessed() ? AbaddonBalance.POSSESSED_DAMAGE : 1f);
        Vec3 facing = Vec3.directionFromRotation(0, getYRot()).multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(AbaddonBalance.SLASH_ARC / 2));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(AbaddonBalance.SLASH_REACH))) {
            if (e == this || isAlliedTo(e) || !e.isAlive() || e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            if (to.length() > AbaddonBalance.SLASH_REACH || (to.lengthSqr() > 0.25 && to.normalize().dot(facing) < cos)) continue;
            if (e.hurt(damageSources().mobAttack(this), dmg)) {
                e.push(facing.x * 0.5, 0.2, facing.z * 0.5);
                e.hurtMarked = true;
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
    }

    // --- sounds -----------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.abaddon("knight_ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.abaddon("knight_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.abaddon("knight_death");
    }

    // --- save -------------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (possessor != null) tag.putUUID("Possessor", possessor);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        setPossessor(tag.hasUUID("Possessor") ? tag.getUUID("Possessor") : null);
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    private static String clip(String name) {
        return "animation." + AbaddonAssets.KNIGHT + "." + name;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(clip("idle")), walk = RawAnimation.begin().thenLoop(clip("walk")),
                ridden = RawAnimation.begin().thenLoop(clip(AbaddonAnimations.KNIGHT_POSSESSED));
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (possessed() && !state.isMoving()) return state.setAndContinue(ridden);
            return state.setAndContinue(state.isMoving() ? walk : idle);
        }));
        AnimationController<AbaddonKnightEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim(AbaddonAnimations.KNIGHT_SLASH, RawAnimation.begin().thenPlay(clip(AbaddonAnimations.KNIGHT_SLASH)));
        action.triggerableAnim(AbaddonAnimations.KNIGHT_GUARD, RawAnimation.begin().thenPlay(clip(AbaddonAnimations.KNIGHT_GUARD)));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    // --- his fight --------------------------------------------------------------------------------------------------------

    /** Closes in on his target; slashes in reach; now and then raises his shield as a hunter closes. */
    static final class FightGoal extends Goal {
        private final AbaddonKnightEntity knight;
        private int repath;

        FightGoal(AbaddonKnightEntity knight) {
            this.knight = knight;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = knight.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            knight.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = knight.getTarget();
            if (t == null) return;
            knight.getLookControl().setLookAt(t, 30f, 30f);
            if (knight.busy()) return;
            double d2 = knight.distanceToSqr(t);
            double reach = AbaddonBalance.SLASH_REACH - 0.4;
            if (d2 <= reach * reach && knight.slashCooldown <= 0) {
                knight.startSlash();
                return;
            }
            if (d2 < 5 * 5 && knight.guardCooldown <= 0 && !knight.possessed() && knight.getRandom().nextInt(80) == 0) {
                knight.startGuard();
                return;
            }
            if (--repath <= 0) {
                repath = 8;
                knight.getNavigation().moveTo(t, 1.0);
            }
        }
    }
}
