package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.nbt.CompoundTag;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * One of Metatron's constructs: a thing his will holds in the air, not a creature. It cannot be hurt,
 * pushed or targeted; it drifts beside him until one of his attacks orders it somewhere (smoothly, over
 * a number of ticks), and it goes when he does.
 */
public abstract class ScribeConstruct extends Entity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerId;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private int moveTicks, moveTotal;
    private boolean ordered;
    private float wantYaw;

    protected ScribeConstruct(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void bind(MetatronEntity owner) {
        ownerId = owner.getUUID();
    }

    public @Nullable MetatronEntity owner() {
        return level() instanceof ServerLevel server && ownerId != null && server.getEntity(ownerId) instanceof MetatronEntity m ? m : null;
    }

    /** Glides to {@code pos} over {@code ticks}, and stays there until released or ordered again. */
    public void order(Vec3 pos, int ticks) {
        from = position();
        to = pos;
        moveTotal = Math.max(1, ticks);
        moveTicks = 0;
        ordered = true;
    }

    /** Back to drifting beside its master. */
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        MetatronEntity owner = owner();
        if (owner == null || !owner.isAlive() || owner.isRemoved()) {
            vanish();
            return;
        }
        if (ordered && moveTicks < moveTotal) {
            moveTicks++;
            double t = moveTicks / (double) moveTotal;
            double eased = t * t * (3 - 2 * t);
            Vec3 at = from.lerp(to, eased);
            setPos(at.x, at.y, at.z);
        } else if (!ordered) {
            Vec3 want = owner.constructRest(this);
            Vec3 at = position().lerp(want, 0.12);
            setPos(at.x, at.y + Math.sin((tickCount + getId()) * 0.08) * 0.02, at.z);
            Vec3 look = owner.position();
            wantYaw = (float) (Mth.atan2(look.z - getZ(), look.x - getX()) * Mth.RAD_TO_DEG) - 90f;
        }
        setYRot(Mth.approachDegrees(getYRot(), wantYaw, 12f));
    }

    /** Client side, every tick: its glow and motes. */
    protected void clientTick() {
        if (random.nextFloat() < 0.3f) {
            level().addParticle(AllParticles.GRACE.get(), getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2,
                    getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.01, 0);
        }
    }

    public void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.GRACE.get(), getX(), getY() + 1, getZ(), 40, 1, 1, 1, 0.05);
        }
        discard();
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
        return distance < 128 * 128;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }
}
