package org.papiricoh.supernaturalcraft.darkness.chuck;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckAnimations;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckLook;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Chuck in season 11 (v0.42), the man who turns out to be God: one entity for his two roles, the visit at dawn that shows who
 * he is ({@link #ENVOY}: staged, he only talks) and his part in his sister's fight ({@link #ALLY}, phase IV-VI: he stands in
 * the town, his light ({@link #light()}) is what she tears at and what the hunters protect). Untouchable, drawn with the
 * Author's rig ({@link ChuckLook}): his cracks are the light she has torn from him.
 */
public class ChuckAllyEntity extends PathfinderMob implements GeoEntity, ChuckLook {

    public static final byte ENVOY = 0, ALLY = 1;

    private static final EntityDataAccessor<Float> LIGHT = SynchedEntityData.defineId(ChuckAllyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ROLE = SynchedEntityData.defineId(ChuckAllyEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public ChuckAllyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIGHT, 1f);
        builder.define(ROLE, ENVOY);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16f, 1f));
    }

    public byte role() {
        return entityData.get(ROLE);
    }

    public void setRole(byte role) {
        entityData.set(ROLE, role);
    }

    /** His light, 0..1 (1 whole). */
    public float light() {
        return entityData.get(LIGHT);
    }

    public void setLight(float light) {
        entityData.set(LIGHT, Math.max(0f, Math.min(1f, light)));
    }

    // --- look ------------------------------------------------------------------------------------------------------------

    @Override
    public Chapter.Outfit outfit() {
        return Chapter.Outfit.FLANNEL;
    }

    @Override
    public boolean divine() {
        return false;
    }

    @Override
    public float crack() {
        return role() == ALLY ? 1f - light() : 0f;
    }

    // --- untouchable -----------------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /** Vanishes in a little light (his visit ends, or the fight does). */
    public void leave() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Light", light());
        tag.putByte("Role", role());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Light")) setLight(tag.getFloat("Light"));
        setRole(tag.getByte("Role"));
    }

    // --- GeckoLib --------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(ChuckAnimations.HUMAN + "idle");
        RawAnimation talk = RawAnimation.begin().thenLoop(ChuckAnimations.HUMAN + "talk");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(role() == ENVOY ? talk : idle)));
        AnimationController<ChuckAllyEntity> action = new AnimationController<>(this, "action", 2, state -> PlayState.STOP);
        action.triggerableAnim("rise", RawAnimation.begin().thenPlay(ChuckAnimations.HUMAN + "rise"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
