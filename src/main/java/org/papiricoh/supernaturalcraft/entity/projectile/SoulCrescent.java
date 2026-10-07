package org.papiricoh.supernaturalcraft.entity.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.papiricoh.supernaturalcraft.magic.spell.ResolvedSpell;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The Soul Scythe's thrown crescent: wide, flat, passes through every body in its way. */
public class SoulCrescent extends ThrowableProjectile {

    public static final float DAMAGE = 8f;
    public static final int LIFETIME = 30;
    private final Set<UUID> cut = new HashSet<>();

    public SoulCrescent(EntityType<? extends SoulCrescent> type, Level level) {
        super(type, level);
    }

    public SoulCrescent(Level level, LivingEntity owner) {
        super(AllEntities.SOUL_CRESCENT.get(), owner, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIFETIME) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !cut.contains(target.getUUID())
                && !(getOwner() instanceof LivingEntity o && ResolvedSpell.isFriend(o, target));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level() instanceof ServerLevel server && cut.add(result.getEntity().getUUID())) {
            result.getEntity().hurt(AllDamageTypes.source(server, AllDamageTypes.SPELL, this, getOwner()), DAMAGE);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, getX(), getY(), getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) discard();
    }
}
