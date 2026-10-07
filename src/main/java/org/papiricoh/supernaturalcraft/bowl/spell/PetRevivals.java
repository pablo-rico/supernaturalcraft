package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bringing dead pets back from their {@link PetLedger} snapshot: the Revive Pet spell, and the
 * crossroads demon's "recover what was lost" ({@link #HOOK}).
 */
public final class PetRevivals {

    /** Saved state a revived pet must not keep: it comes back whole, still and unhurt. */
    static final List<String> STRIPPED = List.of("Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Fire", "FallDistance",
            "Motion", "active_effects", "AbsorptionAmount", "Leash", "Passengers", "Brain", "Air");

    /** The crossroads demon's way to bring back a player's most recently dead pet. */
    public static final CrossroadsHooks.PetRevival HOOK = new CrossroadsHooks.PetRevival() {
        @Override
        public boolean available(ServerPlayer player) {
            return PetLedger.get(player.server).mostRecentDead(player.getUUID()).isPresent();
        }

        @Override
        public boolean revive(ServerPlayer player, Vec3 at) {
            Optional<PetLedger.Entry> e = PetLedger.get(player.server).mostRecentDead(player.getUUID());
            return e.isPresent() && PetRevivals.revive(player.serverLevel(), e.get().pet(), at) != null;
        }
    };

    private PetRevivals() {
    }

    /** Why {@code pet} cannot be brought back (a translation key), or null if it can. */
    @Nullable
    public static String whyNot(ServerLevel level, UUID pet) {
        Optional<PetLedger.Entry> entry = PetLedger.get(level.getServer()).entry(pet);
        if (living(level, pet) != null) return "message.supernaturalcraft.revive.alive";
        if (entry.isEmpty() || !entry.get().revivable()) return "message.supernaturalcraft.revive.no_trace";
        return null;
    }

    /** The pet alive somewhere in the world, if it is. */
    @Nullable
    static Entity living(ServerLevel level, UUID pet) {
        for (ServerLevel l : level.getServer().getAllLevels()) {
            Entity e = l.getEntity(pet);
            if (e != null && e.isAlive() && !e.isRemoved()) return e;
        }
        return null;
    }

    /**
     * Brings {@code pet} back at {@code at} in {@code level}, whole, from its snapshot.
     * @return the pet, or null if it could not be (alive already, nothing recorded)
     */
    @Nullable
    public static Entity revive(ServerLevel level, UUID pet, Vec3 at) {
        if (whyNot(level, pet) != null) return null;
        PetLedger ledger = PetLedger.get(level.getServer());
        PetLedger.Entry entry = ledger.entry(pet).orElseThrow();
        // Its body may still be lying there (the death animation): it gives way to the living pet.
        for (ServerLevel l : level.getServer().getAllLevels()) {
            Entity corpse = l.getEntity(pet);
            if (corpse != null) corpse.discard();
        }
        CompoundTag tag = entry.snapshot().copy();
        STRIPPED.forEach(tag::remove);
        Optional<Entity> made = EntityType.create(tag, level);
        if (made.isEmpty()) return null;
        Entity e = made.get();
        e.moveTo(at.x, at.y, at.z, e.getYRot(), 0);
        e.setDeltaMovement(Vec3.ZERO);
        if (e instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
            living.deathTime = 0;
            living.hurtTime = 0;
        }
        e.clearFire();
        if (!level.addFreshEntity(e)) return null;
        ledger.revived(e);
        level.sendParticles(ParticleTypes.HEART, at.x, at.y + 1.0, at.z, 6, 0.4, 0.3, 0.4, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 20, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.6f, 1.4f);
        return e;
    }
}
