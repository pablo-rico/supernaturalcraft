package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.memory.scenes.Scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The memories' world state (v0.18, kept on the overworld): what each hunter's shrines hold (so a guest can relive a memory while
 * its hunter is away) and the memories staged right now, each with the private arena that wrote it, its figures, who is inside
 * and where they stepped in from.
 */
public class MemoryData extends SavedData {

    private static final String NAME = "supernaturalcraft_memories";

    /** Where a visitor stepped in from (they go back there). */
    public record Return(Vec3 pos, float yRot, float xRot) {
    }

    /** One staged memory. */
    public static final class Stage {
        public static final int WRITING = 0, LIVE = 1, RESTORING = 2;

        public final ResourceKey<Level> dimension;
        public final BlockPos centre;
        public final UUID owner;
        public final Memory memory;
        public final ArenaController arena;
        public final List<UUID> figures = new ArrayList<>();
        public final Map<UUID, Return> visitors = new LinkedHashMap<>();
        public int status = WRITING;
        /** Cells of the scene written so far. */
        public int cursor;
        /** Where the way out stands (its lower block), once written. */
        public @Nullable BlockPos exit;
        public long lastOccupied;
        /** The scene's entry (clamped), tint and title, for whoever arrives while it is live. */
        public int[] entry = {0, 0, -8};
        public int tint = 0xFFFFFFFF;
        public String title = "";
        /** The scene being written; only known while the server runs (a stage still writing when it stopped is put back). */
        public transient @Nullable Scene scene;

        public Stage(ResourceKey<Level> dimension, BlockPos centre, UUID owner, Memory memory, ArenaController arena) {
            this.dimension = dimension;
            this.centre = centre.immutable();
            this.owner = owner;
            this.memory = memory;
            this.arena = arena;
        }
    }

    private final Map<UUID, List<Memory>> shrines = new LinkedHashMap<>();
    private final List<Stage> stages = new ArrayList<>();

    public static MemoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(MemoryData::new, MemoryData::load, null), NAME);
    }

    // --- shrines ---------------------------------------------------------------------------------------------------------

    public List<Memory> shrines(UUID owner) {
        return shrines.getOrDefault(owner, List.of());
    }

    /** @return whether it changed */
    public boolean setShrines(UUID owner, List<Memory> list) {
        if (list.equals(shrines.get(owner))) return false;
        shrines.put(owner, List.copyOf(list));
        setDirty();
        return true;
    }

    // --- stages ----------------------------------------------------------------------------------------------------------

    public Collection<Stage> stages() {
        return List.copyOf(stages);
    }

    public @Nullable Stage stage(ResourceKey<Level> dimension, BlockPos centre) {
        for (Stage s : stages) if (s.dimension.equals(dimension) && s.centre.equals(centre)) return s;
        return null;
    }

    public void add(Stage s) {
        stages.add(s);
        setDirty();
    }

    public void remove(Stage s) {
        stages.remove(s);
        setDirty();
    }

    // --- saving ----------------------------------------------------------------------------------------------------------

    private static MemoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        MemoryData d = new MemoryData();
        CompoundTag sh = tag.getCompound("Shrines");
        for (String key : sh.getAllKeys()) {
            try {
                UUID owner = UUID.fromString(key);
                List<Memory> list = Memory.CODEC.listOf().parse(NbtOps.INSTANCE, sh.get(key)).result().orElse(List.of());
                d.shrines.put(owner, List.copyOf(list));
            } catch (IllegalArgumentException ignored) {
            }
        }
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("Stages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            ResourceLocation dim = ResourceLocation.tryParse(c.getString("Dimension"));
            Memory memory = Memory.CODEC.parse(NbtOps.INSTANCE, c.get("Memory")).result().orElse(null);
            if (dim == null || memory == null || !c.hasUUID("Owner")) continue;
            Stage s = new Stage(ResourceKey.create(Registries.DIMENSION, dim), NbtUtils.readBlockPos(c, "Centre").orElse(BlockPos.ZERO),
                    c.getUUID("Owner"), memory, ArenaController.load(c.getCompound("Arena"), blocks));
            for (Tag u : c.getList("Figures", Tag.TAG_INT_ARRAY)) s.figures.add(NbtUtils.loadUUID(u));
            for (Tag v : c.getList("Visitors", Tag.TAG_COMPOUND)) {
                CompoundTag vc = (CompoundTag) v;
                if (!vc.hasUUID("Id")) continue;
                s.visitors.put(vc.getUUID("Id"), new Return(new Vec3(vc.getDouble("X"), vc.getDouble("Y"), vc.getDouble("Z")),
                        vc.getFloat("YRot"), vc.getFloat("XRot")));
            }
            s.status = c.getInt("Status");
            s.cursor = c.getInt("Cursor");
            if (c.contains("Exit")) s.exit = BlockPos.of(c.getLong("Exit"));
            s.lastOccupied = c.getLong("LastOccupied");
            int[] e = c.getIntArray("Entry");
            if (e.length == 3) s.entry = e;
            if (c.contains("Tint")) s.tint = c.getInt("Tint");
            s.title = c.getString("Title");
            d.stages.add(s);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sh = new CompoundTag();
        shrines.forEach((owner, list) -> Memory.CODEC.listOf().encodeStart(NbtOps.INSTANCE, list).result()
                .ifPresent(t -> sh.put(owner.toString(), t)));
        tag.put("Shrines", sh);
        ListTag list = new ListTag();
        for (Stage s : stages) {
            CompoundTag c = new CompoundTag();
            c.putString("Dimension", s.dimension.location().toString());
            c.put("Centre", NbtUtils.writeBlockPos(s.centre));
            c.putUUID("Owner", s.owner);
            Memory.CODEC.encodeStart(NbtOps.INSTANCE, s.memory).result().ifPresent(t -> c.put("Memory", t));
            c.put("Arena", s.arena.save());
            ListTag figs = new ListTag();
            s.figures.forEach(u -> figs.add(NbtUtils.createUUID(u)));
            c.put("Figures", figs);
            ListTag vis = new ListTag();
            s.visitors.forEach((id, r) -> {
                CompoundTag vc = new CompoundTag();
                vc.putUUID("Id", id);
                vc.putDouble("X", r.pos().x);
                vc.putDouble("Y", r.pos().y);
                vc.putDouble("Z", r.pos().z);
                vc.putFloat("YRot", r.yRot());
                vc.putFloat("XRot", r.xRot());
                vis.add(vc);
            });
            c.put("Visitors", vis);
            c.putInt("Status", s.status);
            c.putInt("Cursor", s.cursor);
            if (s.exit != null) c.putLong("Exit", s.exit.asLong());
            c.putLong("LastOccupied", s.lastOccupied);
            c.putIntArray("Entry", s.entry);
            c.putInt("Tint", s.tint);
            c.putString("Title", s.title);
            list.add(c);
        }
        tag.put("Stages", list);
        return tag;
    }
}
