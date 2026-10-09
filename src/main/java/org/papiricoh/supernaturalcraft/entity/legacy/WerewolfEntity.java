package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A werewolf (v0.17): a man by day ({@code human_*} clips; only fights back), a wolf by night ({@link #wolfForm()}, synced:
 * faster, harder-hitting, hunting players), stronger still under the full moon. Only {@code #silver} kills it for good: any
 * other killing blow leaves it at 1 health, and it flees and heals for {@link #FLEE_TICKS}.
 */
public class WerewolfEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> WOLF = SynchedEntityData.defineId(WerewolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(WerewolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String PREFIX = "animation.werewolf.";
    private static final ResourceLocation WOLF_SPEED = SupernaturalCraft.asResource("werewolf_wolf_speed"),
            WOLF_DAMAGE = SupernaturalCraft.asResource("werewolf_wolf_damage"), MOON = SupernaturalCraft.asResource("werewolf_full_moon");
    public static final int FLEE_TICKS = 300;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int fleeTicks;
    private @Nullable Vec3 fleeFrom;
    /** Tests force a form; -1 = follow the sky. */
    private int forced = -1;

    public WerewolfEntity(EntityType<? extends WerewolfEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WOLF, false);
        builder.define(FLEEING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> wolfForm() && !fleeing()));
    }

    public boolean wolfForm() {
        return entityData.get(WOLF);
    }

    public boolean fleeing() {
        return entityData.get(FLEEING);
    }

    /** Forces the wolf ({@code true}) or the man ({@code false}); null follows the sky again (tests, previews). */
    public void forceForm(@Nullable Boolean wolf) {
        forced = wolf == null ? -1 : wolf ? 1 : 0;
        if (wolf != null) setForm(wolf);
    }

    /** Night (or a sky with no day) brings the wolf out. */
    public boolean nightNow() {
        Level level = level();
        return !level.dimensionType().hasFixedTime() ? level.isNight() : true;
    }

    public boolean fullMoon() {
        return wolfForm() && level().getMoonPhase() == 0 && nightNow();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        boolean wolf = forced >= 0 ? forced == 1 : nightNow();
        if (wolf != wolfForm()) setForm(wolf);
        if (tickCount % 20 == 0) applyMoon();
        if (fleeTicks > 0) {
            fleeTicks--;
            if (tickCount % 20 == 0) heal(wolfForm() ? 2f : 1f);
            if (fleeFrom != null && tickCount % 10 == 0) {
                Vec3 away = position().subtract(fleeFrom).multiply(1, 0, 1).normalize().scale(16).add(position());
                getNavigation().moveTo(away.x, away.y, away.z, 1.4);
            }
            if (fleeTicks == 0) entityData.set(FLEEING, false);
        }
        if (wolfForm() && nightNow() && random.nextInt(900) == 0) howl();
    }

    private void setForm(boolean wolf) {
        entityData.set(WOLF, wolf);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED), damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (speed != null) {
            speed.removeModifier(WOLF_SPEED);
            if (wolf) speed.addTransientModifier(new AttributeModifier(WOLF_SPEED, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (damage != null) {
            damage.removeModifier(WOLF_DAMAGE);
            if (wolf) damage.addTransientModifier(new AttributeModifier(WOLF_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (!wolf) setTarget(null);
        if (tickCount > 1) {
            triggerAnim("action", "turn");
            level().playSound(null, blockPosition(), AllSounds.WEREWOLF_TURN.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        applyMoon();
    }

    private void applyMoon() {
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage == null) return;
        boolean full = fullMoon();
        if (full == damage.hasModifier(MOON)) return;
        damage.removeModifier(MOON);
        if (full) damage.addTransientModifier(new AttributeModifier(MOON, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private void howl() {
        triggerAnim("action", "howl");
        level().playSound(null, blockPosition(), AllSounds.WEREWOLF_HOWL.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !LegacyWeapons.absolute(source) && !LegacyWeapons.silver(source)) {
            // Not silver: it can't die. It breaks off, flees and mends.
            setHealth(1f);
            fleeTicks = FLEE_TICKS;
            entityData.set(FLEEING, true);
            LivingEntity from = source.getEntity() instanceof LivingEntity l ? l : getTarget();
            fleeFrom = from != null ? from.position() : position();
            setTarget(null);
            triggerAnim("action", "flee");
            return;
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return wolfForm() ? AllSounds.WEREWOLF_GROWL.get() : AllSounds.WEREWOLF_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.WEREWOLF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.WEREWOLF_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Flee", fleeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fleeTicks = tag.getInt("Flee");
        entityData.set(FLEEING, fleeTicks > 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = loop("idle"), walk = loop("walk"), run = loop("run"), humanIdle = loop("human_idle"), humanWalk = loop("human_walk");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (!wolfForm()) return state.setAndContinue(state.isMoving() ? humanWalk : humanIdle);
            if (state.isMoving()) return state.setAndContinue(getTarget() != null || fleeing() ? run : walk);
            return state.setAndContinue(idle);
        }));
        AnimationController<WerewolfEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : LegacyAssets.triggered("werewolf")) action.triggerableAnim(clip, RawAnimation.begin().thenPlay(PREFIX + clip));
        controllers.add(action);
    }

    private static RawAnimation loop(String clip) {
        return RawAnimation.begin().thenLoop(PREFIX + clip);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) triggerAnim("action", wolfForm() && random.nextInt(4) == 0 ? "pounce" : "claw");
        return hit;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
