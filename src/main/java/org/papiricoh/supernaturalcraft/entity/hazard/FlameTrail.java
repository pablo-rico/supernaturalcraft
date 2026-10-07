package org.papiricoh.supernaturalcraft.entity.hazard;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.UUID;

/** A patch of hellfire left by the greatsword. Burns what stands in it; never sets blocks alight. */
public class FlameTrail extends Entity {

    public static final int LIFETIME = 100;
    public static final float RADIUS = 0.9f, DAMAGE = 1f;
    private @Nullable UUID ownerId;

    public FlameTrail(EntityType<? extends FlameTrail> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public FlameTrail(Level level, LivingEntity owner, double x, double y, double z) {
        this(AllEntities.FLAME_TRAIL.get(), level);
        ownerId = owner.getUUID();
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextFloat() < 0.6f) {
                level().addParticle(AllParticles.HELLFIRE.get(), getX() + (random.nextDouble() - 0.5), getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            }
            if (random.nextFloat() < 0.1f) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 0, 0.03, 0);
            return;
        }
        if (tickCount > LIFETIME) {
            discard();
            return;
        }
        if (tickCount % 10 != 0) return;
        ServerLevel server = (ServerLevel) level();
        Entity owner = ownerId == null ? null : server.getEntity(ownerId);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS, 1, RADIUS))) {
            if (e == owner || (owner instanceof LivingEntity o && ResolvedSpell.isFriend(o, e))) continue;
            e.hurt(AllDamageTypes.source(server, AllDamageTypes.HELLFIRE, this, owner), DAMAGE);
            e.igniteForSeconds(2);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        tickCount = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Age", tickCount);
    }
}
