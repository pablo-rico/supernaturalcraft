package org.papiricoh.supernaturalcraft.entity.boss.horsemen.death;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.Optional;
import java.util.UUID;

/**
 * The light out of one hunter's limbo (particles only, no model). Only its hunter sees it; Death's fight checks whether
 * they reach it in time and takes it away either way.
 */
public class LimboExitEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(LimboExitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** How close counts as reaching it. */
    public static final double REACH = 1.6;
    private int lifetime = 400;

    public LimboExitEntity(EntityType<? extends LimboExitEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    public void setOwner(UUID owner, int lifetime) {
        entityData.set(OWNER, Optional.of(owner));
        this.lifetime = lifetime;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (org.papiricoh.supernaturalcraft.client.horsemen.LimboView.sees(this)) {
                for (int i = 0; i < 4; i++) {
                    level().addParticle(AllParticles.SOUL_WISP.get(), getRandomX(0.5), getY() + random.nextDouble() * 2.2, getRandomZ(0.5),
                            0, 0.03 + random.nextDouble() * 0.04, 0);
                }
                if (tickCount % 3 == 0) level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 2.4, getZ(), 0, 0.12, 0);
            }
            return;
        }
        if (tickCount > lifetime + 40) discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
