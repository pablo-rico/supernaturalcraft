package org.papiricoh.supernaturalcraft.entity.hazard;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * A pool of the Darkness on the ground. Standing in it slows you and feeds your Consumption;
 * strong light (12 or more) dissolves it.
 */
public class VoidZone extends Entity {

    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(VoidZone.class, EntityDataSerializers.FLOAT);
    private int lifetime = 400;
    /** Damage per second to those standing in it (her Unmaking); zero for an ordinary pool. */
    private float harm;

    public VoidZone(EntityType<? extends VoidZone> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static VoidZone spawn(ServerLevel level, Vec3 at, float radius, int lifetime) {
        return spawn(level, at, radius, lifetime, 0);
    }

    public static VoidZone spawn(ServerLevel level, Vec3 at, float radius, int lifetime, float harmPerSecond) {
        VoidZone z = new VoidZone(AllEntities.VOID_ZONE.get(), level);
        z.harm = harmPerSecond;
        z.entityData.set(RADIUS, radius);
        z.lifetime = lifetime;
        z.setPos(at);
        level.addFreshEntity(z);
        return z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 2.5f);
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    public int lifetime() {
        return lifetime;
    }

    public boolean contains(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        return dx * dx + dz * dz <= radius() * radius() && Math.abs(e.getY() - getY()) < 2.5;
    }

    @Override
    public void tick() {
        super.tick();
        float r = radius();
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
                level().addParticle(AllParticles.VOID_MOTE.get(), getX() + Math.cos(a) * d, getY() + 0.1, getZ() + Math.sin(a) * d,
                        0, 0.02 + random.nextDouble() * 0.03, 0);
            }
            return;
        }
        if (tickCount > lifetime || level().getBrightness(LightLayer.BLOCK, BlockPos.containing(position().add(0, 0.5, 0))) >= AmaraBalance.LANCE_PROOF) {
            ((ServerLevel) level()).sendParticles(AllParticles.GRACE.get(), getX(), getY() + 0.3, getZ(), 12, r * 0.4, 0.1, r * 0.4, 0.02);
            discard();
            return;
        }
        if (tickCount % 10 == 0) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2, r), this::contains)) {
                if (e.getType().is(AllTags.Entities.DARKNESS)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                if (harm > 0) {
                    e.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(level(),
                            org.papiricoh.supernaturalcraft.registry.AllDamageTypes.VOID, null), harm / 2);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(RADIUS, tag.getFloat("Radius"));
        lifetime = tag.getInt("Lifetime");
        harm = tag.getFloat("Harm");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", radius());
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("Harm", harm);
    }
}
