package org.papiricoh.supernaturalcraft.entity.leviathan;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.papiricoh.supernaturalcraft.leviathan.Borax;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** A thrown bottle of borax spray (v0.25): bursts in a small radius ({@link Borax#splash}). */
public class ThrownBoraxEntity extends ThrowableItemProjectile {

    public static final double RADIUS = 2.5;

    public ThrownBoraxEntity(EntityType<? extends ThrownBoraxEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBoraxEntity(Level level, LivingEntity owner) {
        super(AllEntities.THROWN_BORAX.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return AllItems.BORAX_SPRAY.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            Borax.splash(server, position(), RADIUS, getOwner());
            server.playSound(null, blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0f, 1.4f);
            discard();
        }
    }
}
