package org.papiricoh.supernaturalcraft.author;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Author's world state, kept in the overworld: where his cabin stands (once computed, and whether it is known to be
 * built), whether "Find the Author" has been cast, and who has met him, been given his spell's page, or passed his test.
 */
public class AuthorSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_author";

    private @Nullable BlockPos cabin;
    private int rotation;
    private boolean built;
    private boolean spellCast;
    private final Set<UUID> met = new HashSet<>(), rewarded = new HashSet<>(), paged = new HashSet<>();

    /** The overworld's record (whatever level is asked). */
    public static AuthorSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(AuthorSavedData::new, AuthorSavedData::load, null), NAME);
    }

    private static AuthorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuthorSavedData d = new AuthorSavedData();
        d.cabin = NbtUtils.readBlockPos(tag, "Cabin").orElse(null);
        d.rotation = tag.getInt("Rotation");
        d.built = tag.getBoolean("Built");
        d.spellCast = tag.getBoolean("SpellCast");
        read(tag, "Met", d.met);
        read(tag, "Rewarded", d.rewarded);
        read(tag, "Paged", d.paged);
        return d;
    }

    private static void read(CompoundTag tag, String key, Set<UUID> into) {
        for (Tag t : tag.getList(key, Tag.TAG_INT_ARRAY)) into.add(NbtUtils.loadUUID(t));
    }

    private static void write(CompoundTag tag, String key, Set<UUID> from) {
        ListTag list = new ListTag();
        for (UUID u : from) list.add(NbtUtils.createUUID(u));
        tag.put(key, list);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (cabin != null) tag.put("Cabin", NbtUtils.writeBlockPos(cabin));
        tag.putInt("Rotation", rotation);
        tag.putBoolean("Built", built);
        tag.putBoolean("SpellCast", spellCast);
        write(tag, "Met", met);
        write(tag, "Rewarded", rewarded);
        write(tag, "Paged", paged);
        return tag;
    }

    /** The cabin's floor centre ({@link CabinLayout} origin), or null until it has been worked out. */
    public @Nullable BlockPos cabin() {
        return cabin;
    }

    /** The cabin's turn ({@link CabinLayout#rotate}). */
    public int rotation() {
        return rotation;
    }

    /** Whether the cabin is known to stand at {@link #cabin()} (found in the world, or built there). */
    public boolean built() {
        return built;
    }

    public void setCabin(@Nullable BlockPos origin, int rotation, boolean built) {
        this.cabin = origin == null ? null : origin.immutable();
        this.rotation = Math.floorMod(rotation, 4);
        this.built = built;
        setDirty();
    }

    public boolean spellCast() {
        return spellCast;
    }

    public void setSpellCast(boolean cast) {
        spellCast = cast;
        setDirty();
    }

    /** Hunters who have talked to him. */
    public boolean met(UUID hunter) {
        return met.contains(hunter);
    }

    public boolean meet(UUID hunter) {
        boolean first = met.add(hunter);
        if (first) setDirty();
        return first;
    }

    /** Hunters who passed his test (and got "The End", the Pen and the amulet). */
    public boolean rewarded(UUID hunter) {
        return rewarded.contains(hunter);
    }

    public boolean reward(UUID hunter) {
        boolean first = rewarded.add(hunter);
        if (first) setDirty();
        return first;
    }

    /** Hunters who were handed the page of "Find the Author". */
    public boolean paged(UUID hunter) {
        return paged.contains(hunter);
    }

    public boolean page(UUID hunter) {
        boolean first = paged.add(hunter);
        if (first) setDirty();
        return first;
    }

    /** Everything as a tag (tests save and restore the world's state around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    public void restore(CompoundTag tag) {
        AuthorSavedData d = load(tag, null);
        cabin = d.cabin;
        rotation = d.rotation;
        built = d.built;
        spellCast = d.spellCast;
        met.clear();
        met.addAll(d.met);
        rewarded.clear();
        rewarded.addAll(d.rewarded);
        paged.clear();
        paged.addAll(d.paged);
        setDirty();
    }
}
