package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.bowl.spell.PetLedger;
import org.papiricoh.supernaturalcraft.bowl.spell.PetRevivals;

import java.util.Optional;
import java.util.UUID;

/**
 * The wild REVIVE wish: a hunter's most recently dead pet, brought back from its {@link PetLedger} snapshot, or, if nothing of
 * it is left to bring back, made anew: a fresh pet of the same kind, the same name and the same owner (it even answers to the
 * same identity, so the ledger counts it alive again).
 */
public final class WildPets {

    /** What the ledger remembers of a dead pet. */
    public record Lost(UUID pet, String name, ResourceLocation type, long stamp, boolean snapshot) {
    }

    private WildPets() {
    }

    /**
     * {@code owner}'s pet that died last, snapshot or not, if it is not walking about after all. (The ledger only hands out
     * revivable entries, so its saved form is read.)
     */
    public static Optional<Lost> mostRecentDead(MinecraftServer server, UUID owner) {
        CompoundTag saved = PetLedger.get(server).save(new CompoundTag(), server.registryAccess());
        ListTag list = saved.getList("Pets", Tag.TAG_COMPOUND);
        Lost best = null;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.getBoolean("Alive") || !t.hasUUID("Owner") || !t.hasUUID("Pet") || !t.getUUID("Owner").equals(owner)) continue;
            ResourceLocation type = ResourceLocation.tryParse(t.getString("Type"));
            if (type == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(type)) continue;
            Lost lost = new Lost(t.getUUID("Pet"), t.getString("Name"), type, t.getLong("Stamp"), t.contains("Snapshot", Tag.TAG_COMPOUND));
            if (best == null || lost.stamp() > best.stamp()) best = lost;
        }
        if (best == null || living(server, best.pet()) != null) return Optional.empty();
        return Optional.of(best);
    }

    /** Brings {@code owner}'s last dead pet back at {@code at}. @return the pet, or null if there was none */
    public static @Nullable Entity revive(ServerPlayer owner, Vec3 at) {
        Optional<Lost> lost = mostRecentDead(owner.server, owner.getUUID());
        if (lost.isEmpty()) return null;
        Entity back = lost.get().snapshot() ? PetRevivals.revive(owner.serverLevel(), lost.get().pet(), at) : null;
        return back != null ? back : fresh(owner.serverLevel(), lost.get(), owner, at);
    }

    /** A new pet in place of {@code lost}: its kind, its name, tamed by {@code owner}. @return null if the kind cannot be made */
    public static @Nullable Entity fresh(ServerLevel level, Lost lost, ServerPlayer owner, Vec3 at) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(lost.type());
        // Whatever is left of the old body gives way to the new one.
        Entity corpse = living(level.getServer(), lost.pet());
        if (corpse != null) return null;
        for (ServerLevel l : level.getServer().getAllLevels()) {
            Entity e = l.getEntity(lost.pet());
            if (e != null) e.discard();
        }
        Entity e = type.create(level);
        if (e == null) return null;
        e.setUUID(lost.pet());
        e.moveTo(at.x, at.y, at.z, owner.getYRot() + 180, 0);
        if (e instanceof Mob mob) mob.finalizeSpawn(level, level.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (e instanceof TamableAnimal pet) {
            pet.setTame(true, true);
            pet.setOwnerUUID(owner.getUUID());
        }
        if (!lost.name().isEmpty() && !lost.name().equals(type.getDescription().getString())) e.setCustomName(Component.literal(lost.name()));
        if (e instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
        if (!level.addFreshEntity(e)) return null;
        PetLedger.get(level.getServer()).revived(e);
        level.sendParticles(ParticleTypes.HEART, at.x, at.y + 1.0, at.z, 6, 0.4, 0.3, 0.4, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 20, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.6f, 0.8f);
        return e;
    }

    private static @Nullable Entity living(MinecraftServer server, UUID id) {
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null && e.isAlive() && !e.isRemoved()) return e;
        }
        return null;
    }
}
