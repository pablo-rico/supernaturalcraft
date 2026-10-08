package org.papiricoh.supernaturalcraft.entity.boss.michael.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * One of Michael's steel feathers, loosed in fans from his open wings: it flies straight (no drop), cuts whatever it meets
 * and is gone a moment after it strikes the ground.
 */
public class SteelFeatherEntity extends AbstractArrow {

    public static final float DAMAGE = 5f;
    public static final float SPEED = 1.4f;
    private boolean spent;

    public SteelFeatherEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
    }

    /** One feather from {@code from}, flying along {@code dir}. */
    public static SteelFeatherEntity loose(LuciferEntity boss, Vec3 from, Vec3 dir) {
        SteelFeatherEntity f = new SteelFeatherEntity(AllEntities.STEEL_FEATHER.get(), boss.level());
        f.setOwner(boss);
        f.setPos(from.x, from.y, from.z);
        f.shoot(dir.x, dir.y, dir.z, SPEED, 0.0f);
        return f;
    }

    /** A fan of {@code count} feathers spread over {@code spread} degrees, from {@code from} toward {@code at}. */
    public static void fan(LuciferEntity boss, Vec3 from, Vec3 at, int count, float spread) {
        Vec3 dir = at.subtract(from).normalize();
        double yaw = Math.atan2(dir.z, dir.x), pitch = Math.asin(Math.max(-1, Math.min(1, dir.y)));
        for (int i = 0; i < count; i++) {
            double off = count == 1 ? 0 : Math.toRadians(-spread / 2 + spread * i / (count - 1));
            double a = yaw + off;
            Vec3 d = new Vec3(Math.cos(a) * Math.cos(pitch), Math.sin(pitch), Math.sin(a) * Math.cos(pitch));
            boss.level().addFreshEntity(loose(boss, from, d));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (tickCount > 100 || (inGround && inGroundTime > 30))) discard();
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !spent && e instanceof LivingEntity living
                && !(getOwner() instanceof LuciferEntity boss && !MichaelAttacks.isFoe(boss, living));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        spent = true;
        float mult = getOwner() instanceof LuciferEntity boss ? boss.attackDamageMultiplier() : 1f;
        hit.getEntity().hurt(AllDamageTypes.source(level(), AllDamageTypes.STEEL_FEATHER, this, getOwner()), DAMAGE * mult);
        if (level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.2);
        discard();
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.CHAIN_HIT;
    }
}
