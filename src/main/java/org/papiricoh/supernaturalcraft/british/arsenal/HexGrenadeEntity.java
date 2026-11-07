package org.papiricoh.supernaturalcraft.british.arsenal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;

/** A thrown hex grenade (v0.41): it bursts where it lands ({@link Hex#burst}). */
public class HexGrenadeEntity extends ThrowableItemProjectile {

    public HexGrenadeEntity(EntityType<? extends HexGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public HexGrenadeEntity(Level level, LivingEntity owner) {
        super(BritishRegistry.HEX_GRENADE_ENTITY.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return BritishRegistry.HEX_GRENADE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            Hex.burst(server, position(), this, getOwner());
            discard();
        }
    }
}
