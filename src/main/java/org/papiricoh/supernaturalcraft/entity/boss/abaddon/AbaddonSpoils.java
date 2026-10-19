package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What Abaddon leaves (v0.21). The <b>Book of the Damned</b> goes to every hunter who fought her, straight into the inventory
 * (or owed if they are away: {@link BossShares}), every time — a lost Book is won again by fighting her again. Per hunter: the
 * first victory (before they hold {@link #ADVANCEMENT}) the Knights' Cleaver and her bust; a rematch the bust. The Ascension
 * shard and the heart of Vitality come on their own.
 */
public final class AbaddonSpoils {

    public static final String ADVANCEMENT = "main/king_of_the_damned";
    /** Granted on reading the Book (impossible: by code). */
    public static final String BOOK_ADVANCEMENT = "main/book_of_the_damned";

    private AbaddonSpoils() {
    }

    public static void drop(ServerLevel level, AbaddonEntity boss, List<ServerPlayer> hunters, Vec3 at) {
        ItemStack book = new ItemStack(AllItems.BOOK_OF_THE_DAMNED.get());
        boolean shared = BossShares.share(level, boss, book);
        if (!shared) {
            // Nobody known in her arena (a command, a test): each hunter we know of, or one on the ground.
            if (hunters.isEmpty()) spawn(level, at, book.copy());
            for (ServerPlayer p : hunters) Stakes.give(level.getServer(), p.getUUID(), List.of(book.copy()), false);
        }
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch()) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter)) spawn(level, at, s);
    }

    public static List<ItemStack> share(ServerPlayer hunter) {
        return AuthorWorld.done(hunter, ADVANCEMENT) ? rematch() : first();
    }

    /** The first victory: the Knights' Cleaver and her bust. */
    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.KNIGHTS_CLEAVER.get()));
        out.add(new ItemStack(AllItems.ABADDON_TROPHY.get()));
        return out;
    }

    /** A rematch: her bust. */
    public static List<ItemStack> rematch() {
        return new ArrayList<>(List.of(new ItemStack(AllItems.ABADDON_TROPHY.get())));
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
