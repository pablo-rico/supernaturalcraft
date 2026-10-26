package org.papiricoh.supernaturalcraft.entity.chain;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * The missiles of the Chain's rank and file, one class for three entity types: the Sulfur Priest's ball of burning sulfur
 * (sets its mark alight and leaves a puff of choking smoke), the Herald's lance of light (fast and straight, holy) and the
 * Tormentor's hook (drags its mark to the thrower and roots it). They never hit their own side ({@link Kin#sameSide}).
 */
public class ChainMissile extends ThrowableProjectile {

    public enum Kind { SULFUR, LANCE, HOOK }

    /** Damage of each; the hook's pull lasts {@link #HOOK_ROOT} ticks. */
    public static final float SULFUR_DAMAGE = 5, LANCE_DAMAGE = 7, HOOK_DAMAGE = 3;
    public static final int HOOK_ROOT = 30, LIFE = 60;

    public ChainMissile(EntityType<? extends ChainMissile> type, Level level) {
        super(type, level);
    }

    public static ChainMissile shoot(EntityType<? extends ChainMissile> type, LivingEntity owner, LivingEntity target, float speed) {
        ChainMissile m = type.create(owner.level());
        if (m == null) return null;
        m.setOwner(owner);
        Vec3 from = owner.getEyePosition().add(owner.getLookAngle().scale(0.6)).subtract(0, 0.25, 0);
        m.moveTo(from.x, from.y, from.z, owner.getYRot(), owner.getXRot());
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(from);
        m.shoot(to.x, to.y + (m.kind() == Kind.LANCE ? 0 : to.horizontalDistance() * 0.08), to.z, speed, 1.0f);
        owner.level().addFreshEntity(m);
        return m;
    }

    public Kind kind() {
        return getType() == AllEntities.LIGHT_LANCE.get() ? Kind.LANCE : getType() == AllEntities.TORMENTOR_HOOK.get() ? Kind.HOOK : Kind.SULFUR;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return switch (kind()) { case LANCE -> 0; case SULFUR -> 0.012; case HOOK -> 0.02; };
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            switch (kind()) {
                case SULFUR -> {
                    level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0.01, 0);
                    level().addParticle(new DustParticleOptions(new Vector3f(0.85f, 0.75f, 0.2f), 1.2f), getX(), getY(), getZ(), 0, 0, 0);
                }
                case LANCE -> level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
                case HOOK -> { }
            }
        } else if (tickCount > LIFE) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        return super.canHitEntity(target) && (owner == null || target != owner && !Kin.sameSide(owner, target));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity victim)) return;
        Entity owner = getOwner();
        switch (kind()) {
            case SULFUR -> {
                victim.hurt(AllDamageTypes.source(level, AllDamageTypes.SPELL, this, owner), SULFUR_DAMAGE);
                victim.igniteForSeconds(3);
            }
            case LANCE -> victim.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, this, owner), LANCE_DAMAGE);
            case HOOK -> {
                victim.hurt(level.damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), HOOK_DAMAGE);
                if (owner != null) {
                    Vec3 pull = owner.position().subtract(victim.position()).multiply(1, 0, 1);
                    victim.setDeltaMovement(pull.normalize().scale(Math.min(1.6, pull.length() * 0.25)).add(0, 0.3, 0));
                    victim.hurtMarked = true;
                }
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HOOK_ROOT, 4), owner);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel level)) return;
        if (kind() == Kind.SULFUR) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
            cloud.setRadius(1.6f);
            cloud.setDuration(60);
            cloud.setRadiusPerTick(-0.01f);
            cloud.setParticle(new DustParticleOptions(new Vector3f(0.8f, 0.72f, 0.25f), 1.4f));
            cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80));
            if (getOwner() instanceof LivingEntity l) cloud.setOwner(l);
            level.addFreshEntity(cloud);
        }
        if (kind() == Kind.LANCE) level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.08);
        discard();
    }
}
