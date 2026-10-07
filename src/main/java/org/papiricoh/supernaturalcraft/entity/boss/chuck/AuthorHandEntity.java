package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of the Author's two giant hands, floating apart from the light in chapters 3-5 (a construct, not a creature:
 * it cannot be hurt). Drawn with geo/entity/author_hand.geo.json, mirrored when {@link #left()}.
 *
 * <p>Like Metatron's constructs it drifts beside its owner ({@link ChuckEntity#handRest}) until an attack orders it
 * somewhere ({@link #order}, smoothly over a number of ticks) and lets it go ({@link #release}); it goes when he does.
 * Clips: {@link ChuckAnimations#HAND} + {@code idle}, and the one-shots of {@link ChuckAnimations#HAND_TRIGGERED}
 * on the {@code action} controller.
 */
public class AuthorHandEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> LEFT = SynchedEntityData.defineId(AuthorHandEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerId;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private int moveTicks, moveTotal;
    private boolean ordered;
    private float wantYaw;

    public AuthorHandEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LEFT, false);
    }

    /** Whether this is his left hand (the model is a right hand, mirrored). */
    public boolean left() {
        return entityData.get(LEFT);
    }

    public void setLeft(boolean value) {
        entityData.set(LEFT, value);
    }

    public void bind(ChuckEntity owner) {
        ownerId = owner.getUUID();
    }

    public @Nullable ChuckEntity owner() {
        return ChuckEntity.find(level(), ownerId);
    }

    /** Glides to {@code pos} over {@code ticks}, and stays there until released or ordered again. */
    public void order(Vec3 pos, int ticks) {
        from = position();
        to = pos;
        moveTotal = Math.max(1, ticks);
        moveTicks = 0;
        ordered = true;
    }

    /** Back to floating beside its master. */
    public void release() {
        ordered = false;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public void face(Vec3 at) {
        wantYaw = (float) (Mth.atan2(at.z - getZ(), at.x - getX()) * Mth.RAD_TO_DEG) - 90f;
    }

    public void faceYaw(float yaw) {
        wantYaw = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.3f) {
                level().addParticle(AllParticles.GOLDEN_MOTE.get(), getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 3,
                        getZ() + (random.nextDouble() - 0.5) * 3, 0, 0.01, 0);
            }
            return;
        }
        ChuckEntity owner = owner();
        if (owner == null || !owner.isAlive()) {
            vanish();
            return;
        }
        if (tickCount == 3) triggerAnim("action", "appear");
        if (ordered && moveTicks < moveTotal) {
            moveTicks++;
            double t = moveTicks / (double) moveTotal;
            double eased = t * t * (3 - 2 * t);
            Vec3 at = from.lerp(to, eased);
            setPos(at.x, at.y, at.z);
        } else if (!ordered) {
            Vec3 want = owner.handRest(this);
            Vec3 at = position().lerp(want, 0.1);
            setPos(at.x, at.y + Math.sin((tickCount + getId()) * 0.07) * 0.03, at.z);
            wantYaw = owner.getYRot();
        }
        setYRot(Mth.approachDegrees(getYRot(), wantYaw, 10f));
    }

    public void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.GOLDEN_MOTE.get(), getX(), getY() + 1.5, getZ(), 40, 1.5, 1.5, 1.5, 0.05);
        }
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(ChuckAnimations.HAND + "idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(idle)));
        AnimationController<AuthorHandEntity> action = new AnimationController<>(this, "action", 3, state -> PlayState.STOP);
        for (String name : ChuckAnimations.HAND_TRIGGERED) {
            action.triggerableAnim(name, name.equals("vanish")
                    ? RawAnimation.begin().thenPlayAndHold(ChuckAnimations.HAND + name)
                    : RawAnimation.begin().thenPlay(ChuckAnimations.HAND + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** A giant hand: drawn whenever any of it is in view. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        setLeft(tag.getBoolean("Left"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putBoolean("Left", left());
    }
}
