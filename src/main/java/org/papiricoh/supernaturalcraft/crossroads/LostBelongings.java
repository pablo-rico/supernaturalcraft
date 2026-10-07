package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each player dropped at their last death, and how much of it nobody has picked up since:
 * the crossroads demon can bring back what was lost (lava, the void, despawning), never what is
 * already back in someone's hands.
 *
 * <p>Each dropped {@link ItemEntity} is tagged ({@link #TAG}: owner, death stamp, index) so a
 * pickup can be credited to its entry. Once the demon returns a death's belongings, any of its
 * item entities still lying about are discarded (now, or when their chunk loads), so nothing is
 * had twice.
 */
public class LostBelongings extends SavedData {

    public static final String TAG = "supernaturalcraft:lost";
    private static final String NAME = "supernaturalcraft_lost_belongings";

    /** One death: its stamp and the count still unclaimed of each stack dropped. */
    private static final class Death {
        final long stamp;
        final List<ItemStack> stacks;

        Death(long stamp, List<ItemStack> stacks) {
            this.stamp = stamp;
            this.stacks = stacks;
        }
    }

    private final Map<UUID, Death> deaths = new HashMap<>();

    public static LostBelongings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(LostBelongings::new, LostBelongings::load, null), NAME);
    }

    private static LostBelongings load(CompoundTag tag, HolderLookup.Provider registries) {
        LostBelongings d = new LostBelongings();
        ListTag list = tag.getList("Deaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            List<ItemStack> stacks = new ArrayList<>();
            ListTag items = t.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                CompoundTag it = items.getCompound(j);
                int left = it.getInt("Left");
                ItemStack s = left > 0 ? ItemStack.parseOptional(registries, it.getCompound("Stack")) : ItemStack.EMPTY;
                stacks.add(s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(left));
            }
            d.deaths.put(t.getUUID("Owner"), new Death(t.getLong("Stamp"), stacks));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        deaths.forEach((owner, death) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("Owner", owner);
            t.putLong("Stamp", death.stamp);
            ListTag items = new ListTag();
            for (ItemStack s : death.stacks) {
                CompoundTag it = new CompoundTag();
                // A fully claimed entry is kept as an empty placeholder (indices must not shift).
                if (!s.isEmpty()) it.put("Stack", s.copyWithCount(1).saveOptional(registries));
                it.putInt("Left", s.isEmpty() ? 0 : s.getCount());
                items.add(it);
            }
            t.put("Items", items);
            list.add(t);
        });
        tag.put("Deaths", list);
        return tag;
    }

    /** A player died and dropped {@code drops}: they become this player's lost belongings. */
    public void record(UUID owner, long stamp, Collection<ItemEntity> drops) {
        List<ItemStack> stacks = new ArrayList<>();
        int i = 0;
        for (ItemEntity e : drops) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Owner", owner);
            t.putLong("Stamp", stamp);
            t.putInt("Index", i++);
            e.getPersistentData().put(TAG, t);
            stacks.add(e.getItem().copy());
        }
        deaths.put(owner, new Death(stamp, stacks));
        setDirty();
    }

    /** Someone picked up {@code amount} from a (possibly tagged) item entity. */
    public void picked(ItemEntity e, ItemStack what, int amount) {
        if (amount <= 0 || !e.getPersistentData().contains(TAG)) return;
        CompoundTag t = e.getPersistentData().getCompound(TAG);
        Death death = deaths.get(t.getUUID("Owner"));
        if (death == null || death.stamp != t.getLong("Stamp")) return;
        int index = t.getInt("Index");
        // Item entities merge on the ground: credit the tagged entry first, then its twins.
        if (index >= 0 && index < death.stacks.size()) amount = take(death.stacks.get(index), what, amount);
        for (int k = 0; k < death.stacks.size() && amount > 0; k++) amount = take(death.stacks.get(k), what, amount);
        setDirty();
    }

    private static int take(ItemStack entry, ItemStack what, int amount) {
        if (entry.isEmpty() || !ItemStack.isSameItemSameComponents(entry, what)) return amount;
        int n = Math.min(amount, entry.getCount());
        entry.shrink(n);
        return amount - n;
    }

    /** Copies of what is still unclaimed from {@code owner}'s last death. */
    public List<ItemStack> unclaimed(UUID owner) {
        Death death = deaths.get(owner);
        if (death == null) return List.of();
        return death.stacks.stream().filter(s -> !s.isEmpty()).map(ItemStack::copy).toList();
    }

    public boolean hasUnclaimed(UUID owner) {
        Death death = deaths.get(owner);
        return death != null && death.stacks.stream().anyMatch(s -> !s.isEmpty());
    }

    /**
     * Hands back everything unclaimed and closes the death: its item entities still in loaded
     * chunks vanish now, the others when they load.
     */
    public List<ItemStack> claimAll(MinecraftServer server, UUID owner) {
        List<ItemStack> out = unclaimed(owner);
        Death death = deaths.get(owner);
        if (death == null) return out;
        death.stacks.replaceAll(s -> ItemStack.EMPTY);
        setDirty();
        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity e : level.getEntities(EntityType.ITEM, this::isClosed)) e.discard();
        }
        return out;
    }

    /** Whether this item entity belongs to a death whose belongings were already handed back. */
    public boolean isClosed(ItemEntity e) {
        @Nullable CompoundTag t = e.getPersistentData().contains(TAG) ? e.getPersistentData().getCompound(TAG) : null;
        if (t == null) return false;
        Death death = deaths.get(t.getUUID("Owner"));
        if (death == null || death.stamp != t.getLong("Stamp")) return false;
        return death.stacks.stream().allMatch(ItemStack::isEmpty);
    }
}
