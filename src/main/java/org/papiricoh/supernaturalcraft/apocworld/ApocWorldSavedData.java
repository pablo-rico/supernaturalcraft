package org.papiricoh.supernaturalcraft.apocworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Apocalypse World's state in this world (v0.41): where its landing is (found once), and the hunters owed AU Michael's flight
 * who were away when he fled (it is settled on their next login). Kept in the Overworld's storage.
 */
public class ApocWorldSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_apocalypse_world";

    private @Nullable BlockPos landing;
    private final Set<UUID> owedFlight = new LinkedHashSet<>();

    public static ApocWorldSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ApocWorldSavedData::new, ApocWorldSavedData::load, null), NAME);
    }

    private static ApocWorldSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ApocWorldSavedData d = new ApocWorldSavedData();
        if (tag.contains("Landing")) d.landing = NbtUtils.readBlockPos(tag, "Landing").orElse(null);
        for (Tag t : tag.getList("OwedFlight", Tag.TAG_INT_ARRAY)) d.owedFlight.add(NbtUtils.loadUUID(t));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (landing != null) tag.put("Landing", NbtUtils.writeBlockPos(landing));
        ListTag owed = new ListTag();
        for (UUID u : owedFlight) owed.add(NbtUtils.createUUID(u));
        tag.put("OwedFlight", owed);
        return tag;
    }

    public @Nullable BlockPos landing() {
        return landing;
    }

    public void setLanding(@Nullable BlockPos at) {
        landing = at == null ? null : at.immutable();
        setDirty();
    }

    public void owe(UUID hunter) {
        if (owedFlight.add(hunter)) setDirty();
    }

    /** @return whether {@code hunter} was owed it (and is no longer) */
    public boolean settle(UUID hunter) {
        boolean was = owedFlight.remove(hunter);
        if (was) setDirty();
        return was;
    }

    public boolean owed(UUID hunter) {
        return owedFlight.contains(hunter);
    }
}
