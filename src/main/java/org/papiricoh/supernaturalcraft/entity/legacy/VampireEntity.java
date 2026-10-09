package org.papiricoh.supernaturalcraft.entity.legacy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A vampire of a nest (v0.17). Its bite bleeds ({@code BLEEDING}) and feeds it. Only a killing blow with a {@code #beheading}
 * blade keeps it down: anything else drops it to the ground ({@link #downed()}) and after {@link #DOWNED_TICKS} it rises again,
 * half healed. Dead man's blood stuns it ({@link #stun}): it can't move or bite for {@link #STUN_TICKS}.
 */
public class VampireEntity extends Monster implements GeoEntity {

    private static final EntityDataAccessor<Boolean> FANGS = SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STUNNED = SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DOWNED = SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.INT);
    private static final String PREFIX = "animation.vampire.";
    public static final int STUN_TICKS = 160, DOWNED_TICKS = 200, BLEED_TICKS = 120;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public VampireEntity(EntityType<? extends VampireEntity> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FANGS, false);
        builder.define(STUNNED, 0);
        builder.define(DOWNED, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean fangsOut() {
        return entityData.get(FANGS);
    }

    /** Stunned by dead man's blood. */
    public boolean stunned() {
        return entityData.get(STUNNED) > 0;
    }

    /** Down after a killing blow that didn't take its head: it will rise again. */
    public boolean downed() {
        return entityData.get(DOWNED) > 0;
    }

    /** Dead man's blood: it reels and can't move or bite for a while. */
    public void stun() {
        entityData.set(STUNNED, STUN_TICKS);
        setNoAi(true);
        setTarget(null);
        triggerAnim("action", "hiss");
        level().playSound(null, blockPosition(), AllSounds.VAMPIRE_HISS.get(), SoundSource.HOSTILE, 1.0f, 0.7f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        int stun = entityData.get(STUNNED), down = entityData.get(DOWNED);
        if (stun > 0) entityData.set(STUNNED, stun - 1);
        if (down > 0) {
            entityData.set(DOWNED, down - 1);
            if (down == 1) rise();
        }
        if (isNoAi() && stun <= 1 && down <= 1 && !persistentNoAi) setNoAi(false);
        boolean fangs = getTarget() != null && !stunned() && !downed();
        if (fangs != fangsOut()) {
            entityData.set(FANGS, fangs);
            if (fangs) triggerAnim("action", "fangs_out");
        }
    }

    private boolean persistentNoAi;

    private void rise() {
        setHealth(getMaxHealth() * 0.5f);
        triggerAnim("action", "rise");
        level().playSound(null, blockPosition(), AllSounds.VAMPIRE_HISS.get(), SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (stunned() || downed()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity victim) {
            triggerAnim("action", "bite");
            victim.addEffect(new MobEffectInstance(AllMobEffects.BLEEDING, BLEED_TICKS, 0), this);
            heal(2f);
            level().playSound(null, blockPosition(), AllSounds.VAMPIRE_BITE.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !LegacyWeapons.absolute(source) && !LegacyWeapons.beheads(source)) {
            // Not its head: it goes down, and will get up again.
            setHealth(1f);
            if (!downed()) {
                entityData.set(DOWNED, DOWNED_TICKS);
                setNoAi(true);
                setTarget(null);
            }
            return;
        }
        if (!level().isClientSide && LegacyWeapons.beheads(source)) triggerAnim("action", "decapitated");
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AllSounds.VAMPIRE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return AllSounds.VAMPIRE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AllSounds.VAMPIRE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stunned", entityData.get(STUNNED));
        tag.putInt("Downed", entityData.get(DOWNED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(STUNNED, tag.getInt("Stunned"));
        entityData.set(DOWNED, tag.getInt("Downed"));
        persistentNoAi = isNoAi() && !stunned() && !downed();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(PREFIX + "idle"), walk = RawAnimation.begin().thenLoop(PREFIX + "walk"),
                run = RawAnimation.begin().thenLoop(PREFIX + "run"), stunnedLoop = RawAnimation.begin().thenLoop(PREFIX + "stunned");
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (stunned() || downed()) return state.setAndContinue(stunnedLoop);
            if (state.isMoving()) return state.setAndContinue(getTarget() != null || fangsOut() ? run : walk);
            return state.setAndContinue(idle);
        }));
        AnimationController<VampireEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        for (String clip : LegacyAssets.CLIPS.get("vampire")) {
            if (LegacyAssets.LOOPS.get("vampire").contains(clip)) continue;
            action.triggerableAnim(clip, clip.equals("decapitated") ? RawAnimation.begin().thenPlayAndHold(PREFIX + clip)
                    : RawAnimation.begin().thenPlay(PREFIX + clip));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
