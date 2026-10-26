package org.papiricoh.supernaturalcraft.entity.boss.dick;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.ArrayList;
import java.util.List;

/**
 * What Dick Roman leaves each hunter who fought him (v0.25), on top of the shards, hearts and credit every boss gives: the first
 * time, a shard of the Leviathan Tablet (it teaches Leviathan's Bane) and Roman Enterprises' own borax sprayer; a rematch, a
 * few boxes of borax and, now and then, another sprayer. Dropped at each hunter's feet.
 */
public final class DickSpoils {

    public static final float REMATCH_SPRAYER_CHANCE = 0.35f;
    public static final int REMATCH_BORAX = 6;

    private DickSpoils() {
    }

    public static void give(ServerLevel level, List<ServerPlayer> hunters, Vec3 at, RandomSource random) {
        for (ServerPlayer p : hunters) for (ItemStack s : share(p, random)) spawn(level, p.position(), s);
    }

    public static List<ItemStack> share(ServerPlayer hunter, RandomSource random) {
        return beatenBefore(hunter) ? rematch(random) : first();
    }

    public static boolean beatenBefore(ServerPlayer hunter) {
        var server = hunter.getServer();
        if (server == null) return false;
        AdvancementHolder adv = server.getAdvancements().get(SupernaturalCraft.asResource(BossProgression.Boss.DICK_ROMAN.advancement));
        return adv != null && hunter.getAdvancements().getOrStartProgress(adv).isDone();
    }

    public static List<ItemStack> first() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.LEVIATHAN_TABLET.get()));
        out.add(new ItemStack(AllItems.BORAX_SPRAYER.get()));
        return out;
    }

    public static List<ItemStack> rematch(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(AllItems.BORAX.get(), REMATCH_BORAX));
        if (random.nextFloat() < REMATCH_SPRAYER_CHANCE) out.add(new ItemStack(AllItems.BORAX_SPRAYER.get()));
        return out;
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y + 0.5, at.z, stack);
        item.setNoPickUpDelay();
        level.addFreshEntity(item);
    }
}
