package org.papiricoh.supernaturalcraft.bowl.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every collared (or named and tamed) pet the world knows of: whose it is, what it is called, where
 * it was last seen, and, once it has died, a snapshot of it to bring back (the Revive Pet spell, the
 * crossroads' "recover what was lost"). Kept on the overworld. Snapshots live here, never on the
 * collar item, so a collar cannot be duplicated into two pets.
 */
public class PetLedger extends SavedData {

    private static final String NAME = "supernaturalcraft_pet_ledger";

    /** What the ledger knows of one pet. {@code snapshot} is set only while it lies dead. */
    public record Entry(UUID pet, UUID owner, String name, ResourceLocation type, ResourceKey<Level> dimension,
                        BlockPos lastPos, boolean alive, long stamp, @Nullable CompoundTag snapshot) {

        public boolean revivable() {
            return !alive && snapshot != null;
        }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    public static PetLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PetLedger::new, PetLedger::load, null), NAME);
    }

    private static PetLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        PetLedger d = new PetLedger();
        ListTag list = tag.getList("Pets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceLocation type = ResourceLocation.tryParse(t.getString("Type"));
            ResourceLocation dim = ResourceLocation.tryParse(t.getString("Dimension"));
            if (type == null || dim == null || !t.hasUUID("Pet") || !t.hasUUID("Owner")) continue;
            BlockPos pos = NbtUtils.readBlockPos(t, "Pos").orElse(BlockPos.ZERO);
            Entry e = new Entry(t.getUUID("Pet"), t.getUUID("Owner"), t.getString("Name"), type,
                    ResourceKey.create(Registries.DIMENSION, dim), pos, t.getBoolean("Alive"), t.getLong("Stamp"),
                    t.contains("Snapshot", Tag.TAG_COMPOUND) ? t.getCompound("Snapshot") : null);
            d.entries.put(e.pet(), e);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Pet", e.pet());
            t.putUUID("Owner", e.owner());
            t.putString("Name", e.name());
            t.putString("Type", e.type().toString());
            t.putString("Dimension", e.dimension().location().toString());
            t.put("Pos", NbtUtils.writeBlockPos(e.lastPos()));
            t.putBoolean("Alive", e.alive());
            t.putLong("Stamp", e.stamp());
            if (e.snapshot() != null) t.put("Snapshot", e.snapshot());
            list.add(t);
        }
        tag.put("Pets", list);
        return tag;
    }

    public Optional<Entry> entry(UUID pet) {
        return Optional.ofNullable(entries.get(pet));
    }

    /** The pet is alive and here: remember where (and whose it is, and what it is called). */
    public void seen(TamableAnimal pet) {
        UUID owner = ownerOf(pet);
        if (owner == null) return;
        entries.put(pet.getUUID(), new Entry(pet.getUUID(), owner, nameOf(pet), typeOf(pet), pet.level().dimension(),
                pet.blockPosition(), true, pet.level().getGameTime(), null));
        setDirty();
    }

    /** The pet died: remember it as it was, to bring it back. */
    public void died(TamableAnimal pet) {
        UUID owner = ownerOf(pet);
        if (owner == null) return;
        entries.put(pet.getUUID(), new Entry(pet.getUUID(), owner, nameOf(pet), typeOf(pet), pet.level().dimension(),
                pet.blockPosition(), false, pet.level().getGameTime(), snapshot(pet)));
        setDirty();
    }

    /** The pet is back among the living (its snapshot is spent). */
    public void revived(Entity pet) {
        Entry old = entries.get(pet.getUUID());
        if (old == null) return;
        entries.put(pet.getUUID(), new Entry(old.pet(), old.owner(), old.name(), old.type(), pet.level().dimension(),
                pet.blockPosition(), true, pet.level().getGameTime(), null));
        setDirty();
    }

    /** {@code owner}'s pet that died last and can still be brought back. */
    public Optional<Entry> mostRecentDead(UUID owner) {
        return entries.values().stream().filter(e -> e.owner().equals(owner) && e.revivable())
                .max(Comparator.comparingLong(Entry::stamp));
    }

    /** The entity as it would be saved, with its type id: what {@code EntityType.create(tag, level)} reads. */
    public static CompoundTag snapshot(Entity e) {
        CompoundTag tag = e.saveWithoutId(new CompoundTag());
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
        return tag;
    }

    @Nullable
    private UUID ownerOf(TamableAnimal pet) {
        UUID owner = pet.getOwnerUUID();
        if (owner != null) return owner;
        Entry old = entries.get(pet.getUUID());
        return old == null ? null : old.owner();
    }

    static String nameOf(Entity pet) {
        return pet.hasCustomName() ? pet.getCustomName().getString() : pet.getType().getDescription().getString();
    }

    static ResourceLocation typeOf(Entity pet) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType());
    }
}
