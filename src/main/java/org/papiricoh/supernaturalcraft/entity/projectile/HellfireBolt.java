package org.papiricoh.supernaturalcraft.entity.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * A bolt of hellfire. Burns what it hits and splashes a little; never sets blocks alight, so a
 * fight with demons doesn't burn the player's base down. Demons and their master are immune.
 */
public class HellfireBolt extends AbstractHurtingProjectile {

    private float damage = 5.0f;
    private float splash = 1.0f;
    /** Radius of real block damage on impact; only ever non-zero when the server allows it. */
    private float breakRadius = 0f;

    public HellfireBolt(EntityType<? extends HellfireBolt> type, Level level) {
        super(type, level);
    }

    public HellfireBolt(Level level, LivingEntity owner, Vec3 direction, float damage, float splash) {
        super(AllEntities.HELLFIRE_BOLT.get(), owner, direction, level);
        this.damage = damage;
        this.splash = splash;
        this.accelerationPower = 0.12;
    }

    public HellfireBolt breaking(float radius) {
        this.breakRadius = radius;
        return this;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return AllParticles.HELLFIRE.get();
    }

    @Override
    protected float getInertia() {
        return 0.98f;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !target.getType().is(AllTags.Entities.DEMONS)
                && !target.getType().is(AllTags.Entities.CAGE_DWELLERS);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
            Entity target = result.getEntity();
            if (target.hurt(AllDamageTypes.source(server, AllDamageTypes.HELLFIRE, this, getOwner()), damage)) {
                target.igniteForSeconds(3);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            if (splash > 0) {
                for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(splash))) {
                    if (e != getOwner() && !e.getType().is(AllTags.Entities.DEMONS) && !e.getType().is(AllTags.Entities.CAGE_DWELLERS)
                            && (!(result instanceof EntityHitResult ehr) || ehr.getEntity() != e)) {
                        e.hurt(AllDamageTypes.source(server, AllDamageTypes.HELLFIRE, this, getOwner()), damage * 0.4f);
                    }
                }
            }
            if (breakRadius > 0) {
                net.minecraft.core.BlockPos c = blockPosition();
                int r = (int) Math.ceil(breakRadius);
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
                    var state = server.getBlockState(p);
                    if (p.distSqr(c) <= breakRadius * breakRadius && !state.isAir() && server.getBlockEntity(p) == null
                            && !state.is(org.papiricoh.supernaturalcraft.registry.AllTags.Blocks.ARENA_IMMUNE)
                            && state.getDestroySpeed(server, p) >= 0) {
                        server.destroyBlock(p, true, this);
                    }
                }
            }
            server.sendParticles(AllParticles.HELLFIRE.get(), getX(), getY(), getZ(), 18 + (int) (splash * 10), 0.3 + splash * 0.3,
                    0.3, 0.3 + splash * 0.3, 0.06);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 4, 0.2, 0.2, 0.2, 0.01);
            server.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8f, 0.6f);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Splash", splash);
        tag.putFloat("BreakRadius", breakRadius);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        splash = tag.getFloat("Splash");
        breakRadius = tag.getFloat("BreakRadius");
    }
}
