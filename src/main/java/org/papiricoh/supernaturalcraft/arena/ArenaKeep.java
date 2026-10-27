package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SNConfig;

/**
 * Dying in a boss fight costs nothing you carry: a challenger who falls inside an active arena keeps their pack, their
 * experience and their Curios, as under keepInventory. The pack is set aside before anything drops, so a grave mod finds
 * nothing to bury, and it comes back at respawn. The stash lives in the player's persistent data, so it survives a logout
 * (or a restart) on the death screen.
 */
public final class ArenaKeep {

    static final String KEY = "supernaturalcraft:arena_kept";

    private ArenaKeep() {
    }

    /** Whether {@code player} is dying as a challenger of an active fight, where the gamerule would not already keep their pack. */
    public static boolean inFight(ServerPlayer player) {
        if (!SNConfig.KEEP_INVENTORY.get() || player.isSpectator()
                || player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return false;
        }
        for (ArenaController arena : ArenaSavedData.get(player.serverLevel()).all()) {
            if (arena.isActive() && arena.isParticipant(player)
                    && arena.horizontalDistance(player.position()) <= arena.radius() + ArenaController.EDGE_MARGIN) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code player}'s pack is set aside (they died in a fight and have not respawned yet). */
    public static boolean kept(Player player) {
        return player.getPersistentData().contains(KEY);
    }

    /** At death: sets the pack and the experience aside and empties the inventory. */
    public static boolean stash(ServerPlayer player) {
        if (kept(player) || !inFight(player)) return false;
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", player.getInventory().save(new ListTag()));
        tag.putInt("Level", player.experienceLevel);
        tag.putFloat("Progress", player.experienceProgress);
        tag.putInt("Total", player.totalExperience);
        player.getPersistentData().put(KEY, tag);
        player.getInventory().clearContent();
        return true;
    }

    /** Hands the stash of {@code from} to {@code to} (the respawned player, or the same one if the death was undone). */
    public static boolean restore(Player from, ServerPlayer to) {
        if (!kept(from)) return false;
        CompoundTag tag = from.getPersistentData().getCompound(KEY);
        from.getPersistentData().remove(KEY);
        to.getPersistentData().remove(KEY);
        to.getInventory().load(tag.getList("Inventory", Tag.TAG_COMPOUND));
        to.experienceProgress = tag.getFloat("Progress");
        to.totalExperience = tag.getInt("Total");
        to.setExperienceLevels(tag.getInt("Level"));
        return true;
    }

    // --- events ---------------------------------------------------------------------------

    /**
     * Once no listener has undone the death (a First Blade's mark looks at the hands), and before the drops: grave mods take
     * the inventory from the drops or from {@code dropEquipment}, which come after.
     */
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) stash(player);
    }

    /** After that, also when cancelled: a death undone later on (another mod's totem) gives the pack straight back. */
    public static void onDeathSettled(LivingDeathEvent event) {
        if (event.isCanceled() && event.getEntity() instanceof ServerPlayer player) restore(player, player);
    }

    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Player player && kept(player)) event.setDroppedExperience(0);
    }

    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player && restore(event.getOriginal(), player)) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.arena.kept").withStyle(ChatFormatting.GOLD), false);
        }
    }
}
