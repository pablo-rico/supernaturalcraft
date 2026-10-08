package org.papiricoh.supernaturalcraft.entity.boss.michael.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelAttacks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * One of the twelve spears of light of Michael's halo, loosed in volleys in his true form: it hangs where the halo let it
 * go for a moment, turning to its mark, then flies straight (no drop) and bursts where it lands.
 */
public class LightSpearEntity extends AbstractArrow {

    public static final float DAMAGE = 8f;
    public static final float SPEED = 1.8f;
    private int hold;
    private @Nullable Vec3 aim;

    public LightSpearEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
        setNoGravity(true);
    }

    /** A spear at {@code from} that waits {@code hold} ticks, then flies at {@code at}. */
    public static LightSpearEntity raise(LuciferEntity boss, Vec3 from, Vec3 at, int hold) {
        LightSpearEntity s = new LightSpearEntity(AllEntities.LIGHT_SPEAR.get(), boss.level());
        s.setOwner(boss);
        s.setPos(from.x, from.y, from.z);
        s.hold = hold;
        s.aim = at;
        s.setNoPhysics(true);
        Vec3 d = at.subtract(from);
        // Point it at its mark while it waits.
        s.setYRot((float) (Math.atan2(d.x, d.z) * 180 / Math.PI));
        s.setXRot((float) (Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI));
        s.yRotO = s.getYRot();
        s.xRotO = s.getXRot();
        return s;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (hold > 0) {
                setDeltaMovement(Vec3.ZERO);
                if (--hold == 0 && aim != null) {
                    setNoPhysics(false);
                    Vec3 d = aim.subtract(position());
                    shoot(d.x, d.y, d.z, SPEED, 0.0f);
                    playSound(AllSounds.MICHAEL_LANCE_THROW.get(), 0.8f, 1.6f);
                }
            }
            if (tickCount > 140 || (inGround && inGroundTime > 4)) {
                discard();
                return;
            }
        }
        super.tick();
        if (level().isClientSide && hold <= 0) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    /** Whether it is still hanging in the air, waiting. */
    public boolean holding() {
        return hold > 0;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return hold <= 0 && super.canHitEntity(e) && e instanceof LivingEntity living
                && !(getOwner() instanceof LuciferEntity boss && !MichaelAttacks.isFoe(boss, living));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        float mult = getOwner() instanceof LuciferEntity boss ? boss.attackDamageMultiplier() : 1f;
        hit.getEntity().hurt(AllDamageTypes.source(level(), AllDamageTypes.SMITE, this, getOwner()), DAMAGE * mult);
        burst();
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        burst();
    }

    private void burst() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(AllParticles.GRACE.get(), getX(), getY(), getZ(), 12, 0.3, 0.3, 0.3, 0.08);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return AllSounds.MICHAEL_LANCE_IMPACT.get();
    }
}
