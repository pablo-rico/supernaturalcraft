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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * A defeated enemy written back in ink for one attack (chapters 3-5): drawn with that boss's own model in the ink
 * texture ({@code textures/entity/ink/<boss>.png}), it does its most famous attack and runs down the page.
 *
 * <p>The attack ({@code ChuckAttacks.InkEcho}) does the harm; the echo itself only stands there, faces its prey, and
 * once told to {@link #dissolve} runs ({@link #fade()} 0 → 1 over {@link #FADE_TICKS}) and is gone. It never outlives
 * {@link #MAX_LIFE} nor its author.
 */
public class InkEchoEntity extends Entity implements GeoEntity {

    public static final int FADE_TICKS = 30, MAX_LIFE = 260;

    private static final EntityDataAccessor<String> BOSS = SynchedEntityData.defineId(InkEchoEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> FADE = SynchedEntityData.defineId(InkEchoEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerId;
    private int fading = -1;

    public InkEchoEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BOSS, "lucifer");
        builder.define(FADE, 0f);
    }

    /** Whose echo: a {@code BossProgression.Boss.entity} path (lucifer, amara, metatron…). */
    public String boss() {
        return entityData.get(BOSS);
    }

    public void setBoss(String value) {
        entityData.set(BOSS, value);
    }

    /** How far it has run, 0 (whole) to 1 (gone). */
    public float fade() {
        return entityData.get(FADE);
    }

    public void setFade(float value) {
        entityData.set(FADE, value);
    }

    public void bind(ChuckEntity owner) {
        ownerId = owner.getUUID();
    }

    /** Runs down the page and is gone. */
    public void dissolve() {
        if (fading < 0) fading = 0;
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
            if (random.nextFloat() < 0.5f) {
                level().addParticle(AllParticles.INK.get(), getRandomX(0.8), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.8),
                        0, -0.03, 0);
            }
            return;
        }
        if (tickCount == 1 && level() instanceof ServerLevel server) {
            server.sendParticles(AllParticles.INK_LETTER.get(), getX(), getY() + 1, getZ(), 40, 0.6, 1.0, 0.6, 0.05);
        }
        ChuckEntity owner = ChuckEntity.find(level(), ownerId);
        if (owner == null || !owner.isAlive() || tickCount > MAX_LIFE) dissolve();
        if (fading >= 0) {
            fading++;
            setFade(Math.min(1f, fading / (float) FADE_TICKS));
            if (level() instanceof ServerLevel server && fading % 3 == 0) {
                server.sendParticles(AllParticles.INK.get(), getX(), getY() + getBbHeight() * (1 - fade()), getZ(), 6, 0.5, 0.2, 0.5, 0.02);
            }
            if (fading >= FADE_TICKS) discard();
        }
    }

    /** The echo is drawn with its boss's own model and clips; it holds still, so no controller of its own. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        setBoss(tag.getString("Boss"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putString("Boss", boss());
    }
}
