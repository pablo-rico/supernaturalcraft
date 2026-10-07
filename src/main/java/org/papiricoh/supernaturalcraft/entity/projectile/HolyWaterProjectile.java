package org.papiricoh.supernaturalcraft.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/** Splashes in a small radius: scalds demons, puts out burning things, harmless to everyone else. */
public class HolyWaterProjectile extends ThrowableItemProjectile {

    public static final double RADIUS = 2.5;
    public static final float DEMON_DAMAGE = 8.0f;

    public HolyWaterProjectile(EntityType<? extends HolyWaterProjectile> type, Level level) {
        super(type, level);
    }

    public HolyWaterProjectile(Level level, LivingEntity owner) {
        super(AllEntities.HOLY_WATER.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return AllItems.HOLY_WATER.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            AABB area = getBoundingBox().inflate(RADIUS, RADIUS / 2, RADIUS);
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, area)) {
                if (target.distanceToSqr(this) > RADIUS * RADIUS * 4) continue;
                target.clearFire();
                if (target.getType().is(AllTags.Entities.DEMONS)) {
                    target.hurt(AllDamageTypes.source(server, AllDamageTypes.HOLY_WATER, this, getOwner()), DEMON_DAMAGE);
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                            target.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                    server.playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0f, 0.7f);
                }
            }
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 40, RADIUS / 2, 0.2, RADIUS / 2, 0.1);
            server.playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0f, 1.2f);
            discard();
        }
    }
}
