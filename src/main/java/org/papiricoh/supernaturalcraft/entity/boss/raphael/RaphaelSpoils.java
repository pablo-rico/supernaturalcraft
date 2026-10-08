package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What Raphael leaves, per hunter who fought him (v0.16): the first victory gives his Stormcaller and his bust; a rematch the
 * bust and, half the time, another Stormcaller. A hunter's first victory is the one before they hold his advancement
 * ({@link #ADVANCEMENT}, granted by the kill itself just after the spoils fall). The Ascension shard and the heart of Vitality
 * come on their own (ShardSpoils, DefenceEvents).
 */
public final class RaphaelSpoils {

    public static final String ADVANCEMENT = "main/free_to_be_you_and_me";
    /** A rematch's chance of another Stormcaller. */
    public static final float REMATCH_STAFF_CHANCE = 0.5f;

    private RaphaelSpoils() {
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

    /** The first victory: the Stormcaller and the bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.RAPHAELS_STORMCALLER.get()));
        out.add(new ItemStack(AllItems.RAPHAEL_TROPHY.get()));
        return out;
    }

    /** A rematch: the bust, and half the time the Stormcaller. */
    public static List<ItemStack> rematch(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.RAPHAEL_TROPHY.get()));
        if (random.nextFloat() < REMATCH_STAFF_CHANCE) out.add(new ItemStack(AllItems.RAPHAELS_STORMCALLER.get()));
        return out;
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
