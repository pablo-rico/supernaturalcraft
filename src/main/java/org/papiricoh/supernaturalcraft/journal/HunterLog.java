package org.papiricoh.supernaturalcraft.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What a hunter has written down: the creatures they have seen and slain, the things they have
 * held, which journal entries they have read and marked, and their library of spell designs.
 * Mutable, stored as a data attachment that survives death (the journal is not lost with the body).
 */
public class HunterLog {

    public static final int MAX_BOOKMARKS = 12;
    public static final int LIBRARY_SLOTS = 24;

    public static final Codec<HunterLog> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("seen", List.of()).forGetter(d -> List.copyOf(d.seen)),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(d -> Map.copyOf(d.kills)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(d -> List.copyOf(d.items)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("read", List.of()).forGetter(d -> List.copyOf(d.read)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("bookmarks", List.of()).forGetter(d -> List.copyOf(d.bookmarks)),
            Spell.CODEC.listOf().optionalFieldOf("designs", List.of()).forGetter(d -> List.copyOf(d.designs))
    ).apply(i, HunterLog::new));

    private final Set<ResourceLocation> seen;
    private final Map<ResourceLocation, Integer> kills;
    private final Set<ResourceLocation> items;
    private final Set<ResourceLocation> read;
    private final List<ResourceLocation> bookmarks;
    private final List<Spell> designs;

    /** Transient: something the journal shows has changed. */
    public boolean dirty = true;
    /** Transient: the library has changed. */
    public boolean libraryDirty = true;

    public HunterLog() {
        this(List.of(), Map.of(), List.of(), List.of(), List.of(), List.of());
    }

    private HunterLog(List<ResourceLocation> seen, Map<ResourceLocation, Integer> kills, List<ResourceLocation> items,
                      List<ResourceLocation> read, List<ResourceLocation> bookmarks, List<Spell> designs) {
        this.seen = new LinkedHashSet<>(seen);
        this.kills = new HashMap<>(kills);
        this.items = new LinkedHashSet<>(items);
        this.read = new LinkedHashSet<>(read);
        this.bookmarks = new ArrayList<>(bookmarks.stream().distinct().limit(MAX_BOOKMARKS).toList());
        this.designs = new ArrayList<>(designs.stream().limit(LIBRARY_SLOTS).toList());
        while (this.designs.size() < LIBRARY_SLOTS) this.designs.add(Spell.EMPTY);
    }

    // --- the bestiary ---------------------------------------------------------------------------

    /** @return true if this creature had never been seen before */
    public boolean see(ResourceLocation entity) {
        boolean added = seen.add(entity);
        dirty |= added;
        return added;
    }

    public void kill(ResourceLocation entity) {
        see(entity);
        kills.merge(entity, 1, Integer::sum);
        dirty = true;
    }

    public boolean hasSeen(ResourceLocation entity) {
        return seen.contains(entity);
    }

    public int kills(ResourceLocation entity) {
        return kills.getOrDefault(entity, 0);
    }

    public Set<ResourceLocation> seen() {
        return Collections.unmodifiableSet(seen);
    }

    public Map<ResourceLocation, Integer> kills() {
        return Collections.unmodifiableMap(kills);
    }

    // --- things held ----------------------------------------------------------------------------

    /** @return true if this item had never been held before */
    public boolean obtain(ResourceLocation item) {
        boolean added = items.add(item);
        dirty |= added;
        return added;
    }

    public boolean has(ResourceLocation item) {
        return items.contains(item);
    }

    public Set<ResourceLocation> items() {
        return Collections.unmodifiableSet(items);
    }

    // --- reading --------------------------------------------------------------------------------

    public boolean markRead(ResourceLocation entry) {
        boolean added = read.add(entry);
        dirty |= added;
        return added;
    }

    public Set<ResourceLocation> read() {
        return Collections.unmodifiableSet(read);
    }

    /**
     * Marks or unmarks an entry. A full set of bookmarks refuses another.
     *
     * @return whether the entry is bookmarked afterwards
     */
    public boolean toggleBookmark(ResourceLocation entry) {
        dirty = true;
        if (bookmarks.remove(entry)) return false;
        if (bookmarks.size() >= MAX_BOOKMARKS) return false;
        bookmarks.add(entry);
        return true;
    }

    public List<ResourceLocation> bookmarks() {
        return Collections.unmodifiableList(bookmarks);
    }

    // --- the library ----------------------------------------------------------------------------

    public Spell design(int slot) {
        return slot >= 0 && slot < LIBRARY_SLOTS ? designs.get(slot) : Spell.EMPTY;
    }

    /** Saves (or, with {@link Spell#EMPTY}, clears) a design. Out-of-range slots are ignored. */
    public boolean setDesign(int slot, Spell spell) {
        if (slot < 0 || slot >= LIBRARY_SLOTS) return false;
        designs.set(slot, spell);
        libraryDirty = true;
        return true;
    }

    public List<Spell> designs() {
        return Collections.unmodifiableList(designs);
    }
}
