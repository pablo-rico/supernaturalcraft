package org.papiricoh.supernaturalcraft.legacy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerWorld;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

/**
 * The Men of Letters as an order (v0.17): joining, rising through the ranks (advancement, the rank's gear, a title card) and
 * whether a hunter wears a piece of the order's gear. Research (agent B) calls {@link #checkRank} after each finished topic.
 */
public final class LegacyOrder {

    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    private LegacyOrder() {
    }

    /**
     * Henry's offer accepted: an Aspirant, with the Bunker Key and a map to the bunker.
     *
     * @return false if they were already a member (nothing given twice)
     */
    public static boolean join(ServerPlayer player) {
        Legacy before = Legacies.get(player);
        Legacies.set(player, before.withRank(Math.max(1, before.rank())).withHenry(Legacy.HENRY_JOINED, -1));
        if (before.member()) return false;
        ChorusRewards.award(player, LegacyRules.advancement(1));
        give(player, new ItemStack(AllItems.BUNKER_KEY.get()));
        give(player, BunkerWorld.map(player.serverLevel()));
        rankFx(player, 1);
        return true;
    }

    /**
     * Raises a member to the rank their archive has earned ({@link LegacyRules#earned}): each new rank gives its advancement,
     * its gear and a title card. Ranks never go down.
     *
     * @return the rank now
     */
    public static int checkRank(ServerPlayer player) {
        Legacy legacy = Legacies.get(player);
        if (!legacy.member()) return 0;
        Archive archive = Legacies.archive(player);
        int earned = LegacyRules.earned(legacy.rank(), archive.totalFinished(), archive.kindsFinished());
        if (earned <= legacy.rank()) return legacy.rank();
        Legacies.set(player, legacy.withRank(earned));
        for (int r = legacy.rank() + 1; r <= earned; r++) promoted(player, r);
        return earned;
    }

    /** What reaching rank {@code r} gives (also used by {@code /supernatural legacy rank}). */
    public static void promoted(ServerPlayer player, int r) {
        ChorusRewards.award(player, LegacyRules.advancement(r));
        Item gear = gear(r);
        if (gear != null && !owns(player, gear)) give(player, new ItemStack(gear));
        rankFx(player, r);
    }

    /** The piece of gear a rank brings, or null. */
    public static @Nullable Item gear(int rank) {
        return switch (rank) {
            case 2 -> AllItems.MEN_OF_LETTERS_RING.get();
            case 3 -> AllItems.SPELLWRIGHTS_SPECTACLES.get();
            case 4 -> AllItems.HENRYS_CASE.get();
            case 5 -> AllItems.AQUARIAN_STAR.get();
            default -> null;
        };
    }

    /**
     * Whether {@code player} wears (or, for the ring, the star and the case, carries) a piece of the order's gear: anywhere in
     * the inventory, either hand, an armour slot or a Curios slot. The spectacles count only on the head (or in a Curios slot).
     */
    public static boolean wears(Player player, Item item) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(item)) return true;
        if (CURIOS && CuriosCompat.isWearing(player, item)) return true;
        if (item == AllItems.SPELLWRIGHTS_SPECTACLES.get()) return false;
        return player.getInventory().contains(s -> s.is(item));
    }

    private static boolean owns(Player player, Item item) {
        return player.getInventory().contains(s -> s.is(item)) || CURIOS && CuriosCompat.isWearing(player, item);
    }

    private static void rankFx(ServerPlayer player, int rank) {
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.LEGACY_RANK_UP.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.rank_up",
                Component.translatable("legacy.supernaturalcraft.rank." + LegacyRules.title(rank))).withStyle(ChatFormatting.GOLD), false);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, new LegacyFxPayload(LegacyFxPayload.RANK_UP, player.getId(), rank, ""));
    }

    static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }
}
