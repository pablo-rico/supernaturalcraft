package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Things owed to hunters who were away, or had no room, when they were due (v0.19): stakes handed back, a challenger's share
 * of a boss's spoils. Kept with the Overworld and handed over on login (or as soon as there is room).
 */
public class PendingReturns extends SavedData {

    private static final String NAME = "supernaturalcraft_pending_returns";
    private final Map<UUID, List<ItemStack>> owed = new HashMap<>();
    /** Advancements (mod paths, e.g. {@code main/yellow_eyed}) earned while away: a boss fallen with them in its arena. */
    private final Map<UUID, Set<String>> credits = new HashMap<>();

    public static PendingReturns get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PendingReturns::new, PendingReturns::load), NAME);
    }

    private static PendingReturns load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingReturns data = new PendingReturns();
        for (Tag t : tag.getList("Owed", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            UUID who = UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, e.get("Who")).result().orElse(null);
            if (who == null) continue;
            List<ItemStack> items = new ArrayList<>();
            for (Tag i : e.getList("Items", Tag.TAG_COMPOUND)) ItemStack.parse(registries, i).ifPresent(items::add);
            if (!items.isEmpty()) data.owed.put(who, items);
            Set<String> c = new LinkedHashSet<>();
            for (Tag i : e.getList("Credits", Tag.TAG_STRING)) c.add(i.getAsString());
            if (!c.isEmpty()) data.credits.put(who, c);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        Set<UUID> everyone = new LinkedHashSet<>(owed.keySet());
        everyone.addAll(credits.keySet());
        for (UUID who : everyone) {
            CompoundTag e = new CompoundTag();
            e.put("Who", UUIDUtil.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, who).getOrThrow());
            ListTag l = new ListTag();
            for (ItemStack s : owed.getOrDefault(who, List.of())) if (!s.isEmpty()) l.add(s.save(registries));
            e.put("Items", l);
            ListTag c = new ListTag();
            for (String id : credits.getOrDefault(who, Set.of())) c.add(net.minecraft.nbt.StringTag.valueOf(id));
            e.put("Credits", c);
            list.add(e);
        }
        tag.put("Owed", list);
        return tag;
    }

    public void add(UUID who, List<ItemStack> items) {
        List<ItemStack> l = owed.computeIfAbsent(who, k -> new ArrayList<>());
        for (ItemStack s : items) if (!s.isEmpty()) l.add(s.copy());
        if (l.isEmpty()) owed.remove(who);
        setDirty();
    }

    /** What {@code who} is owed, taken off the books. */
    public List<ItemStack> take(UUID who) {
        List<ItemStack> l = owed.remove(who);
        if (l == null) return List.of();
        setDirty();
        return l;
    }

    public void credit(UUID who, String advancement) {
        if (credits.computeIfAbsent(who, k -> new LinkedHashSet<>()).add(advancement)) setDirty();
    }

    /** The advancements earned while away, taken off the books. */
    public Set<String> takeCredits(UUID who) {
        Set<String> c = credits.remove(who);
        if (c == null) return Set.of();
        setDirty();
        return c;
    }

    public boolean owes(UUID who) {
        return owed.containsKey(who);
    }

    public List<ItemStack> peek(UUID who) {
        return List.copyOf(owed.getOrDefault(who, List.of()));
    }
}
