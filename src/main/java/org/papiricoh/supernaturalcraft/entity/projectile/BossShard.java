package org.papiricoh.supernaturalcraft.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.UUID;

/**
 * Thrown things that drift toward their mark: Lucifer's grace feathers and Cage ice, and the
 * glyph orbs of an Enochian catalyst.
 */
public class BossShard extends ThrowableProjectile {

    public enum Kind { FEATHER, ICE, GLYPH, VOID, PAGE }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BossShard.class, EntityDataSerializers.INT);
    private float damage = 5f;
    private float homing;
    private @Nullable UUID targetId;

    public BossShard(EntityType<? extends BossShard> type, Level level) {
        super(type, level);
    }

    public BossShard(Level level, LivingEntity owner, Kind kind, float damage, float homing, @Nullable Entity target) {
        super(AllEntities.BOSS_SHARD.get(), owner, level);
        entityData.set(KIND, kind.ordinal());
        this.damage = damage;
        this.homing = homing;
        this.targetId = target == null ? null : target.getUUID();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
    }

    public Kind kind() {
        return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)];
    }

    private net.minecraft.core.particles.SimpleParticleType particle() {
        return switch (kind()) {
            case FEATHER -> AllParticles.GRACE.get();
            case ICE -> AllParticles.FROST.get();
            case GLYPH -> AllParticles.SIGIL.get();
            case VOID -> AllParticles.VOID_MOTE.get();
            case PAGE -> AllParticles.PAGE.get();
        };
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(particle(), getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (tickCount > 120) {
            discard();
            return;
        }
        if (homing > 0 && targetId != null && tickCount > 6 && ((ServerLevel) level()).getEntity(targetId) instanceof LivingEntity t && t.isAlive()) {
            Vec3 want = t.getEyePosition().subtract(position()).normalize().scale(getDeltaMovement().length());
            setDeltaMovement(getDeltaMovement().lerp(want, homing));
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !target.getType().is(AllTags.Entities.CAGE_DWELLERS)
                && !target.getType().is(AllTags.Entities.DARKNESS) && !(target instanceof org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraPart);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
            var type = switch (kind()) {
                case FEATHER -> AllDamageTypes.GRACE;
                case VOID -> AllDamageTypes.VOID;
                default -> AllDamageTypes.SPELL;
            };
            BossStrike.land(getOwner(), result.getEntity(), AllDamageTypes.source(server, type, this, getOwner()), damage);
            if (kind() == Kind.ICE && result.getEntity() instanceof LivingEntity l && l.canFreeze()) {
                l.setTicksFrozen(Math.min(l.getTicksRequiredToFreeze() + 60, l.getTicksFrozen() + 60));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(particle(), getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.05);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Kind", entityData.get(KIND));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        entityData.set(KIND, tag.getInt("Kind"));
    }
}
