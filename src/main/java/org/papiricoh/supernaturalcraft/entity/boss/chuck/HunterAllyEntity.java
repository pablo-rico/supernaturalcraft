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
 * Dean, Sam or Castiel, stepping out of the light at the very end to hold the Author for the last blow, then fading.
 * An actor of the finale, not a creature: it cannot be hurt.
 *
 * <p>It follows its author's {@link ChuckEntity#finaleStage()}: {@code appear} as it arrives, {@code hold} while he is
 * held, {@code talk} while the blow is awaited, {@code nod} as he approves; {@link #fade} plays {@code fade} and it
 * is gone a moment later (also when he is).
 */
public class HunterAllyEntity extends Entity implements GeoEntity {

    public static final byte DEAN = 0, SAM = 1, CASTIEL = 2;

    /** Model and texture name of each, by {@link #who()}: hunter_dean, hunter_sam, hunter_castiel. */
    public static final String[] NAMES = {"hunter_dean", "hunter_sam", "hunter_castiel"};

    public static final int FADE_TICKS = 40;

    private static final EntityDataAccessor<Byte> WHO = SynchedEntityData.defineId(HunterAllyEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerId;
    private byte seenStage = -1;
    private int fading = -1;

    public HunterAllyEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WHO, (byte) 0);
    }

    /** {@link #DEAN}, {@link #SAM} or {@link #CASTIEL}. */
    public byte who() {
        return entityData.get(WHO);
    }

    public void setWho(byte value) {
        entityData.set(WHO, value);
    }

    public void bind(ChuckEntity owner) {
        ownerId = owner.getUUID();
    }

    public boolean fading() {
        return fading >= 0;
    }

    /** Back into the light. */
    public void fade() {
        if (fading >= 0) return;
        fading = 0;
        triggerAnim("action", "fade");
    }

    public void face(Vec3 at) {
        float yaw = (float) (Mth.atan2(at.z - getZ(), at.x - getX()) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        setYHeadRot(yaw);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount < 30 || fading()) {
                level().addParticle(AllParticles.GOLDEN_MOTE.get(), getRandomX(0.8), getY() + random.nextDouble() * 1.9, getRandomZ(0.8), 0, 0.03, 0);
            }
            return;
        }
        if (fading >= 0) {
            if (++fading >= FADE_TICKS) {
                ((ServerLevel) level()).sendParticles(AllParticles.GOLDEN_MOTE.get(), getX(), getY() + 1, getZ(), 30, 0.4, 0.9, 0.4, 0.05);
                discard();
            }
            return;
        }
        ChuckEntity owner = ChuckEntity.find(level(), ownerId);
        if (owner == null || !owner.isAlive()) {
            fade();
            return;
        }
        face(owner.position());
        byte stage = owner.finaleStage();
        if (tickCount == 3) triggerAnim("action", "appear");
        if (stage != seenStage && tickCount > 3) {
            seenStage = stage;
            switch (stage) {
                case ChuckEntity.FINALE_HOLD -> triggerAnim("action", "hold");
                case ChuckEntity.FINALE_AWAIT_BLOW -> triggerAnim("action", "talk");
                case ChuckEntity.FINALE_APPROVAL -> triggerAnim("action", "nod");
                default -> {
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop(ChuckAnimations.ALLY + "idle");
        controllers.add(new AnimationController<>(this, "base", 6, state -> state.setAndContinue(idle)));
        AnimationController<HunterAllyEntity> action = new AnimationController<>(this, "action", 4, state -> PlayState.STOP);
        for (String name : ChuckAnimations.ALLY_TRIGGERED) {
            boolean hold = name.equals("hold") || name.equals("fade");
            action.triggerableAnim(name, hold
                    ? RawAnimation.begin().thenPlayAndHold(ChuckAnimations.ALLY + name)
                    : RawAnimation.begin().thenPlay(ChuckAnimations.ALLY + name));
        }
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
