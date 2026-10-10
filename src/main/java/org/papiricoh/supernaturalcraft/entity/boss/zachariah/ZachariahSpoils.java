package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What Zachariah leaves, per hunter who fought him (v0.18): the first victory gives his Blade, Heaven's Seal and his bust; a
 * rematch the bust and, half the time, the Blade or the Seal. A hunter's first victory is the one before they hold his
 * advancement ({@link #ADVANCEMENT}, granted by the kill itself just after the spoils fall). The Ascension shard and the heart of
 * Vitality come on their own (ShardSpoils, DefenceEvents).
 * <p>His fall also gives the office's owner their home in Heaven ({@link #unlockHomes}).
 */
public final class ZachariahSpoils {

    public static final String ADVANCEMENT = "main/out_of_office";
    /** A rematch's chance of the Blade or the Seal. */
    public static final float REMATCH_RELIC_CHANCE = 0.5f;

    private ZachariahSpoils() {
    }

    /** Everyone's share at {@code at}; with no hunter to credit, one rematch's share. */
    public static void drop(ServerLevel level, List<ServerPlayer> hunters, Vec3 at, RandomSource random) {
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch(random)) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter, random)) spawn(level, at, s);
    }

    /** {@code hunter}'s share: everything the first time, a rematch's after. */
    public static List<ItemStack> share(ServerPlayer hunter, RandomSource random) {
        return beatenBefore(hunter) ? rematch(random) : first();
    }

    /** Whether {@code hunter} has beaten him before (holds his advancement). */
    public static boolean beatenBefore(ServerPlayer hunter) {
        var server = hunter.getServer();
        if (server == null) return false;
        AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(ADVANCEMENT));
        return adv != null && hunter.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** The first victory: the Blade, the Seal and the bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.ZACHARIAHS_BLADE.get()));
        out.add(new ItemStack(AllItems.HEAVENS_SEAL.get()));
        out.add(new ItemStack(AllItems.ZACHARIAH_TROPHY.get()));
        return out;
    }

    /** A rematch: the bust, and half the time the Blade or the Seal. */
    public static List<ItemStack> rematch(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.ZACHARIAH_TROPHY.get()));
        if (random.nextFloat() < REMATCH_RELIC_CHANCE) {
            out.add(new ItemStack(random.nextBoolean() ? AllItems.ZACHARIAHS_BLADE.get() : AllItems.HEAVENS_SEAL.get()));
        }
        return out;
    }

    /**
     * His fall unlocks a home in Heaven: the office's owner's (a fight in a hunter's own Heaven) or, for an egg or a command, every
     * hunter who fought him. Each gets {@code homeUnlocked} and one more victory in their {@link HeavenStanding}.
     * @return who was credited
     */
    public static List<ServerPlayer> unlockHomes(ServerLevel level, @Nullable UUID owner, List<ServerPlayer> hunters) {
        List<ServerPlayer> credited = new ArrayList<>();
        if (owner != null) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
            if (p == null) p = hunters.stream().filter(h -> h.getUUID().equals(owner)).findFirst().orElse(null);
            if (p != null) credited.add(p);
            else org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots.onZachariahDefeated(owner); // offline: kept for their return
        } else {
            credited.addAll(hunters);
        }
        for (ServerPlayer p : credited) unlockHome(p);
        return credited;
    }

    /** One hunter's home is theirs, and the victory is written down. */
    public static void unlockHome(ServerPlayer p) {
        // The world's API: the standing (synced), and the plot's home door opens.
        org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots.onZachariahDefeated(p);
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
