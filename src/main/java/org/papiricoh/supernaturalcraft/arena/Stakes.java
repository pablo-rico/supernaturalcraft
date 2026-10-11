package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Progression keys are never lost (v0.19). A ritual that summons hands the keys it spent to the arena it opens ({@link #offer} →
 * {@link ArenaSavedData#create}); an arena that closes without a victory gives them back to their owner, straight into the
 * inventory or, if they are away or out of room, through {@link PendingReturns}. Loose keys on the ground never despawn.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class Stakes {

    /** The stake of the summoning running right now (server thread; set and cleared around one ritual's effect). */
    private static @Nullable Stake pending;

    private Stakes() {
    }

    public static boolean isKey(ItemStack stack) {
        return stack.is(AllTags.Items.PROGRESSION_KEYS);
    }

    /** The progression keys among {@code spent}, as one stake for {@code owner} (null if there are none). */
    public static @Nullable Stake of(UUID owner, List<ItemStack> spent) {
        List<ItemStack> keys = new ArrayList<>();
        for (ItemStack s : spent) if (!s.isEmpty() && isKey(s)) keys.add(s.copy());
        return keys.isEmpty() ? null : new Stake(owner, keys, false);
    }

    /** Puts a stake up for the next arena opened on this thread (a summoning ritual's effect). */
    public static void offer(@Nullable Stake stake) {
        pending = stake;
    }

    /** The stake waiting for an arena, taken (null if none). */
    public static @Nullable Stake claim() {
        Stake s = pending;
        pending = null;
        return s;
    }

    /** Hands back an arena's stake once it closes, unless it was won (and the stake was not only lent). */
    public static void settle(MinecraftServer server, ArenaController arena, boolean victory, boolean stopping) {
        Stake s = arena.takeStake();
        if (s == null || s.isEmpty() || (victory && !s.always())) return;
        give(server, s.owner(), s.items(), stopping);
    }

    /**
     * Gives {@code items} to {@code who}: into their inventory if they are here (and the server is not stopping), the rest
     * owed until they are back or have room.
     */
    public static void give(MinecraftServer server, UUID who, List<ItemStack> items, boolean stopping) {
        if (items.isEmpty()) return;
        ServerPlayer p = stopping ? null : server.getPlayerList().getPlayer(who);
        if (p == null) {
            PendingReturns.get(server).add(who, items);
            return;
        }
        List<ItemStack> left = new ArrayList<>();
        List<ItemStack> given = new ArrayList<>();
        for (ItemStack s : items) {
            ItemStack copy = s.copy();
            int before = copy.getCount();
            p.getInventory().add(copy);
            if (copy.getCount() < before) given.add(s.copyWithCount(before - copy.getCount()));
            if (!copy.isEmpty()) left.add(copy);
        }
        if (!given.isEmpty()) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.stake.returned", list(given)).withStyle(ChatFormatting.GOLD), false);
            p.level().playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        if (!left.isEmpty()) {
            PendingReturns.get(server).add(who, left);
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.stake.no_room", list(left)).withStyle(ChatFormatting.GOLD), false);
        }
    }

    private static Component list(List<ItemStack> items) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out.append(", ");
            ItemStack s = items.get(i);
            out.append(s.getCount() > 1 ? Component.literal(s.getCount() + "× ").append(s.getHoverName()) : s.getHoverName());
        }
        return out;
    }

    /** Hands over whatever is owed to {@code p}. */
    public static void deliver(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        PendingReturns data = PendingReturns.get(server);
        if (!data.owes(p.getUUID())) return;
        give(server, p.getUUID(), data.take(p.getUUID()), false);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        deliver(p);
        for (String adv : PendingReturns.get(p.getServer()).takeCredits(p.getUUID())) {
            org.papiricoh.supernaturalcraft.reward.ChorusRewards.award(p, adv);
        }
    }

    /** Someone who had no room gets the rest once they have made some (checked every ten seconds). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 200 == 0 && p.getInventory().getFreeSlot() >= 0) deliver(p);
    }

    /** A progression key lying on the ground waits for its owner: it never despawns. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item && isKey(item.getItem())) item.setUnlimitedLifetime();
    }
}
