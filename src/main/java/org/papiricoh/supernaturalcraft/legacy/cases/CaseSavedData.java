package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The case sites that are out in the world (overworld): each with the set piece it wrote (an {@link ArenaController} of its
 * own, never registered as a fight arena: no dome, no barrier), the creatures still to be put down, the hostage, and when its
 * hunter was last near.
 */
public class CaseSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_cases";

    /** One active site. */
    public static final class Site {
        public final UUID owner;
        public final int index;
        public final BlockPos centre;
        public final ArenaController piece;
        public final Set<UUID> targets = new LinkedHashSet<>();
        public @Nullable UUID hostage;
        public long lastNear;
        /** 0 while out; {@code CaseFile.SOLVED} or {@code LOST} once over and waiting for its hunter to hear of it. */
        public int outcome;

        public Site(UUID owner, int index, BlockPos centre, ArenaController piece) {
            this.owner = owner;
            this.index = index;
            this.centre = centre.immutable();
            this.piece = piece;
        }

        public String key() {
            return CaseSavedData.key(owner, index);
        }
    }

    private final Map<String, Site> sites = new LinkedHashMap<>();

    public static CaseSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(CaseSavedData::new, CaseSavedData::load, null), NAME);
    }

    public static String key(UUID owner, int index) {
        return owner + "#" + index;
    }

    private static CaseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CaseSavedData d = new CaseSavedData();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("Sites", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Site s = new Site(c.getUUID("Owner"), c.getInt("Index"), NbtUtils.readBlockPos(c, "Centre").orElse(BlockPos.ZERO),
                    ArenaController.load(c.getCompound("Piece"), blocks));
            for (Tag u : c.getList("Targets", Tag.TAG_INT_ARRAY)) s.targets.add(NbtUtils.loadUUID(u));
            if (c.hasUUID("Hostage")) s.hostage = c.getUUID("Hostage");
            s.lastNear = c.getLong("LastNear");
            s.outcome = c.getInt("Outcome");
            d.sites.put(s.key(), s);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Site s : sites.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("Owner", s.owner);
            c.putInt("Index", s.index);
            c.put("Centre", NbtUtils.writeBlockPos(s.centre));
            c.put("Piece", s.piece.save());
            ListTag targets = new ListTag();
            s.targets.forEach(u -> targets.add(NbtUtils.createUUID(u)));
            c.put("Targets", targets);
            c.putInt("Outcome", s.outcome);
            if (s.hostage != null) c.putUUID("Hostage", s.hostage);
            c.putLong("LastNear", s.lastNear);
            list.add(c);
        }
        tag.put("Sites", list);
        return tag;
    }

    public @Nullable Site get(UUID owner, int index) {
        return sites.get(key(owner, index));
    }

    public @Nullable Site get(String key) {
        return sites.get(key);
    }

    public void put(Site site) {
        sites.put(site.key(), site);
        setDirty();
    }

    public void remove(Site site) {
        sites.remove(site.key());
        setDirty();
    }

    public Collection<Site> all() {
        return sites.values();
    }

    public List<Site> snapshot() {
        return new ArrayList<>(sites.values());
    }
}
