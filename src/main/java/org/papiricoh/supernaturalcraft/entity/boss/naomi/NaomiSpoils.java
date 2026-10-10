package org.papiricoh.supernaturalcraft.entity.boss.naomi;

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
 * What Naomi leaves, per hunter who fought her (v0.18): the first victory gives her drill, her diadem and her bust; a rematch the
 * bust and, half the time, the drill or the diadem. A hunter's first victory is the one before they hold her advancement
 * ({@link #ADVANCEMENT}, granted by the kill itself just after the spoils fall). The Ascension shard and the heart of Vitality
 * come on their own (ShardSpoils, DefenceEvents).
 */
public final class NaomiSpoils {

    public static final String ADVANCEMENT = "main/deprogrammed";
    /** A rematch's chance of the drill or the diadem (either, evenly). */
    public static final float REMATCH_RELIC_CHANCE = 0.5f;

    private NaomiSpoils() {
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

    /** Whether {@code hunter} has beaten her before (holds her advancement). */
    public static boolean beatenBefore(ServerPlayer hunter) {
        var server = hunter.getServer();
        if (server == null) return false;
        AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(ADVANCEMENT));
        return adv != null && hunter.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** The first victory: the drill, the diadem and the bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.NAOMIS_DRILL.get()));
        out.add(new ItemStack(AllItems.NAOMIS_DIADEM.get()));
        out.add(new ItemStack(AllItems.NAOMI_TROPHY.get()));
        return out;
    }

    /** A rematch: the bust, and half the time the drill or the diadem. */
    public static List<ItemStack> rematch(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.NAOMI_TROPHY.get()));
        if (random.nextFloat() < REMATCH_RELIC_CHANCE) {
            out.add(new ItemStack(random.nextBoolean() ? AllItems.NAOMIS_DRILL.get() : AllItems.NAOMIS_DIADEM.get()));
        }
        return out;
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
