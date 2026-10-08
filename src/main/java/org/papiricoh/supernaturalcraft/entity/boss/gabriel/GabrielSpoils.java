package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.trickster.TricksterLedger;

import java.util.ArrayList;
import java.util.List;

/**
 * What Gabriel leaves, per hunter who fought him (v0.14), kept in each hunter's {@link TricksterLedger}: the first victory
 * gives everything (the Trickster's Remote, Gabriel's Blade, his trophy and a handful of Trickster Candy); a rematch gives the
 * trophy, the candy and, half the time, the remote or the blade. The advancement {@code changing_channels} comes from the
 * kill itself (the shared boss credit).
 */
public final class GabrielSpoils {

    /** Candy per share: between these, both included. */
    public static final int CANDY_MIN = 4, CANDY_MAX = 6;
    /** A rematch's chance of the remote or the blade. */
    public static final float REMATCH_PRIZE_CHANCE = 0.5f;

    private GabrielSpoils() {
    }

    /** Everyone's share at {@code at}; with no hunter to credit, one rematch's share. */
    public static void drop(ServerLevel level, List<ServerPlayer> hunters, Vec3 at, RandomSource random) {
        if (hunters.isEmpty()) {
            for (ItemStack s : rematch(random)) spawn(level, at, s);
            return;
        }
        for (ServerPlayer hunter : hunters) for (ItemStack s : share(hunter, random)) spawn(level, at, s);
    }

    /** {@code hunter}'s share, their victory written down. */
    public static List<ItemStack> share(ServerPlayer hunter, RandomSource random) {
        TricksterLedger ledger = hunter.getData(AllAttachments.TRICKSTER);
        hunter.setData(AllAttachments.TRICKSTER, ledger.won());
        return ledger.victories() == 0 ? first(random) : rematch(random);
    }

    /** The first victory: everything. */
    public static List<ItemStack> first(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.TRICKSTER_REMOTE.get()));
        out.add(new ItemStack(AllItems.GABRIEL_BLADE.get()));
        out.add(new ItemStack(AllItems.GABRIEL_TROPHY.get()));
        out.add(candy(random));
        return out;
    }

    /** A rematch: the trophy, the candy, and half the time the remote or the blade. */
    public static List<ItemStack> rematch(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.GABRIEL_TROPHY.get()));
        out.add(candy(random));
        if (random.nextFloat() < REMATCH_PRIZE_CHANCE) {
            out.add(new ItemStack(random.nextBoolean() ? AllItems.TRICKSTER_REMOTE.get() : AllItems.GABRIEL_BLADE.get()));
        }
        return out;
    }

    private static ItemStack candy(RandomSource random) {
        return new ItemStack(AllItems.TRICKSTER_CANDY.get(), CANDY_MIN + random.nextInt(CANDY_MAX - CANDY_MIN + 1));
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
