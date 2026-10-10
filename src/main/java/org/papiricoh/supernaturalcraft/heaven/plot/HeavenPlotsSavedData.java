package org.papiricoh.supernaturalcraft.heaven.plot;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every plot of Heaven (v0.18), kept in the level they stand in (Heaven itself; a test level on a test server): who has which
 * index, and the queue of plots still being written (one written at a time, in the order they were asked for).
 */
public class HeavenPlotsSavedData extends SavedData {

    public static final String NAME = "supernaturalcraft_heaven_plots";
    private final Map<UUID, HeavenPlot> byOwner = new LinkedHashMap<>();
    private final Map<Integer, HeavenPlot> byIndex = new HashMap<>();
    private final Deque<UUID> queue = new ArrayDeque<>();
    private int nextIndex = 1;

    public static HeavenPlotsSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    /** The data if this level has any, without creating it. */
    public static @Nullable HeavenPlotsSavedData peek(ServerLevel level) {
        return level.getDataStorage().get(factory(), NAME);
    }

    private static Factory<HeavenPlotsSavedData> factory() {
        return new Factory<>(HeavenPlotsSavedData::new, HeavenPlotsSavedData::load);
    }

    /** The data as saved in {@code tag} (what a restart reads back). */
    public static HeavenPlotsSavedData read(CompoundTag tag, HolderLookup.Provider registries) {
        return load(tag, registries);
    }

    private static HeavenPlotsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HeavenPlotsSavedData data = new HeavenPlotsSavedData();
        for (Tag t : tag.getList("Plots", Tag.TAG_COMPOUND)) data.put(HeavenPlot.load((CompoundTag) t));
        for (Tag u : tag.getList("Queue", Tag.TAG_INT_ARRAY)) data.queue.add(NbtUtils.loadUUID(u));
        data.nextIndex = Math.max(1, tag.getInt("Next"));
        for (HeavenPlot p : data.byOwner.values()) data.nextIndex = Math.max(data.nextIndex, p.index + 1);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        byOwner.values().forEach(p -> list.add(p.save()));
        tag.put("Plots", list);
        ListTag q = new ListTag();
        queue.forEach(u -> q.add(NbtUtils.createUUID(u)));
        tag.put("Queue", q);
        tag.putInt("Next", nextIndex);
        return tag;
    }

    private void put(HeavenPlot p) {
        byOwner.put(p.owner, p);
        byIndex.put(p.index, p);
    }

    public @Nullable HeavenPlot of(UUID owner) {
        return byOwner.get(owner);
    }

    public @Nullable HeavenPlot at(int index) {
        return byIndex.get(index);
    }

    public Collection<HeavenPlot> all() {
        return byOwner.values();
    }

    /** Gives {@code owner} the next free index (or {@code index} itself for the Roadhouse). */
    public HeavenPlot allocate(UUID owner, @Nullable Integer index, String name, HeavenPlotLayout.Style style, long seed) {
        HeavenPlot existing = byOwner.get(owner);
        if (existing != null) return existing;
        int i;
        if (index != null) {
            i = index;
        } else {
            while (byIndex.containsKey(nextIndex)) nextIndex++;
            i = nextIndex++;
        }
        HeavenPlot p = new HeavenPlot(owner, i, name, style, seed);
        put(p);
        setDirty();
        return p;
    }

    /** Puts a plot in the writing queue (at the back), unless it is already there. */
    public void enqueue(UUID owner) {
        if (!queue.contains(owner)) queue.addLast(owner);
        setDirty();
    }

    public @Nullable UUID head() {
        return queue.peekFirst();
    }

    public void dequeue(UUID owner) {
        queue.remove(owner);
        setDirty();
    }

    public boolean queued(UUID owner) {
        return queue.contains(owner);
    }

    public boolean writing() {
        return !queue.isEmpty();
    }
}
